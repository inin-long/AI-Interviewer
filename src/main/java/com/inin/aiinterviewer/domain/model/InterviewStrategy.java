package com.inin.aiinterviewer.domain.model;

import com.inin.aiinterviewer.domain.enums.InterviewStage;
import com.inin.aiinterviewer.domain.enums.PressureLevel;
import com.inin.aiinterviewer.domain.enums.ProbeStrategy;

import java.io.Serializable;

public record InterviewStrategy(
        InterviewStage stage,
        ProbeStrategy probeStrategy,
        String targetClaimId,
        String targetCompetencyCode,
        String objective,
        PressureLevel pressureLevel,
        int remainingQuestions,
        boolean scenarioTurn,
        String reason
) implements Serializable {

    public InterviewStrategy {
        stage = stage == null ? InterviewStage.INTRODUCTION : stage;
        probeStrategy = probeStrategy == null ? ProbeStrategy.CLARIFY_CONCEPT : probeStrategy;
        targetClaimId = text(targetClaimId);
        targetCompetencyCode = text(targetCompetencyCode);
        objective = text(objective);
        pressureLevel = pressureLevel == null ? PressureLevel.STANDARD : pressureLevel;
        remainingQuestions = Math.max(0, remainingQuestions);
        reason = text(reason);
    }

    public static InterviewStrategy empty() {
        return new InterviewStrategy(
                InterviewStage.INTRODUCTION, ProbeStrategy.CLARIFY_CONCEPT,
                "", "", "", PressureLevel.STANDARD, 0, false, "");
    }

    private static String text(String value) {
        return value == null ? "" : value.strip();
    }
}
