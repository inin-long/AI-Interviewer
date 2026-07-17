package com.inin.aiinterviewer.domain.model;

import com.inin.aiinterviewer.domain.enums.ScenarioEventType;

import java.io.Serializable;
import java.util.Map;

public record ScenarioEvent(
        String id,
        int round,
        ScenarioEventType type,
        String description,
        String triggeredByDecisionId,
        Map<String, Object> changes,
        Map<String, Object> variablesBefore,
        Map<String, Object> variablesAfter,
        String question
) implements Serializable {
    public ScenarioEvent {
        id = id == null ? "" : id.strip();
        description = description == null ? "" : description.strip();
        triggeredByDecisionId = triggeredByDecisionId == null ? "" : triggeredByDecisionId.strip();
        changes = changes == null ? Map.of() : Map.copyOf(changes);
        variablesBefore = variablesBefore == null ? Map.of() : Map.copyOf(variablesBefore);
        variablesAfter = variablesAfter == null ? Map.of() : Map.copyOf(variablesAfter);
        question = question == null ? "" : question.strip();
    }
}
