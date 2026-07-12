package com.inin.aiinterviewer.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inin.aiinterviewer.agent.stage.StageManager;
import com.inin.aiinterviewer.agent.state.InterviewState;
import com.inin.aiinterviewer.agent.state.StateSerializer;
import com.inin.aiinterviewer.application.dto.InterviewMessageDto;
import com.inin.aiinterviewer.application.dto.InterviewPlanDto;
import com.inin.aiinterviewer.application.dto.InterviewSessionDto;
import com.inin.aiinterviewer.application.exception.BusinessException;
import com.inin.aiinterviewer.application.exception.ErrorCode;
import com.inin.aiinterviewer.application.exception.SystemException;
import com.inin.aiinterviewer.domain.entity.AgentCheckpointEntity;
import com.inin.aiinterviewer.domain.entity.InterviewMessageEntity;
import com.inin.aiinterviewer.domain.entity.InterviewSessionEntity;
import com.inin.aiinterviewer.domain.enums.InterviewStage;
import com.inin.aiinterviewer.domain.enums.InterviewStatus;
import com.inin.aiinterviewer.domain.model.Message;
import com.inin.aiinterviewer.domain.model.AnswerAnalysis;
import com.inin.aiinterviewer.infrastructure.database.mapper.AgentCheckpointMapper;
import com.inin.aiinterviewer.infrastructure.database.mapper.InterviewMessageMapper;
import com.inin.aiinterviewer.infrastructure.database.mapper.InterviewSessionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class InterviewSessionService {

    private static final Logger log = LoggerFactory.getLogger(InterviewSessionService.class);
    private static final String PROMPT_VERSION = "v1.0";

    private final InterviewPlanService planService;
    private final InterviewSessionMapper sessionMapper;
    private final InterviewMessageMapper messageMapper;
    private final AgentCheckpointMapper checkpointMapper;
    private final StateSerializer stateSerializer;
    private final StageManager stageManager;
    private final ObjectMapper objectMapper;

    public InterviewSessionService(
            InterviewPlanService planService,
            InterviewSessionMapper sessionMapper,
            InterviewMessageMapper messageMapper,
            AgentCheckpointMapper checkpointMapper,
            StateSerializer stateSerializer,
            StageManager stageManager,
            ObjectMapper objectMapper
    ) {
        this.planService = planService;
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
        this.checkpointMapper = checkpointMapper;
        this.stateSerializer = stateSerializer;
        this.stageManager = stageManager;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public InterviewSessionDto startOrResume(long userId, long planId) {
        Optional<InterviewSessionEntity> active = sessionMapper.findResumableByPlan(userId, planId);
        if (active.isPresent()) {
            InterviewSessionEntity session = active.get();
            if (session.getStatus() != InterviewStatus.RUNNING) {
                resumeInternal(userId, session);
            }
            return require(userId, session.getId());
        }
        return create(userId, planId);
    }

    @Transactional
    public InterviewSessionDto create(long userId, long planId) {
        InterviewPlanDto plan = planService.require(planId, userId);
        InterviewStage initialStage = initialStage(plan.stages());

        InterviewSessionEntity entity = new InterviewSessionEntity();
        entity.setUserId(userId);
        entity.setPlanId(plan.id());
        entity.setResumeId(plan.resumeId());
        entity.setTitle(plan.name());
        entity.setJobTitle(plan.jobTitle());
        entity.setPlanSnapshotJson(writeJson(plan));
        entity.setStage(initialStage);
        entity.setStatus(InterviewStatus.RUNNING);
        entity.setPromptVersion(PROMPT_VERSION);
        sessionMapper.insert(entity);

        InterviewState state = new InterviewState(
                InterviewState.CURRENT_VERSION, entity.getId(), userId, initialStage,
                List.of(), "", "", null, null, null, plan.rules(), "");
        saveCheckpointInternal(userId, entity.getId(), "session_started", state);
        return require(userId, entity.getId());
    }

    @Transactional(readOnly = true)
    public InterviewSessionDto require(long userId, long sessionId) {
        return toDto(requireEntity(userId, sessionId));
    }

    @Transactional(readOnly = true)
    public Optional<InterviewSessionDto> findResumable(long userId, long planId) {
        return sessionMapper.findResumableByPlan(userId, planId).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public List<InterviewSessionDto> list(long userId) {
        return sessionMapper.findAllByUserId(userId).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<InterviewMessageDto> messages(long userId, long sessionId) {
        requireEntity(userId, sessionId);
        return messageMapper.findAll(userId, sessionId).stream().map(this::toMessageDto).toList();
    }

    @Transactional
    public InterviewState appendUserAnswer(long userId, long sessionId, String answer) {
        InterviewSessionEntity session = requireEntity(userId, sessionId);
        if (session.getStatus() != InterviewStatus.RUNNING || answer == null || answer.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_STATE);
        }

        InterviewMessageEntity message = new InterviewMessageEntity();
        message.setUserId(userId);
        message.setSessionId(sessionId);
        message.setSequenceNo(messageMapper.nextSequence(userId, sessionId));
        message.setRole(Message.Role.USER);
        message.setContent(answer.strip());
        message.setMetadataJson("{}");
        messageMapper.insert(message);

        List<Message> messages = domainMessages(userId, sessionId);
        InterviewState previous = loadLatestStateInternal(userId, sessionId)
                .orElseGet(() -> baseState(session));
        InterviewState updated = new InterviewState(
                previous.stateVersion(), sessionId, userId, previous.stage(), messages,
                previous.currentQuestion(), answer.strip(), previous.analysis(), previous.evaluation(),
                previous.profile(), previous.rules(), previous.summary());
        saveCheckpointInternal(userId, sessionId, "user_answer_saved", updated);
        return updated;
    }

    @Transactional
    public InterviewState saveAssistantOutput(
            long userId,
            long sessionId,
            String question,
            AnswerAnalysis analysis,
            boolean partial
    ) {
        InterviewSessionEntity session = requireEntity(userId, sessionId);
        if (session.getStatus() != InterviewStatus.RUNNING || question == null || question.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_STATE);
        }

        InterviewMessageEntity message = new InterviewMessageEntity();
        message.setUserId(userId);
        message.setSessionId(sessionId);
        message.setSequenceNo(messageMapper.nextSequence(userId, sessionId));
        message.setRole(Message.Role.ASSISTANT);
        message.setContent(question.strip());
        message.setMetadataJson(partial ? "{\"partial\":true}" : "{}");
        messageMapper.insert(message);

        InterviewState previous = loadLatestStateInternal(userId, sessionId)
                .orElseGet(() -> baseState(session));
        List<Message> allMessages = domainMessages(userId, sessionId);
        String summary = allMessages.size() > 10 ? compactSummary(allMessages) : previous.summary();
        InterviewState updated = new InterviewState(
                previous.stateVersion(), sessionId, userId, session.getStage(),
                allMessages, question.strip(), previous.latestAnswer(),
                analysis == null ? previous.analysis() : analysis, previous.evaluation(),
                previous.profile(), previous.rules(), summary);
        saveCheckpointInternal(userId, sessionId,
                partial ? "question_stream_interrupted" : "agent_turn_completed", updated);
        return updated;
    }

    @Transactional
    public InterviewSessionDto pause(long userId, long sessionId) {
        InterviewSessionEntity session = requireEntity(userId, sessionId);
        if (session.getStatus() != InterviewStatus.RUNNING) {
            throw new BusinessException(ErrorCode.INVALID_STATE);
        }
        InterviewState state = loadLatestStateInternal(userId, sessionId)
                .orElseGet(() -> baseState(session));
        sessionMapper.updateStatus(sessionId, userId, InterviewStatus.PAUSED);
        saveCheckpointInternal(userId, sessionId, "session_paused", state);
        return require(userId, sessionId);
    }

    @Transactional
    public InterviewSessionDto resume(long userId, long sessionId) {
        InterviewSessionEntity session = requireEntity(userId, sessionId);
        resumeInternal(userId, session);
        return require(userId, sessionId);
    }

    @Transactional
    public InterviewState transitionStage(long userId, long sessionId, InterviewStage nextStage) {
        InterviewSessionEntity session = requireEntity(userId, sessionId);
        if (session.getStatus() != InterviewStatus.RUNNING) {
            throw new BusinessException(ErrorCode.INVALID_STATE);
        }
        InterviewStage stage = stageManager.transition(session.getStage(), nextStage);
        sessionMapper.updateStage(sessionId, userId, stage);
        InterviewState previous = loadLatestStateInternal(userId, sessionId)
                .orElseGet(() -> baseState(session));
        InterviewState updated = new InterviewState(
                previous.stateVersion(), sessionId, userId, stage, previous.messages(),
                previous.currentQuestion(), previous.latestAnswer(), previous.analysis(), previous.evaluation(),
                previous.profile(), previous.rules(), previous.summary());
        saveCheckpointInternal(userId, sessionId, "stage_" + stage.name().toLowerCase(), updated);
        return updated;
    }

    @Transactional
    public void saveCheckpoint(long userId, long sessionId, String nodeName, InterviewState state) {
        requireEntity(userId, sessionId);
        validateStateIdentity(userId, sessionId, state);
        saveCheckpointInternal(userId, sessionId, nodeName, state);
    }

    @Transactional(readOnly = true)
    public Optional<InterviewState> loadLatestState(long userId, long sessionId) {
        requireEntity(userId, sessionId);
        return loadLatestStateInternal(userId, sessionId);
    }

    private void resumeInternal(long userId, InterviewSessionEntity session) {
        if (session.getStatus() != InterviewStatus.PAUSED && session.getStatus() != InterviewStatus.CREATED) {
            throw new BusinessException(ErrorCode.INVALID_STATE);
        }
        InterviewState state = loadLatestStateInternal(userId, session.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CHECKPOINT_NOT_FOUND));
        sessionMapper.updateStatus(session.getId(), userId, InterviewStatus.RUNNING);
        saveCheckpointInternal(userId, session.getId(), "session_resumed", state);
    }

    private void saveCheckpointInternal(long userId, long sessionId, String nodeName, InterviewState state) {
        validateStateIdentity(userId, sessionId, state);
        if (nodeName == null || nodeName.isBlank() || nodeName.length() > 128) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        AgentCheckpointEntity checkpoint = new AgentCheckpointEntity();
        checkpoint.setUserId(userId);
        checkpoint.setSessionId(sessionId);
        checkpoint.setNodeName(nodeName.strip());
        checkpoint.setStateJson(stateSerializer.serialize(state));
        checkpoint.setStateVersion(state.stateVersion());
        checkpointMapper.insert(checkpoint);
    }

    private Optional<InterviewState> loadLatestStateInternal(long userId, long sessionId) {
        for (AgentCheckpointEntity checkpoint : checkpointMapper.findLatestFirst(userId, sessionId)) {
            try {
                InterviewState state = stateSerializer.deserialize(checkpoint.getStateJson());
                validateStateIdentity(userId, sessionId, state);
                return Optional.of(state);
            } catch (RuntimeException exception) {
                log.warn("Ignoring invalid checkpoint {} for session {}", checkpoint.getId(), sessionId);
            }
        }
        return Optional.empty();
    }

    private InterviewState baseState(InterviewSessionEntity session) {
        InterviewPlanDto snapshot = readPlan(session.getPlanSnapshotJson());
        return new InterviewState(
                InterviewState.CURRENT_VERSION, session.getId(), session.getUserId(), session.getStage(),
                domainMessages(session.getUserId(), session.getId()), "", "", null, null, null,
                snapshot.rules() == null ? Map.of() : snapshot.rules(), "");
    }

    private void validateStateIdentity(long userId, long sessionId, InterviewState state) {
        if (state == null || state.userId() != userId || state.sessionId() != sessionId) {
            throw new BusinessException(ErrorCode.INVALID_STATE);
        }
    }

    private InterviewSessionEntity requireEntity(long userId, long sessionId) {
        return sessionMapper.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERVIEW_NOT_FOUND));
    }

    private InterviewSessionDto toDto(InterviewSessionEntity entity) {
        return new InterviewSessionDto(
                entity.getId(), entity.getPlanId(), entity.getResumeId(), entity.getTitle(), entity.getJobTitle(),
                readPlan(entity.getPlanSnapshotJson()), entity.getStage(), entity.getStatus(),
                entity.getPromptVersion(), entity.getStartedTime(), entity.getCompletedTime(),
                entity.getCreateTime(), entity.getUpdateTime());
    }

    private InterviewMessageDto toMessageDto(InterviewMessageEntity entity) {
        return new InterviewMessageDto(entity.getSequenceNo(), entity.getRole(), entity.getContent(), entity.getCreateTime());
    }

    private List<Message> domainMessages(long userId, long sessionId) {
        return messageMapper.findAll(userId, sessionId).stream()
                .map(entity -> new Message(entity.getRole(), entity.getContent(), entity.getCreateTime()))
                .toList();
    }

    private String compactSummary(List<Message> messages) {
        int keepRecent = 8;
        int end = Math.max(0, messages.size() - keepRecent);
        StringBuilder summary = new StringBuilder();
        for (int index = 0; index < end; index++) {
            Message message = messages.get(index);
            String content = message.content().replaceAll("\\s+", " ").strip();
            if (content.length() > 160) content = content.substring(0, 160) + "…";
            if (summary.length() + content.length() > 2000) break;
            summary.append(message.role() == Message.Role.USER ? "候选人：" : "面试官：")
                    .append(content).append("\n");
        }
        return summary.toString().strip();
    }

    private InterviewStage initialStage(List<String> configuredStages) {
        if (configuredStages != null) {
            for (String value : configuredStages) {
                try {
                    return InterviewStage.valueOf(value);
                } catch (IllegalArgumentException ignored) {
                    // Skip outdated or unknown stage names in saved configuration.
                }
            }
        }
        return InterviewStage.INTRODUCTION;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new SystemException(ErrorCode.SYSTEM_ERROR, exception);
        }
    }

    private InterviewPlanDto readPlan(String json) {
        try {
            return objectMapper.readValue(json, InterviewPlanDto.class);
        } catch (JsonProcessingException exception) {
            throw new SystemException(ErrorCode.SYSTEM_ERROR, exception);
        }
    }
}
