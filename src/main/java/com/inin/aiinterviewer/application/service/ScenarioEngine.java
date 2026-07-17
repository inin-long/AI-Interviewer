package com.inin.aiinterviewer.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inin.aiinterviewer.application.exception.BusinessException;
import com.inin.aiinterviewer.application.exception.ErrorCode;
import com.inin.aiinterviewer.application.exception.SystemException;
import com.inin.aiinterviewer.domain.entity.ScenarioSessionEntity;
import com.inin.aiinterviewer.domain.enums.ScenarioStatus;
import com.inin.aiinterviewer.domain.model.CandidateDecision;
import com.inin.aiinterviewer.domain.model.ScenarioAdvanceCommand;
import com.inin.aiinterviewer.domain.model.ScenarioDefinition;
import com.inin.aiinterviewer.domain.model.ScenarioEvent;
import com.inin.aiinterviewer.domain.model.ScenarioState;
import com.inin.aiinterviewer.infrastructure.database.mapper.InterviewMessageMapper;
import com.inin.aiinterviewer.infrastructure.database.mapper.InterviewSessionMapper;
import com.inin.aiinterviewer.infrastructure.database.mapper.ScenarioSessionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class ScenarioEngine {

    private final ScenarioSessionMapper scenarioMapper;
    private final InterviewSessionMapper sessionMapper;
    private final InterviewMessageMapper messageMapper;
    private final ObjectMapper objectMapper;

    public ScenarioEngine(
            ScenarioSessionMapper scenarioMapper,
            InterviewSessionMapper sessionMapper,
            InterviewMessageMapper messageMapper,
            ObjectMapper objectMapper
    ) {
        this.scenarioMapper = scenarioMapper;
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ScenarioState start(long userId, long sessionId, ScenarioDefinition definition) {
        requireSession(userId, sessionId);
        validateDefinition(definition);
        if (scenarioMapper.findActive(userId, sessionId).isPresent()) {
            throw new BusinessException(ErrorCode.INVALID_STATE);
        }
        LocalDateTime now = LocalDateTime.now();
        ScenarioState state = new ScenarioState(
                UUID.randomUUID().toString(), sessionId, definition.type(), definition.objective(),
                definition.background(), definition.candidateRole(), definition.knownFacts(),
                definition.assumptions(), definition.hiddenInformation(), definition.initialVariables(),
                definition.initialVariables(), definition.constraints(), List.of(), List.of(),
                definition.evaluatedCompetencies(), definition.endConditions(), definition.maxRounds(),
                0, ScenarioStatus.ACTIVE, "", now, now);
        validateTimeline(state);

        ScenarioSessionEntity entity = new ScenarioSessionEntity();
        entity.setId(state.id());
        entity.setUserId(userId);
        entity.setInterviewSessionId(sessionId);
        entity.setScenarioType(state.type());
        entity.setStatus(state.status());
        entity.setStateJson(writeState(state));
        entity.setCurrentRound(0);
        scenarioMapper.insert(entity);
        return state;
    }

    @Transactional(readOnly = true)
    public Optional<ScenarioState> findActive(long userId, long sessionId) {
        requireSession(userId, sessionId);
        return scenarioMapper.findActive(userId, sessionId).map(this::readState);
    }

    @Transactional(readOnly = true)
    public boolean hasScenario(long userId, long sessionId) {
        requireSession(userId, sessionId);
        return scenarioMapper.countBySession(userId, sessionId) > 0;
    }

    @Transactional(readOnly = true)
    public ScenarioState require(long userId, long sessionId, String scenarioId) {
        requireSession(userId, sessionId);
        return readState(scenarioMapper.findById(scenarioId, userId, sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCENARIO_NOT_FOUND)));
    }

    @Transactional
    public ScenarioState markIntroduced(long userId, long sessionId, String scenarioId) {
        ScenarioSessionEntity entity = scenarioMapper.findById(scenarioId, userId, sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCENARIO_NOT_FOUND));
        ScenarioState current = readState(entity);
        if (current.status() != ScenarioStatus.ACTIVE || current.introduced()
                || current.currentRound() != 0) {
            throw new BusinessException(ErrorCode.INVALID_STATE);
        }
        ScenarioState introduced = withIntroduced(current, LocalDateTime.now());
        update(entity, userId, sessionId, introduced, current.currentRound());
        return introduced;
    }

    @Transactional
    public ScenarioState advance(
            long userId,
            long sessionId,
            String scenarioId,
            ScenarioAdvanceCommand command
    ) {
        validateAdvance(command);
        ScenarioSessionEntity entity = scenarioMapper.findById(scenarioId, userId, sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCENARIO_NOT_FOUND));
        ScenarioState current = readState(entity);
        if (current.status() != ScenarioStatus.ACTIVE || !current.introduced()
                || current.currentRound() >= current.maxRounds()) {
            throw new BusinessException(ErrorCode.INVALID_STATE);
        }
        var latestAnswer = messageMapper.findLatestUserMessage(userId, sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_STATE));
        if (current.decisions().stream()
                .anyMatch(decision -> decision.sourceMessageId() == latestAnswer.getId())) {
            throw new BusinessException(ErrorCode.INVALID_STATE);
        }
        validateChanges(current.variables(), command.changes());

        int nextRound = current.currentRound() + 1;
        LocalDateTime now = LocalDateTime.now();
        CandidateDecision decision = new CandidateDecision(
                UUID.randomUUID().toString(), nextRound, latestAnswer.getId(), latestAnswer.getContent(),
                command.action(), command.rationale(), now);
        Map<String, Object> before = new LinkedHashMap<>(current.variables());
        Map<String, Object> after = new LinkedHashMap<>(before);
        after.putAll(command.changes());
        ScenarioEvent event = new ScenarioEvent(
                UUID.randomUUID().toString(), nextRound, command.eventType(),
                command.eventDescription(), decision.id(), command.changes(), before, after,
                command.nextQuestion());

        List<CandidateDecision> decisions = new ArrayList<>(current.decisions());
        decisions.add(decision);
        List<ScenarioEvent> events = new ArrayList<>(current.events());
        events.add(event);
        boolean completed = command.completeAfterEvent() || nextRound >= current.maxRounds();
        ScenarioStatus status = completed ? ScenarioStatus.COMPLETED : ScenarioStatus.ACTIVE;
        String reason = !completed ? "" : command.completeAfterEvent()
                ? "场景结束条件已满足" : "场景达到最大轮次";
        ScenarioState updated = copy(
                current, after, events, decisions, nextRound, status, reason, now);
        validateTimeline(updated);
        update(entity, userId, sessionId, updated, current.currentRound());
        return updated;
    }

    @Transactional
    public ScenarioState failActive(long userId, long sessionId, String reason) {
        ScenarioSessionEntity entity = scenarioMapper.findActive(userId, sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCENARIO_NOT_FOUND));
        ScenarioState current = readState(entity);
        String safeReason = reason == null || reason.isBlank()
                ? "场景执行失败，已返回普通面试流程" : reason.strip();
        ScenarioState failed = copy(
                current, current.variables(), current.events(), current.decisions(),
                current.currentRound(), ScenarioStatus.FAILED, safeReason, LocalDateTime.now());
        update(entity, userId, sessionId, failed, current.currentRound());
        return failed;
    }

    @Transactional
    public ScenarioState abort(long userId, long sessionId, String scenarioId, String reason) {
        ScenarioSessionEntity entity = scenarioMapper.findById(scenarioId, userId, sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCENARIO_NOT_FOUND));
        ScenarioState current = readState(entity);
        if (current.status() != ScenarioStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.INVALID_STATE);
        }
        ScenarioState aborted = copy(
                current, current.variables(), current.events(), current.decisions(),
                current.currentRound(), ScenarioStatus.ABORTED,
                reason == null ? "" : reason.strip(), LocalDateTime.now());
        update(entity, userId, sessionId, aborted, current.currentRound());
        return aborted;
    }

    private ScenarioState copy(
            ScenarioState source,
            Map<String, Object> variables,
            List<ScenarioEvent> events,
            List<CandidateDecision> decisions,
            int currentRound,
            ScenarioStatus status,
            String terminationReason,
            LocalDateTime updateTime
    ) {
        return new ScenarioState(
                source.id(), source.sessionId(), source.type(), source.objective(), source.background(),
                source.candidateRole(), source.knownFacts(), source.assumptions(), source.hiddenInformation(),
                source.initialVariables(), variables, source.constraints(), events, decisions,
                source.evaluatedCompetencies(), source.endConditions(), source.introduced(),
                source.maxRounds(), currentRound,
                status, terminationReason, source.createTime(), updateTime);
    }

    private void update(
            ScenarioSessionEntity entity,
            long userId,
            long sessionId,
            ScenarioState state,
            int expectedRound
    ) {
        if (scenarioMapper.updateState(
                entity.getId(), userId, sessionId, state.status(), writeState(state),
                state.currentRound(), expectedRound) != 1) {
            throw new BusinessException(ErrorCode.INVALID_STATE);
        }
    }

    private void validateDefinition(ScenarioDefinition definition) {
        if (definition == null || definition.type() == null || definition.objective().isBlank()
                || definition.background().isBlank() || definition.candidateRole().isBlank()
                || definition.knownFacts().isEmpty() || definition.initialVariables().isEmpty()
                || definition.constraints().isEmpty() || definition.evaluatedCompetencies().isEmpty()
                || definition.endConditions().isEmpty() || definition.maxRounds() < 1
                || definition.maxRounds() > 10
                || definition.constraints().stream().anyMatch(value ->
                value.code().isBlank() || value.description().isBlank())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        validateVariableKeys(definition.initialVariables());
    }

    private void validateAdvance(ScenarioAdvanceCommand command) {
        if (command == null || command.action().isBlank() || command.rationale().isBlank()
                || command.eventType() == null || command.eventDescription().isBlank()
                || command.changes().isEmpty() || command.nextQuestion().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        validateVariableKeys(command.changes());
    }

    private void validateChanges(Map<String, Object> current, Map<String, Object> changes) {
        if (!current.keySet().containsAll(changes.keySet())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
    }

    private void validateVariableKeys(Map<String, Object> variables) {
        if (variables.keySet().stream().anyMatch(key -> key == null || key.isBlank())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
    }

    private ScenarioState readState(ScenarioSessionEntity entity) {
        try {
            var tree = objectMapper.readTree(entity.getStateJson());
            boolean legacyWithoutIntroductionState = !tree.has("introduced");
            ScenarioState state = objectMapper.treeToValue(tree, ScenarioState.class);
            if (legacyWithoutIntroductionState) {
                state = withIntroduced(state, state.updateTime());
            }
            if (!entity.getId().equals(state.id())
                    || entity.getInterviewSessionId() != state.sessionId()
                    || entity.getScenarioType() != state.type()
                    || entity.getStatus() != state.status()
                    || entity.getCurrentRound() != state.currentRound()) {
                throw new BusinessException(ErrorCode.SCENARIO_STATE_INVALID);
            }
            validateTimeline(state);
            return state;
        } catch (JsonProcessingException exception) {
            throw new SystemException(ErrorCode.DATA_ACCESS_FAILED, exception);
        }
    }

    private ScenarioState withIntroduced(ScenarioState source, LocalDateTime updateTime) {
        return new ScenarioState(
                source.id(), source.sessionId(), source.type(), source.objective(), source.background(),
                source.candidateRole(), source.knownFacts(), source.assumptions(), source.hiddenInformation(),
                source.initialVariables(), source.variables(), source.constraints(), source.events(),
                source.decisions(), source.evaluatedCompetencies(), source.endConditions(), true,
                source.maxRounds(), source.currentRound(), source.status(), source.terminationReason(),
                source.createTime(), updateTime);
    }

    private void validateTimeline(ScenarioState state) {
        if (state.events().size() != state.currentRound()
                || state.decisions().size() != state.currentRound()) {
            throw new BusinessException(ErrorCode.SCENARIO_STATE_INVALID);
        }
        Map<String, Object> expected = new LinkedHashMap<>(state.initialVariables());
        for (int index = 0; index < state.currentRound(); index++) {
            int round = index + 1;
            CandidateDecision decision = state.decisions().get(index);
            ScenarioEvent event = state.events().get(index);
            if (decision.round() != round || event.round() != round
                    || !event.triggeredByDecisionId().equals(decision.id())
                    || !event.variablesBefore().equals(expected)) {
                throw new BusinessException(ErrorCode.SCENARIO_STATE_INVALID);
            }
            expected.putAll(event.changes());
            if (!event.variablesAfter().equals(expected)) {
                throw new BusinessException(ErrorCode.SCENARIO_STATE_INVALID);
            }
        }
        if (!state.variables().equals(expected)) {
            throw new BusinessException(ErrorCode.SCENARIO_STATE_INVALID);
        }
    }

    private String writeState(ScenarioState state) {
        try {
            return objectMapper.writeValueAsString(state);
        } catch (JsonProcessingException exception) {
            throw new SystemException(ErrorCode.DATA_ACCESS_FAILED, exception);
        }
    }

    private void requireSession(long userId, long sessionId) {
        if (sessionMapper.findByIdAndUserId(sessionId, userId).isEmpty()) {
            throw new BusinessException(ErrorCode.INTERVIEW_NOT_FOUND);
        }
    }
}
