package com.inin.aiinterviewer.agent.model;

import com.inin.aiinterviewer.domain.model.LogicGap;

import java.io.Serializable;
import java.util.List;

public record LogicChainResult(
        List<String> premises,
        String problemDiagnosis,
        List<String> alternatives,
        String decision,
        String reasoning,
        List<String> actions,
        String outcome,
        String validation,
        String reflection,
        List<LogicGap> gaps,
        boolean skipped,
        boolean degraded,
        String failureReason
) implements Serializable {
    public LogicChainResult {
        premises = copy(premises);
        problemDiagnosis = text(problemDiagnosis);
        alternatives = copy(alternatives);
        decision = text(decision);
        reasoning = text(reasoning);
        actions = copy(actions);
        outcome = text(outcome);
        validation = text(validation);
        reflection = text(reflection);
        gaps = gaps == null ? List.of() : List.copyOf(gaps);
        failureReason = text(failureReason);
    }

    public static LogicChainResult skippedResult() {
        return new LogicChainResult(
                List.of(), "", List.of(), "", "", List.of(), "", "", "",
                List.of(), true, false, "");
    }

    public static LogicChainResult degraded(String reason) {
        return new LogicChainResult(
                List.of(), "", List.of(), "", "", List.of(), "", "", "",
                List.of(), false, true, reason);
    }

    private static List<String> copy(List<String> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    private static String text(String value) {
        return value == null ? "" : value.strip();
    }
}
