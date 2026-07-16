package com.inin.aiinterviewer.agent.state;

import com.inin.aiinterviewer.application.exception.ErrorCode;
import com.inin.aiinterviewer.application.exception.SystemException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class JacksonStateSerializer implements StateSerializer {

    private final ObjectMapper objectMapper;

    public JacksonStateSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String serialize(InterviewState state) {
        try {
            return objectMapper.writeValueAsString(state);
        } catch (JsonProcessingException exception) {
            throw new SystemException(ErrorCode.SYSTEM_ERROR, exception);
        }
    }

    @Override
    public InterviewState deserialize(String json) {
        try {
            InterviewState state = objectMapper.readValue(json, InterviewState.class);
            if (InterviewState.CURRENT_VERSION.equals(state.stateVersion())) return state;
            if ("1.0".equals(state.stateVersion()) || "2.0".equals(state.stateVersion())
                    || "2.1".equals(state.stateVersion()) || "2.2".equals(state.stateVersion())
                    || "2.3".equals(state.stateVersion()) || "2.4".equals(state.stateVersion())
                    || "2.5".equals(state.stateVersion()) || "2.6".equals(state.stateVersion())
                    || "2.7".equals(state.stateVersion())) {
                return new InterviewState(
                        InterviewState.CURRENT_VERSION, state.sessionId(), state.userId(), state.stage(),
                        state.messages(), state.currentQuestion(), state.latestAnswer(), state.analysis(),
                        state.evaluation(), state.profile(), state.rules(), state.summary(),
                        state.claimLedger(), state.evidenceLedger(), state.logicChainResult(),
                        state.probePlan(), state.deferredProbes(), state.pressureState(),
                        migrateScenario(state.activeScenario()));
            }
            throw new IllegalStateException("Unsupported interview state version: " + state.stateVersion());
        } catch (JsonProcessingException exception) {
            throw new SystemException(ErrorCode.SYSTEM_ERROR, exception);
        }
    }

    private com.inin.aiinterviewer.domain.model.ScenarioState migrateScenario(
            com.inin.aiinterviewer.domain.model.ScenarioState scenario
    ) {
        if (scenario == null || scenario.introduced()) return scenario;
        return new com.inin.aiinterviewer.domain.model.ScenarioState(
                scenario.id(), scenario.sessionId(), scenario.type(), scenario.objective(),
                scenario.background(), scenario.candidateRole(), scenario.knownFacts(),
                scenario.assumptions(), scenario.hiddenInformation(), scenario.initialVariables(),
                scenario.variables(), scenario.constraints(), scenario.events(), scenario.decisions(),
                scenario.evaluatedCompetencies(), scenario.endConditions(), true, scenario.maxRounds(),
                scenario.currentRound(), scenario.status(), scenario.terminationReason(),
                scenario.createTime(), scenario.updateTime());
    }
}
