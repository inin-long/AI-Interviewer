package com.inin.aiinterviewer.domain.model;

import com.inin.aiinterviewer.domain.enums.SimulationType;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public record ScenarioDefinition(
        SimulationType type,
        String objective,
        String background,
        String candidateRole,
        List<String> knownFacts,
        List<String> assumptions,
        Map<String, Object> hiddenInformation,
        Map<String, Object> initialVariables,
        List<ScenarioConstraint> constraints,
        List<String> evaluatedCompetencies,
        List<String> endConditions,
        int maxRounds
) implements Serializable {
    public ScenarioDefinition {
        objective = objective == null ? "" : objective.strip();
        background = background == null ? "" : background.strip();
        candidateRole = candidateRole == null ? "" : candidateRole.strip();
        knownFacts = knownFacts == null ? List.of() : List.copyOf(knownFacts);
        assumptions = assumptions == null ? List.of() : List.copyOf(assumptions);
        hiddenInformation = hiddenInformation == null ? Map.of() : Map.copyOf(hiddenInformation);
        initialVariables = initialVariables == null ? Map.of() : Map.copyOf(initialVariables);
        constraints = constraints == null ? List.of() : List.copyOf(constraints);
        evaluatedCompetencies = evaluatedCompetencies == null
                ? List.of() : List.copyOf(evaluatedCompetencies);
        endConditions = endConditions == null ? List.of() : List.copyOf(endConditions);
    }
}
