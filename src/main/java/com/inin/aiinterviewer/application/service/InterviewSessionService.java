package com.inin.aiinterviewer.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inin.aiinterviewer.agent.stage.StageManager;
import com.inin.aiinterviewer.agent.model.ProbePlan;
import com.inin.aiinterviewer.agent.model.LogicChainResult;
import com.inin.aiinterviewer.agent.state.InterviewState;
import com.inin.aiinterviewer.agent.state.StateSerializer;
import com.inin.aiinterviewer.application.dto.InterviewMessageDto;
import com.inin.aiinterviewer.application.dto.InterviewPlanDto;
import com.inin.aiinterviewer.application.dto.InterviewSessionDto;
import com.inin.aiinterviewer.application.dto.CandidateProfileDto;
import com.inin.aiinterviewer.application.dto.KnowledgeCitationDto;
import com.inin.aiinterviewer.application.dto.KnowledgeDocumentSnapshotDto;
import com.inin.aiinterviewer.application.dto.DomainPackDto;
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
import com.inin.aiinterviewer.domain.model.CandidateProfile;
import com.inin.aiinterviewer.domain.model.ClaimLedger;
import com.inin.aiinterviewer.domain.model.DomainPackSnapshot;
import com.inin.aiinterviewer.domain.model.EvidenceLedger;
import com.inin.aiinterviewer.domain.model.DeferredProbe;
import com.inin.aiinterviewer.domain.model.PressureState;
import com.inin.aiinterviewer.domain.model.InterviewCoverage;
import com.inin.aiinterviewer.domain.model.InterviewStrategy;
import com.inin.aiinterviewer.infrastructure.database.mapper.AgentCheckpointMapper;
import com.inin.aiinterviewer.infrastructure.database.mapper.InterviewMessageMapper;
import com.inin.aiinterviewer.infrastructure.database.mapper.InterviewClaimMapper;
import com.inin.aiinterviewer.infrastructure.database.mapper.EvaluationEvidenceMapper;
import com.inin.aiinterviewer.infrastructure.database.mapper.ConsistencyIssueMapper;
import com.inin.aiinterviewer.infrastructure.database.mapper.DeferredProbeMapper;
import com.inin.aiinterviewer.infrastructure.database.mapper.ScenarioSessionMapper;
import com.inin.aiinterviewer.infrastructure.database.mapper.InterviewResultMapper;
import com.inin.aiinterviewer.infrastructure.database.mapper.InterviewSessionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import com.fasterxml.jackson.core.type.TypeReference;

@Service
public class InterviewSessionService {

    private static final Logger log = LoggerFactory.getLogger(InterviewSessionService.class);
    private static final String PROMPT_VERSION = "v2.0-s1";

    private final InterviewPlanService planService;
    private final CandidateProfileService profileService;
    private final KnowledgeDocumentService knowledgeService;
    private final DomainPackService domainPackService;
    private final InterviewSessionMapper sessionMapper;
    private final InterviewMessageMapper messageMapper;
    private final InterviewClaimMapper claimMapper;
    private final EvaluationEvidenceMapper evidenceMapper;
    private final ConsistencyIssueMapper consistencyIssueMapper;
    private final DeferredProbeMapper deferredProbeMapper;
    private final ScenarioSessionMapper scenarioSessionMapper;
    private final AgentCheckpointMapper checkpointMapper;
    private final InterviewResultMapper resultMapper;
    private final StateSerializer stateSerializer;
    private final StageManager stageManager;
    private final ObjectMapper objectMapper;

    public InterviewSessionService(
            InterviewPlanService planService,
            CandidateProfileService profileService,
            KnowledgeDocumentService knowledgeService,
            DomainPackService domainPackService,
            InterviewSessionMapper sessionMapper,
            InterviewMessageMapper messageMapper,
            InterviewClaimMapper claimMapper,
            EvaluationEvidenceMapper evidenceMapper,
            ConsistencyIssueMapper consistencyIssueMapper,
            DeferredProbeMapper deferredProbeMapper,
            ScenarioSessionMapper scenarioSessionMapper,
            AgentCheckpointMapper checkpointMapper,
            InterviewResultMapper resultMapper,
            StateSerializer stateSerializer,
            StageManager stageManager,
            ObjectMapper objectMapper
    ) {
        this.planService = planService;
        this.profileService = profileService;
        this.knowledgeService = knowledgeService;
        this.domainPackService = domainPackService;
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
        this.claimMapper = claimMapper;
        this.evidenceMapper = evidenceMapper;
        this.consistencyIssueMapper = consistencyIssueMapper;
        this.deferredProbeMapper = deferredProbeMapper;
        this.scenarioSessionMapper = scenarioSessionMapper;
        this.checkpointMapper = checkpointMapper;
        this.resultMapper = resultMapper;
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
        CandidateProfileDto profileSnapshot = plan.profileId() == null
                ? null : profileService.requireConfirmed(userId, plan.profileId());
        List<KnowledgeDocumentSnapshotDto> knowledgeSnapshot = knowledgeService
                .requireReadyAll(userId, plan.knowledgeDocumentIds()).stream()
                .map(KnowledgeDocumentSnapshotDto::from)
                .toList();
        DomainPackSnapshot domainPackSnapshot = domainPackService.snapshot(plan.domainPackId());
        InterviewStage initialStage = initialStage(plan.stages());

        InterviewSessionEntity entity = new InterviewSessionEntity();
        entity.setUserId(userId);
        entity.setPlanId(plan.id());
        entity.setResumeId(plan.resumeId());
        entity.setProfileId(plan.profileId());
        entity.setTitle(plan.name());
        entity.setJobTitle(plan.jobTitle());
        entity.setPlanSnapshotJson(writeJson(plan));
        entity.setProfileSnapshotJson(profileSnapshot == null ? "{}" : writeJson(profileSnapshot));
        entity.setKnowledgeSnapshotJson(writeJson(knowledgeSnapshot));
        entity.setDomainPackId(domainPackSnapshot.id());
        entity.setDomainPackVersion(domainPackSnapshot.version());
        entity.setDomainPackSnapshotJson(writeJson(domainPackSnapshot));
        entity.setStage(initialStage);
        entity.setStatus(InterviewStatus.RUNNING);
        entity.setPromptVersion(PROMPT_VERSION);
        sessionMapper.insert(entity);

        InterviewState state = new InterviewState(
                InterviewState.CURRENT_VERSION, entity.getId(), userId, initialStage,
                List.of(), "", "", null, null, stateProfile(profileSnapshot), plan.rules(), "",
                ClaimLedger.empty(), EvidenceLedger.empty(), LogicChainResult.skippedResult(),
                null, List.of(), PressureState.initial(), null,
                InterviewCoverage.fromDomainPack(domainPackSnapshot.content()),
                InterviewStrategy.empty());
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
    public Optional<CandidateProfileDto> profileSnapshot(long userId, long sessionId) {
        InterviewSessionEntity session = requireEntity(userId, sessionId);
        return Optional.ofNullable(readProfile(session.getProfileSnapshotJson()));
    }

    @Transactional(readOnly = true)
    public List<KnowledgeDocumentSnapshotDto> knowledgeSnapshot(long userId, long sessionId) {
        InterviewSessionEntity session = requireEntity(userId, sessionId);
        return readKnowledgeSnapshot(session.getKnowledgeSnapshotJson());
    }

    @Transactional(readOnly = true)
    public List<Long> knowledgeDocumentIdsSnapshot(long userId, long sessionId) {
        return knowledgeSnapshot(userId, sessionId).stream()
                .map(KnowledgeDocumentSnapshotDto::id)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<DomainPackSnapshot> domainPackSnapshot(long userId, long sessionId) {
        InterviewSessionEntity session = requireEntity(userId, sessionId);
        return Optional.ofNullable(readDomainPackSnapshot(session.getDomainPackSnapshotJson()));
    }

    @Transactional(readOnly = true)
    public List<InterviewMessageDto> messages(long userId, long sessionId) {
        requireEntity(userId, sessionId);
        return messageMapper.findAll(userId, sessionId).stream().map(this::toMessageDto).toList();
    }

    @Transactional(readOnly = true)
    public Map<Long, Integer> messageQuestionNumbers(long userId, long sessionId) {
        requireEntity(userId, sessionId);
        Map<Long, Integer> result = new java.util.LinkedHashMap<>();
        int questionNumber = 0;
        for (InterviewMessageEntity message : messageMapper.findAll(userId, sessionId)) {
            if (message.getRole() == Message.Role.ASSISTANT) questionNumber++;
            if (questionNumber > 0) result.put(message.getId(), questionNumber);
        }
        return Map.copyOf(result);
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
                previous.profile(), previous.rules(), previous.summary(), previous.claimLedger(),
                previous.evidenceLedger(), previous.logicChainResult(), previous.probePlan(),
                previous.deferredProbes(), previous.pressureState(), previous.activeScenario(),
                previous.coverage(), previous.strategy());
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
        return saveAssistantOutput(userId, sessionId, question, analysis, partial, List.of());
    }

    @Transactional
    public InterviewState saveAssistantOutput(
            long userId,
            long sessionId,
            String question,
            AnswerAnalysis analysis,
            boolean partial,
            List<KnowledgeCitationDto> citations
    ) {
        InterviewSessionEntity session = requireEntity(userId, sessionId);
        if (session.getStatus() != InterviewStatus.RUNNING || question == null || question.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_STATE);
        }
        List<KnowledgeCitationDto> normalizedCitations = normalizeCitations(session, citations);

        InterviewMessageEntity message = new InterviewMessageEntity();
        message.setUserId(userId);
        message.setSessionId(sessionId);
        message.setSequenceNo(messageMapper.nextSequence(userId, sessionId));
        message.setRole(Message.Role.ASSISTANT);
        message.setContent(question.strip());
        message.setMetadataJson(writeJson(new MessageMetadata(partial, normalizedCitations)));
        messageMapper.insert(message);

        InterviewState previous = loadLatestStateInternal(userId, sessionId)
                .orElseGet(() -> baseState(session));
        List<Message> allMessages = domainMessages(userId, sessionId);
        String summary = allMessages.size() > 10 ? compactSummary(allMessages) : previous.summary();
        InterviewState updated = new InterviewState(
                previous.stateVersion(), sessionId, userId, session.getStage(),
                allMessages, question.strip(), previous.latestAnswer(),
                analysis == null ? previous.analysis() : analysis, previous.evaluation(),
                previous.profile(), previous.rules(), summary, previous.claimLedger(),
                previous.evidenceLedger(), previous.logicChainResult(), previous.probePlan(),
                previous.deferredProbes(), previous.pressureState(), previous.activeScenario(),
                previous.coverage(), previous.strategy());
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
                previous.profile(), previous.rules(), previous.summary(), previous.claimLedger(),
                previous.evidenceLedger(), previous.logicChainResult(), previous.probePlan(),
                previous.deferredProbes(), previous.pressureState(), previous.activeScenario(),
                previous.coverage(), previous.strategy());
        saveCheckpointInternal(userId, sessionId, "stage_" + stage.name().toLowerCase(), updated);
        return updated;
    }

    @Transactional
    public InterviewState updateClaimLedger(long userId, long sessionId, ClaimLedger claimLedger) {
        InterviewSessionEntity session = requireEntity(userId, sessionId);
        InterviewState previous = loadLatestStateInternal(userId, sessionId)
                .orElseGet(() -> baseState(session));
        InterviewState updated = new InterviewState(
                InterviewState.CURRENT_VERSION, sessionId, userId, previous.stage(), previous.messages(),
                previous.currentQuestion(), previous.latestAnswer(), previous.analysis(), previous.evaluation(),
                previous.profile(), previous.rules(), previous.summary(),
                claimLedger == null ? ClaimLedger.empty() : claimLedger,
                previous.evidenceLedger(), previous.logicChainResult(), previous.probePlan(),
                previous.deferredProbes(), previous.pressureState(), previous.activeScenario(),
                previous.coverage(), previous.strategy());
        saveCheckpointInternal(userId, sessionId, "claim_ledger_updated", updated);
        return updated;
    }

    @Transactional
    public InterviewState updateProbePlan(long userId, long sessionId, ProbePlan probePlan) {
        if (probePlan == null || probePlan.objective().isBlank() || probePlan.strategy() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        InterviewSessionEntity session = requireEntity(userId, sessionId);
        InterviewState previous = loadLatestStateInternal(userId, sessionId)
                .orElseGet(() -> baseState(session));
        InterviewState updated = new InterviewState(
                InterviewState.CURRENT_VERSION, sessionId, userId, previous.stage(), previous.messages(),
                previous.currentQuestion(), previous.latestAnswer(), previous.analysis(), previous.evaluation(),
                previous.profile(), previous.rules(), previous.summary(), previous.claimLedger(),
                previous.evidenceLedger(), previous.logicChainResult(), probePlan,
                previous.deferredProbes(), previous.pressureState(), previous.activeScenario(),
                previous.coverage(), previous.strategy());
        saveCheckpointInternal(userId, sessionId, "probe_planned", updated);
        return updated;
    }

    @Transactional
    public InterviewState updateLogicChain(long userId, long sessionId, LogicChainResult logicChainResult) {
        if (logicChainResult == null) throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        InterviewSessionEntity session = requireEntity(userId, sessionId);
        InterviewState previous = loadLatestStateInternal(userId, sessionId)
                .orElseGet(() -> baseState(session));
        InterviewState updated = new InterviewState(
                InterviewState.CURRENT_VERSION, sessionId, userId, previous.stage(), previous.messages(),
                previous.currentQuestion(), previous.latestAnswer(), previous.analysis(), previous.evaluation(),
                previous.profile(), previous.rules(), previous.summary(), previous.claimLedger(),
                previous.evidenceLedger(), logicChainResult, previous.probePlan(),
                previous.deferredProbes(), previous.pressureState(), previous.activeScenario(),
                previous.coverage(), previous.strategy());
        saveCheckpointInternal(userId, sessionId, "logic_chain_evaluated", updated);
        return updated;
    }

    @Transactional
    public InterviewState updateEvidenceLedger(long userId, long sessionId, EvidenceLedger evidenceLedger) {
        InterviewSessionEntity session = requireEntity(userId, sessionId);
        InterviewState previous = loadLatestStateInternal(userId, sessionId)
                .orElseGet(() -> baseState(session));
        InterviewState updated = new InterviewState(
                InterviewState.CURRENT_VERSION, sessionId, userId, previous.stage(), previous.messages(),
                previous.currentQuestion(), previous.latestAnswer(), previous.analysis(), previous.evaluation(),
                previous.profile(), previous.rules(), previous.summary(), previous.claimLedger(),
                evidenceLedger == null ? EvidenceLedger.empty() : evidenceLedger,
                previous.logicChainResult(), previous.probePlan(), previous.deferredProbes(),
                previous.pressureState(), previous.activeScenario(), previous.coverage(),
                previous.strategy());
        saveCheckpointInternal(userId, sessionId, "evidence_ledger_updated", updated);
        return updated;
    }

    @Transactional
    public InterviewState updateCoverageAndStrategy(
            long userId,
            long sessionId,
            InterviewCoverage coverage,
            InterviewStrategy strategy
    ) {
        if (coverage == null || strategy == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        InterviewSessionEntity session = requireEntity(userId, sessionId);
        InterviewState previous = loadLatestStateInternal(userId, sessionId)
                .orElseGet(() -> baseState(session));
        InterviewState updated = new InterviewState(
                InterviewState.CURRENT_VERSION, sessionId, userId, previous.stage(), previous.messages(),
                previous.currentQuestion(), previous.latestAnswer(), previous.analysis(), previous.evaluation(),
                previous.profile(), previous.rules(), previous.summary(), previous.claimLedger(),
                previous.evidenceLedger(), previous.logicChainResult(), previous.probePlan(),
                previous.deferredProbes(), previous.pressureState(), previous.activeScenario(),
                coverage, strategy);
        saveCheckpointInternal(userId, sessionId, "coverage_strategy_updated", updated);
        return updated;
    }

    @Transactional
    public InterviewState updateDeferredProbes(
            long userId,
            long sessionId,
            List<DeferredProbe> deferredProbes
    ) {
        InterviewSessionEntity session = requireEntity(userId, sessionId);
        InterviewState previous = loadLatestStateInternal(userId, sessionId)
                .orElseGet(() -> baseState(session));
        InterviewState updated = new InterviewState(
                InterviewState.CURRENT_VERSION, sessionId, userId, previous.stage(), previous.messages(),
                previous.currentQuestion(), previous.latestAnswer(), previous.analysis(), previous.evaluation(),
                previous.profile(), previous.rules(), previous.summary(), previous.claimLedger(),
                previous.evidenceLedger(), previous.logicChainResult(), previous.probePlan(),
                deferredProbes == null ? List.of() : deferredProbes,
                previous.pressureState(), previous.activeScenario(), previous.coverage(),
                previous.strategy());
        saveCheckpointInternal(userId, sessionId, "deferred_probes_updated", updated);
        return updated;
    }

    @Transactional
    public InterviewState updatePressureState(
            long userId,
            long sessionId,
            PressureState pressureState
    ) {
        InterviewSessionEntity session = requireEntity(userId, sessionId);
        InterviewState previous = loadLatestStateInternal(userId, sessionId)
                .orElseGet(() -> baseState(session));
        InterviewState updated = new InterviewState(
                InterviewState.CURRENT_VERSION, sessionId, userId, previous.stage(), previous.messages(),
                previous.currentQuestion(), previous.latestAnswer(), previous.analysis(), previous.evaluation(),
                previous.profile(), previous.rules(), previous.summary(), previous.claimLedger(),
                previous.evidenceLedger(), previous.logicChainResult(), previous.probePlan(),
                previous.deferredProbes(),
                pressureState == null ? PressureState.initial() : pressureState,
                previous.activeScenario(), previous.coverage(), previous.strategy());
        saveCheckpointInternal(userId, sessionId, "pressure_controlled", updated);
        return updated;
    }

    @Transactional
    public InterviewState updateScenarioState(
            long userId,
            long sessionId,
            com.inin.aiinterviewer.domain.model.ScenarioState scenarioState
    ) {
        InterviewSessionEntity session = requireEntity(userId, sessionId);
        InterviewState previous = loadLatestStateInternal(userId, sessionId)
                .orElseGet(() -> baseState(session));
        if (scenarioState != null && scenarioState.sessionId() != sessionId) {
            throw new BusinessException(ErrorCode.INVALID_STATE);
        }
        InterviewState updated = new InterviewState(
                InterviewState.CURRENT_VERSION, sessionId, userId, previous.stage(), previous.messages(),
                previous.currentQuestion(), previous.latestAnswer(), previous.analysis(), previous.evaluation(),
                previous.profile(), previous.rules(), previous.summary(), previous.claimLedger(),
                previous.evidenceLedger(), previous.logicChainResult(), previous.probePlan(),
                previous.deferredProbes(), previous.pressureState(), scenarioState,
                previous.coverage(), previous.strategy());
        saveCheckpointInternal(userId, sessionId, "scenario_state_updated", updated);
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
        CandidateProfileDto profileSnapshot = readProfile(session.getProfileSnapshotJson());
        DomainPackSnapshot domainPack = readDomainPackSnapshot(session.getDomainPackSnapshotJson());
        return new InterviewState(
                InterviewState.CURRENT_VERSION, session.getId(), session.getUserId(), session.getStage(),
                domainMessages(session.getUserId(), session.getId()), "", "", null, null,
                stateProfile(profileSnapshot),
                snapshot.rules() == null ? Map.of() : snapshot.rules(), "", ClaimLedger.empty(),
                EvidenceLedger.empty(), LogicChainResult.skippedResult(), null, List.of(),
                PressureState.initial(), null,
                InterviewCoverage.fromDomainPack(domainPack == null ? null : domainPack.content()),
                InterviewStrategy.empty());
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
                entity.getId(), entity.getPlanId(), entity.getResumeId(), entity.getProfileId(),
                entity.getTitle(), entity.getJobTitle(), readPlan(entity.getPlanSnapshotJson()),
                readProfile(entity.getProfileSnapshotJson()),
                readKnowledgeSnapshot(entity.getKnowledgeSnapshotJson()), entity.getStage(), entity.getStatus(),
                entity.getPromptVersion(), entity.getStartedTime(), entity.getCompletedTime(),
                entity.getCreateTime(), entity.getUpdateTime(), domainPackDto(entity));
    }

    private DomainPackDto domainPackDto(InterviewSessionEntity entity) {
        DomainPackSnapshot snapshot = readDomainPackSnapshot(entity.getDomainPackSnapshotJson());
        if (snapshot != null && snapshot.content() != null) {
            var pack = snapshot.content();
            return new DomainPackDto(pack.id(), pack.roleCode(), pack.industryCode(),
                    snapshot.version(), pack.displayName());
        }
        if (entity.getDomainPackId() == null || entity.getDomainPackId().isBlank()) return null;
        return new DomainPackDto(entity.getDomainPackId(), "legacy", null,
                entity.getDomainPackVersion(), entity.getDomainPackId());
    }

    private InterviewMessageDto toMessageDto(InterviewMessageEntity entity) {
        MessageMetadata metadata = readMessageMetadata(entity.getMetadataJson(), entity.getId());
        return new InterviewMessageDto(
                entity.getSequenceNo(), entity.getRole(), entity.getContent(), entity.getCreateTime(),
                metadata.partial(), metadata.citations(), entity.getId());
    }

    private List<KnowledgeCitationDto> normalizeCitations(
            InterviewSessionEntity session,
            List<KnowledgeCitationDto> citations
    ) {
        if (citations == null || citations.isEmpty()) return List.of();
        if (citations.size() > 10) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        Map<Long, KnowledgeDocumentSnapshotDto> allowedDocuments = new LinkedHashMap<>();
        for (KnowledgeDocumentSnapshotDto document : readKnowledgeSnapshot(session.getKnowledgeSnapshotJson())) {
            allowedDocuments.put(document.id(), document);
        }
        Map<String, KnowledgeCitationDto> unique = new LinkedHashMap<>();
        for (KnowledgeCitationDto citation : citations) {
            KnowledgeDocumentSnapshotDto document = citation == null
                    ? null : allowedDocuments.get(citation.documentId());
            if (document == null || citation.chunkIndex() < 0 || citation.excerpt().isBlank()
                    || !Double.isFinite(citation.score())) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED);
            }
            KnowledgeCitationDto normalized = new KnowledgeCitationDto(
                    document.id(), document.name(), citation.chunkIndex(), citation.excerpt(), citation.score());
            unique.putIfAbsent(document.id() + ":" + citation.chunkIndex(), normalized);
        }
        return List.copyOf(unique.values());
    }

    private MessageMetadata readMessageMetadata(String json, Long messageId) {
        if (json == null || json.isBlank() || "{}".equals(json.strip())) {
            return MessageMetadata.empty();
        }
        try {
            MessageMetadata metadata = objectMapper.readValue(json, MessageMetadata.class);
            return metadata == null ? MessageMetadata.empty() : metadata;
        } catch (JsonProcessingException exception) {
            log.warn("Ignoring invalid metadata for interview message {}", messageId);
            return MessageMetadata.empty();
        }
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

    private CandidateProfileDto readProfile(String json) {
        if (json == null || json.isBlank() || "{}".equals(json.strip())) return null;
        try {
            return objectMapper.readValue(json, CandidateProfileDto.class);
        } catch (JsonProcessingException exception) {
            throw new SystemException(ErrorCode.SYSTEM_ERROR, exception);
        }
    }

    private List<KnowledgeDocumentSnapshotDto> readKnowledgeSnapshot(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            List<KnowledgeDocumentSnapshotDto> documents = objectMapper.readValue(json, new TypeReference<>() {});
            return documents == null ? List.of() : List.copyOf(documents);
        } catch (JsonProcessingException exception) {
            throw new SystemException(ErrorCode.SYSTEM_ERROR, exception);
        }
    }

    private DomainPackSnapshot readDomainPackSnapshot(String json) {
        if (json == null || json.isBlank() || "{}".equals(json.strip())) return null;
        try {
            return objectMapper.readValue(json, DomainPackSnapshot.class);
        } catch (JsonProcessingException exception) {
            throw new SystemException(ErrorCode.SYSTEM_ERROR, exception);
        }
    }

    private record MessageMetadata(boolean partial, List<KnowledgeCitationDto> citations) {
        private MessageMetadata {
            citations = citations == null ? List.of() : List.copyOf(citations);
        }

        private static MessageMetadata empty() {
            return new MessageMetadata(false, List.of());
        }
    }

    private CandidateProfile stateProfile(CandidateProfileDto snapshot) {
        if (snapshot == null) return null;
        var content = snapshot.content();
        return new CandidateProfile(
                content.skills(),
                content.projects().stream().map(value -> Map.<String, Object>of("description", value)).toList(),
                content.experience().stream().map(value -> Map.<String, Object>of("description", value)).toList(),
                content.education().isBlank() ? Map.of() : Map.of("description", content.education()),
                content.summary());
    }

    @Transactional
    public void delete(long userId, long sessionId) {
        requireEntity(userId, sessionId);
        resultMapper.deleteReport(userId, sessionId);
        resultMapper.deleteEvaluation(userId, sessionId);
        evidenceMapper.deleteBySession(userId, sessionId);
        consistencyIssueMapper.deleteBySession(userId, sessionId);
        deferredProbeMapper.deleteBySession(userId, sessionId);
        scenarioSessionMapper.deleteBySession(userId, sessionId);
        claimMapper.deleteBySession(userId, sessionId);
        messageMapper.deleteBySession(userId, sessionId);
        checkpointMapper.deleteBySession(userId, sessionId);
        sessionMapper.delete(sessionId, userId);
    }
}
