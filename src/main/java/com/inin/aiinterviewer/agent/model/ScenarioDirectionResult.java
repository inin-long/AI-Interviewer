package com.inin.aiinterviewer.agent.model;

import com.inin.aiinterviewer.domain.enums.ScenarioEventType;
import com.inin.aiinterviewer.domain.model.ScenarioAdvanceCommand;

import java.io.Serializable;
import java.util.Map;

public record ScenarioDirectionResult(
        String decisionAction,
        String decisionRationale,
        ScenarioEventType eventType,
        String eventDescription,
        Map<String, Object> changes,
        String nextQuestion,
        boolean completeAfterEvent,
        boolean skipped,
        boolean degraded,
        String failureReason
) implements Serializable {
    public ScenarioDirectionResult {
        decisionAction = decisionAction == null ? "" : decisionAction.strip();
        decisionRationale = decisionRationale == null ? "" : decisionRationale.strip();
        eventDescription = eventDescription == null ? "" : eventDescription.strip();
        changes = changes == null ? Map.of() : Map.copyOf(changes);
        nextQuestion = nextQuestion == null ? "" : nextQuestion.strip();
        failureReason = failureReason == null ? "" : failureReason.strip();
    }

    public static ScenarioDirectionResult skipped(String reason) {
        return new ScenarioDirectionResult(
                "", "", null, "", Map.of(), "", false, true, false, reason);
    }

    public static ScenarioDirectionResult degraded(String reason) {
        return new ScenarioDirectionResult(
                "", "", null, "", Map.of(), "", false, false, true, reason);
    }

    public boolean handled() {
        return !skipped && !degraded && eventType != null;
    }

    public ScenarioAdvanceCommand toCommand() {
        return new ScenarioAdvanceCommand(
                decisionAction, decisionRationale, eventType, eventDescription,
                changes, nextQuestion, completeAfterEvent);
    }
}
