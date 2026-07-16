package com.inin.aiinterviewer.domain.model;

import com.inin.aiinterviewer.domain.enums.ScenarioStatus;
import com.inin.aiinterviewer.domain.enums.SimulationType;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record ScenarioState(
        String id,
        long sessionId,
        SimulationType type,
        String objective,
        String background,
        String candidateRole,
        List<String> knownFacts,
        List<String> assumptions,
        Map<String, Object> hiddenInformation,
        Map<String, Object> initialVariables,
        Map<String, Object> variables,
        List<ScenarioConstraint> constraints,
        List<ScenarioEvent> events,
        List<CandidateDecision> decisions,
        List<String> evaluatedCompetencies,
        List<String> endConditions,
        int maxRounds,
        int currentRound,
        ScenarioStatus status,
        String terminationReason,
        LocalDateTime createTime,
        LocalDateTime updateTime
) implements Serializable {
    public ScenarioState {
        id = id == null ? "" : id.strip();
        objective = objective == null ? "" : objective.strip();
        background = background == null ? "" : background.strip();
        candidateRole = candidateRole == null ? "" : candidateRole.strip();
        knownFacts = knownFacts == null ? List.of() : List.copyOf(knownFacts);
        assumptions = assumptions == null ? List.of() : List.copyOf(assumptions);
        hiddenInformation = hiddenInformation == null ? Map.of() : Map.copyOf(hiddenInformation);
        initialVariables = initialVariables == null ? Map.of() : Map.copyOf(initialVariables);
        variables = variables == null ? Map.of() : Map.copyOf(variables);
        constraints = constraints == null ? List.of() : List.copyOf(constraints);
        events = events == null ? List.of() : List.copyOf(events);
        decisions = decisions == null ? List.of() : List.copyOf(decisions);
        evaluatedCompetencies = evaluatedCompetencies == null
                ? List.of() : List.copyOf(evaluatedCompetencies);
        endConditions = endConditions == null ? List.of() : List.copyOf(endConditions);
        maxRounds = Math.max(1, maxRounds);
        currentRound = Math.max(0, currentRound);
        status = status == null ? ScenarioStatus.ACTIVE : status;
        terminationReason = terminationReason == null ? "" : terminationReason.strip();
    }
}
