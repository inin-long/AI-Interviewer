package com.inin.aiinterviewer.application.service;

import com.inin.aiinterviewer.agent.graph.InterviewGraph;
import com.inin.aiinterviewer.agent.model.InterviewTurnInput;
import com.inin.aiinterviewer.agent.model.InterviewTurnPlan;
import com.inin.aiinterviewer.agent.tool.ToolInput;
import com.inin.aiinterviewer.agent.tool.ToolRegistry;
import com.inin.aiinterviewer.application.dto.InterviewMessageDto;
import com.inin.aiinterviewer.application.dto.InterviewSessionDto;
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

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Map;

@Service
public class InterviewAgentService {

    private static final Logger log = LoggerFactory.getLogger(InterviewAgentService.class);

    private final InterviewSessionService sessionService;
    private final InterviewGraph interviewGraph;
    private final InterviewCompletionService completionService;
    private final ToolRegistry toolRegistry;
    private final ApplicationEventPublisher eventPublisher;

    public InterviewAgentService(
            InterviewSessionService sessionService,
            InterviewGraph interviewGraph,
            InterviewCompletionService completionService,
            ToolRegistry toolRegistry,
            ApplicationEventPublisher eventPublisher
    ) {
        this.sessionService = sessionService;
        this.interviewGraph = interviewGraph;
        this.completionService = completionService;
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
                    retrieveCandidateProfile(userId, sessionId));
            String prompt = interviewGraph.initialQuestionPrompt(input);
            return streamAndPersist(userId, sessionId, session.stage(), prompt, null);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Flux<String> answer(long userId, long sessionId, String answer) {
        return Flux.defer(() -> {
            InterviewSessionDto session = requireRunning(userId, sessionId);
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
            if (askedQuestions >= session.planSnapshot().questionCount()) {
                completionService.complete(userId, sessionId);
                return Flux.empty();
            }
            List<Message> messages = domainMessages(persistedMessages);
            String retrievedContext = retrieveKnowledge(
                    userId, sessionId, answeredState.currentQuestion() + "\n" + answeredState.latestAnswer());
            InterviewTurnPlan turn = interviewGraph.plan(new InterviewTurnInput(
                    session.stage(), answeredState.currentQuestion(), answeredState.latestAnswer(),
                    session.planSnapshot(), messages, answeredState.summary(), retrievedContext,
                    retrieveCandidateProfile(userId, sessionId)));

            if (turn.stage() != session.stage()) {
                sessionService.transitionStage(userId, sessionId, turn.stage());
            }
            return streamAndPersist(
                    userId, sessionId, turn.stage(), turn.questionPrompt(), turn.analysis());
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private Flux<String> streamAndPersist(
            long userId,
            long sessionId,
            InterviewStage stage,
            String prompt,
            AnswerAnalysis analysis
    ) {
        StringBuilder generated = new StringBuilder();
        AtomicBoolean persistenceAttempted = new AtomicBoolean(false);
        return interviewGraph.questionGenerator().stream(prompt)
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
                            userId, sessionId, generated.toString(), analysis, false);
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
                                userId, sessionId, generated.toString(), analysis, true);
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

    private String retrieveKnowledge(long userId, long sessionId, String query) {
        return toolRegistry.find("knowledge_search")
                .map(tool -> tool.execute(new ToolInput(userId, sessionId, Map.of("query", query, "limit", 3))))
                .filter(result -> result.success())
                .map(result -> String.valueOf(result.data().getOrDefault("results", "")))
                .orElse("");
    }

    private String retrieveCandidateProfile(long userId, long sessionId) {
        return toolRegistry.find("candidate_profile_get")
                .map(tool -> tool.execute(new ToolInput(userId, sessionId, Map.of())))
                .filter(result -> result.success())
                .map(result -> result.data().toString())
                .orElse("未关联已确认候选人画像");
    }
}
