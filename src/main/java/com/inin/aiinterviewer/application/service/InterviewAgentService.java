package com.inin.aiinterviewer.application.service;

import com.inin.aiinterviewer.agent.graph.InterviewGraph;
import com.inin.aiinterviewer.agent.model.InterviewTurnInput;
import com.inin.aiinterviewer.agent.model.InterviewTurnPlan;
import com.inin.aiinterviewer.agent.model.ProbePlan;
import com.inin.aiinterviewer.agent.tool.ToolInput;
import com.inin.aiinterviewer.agent.tool.ToolRegistry;
import com.inin.aiinterviewer.application.dto.InterviewMessageDto;
import com.inin.aiinterviewer.application.dto.InterviewSessionDto;
import com.inin.aiinterviewer.application.dto.KnowledgeCitationDto;
import com.inin.aiinterviewer.application.event.InterviewTurnCompletedEvent;
import com.inin.aiinterviewer.application.exception.AIException;
import com.inin.aiinterviewer.application.exception.BusinessException;
import com.inin.aiinterviewer.application.exception.ErrorCode;
import com.inin.aiinterviewer.domain.enums.InterviewStatus;
import com.inin.aiinterviewer.domain.enums.InterviewStage;
import com.inin.aiinterviewer.domain.model.AnswerAnalysis;
import com.inin.aiinterviewer.domain.model.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
public class InterviewAgentService {

    private static final Logger log = LoggerFactory.getLogger(InterviewAgentService.class);

    private final InterviewSessionService sessionService;
    private final InterviewGraph interviewGraph;
    private final ReportGenerationTaskService reportTaskService;
    private final ClaimLedgerService claimLedgerService;
    private final EvidenceLedgerService evidenceLedgerService;
    private final ConsistencyIssueService consistencyIssueService;
    private final DeferredProbeService deferredProbeService;
    private final ToolRegistry toolRegistry;
    private final ApplicationEventPublisher eventPublisher;

    public InterviewAgentService(
            InterviewSessionService sessionService,
            InterviewGraph interviewGraph,
            ReportGenerationTaskService reportTaskService,
            ClaimLedgerService claimLedgerService,
            EvidenceLedgerService evidenceLedgerService,
            ConsistencyIssueService consistencyIssueService,
            DeferredProbeService deferredProbeService,
            ToolRegistry toolRegistry,
            ApplicationEventPublisher eventPublisher
    ) {
        this.sessionService = sessionService;
        this.interviewGraph = interviewGraph;
        this.reportTaskService = reportTaskService;
        this.claimLedgerService = claimLedgerService;
        this.evidenceLedgerService = evidenceLedgerService;
        this.consistencyIssueService = consistencyIssueService;
        this.deferredProbeService = deferredProbeService;
        this.toolRegistry = toolRegistry;
        this.eventPublisher = eventPublisher;
    }

    public Flux<String> generateInitialQuestion(long userId, long sessionId) {
        return Flux.defer(() -> {
            InterviewSessionDto session = requireRunning(userId, sessionId);
            List<InterviewMessageDto> messages = sessionService.messages(userId, sessionId);
            if (!messages.isEmpty()) {
                return Flux.error(new BusinessException(ErrorCode.INVALID_STATE));
            }
            InterviewTurnInput input = new InterviewTurnInput(
                    session.stage(), "", "", session.planSnapshot(), List.of(), "", "",
                    retrieveCandidateProfile(userId, sessionId), domainPackContext(userId, sessionId),
                    claimLedgerService.compactSummary(userId, sessionId),
                    evidenceLedgerService.compactSummary(userId, sessionId));
            String prompt = interviewGraph.initialQuestionPrompt(input);
            return streamAndPersist(userId, sessionId, session.stage(), prompt, null, List.of(), null);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Flux<String> answer(long userId, long sessionId, String answer) {
        return Flux.defer(() -> {
            InterviewSessionDto session = requireRunning(userId, sessionId);
            if (reportTaskService.state(userId, sessionId).completion().finalAnswerSaved()) {
                return Flux.error(new BusinessException(ErrorCode.REPORT_RETRY_REQUIRED));
            }
            var previous = sessionService.loadLatestState(userId, sessionId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.CHECKPOINT_NOT_FOUND));
            if (previous.currentQuestion() == null || previous.currentQuestion().isBlank()) {
                return Flux.error(new BusinessException(ErrorCode.INVALID_STATE));
            }

            var answeredState = sessionService.appendUserAnswer(userId, sessionId, answer);
            List<InterviewMessageDto> persistedMessages = sessionService.messages(userId, sessionId);
            long askedQuestions = persistedMessages.stream()
                    .filter(message -> message.role() == Message.Role.ASSISTANT)
                    .count();
            List<Message> messages = domainMessages(persistedMessages);
            KnowledgeRetrieval retrieval = retrieveKnowledge(
                    userId, sessionId, answeredState.currentQuestion() + "\n" + answeredState.latestAnswer());
            InterviewTurnInput turnInput = new InterviewTurnInput(
                    session.stage(), answeredState.currentQuestion(), answeredState.latestAnswer(),
                    session.planSnapshot(), messages, answeredState.summary(), retrieval.context(),
                    retrieveCandidateProfile(userId, sessionId), domainPackContext(userId, sessionId),
                    claimLedgerService.compactSummary(userId, sessionId),
                    evidenceLedgerService.compactSummary(userId, sessionId));

            var extraction = interviewGraph.extractClaims(turnInput);
            var claimLedger = claimLedgerService.recordLatestAnswer(userId, sessionId, extraction);
            sessionService.updateClaimLedger(userId, sessionId, claimLedger);
            turnInput = turnInput.withClaimContext(
                    extraction, claimLedgerService.compactSummary(userId, sessionId));
            var logicChain = interviewGraph.evaluateLogic(turnInput);
            sessionService.updateLogicChain(userId, sessionId, logicChain);
            turnInput = turnInput.withLogicChainResult(logicChain);
            var evidenceResult = interviewGraph.collectEvidence(turnInput);
            var evidenceLedger = evidenceLedgerService.recordLatestAnswer(userId, sessionId, evidenceResult);
            sessionService.updateEvidenceLedger(userId, sessionId, evidenceLedger);
            turnInput = turnInput.withEvidenceContext(
                    evidenceResult, evidenceLedgerService.compactSummary(userId, sessionId));
            var consistencyContext = consistencyIssueService.prepareContext(userId, sessionId);
            turnInput = turnInput.withConsistencyContext(
                    consistencyContext, null, claimLedgerService.compactSummary(userId, sessionId));
            var consistencyResult = interviewGraph.checkConsistency(turnInput);
            var appliedConsistency = consistencyIssueService.apply(userId, sessionId, consistencyResult);
            sessionService.updateClaimLedger(userId, sessionId, appliedConsistency.ledger());
            turnInput = turnInput.withConsistencyContext(
                    consistencyContext, appliedConsistency.result(),
                    claimLedgerService.compactSummary(userId, sessionId));
            var deferredProbes = deferredProbeService.scheduleLatestAnswer(
                    userId, sessionId, session.planSnapshot(), session.stage(),
                    appliedConsistency.result().degraded());
            sessionService.updateDeferredProbes(userId, sessionId, deferredProbes);
            turnInput = turnInput.withDeferredProbes(deferredProbes);
            turnInput = turnInput.withPressureState(answeredState.pressureState());
            if (askedQuestions >= session.planSnapshot().questionCount()) {
                reportTaskService.enqueue(userId, sessionId);
                return Flux.empty();
            }

            InterviewTurnPlan turn = interviewGraph.plan(turnInput);
            sessionService.updateProbePlan(userId, sessionId, turn.probePlan());
            sessionService.updatePressureState(userId, sessionId, turn.pressureState());

            if (turn.stage() != session.stage()) {
                sessionService.transitionStage(userId, sessionId, turn.stage());
            }
            return streamAndPersist(
                    userId, sessionId, turn.stage(), turn.questionPrompt(), turn.analysis(),
                    retrieval.citations(), turn.probePlan());
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private Flux<String> streamAndPersist(
            long userId,
            long sessionId,
            InterviewStage stage,
            String prompt,
            AnswerAnalysis analysis,
            List<KnowledgeCitationDto> citations,
            ProbePlan probePlan
    ) {
        StringBuilder generated = new StringBuilder();
        AtomicBoolean persistenceAttempted = new AtomicBoolean(false);
        return interviewGraph.questionRenderer().stream(prompt)
                .filter(chunk -> chunk != null && !chunk.isEmpty())
                .switchIfEmpty(Flux.error(new AIException(
                        ErrorCode.AI_CALL_FAILED, new IllegalStateException("AI returned an empty stream"))))
                .doOnNext(generated::append)
                .doOnComplete(() -> {
                    if (generated.toString().isBlank()) {
                        throw new AIException(
                                ErrorCode.AI_CALL_FAILED, new IllegalStateException("AI returned blank content"));
                    }
                    persistenceAttempted.set(true);
                    sessionService.saveAssistantOutput(
                            userId, sessionId, generated.toString(), analysis, false, citations);
                    if (probePlan != null && probePlan.targetsConsistencyIssue()) {
                        var ledger = consistencyIssueService.markClarificationAsked(
                                userId, sessionId, probePlan.targetConsistencyIssueId());
                        sessionService.updateClaimLedger(userId, sessionId, ledger);
                    }
                    if (probePlan != null && probePlan.targetsDeferredProbe()) {
                        var deferredProbes = deferredProbeService.markCompleted(
                                userId, sessionId, probePlan.targetDeferredProbeId());
                        sessionService.updateDeferredProbes(userId, sessionId, deferredProbes);
                    }
                    eventPublisher.publishEvent(
                            new InterviewTurnCompletedEvent(userId, sessionId, stage, false));
                })
                .doOnError(exception -> {
                    if (!persistenceAttempted.compareAndSet(false, true)
                            || generated.toString().isBlank()) {
                        return;
                    }
                    try {
                        sessionService.saveAssistantOutput(
                                userId, sessionId, generated.toString(), analysis, true, citations);
                        eventPublisher.publishEvent(
                                new InterviewTurnCompletedEvent(userId, sessionId, stage, true));
                    } catch (RuntimeException persistenceFailure) {
                        log.error("Cannot preserve partial AI output for session {}", sessionId, persistenceFailure);
                    }
                });
    }

    private InterviewSessionDto requireRunning(long userId, long sessionId) {
        InterviewSessionDto session = sessionService.require(userId, sessionId);
        if (session.status() != InterviewStatus.RUNNING) {
            throw new BusinessException(ErrorCode.INVALID_STATE);
        }
        return session;
    }

    private List<Message> domainMessages(List<InterviewMessageDto> messages) {
        return messages.stream()
                .map(message -> new Message(message.role(), message.content(), message.createTime()))
                .toList();
    }

    private KnowledgeRetrieval retrieveKnowledge(long userId, long sessionId, String query) {
        var result = toolRegistry.find("knowledge_search")
                .map(tool -> tool.execute(new ToolInput(userId, sessionId, Map.of("query", query, "limit", 3))))
                .filter(toolResult -> toolResult.success());
        if (result.isEmpty() || !(result.get().data().get("results") instanceof List<?> items)) {
            return KnowledgeRetrieval.empty();
        }
        List<KnowledgeCitationDto> citations = new ArrayList<>();
        for (Object item : items) {
            if (!(item instanceof Map<?, ?> values)) continue;
            KnowledgeCitationDto citation = toCitation(values);
            if (citation != null) citations.add(citation);
        }
        if (citations.isEmpty()) return KnowledgeRetrieval.empty();
        String context = citations.stream()
                .map(citation -> "[来源：%s，片段 %d]\n%s".formatted(
                        citation.documentName(), citation.chunkIndex() + 1, citation.excerpt()))
                .collect(Collectors.joining("\n\n"));
        return new KnowledgeRetrieval(context, citations);
    }

    private KnowledgeCitationDto toCitation(Map<?, ?> values) {
        if (!(values.get("documentId") instanceof Number documentId)
                || !(values.get("chunkIndex") instanceof Number chunkIndex)
                || !(values.get("score") instanceof Number score)) {
            return null;
        }
        Object documentNameValue = values.get("documentName");
        Object contentValue = values.get("content");
        String documentName = documentNameValue == null ? "未命名文档" : String.valueOf(documentNameValue);
        String content = contentValue == null ? "" : String.valueOf(contentValue);
        if (documentId.longValue() <= 0 || chunkIndex.intValue() < 0 || content.isBlank()) return null;
        return new KnowledgeCitationDto(
                documentId.longValue(), documentName, chunkIndex.intValue(), content, score.doubleValue());
    }

    private String retrieveCandidateProfile(long userId, long sessionId) {
        return toolRegistry.find("candidate_profile_get")
                .map(tool -> tool.execute(new ToolInput(userId, sessionId, Map.of())))
                .filter(result -> result.success())
                .map(result -> result.data().toString())
                .orElse("未关联已确认候选人画像");
    }

    private String domainPackContext(long userId, long sessionId) {
        return sessionService.domainPackSnapshot(userId, sessionId)
                .map(Object::toString)
                .orElse("未关联领域知识包");
    }

    private record KnowledgeRetrieval(String context, List<KnowledgeCitationDto> citations) {
        private KnowledgeRetrieval {
            context = context == null ? "" : context;
            citations = citations == null ? List.of() : List.copyOf(citations);
        }

        private static KnowledgeRetrieval empty() {
            return new KnowledgeRetrieval("", List.of());
        }
    }
}
