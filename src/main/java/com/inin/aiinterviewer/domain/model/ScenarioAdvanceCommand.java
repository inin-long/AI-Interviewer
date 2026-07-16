package com.inin.aiinterviewer.domain.model;

import com.inin.aiinterviewer.domain.enums.ScenarioEventType;

import java.util.Map;

public record ScenarioAdvanceCommand(
        String action,
        String rationale,
        ScenarioEventType eventType,
        String eventDescription,
        Map<String, Object> changes,
        String nextQuestion,
        boolean completeAfterEvent
) {
    public ScenarioAdvanceCommand {
        action = action == null ? "" : action.strip();
        rationale = rationale == null ? "" : rationale.strip();
        eventDescription = eventDescription == null ? "" : eventDescription.strip();
        changes = changes == null ? Map.of() : Map.copyOf(changes);
        nextQuestion = nextQuestion == null ? "" : nextQuestion.strip();
    }
}
