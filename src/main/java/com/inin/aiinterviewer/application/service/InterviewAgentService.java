package com.inin.aiinterviewer.application.service;

import com.inin.aiinterviewer.agent.graph.InterviewGraph;
import com.inin.aiinterviewer.agent.model.ClaimExtractionResult;
import com.inin.aiinterviewer.agent.model.ConsistencyCheckResult;
import com.inin.aiinterviewer.agent.model.EvidenceCollectionResult;
import com.inin.aiinterviewer.agent.model.InterviewTurnInput;
import com.inin.aiinterviewer.agent.model.InterviewTurnPlan;
import com.inin.aiinterviewer.agent.model.LogicChainResult;
import com.inin.aiinterviewer.agent.model.ProbePlan;
import com.inin.aiinterviewer.agent.model.QuestionQualityContext;
import com.inin.aiinterviewer.agent.model.ScenarioDirectionResult;
import com.inin.aiinterviewer.agent.node.QuestionQualityGateNode;
import com.inin.aiinterviewer.agent.prompt.AgentPrompts;
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
import com.inin.aiinterviewer.domain.model.InterviewCoverage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
public class InterviewAgentService {

    private static final Logger log = LoggerFactory.getLogger(InterviewAgentService.class);

    /**
     * 常见无效/过短回答。对这类回答跳过 claims / logic / evidence / consistency 四次 AI 调用，
     * 直接走 plan + question render，显著降低 DeepSeek 等模型的单轮延迟。
     */
    private static final List<String> TRIVIAL_ANSWER_PATTERNS = List.of(
            "不会", "不知道", "不了解", "不清楚", "没做过", "没有经验", "没有做过", "没有相关",
            "不记得", "忘记了", "忘了", "跳过", "pass", "略", "不會", "dontknow", "idk",
            // 常见低信息量回答：无实质内容可供打分，走快速通道跳过四步逐轮评分，缩短面试官响应时间
            "不太懂", "不太会", "不太清楚", "不太了解", "没接触", "没接触过", "没怎么用过",
            "记不清", "记不得", "没印象", "不太记得", "换一题", "换个问题", "下一题",
            "不知道怎么说", "没什么可说", "说不上来", "noidea", "notsure", "skip", "nocomment"
    );

    private final InterviewSessionService sessionService;
    private final InterviewGraph interviewGraph;
    private final ReportGenerationTaskService reportTaskService;
    private final ClaimLedgerService claimLedgerService;
    private final EvidenceLedgerService evidenceLedgerService;
    private final ConsistencyIssueService consistencyIssueService;
    private final DeferredProbeService deferredProbeService;
    private final ScenarioEngine scenarioEngine;
    private final ScenarioSchedulingService scenarioSchedulingService;
    private final CollaborationEvidenceCollector collaborationEvidenceCollector;
    private final QuestionQualityGateNode questionQualityGate;
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
            ScenarioEngine scenarioEngine,
            ScenarioSchedulingService scenarioSchedulingService,
            CollaborationEvidenceCollector collaborationEvidenceCollector,
            QuestionQualityGateNode questionQualityGate,
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
        this.scenarioEngine = scenarioEngine;
        this.scenarioSchedulingService = scenarioSchedulingService;
        this.collaborationEvidenceCollector = collaborationEvidenceCollector;
        this.questionQualityGate = questionQualityGate;
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
                    evidenceLedgerService.compactSummary(userId, sessionId))
                    .withCoverage(coverage(userId, sessionId, InterviewCoverage.empty()));
            String prompt = interviewGraph.initialQuestionPrompt(input);
            return streamAndPersist(
                    userId, sessionId, session.stage(), prompt, null, List.of(), null, null, null,
                    qualityContext(input, session.stage(), null));
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
                    evidenceLedgerService.compactSummary(userId, sessionId))
                    .withCoverage(coverage(userId, sessionId, answeredState.coverage()));

            boolean trivialAnswer = isTrivialAnswer(answeredState.latestAnswer());
            if (trivialAnswer) {
                log.debug("Fast path for trivial answer in session {}: {}",
                        sessionId, answeredState.latestAnswer());
            }

            var extraction = trivialAnswer
                    ? ClaimExtractionResult.degraded("trivial_answer")
                    : interviewGraph.extractClaims(turnInput);
            var claimLedger = claimLedgerService.recordLatestAnswer(userId, sessionId, extraction);
            sessionService.updateClaimLedger(userId, sessionId, claimLedger);
            turnInput = turnInput.withClaimContext(
                    extraction, claimLedgerService.compactSummary(userId, sessionId));

            var logicChain = trivialAnswer
                    ? LogicChainResult.degraded("trivial_answer")
                    : interviewGraph.evaluateLogic(turnInput);
            sessionService.updateLogicChain(userId, sessionId, logicChain);
            turnInput = turnInput.withLogicChainResult(logicChain);

            var evidenceResult = trivialAnswer
                    ? EvidenceCollectionResult.degraded("trivial_answer")
                    : collaborationEvidenceCollector.enrich(
                            answeredState.latestAnswer(), interviewGraph.collectEvidence(turnInput));
            var evidenceLedger = evidenceLedgerService.recordLatestAnswer(userId, sessionId, evidenceResult);
            sessionService.updateEvidenceLedger(userId, sessionId, evidenceLedger);
            turnInput = turnInput.withEvidenceContext(
                    evidenceResult, evidenceLedgerService.compactSummary(userId, sessionId));

            var consistencyContext = consistencyIssueService.prepareContext(userId, sessionId);
            turnInput = turnInput.withConsistencyContext(
                    consistencyContext, null, claimLedgerService.compactSummary(userId, sessionId));
            var consistencyResult = trivialAnswer
                    ? ConsistencyCheckResult.degraded("trivial_answer")
                    : interviewGraph.checkConsistency(turnInput);
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
            var activeScenario = scenarioEngine.findActive(userId, sessionId).orElse(null);
            if (askedQuestions >= session.planSnapshot().questionCount()) {
                if (activeScenario != null) {
                    var aborted = scenarioEngine.abort(
                            userId, sessionId, activeScenario.id(), "面试达到题目上限");
                    sessionService.updateScenarioState(userId, sessionId, aborted);
                }
                reportTaskService.enqueue(userId, sessionId);
                return Flux.empty();
            }
            if (activeScenario == null && !scenarioEngine.hasScenario(userId, sessionId)) {
                var scheduled = sessionService.domainPackSnapshot(userId, sessionId)
                        .flatMap(snapshot -> scenarioSchedulingService.select(
                                sessionId, session.planSnapshot(), snapshot,
                                session.stage(), askedQuestions));
                if (scheduled.isPresent()) {
                    try {
                        activeScenario = scenarioEngine.start(userId, sessionId, scheduled.get());
                        sessionService.updateScenarioState(userId, sessionId, activeScenario);
                    } catch (RuntimeException exception) {
                        log.warn("Cannot start scheduled scenario for session {}; continuing regular interview",
                                sessionId, exception);
                    }
                }
            }
            turnInput = turnInput.withActiveScenario(activeScenario);

            InterviewTurnPlan turn = interviewGraph.plan(turnInput);
            sessionService.updateCoverageAndStrategy(
                    userId, sessionId, turn.coverage(), turn.strategy());
            sessionService.updateProbePlan(userId, sessionId, turn.probePlan());
            sessionService.updatePressureState(userId, sessionId, turn.pressureState());

            ScenarioDirectionResult scenarioDirection = turn.scenarioDirectionResult();
            if (activeScenario != null && scenarioDirection.degraded()) {
                try {
                    var failed = scenarioEngine.failActive(
                            userId, sessionId, "场景导演失败，已返回普通面试流程");
                    sessionService.updateScenarioState(userId, sessionId, failed);
                } catch (RuntimeException exception) {
                    log.warn("Cannot close degraded scenario {} for session {}",
                            activeScenario.id(), sessionId, exception);
                }
                scenarioDirection = null;
            }

            if (turn.stage() != session.stage()) {
                sessionService.transitionStage(userId, sessionId, turn.stage());
                // 模型主动以 COMPLETED 结束面试时（任意阶段提前收尾），
                // 同样触发报告生成，避免"已结束但无报告"的半成品状态。
                if (turn.stage() == InterviewStage.COMPLETED) {
                    reportTaskService.enqueue(userId, sessionId);
                }
            }
            return streamAndPersist(
                    userId, sessionId, turn.stage(), turn.questionPrompt(), turn.analysis(),
                    retrieval.citations(), turn.probePlan(),
                    activeScenario == null ? null : activeScenario.id(), scenarioDirection,
                    qualityContext(turnInput, turn.stage(), turn.probePlan()));
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private Flux<String> streamAndPersist(
            long userId,
            long sessionId,
            InterviewStage stage,
            String prompt,
            AnswerAnalysis analysis,
            List<KnowledgeCitationDto> citations,
            ProbePlan probePlan,
            String scenarioId,
            ScenarioDirectionResult scenarioDirection,
            QuestionQualityContext qualityContext
    ) {
        StringBuilder generated = new StringBuilder();
        AtomicBoolean persistenceAttempted = new AtomicBoolean(false);
        AtomicBoolean qualityFallbackUsed = new AtomicBoolean(false);
        AtomicReference<String> finalQuestionHolder = new AtomicReference<>();
        return reviewedQuestion(prompt, qualityContext)
                .filter(chunk -> chunk != null && !chunk.isEmpty())
                .switchIfEmpty(Flux.error(new AIException(
                        ErrorCode.AI_CALL_FAILED, new IllegalStateException("AI returned an empty stream"))))
                .doOnNext(generated::append)
                .doOnComplete(() -> {
                    String draft = generated.toString();
                    if (draft.isBlank()) {
                        throw new AIException(
                                ErrorCode.AI_CALL_FAILED, new IllegalStateException("AI returned blank content"));
                    }
                    persistenceAttempted.set(true);
                    String finalQuestion = finalizeQuestion(prompt, draft, qualityContext, qualityFallbackUsed);
                    finalQuestionHolder.set(finalQuestion);
                    sessionService.saveAssistantOutput(
                            userId, sessionId, finalQuestion, analysis, false,
                            qualityFallbackUsed.get() ? List.of() : citations);
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
                    if (scenarioId != null && scenarioDirection != null
                            && scenarioDirection.requiresScenarioPrompt()) {
                        try {
                            var scenario = scenarioDirection.kickoff()
                                    ? scenarioEngine.markIntroduced(userId, sessionId, scenarioId)
                                    : scenarioEngine.advance(
                                            userId, sessionId, scenarioId, scenarioDirection.toCommand());
                            sessionService.updateScenarioState(userId, sessionId, scenario);
                        } catch (RuntimeException scenarioFailure) {
                            log.warn("Cannot advance scenario {} for session {}; returning to regular interview",
                                    scenarioId, sessionId, scenarioFailure);
                            try {
                                var failed = scenarioEngine.failActive(
                                        userId, sessionId, "场景状态保存失败，已返回普通面试流程");
                                sessionService.updateScenarioState(userId, sessionId, failed);
                            } catch (RuntimeException closeFailure) {
                                log.error("Cannot close failed scenario {} for session {}",
                                        scenarioId, sessionId, closeFailure);
                            }
                        }
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
                        String partialDraft = generated.toString();
                        String finalQuestion = finalizeQuestion(prompt, partialDraft, qualityContext, qualityFallbackUsed);
                        sessionService.saveAssistantOutput(
                                userId, sessionId, finalQuestion, analysis, true,
                                qualityFallbackUsed.get() ? List.of() : citations);
                        eventPublisher.publishEvent(
                                new InterviewTurnCompletedEvent(userId, sessionId, stage, true));
                    } catch (RuntimeException persistenceFailure) {
                        log.error("Cannot preserve partial AI output for session {}", sessionId, persistenceFailure);
                    }
                });
    }

    /**
     * 直接流式输出原始问题，质量审查在输出完成后在后台进行。
     */
    private Flux<String> reviewedQuestion(String prompt, QuestionQualityContext context) {
        return streamDraft(prompt)
                .onErrorResume(QuestionRenderingException.class, failure -> {
                    Flux<String> partial = Flux.fromIterable(failure.partialChunks());
                    return failure.partialChunks().isEmpty()
                            ? Flux.error(failure.getCause())
                            : partial.concatWith(Flux.error(failure.getCause()));
                });
    }

    private Flux<String> streamDraft(String prompt) {
        return interviewGraph.questionRenderer().stream(prompt)
                .filter(chunk -> chunk != null && !chunk.isEmpty())
                .onErrorMap(exception -> exception instanceof QuestionRenderingException
                        ? exception : new QuestionRenderingException(exception, List.of()));
    }

    /**
     * 对已经流式输出的问题进行后台质量审查。如果未通过则重试，必要时使用兜底问题。
     */
    private String finalizeQuestion(
            String originalPrompt,
            String draft,
            QuestionQualityContext context,
            AtomicBoolean fallbackUsed
    ) {
        try {
            var firstReview = questionQualityGate.review(context, draft);
            if (firstReview.approved()) return draft;
            String retryPrompt = AgentPrompts.regenerateQuestion(
                    originalPrompt, draft, firstReview.issues());
            String retry = collectDraft(retryPrompt);
            if (retry.isBlank()) {
                log.warn("Retry question returned blank for stage {}", context.stage());
                fallbackUsed.set(true);
                return questionQualityGate.fallback(context);
            }
            var secondReview = questionQualityGate.review(context, retry);
            if (secondReview.approved()) return retry;
            log.warn("Question quality gate rejected both drafts for stage {}: first={}, second={}",
                    context.stage(), firstReview.issues(), secondReview.issues());
            fallbackUsed.set(true);
            return questionQualityGate.fallback(context);
        } catch (RuntimeException exception) {
            log.warn("Question finalization failed for stage {}", context.stage(), exception);
            fallbackUsed.set(true);
            return questionQualityGate.fallback(context);
        }
    }

    private String collectDraft(String prompt) {
        try {
            RenderedQuestion rendered = renderDraft(prompt).block(Duration.ofMinutes(2));
            return rendered == null ? "" : rendered.text();
        } catch (RuntimeException exception) {
            log.warn("Failed to render retry draft", exception);
            return "";
        }
    }

    private reactor.core.publisher.Mono<RenderedQuestion> renderDraft(String prompt) {
        List<String> chunks = new ArrayList<>();
        return interviewGraph.questionRenderer().stream(prompt)
                .filter(chunk -> chunk != null && !chunk.isEmpty())
                .doOnNext(chunks::add)
                .collectList()
                .flatMap(ignored -> {
                    String text = String.join("", chunks);
                    if (text.isBlank()) {
                        return reactor.core.publisher.Mono.error(new AIException(
                                ErrorCode.AI_CALL_FAILED,
                                new IllegalStateException("AI returned an empty question stream")));
                    }
                    return reactor.core.publisher.Mono.just(
                            new RenderedQuestion(List.copyOf(chunks), text));
                })
                .onErrorMap(exception -> exception instanceof QuestionRenderingException
                        ? exception : new QuestionRenderingException(exception, List.copyOf(chunks)));
    }

    private QuestionQualityContext qualityContext(
            InterviewTurnInput input,
            InterviewStage stage,
            ProbePlan probePlan
    ) {
        return new QuestionQualityContext(
                stage, input.plan(), probePlan, input.pressureState(), input.activeScenario(),
                input.messages(), input.candidateProfileContext(), input.domainPackContext());
    }

    private record RenderedQuestion(List<String> chunks, String text) {
    }

    private static final class QuestionRenderingException extends RuntimeException {
        private final List<String> partialChunks;

        private QuestionRenderingException(Throwable cause, List<String> partialChunks) {
            super(cause);
            this.partialChunks = partialChunks;
        }

        private List<String> partialChunks() {
            return partialChunks;
        }
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
                .filter(snapshot -> snapshot.content() != null)
                .map(snapshot -> {
                    var pack = snapshot.content();
                    return Map.of(
                            "id", snapshot.id(),
                            "version", snapshot.version(),
                            "displayName", pack.displayName(),
                            "competencies", pack.competencies(),
                            "metrics", pack.metrics(),
                            "failurePatterns", pack.failurePatterns(),
                            "probePlaybooks", pack.probePlaybooks(),
                            "rubrics", pack.rubrics()).toString();
                })
                .orElse("未关联领域知识包");
    }

    private InterviewCoverage coverage(
            long userId,
            long sessionId,
            InterviewCoverage current
    ) {
        return sessionService.domainPackSnapshot(userId, sessionId)
                .map(snapshot -> current.ensureDomainPack(snapshot.content()))
                .orElse(current);
    }

    private boolean isTrivialAnswer(String answer) {
        if (answer == null || answer.isBlank()) return true;
        String normalized = answer.strip()
                .replaceAll("[\\s，。！？；：、,.!?;:\\-_'\"“”‘’（）()]+", "");
        if (normalized.isEmpty()) return true;
        if (normalized.length() < 6) return true;
        String lower = normalized.toLowerCase(java.util.Locale.ROOT);
        return TRIVIAL_ANSWER_PATTERNS.stream().anyMatch(lower::contains);
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
