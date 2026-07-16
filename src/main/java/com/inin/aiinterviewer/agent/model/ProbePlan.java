package com.inin.aiinterviewer.agent.model;

import com.inin.aiinterviewer.domain.enums.PressureLevel;
import com.inin.aiinterviewer.domain.enums.ProbeStrategy;

import java.io.Serializable;
import java.util.List;

public record ProbePlan(
        String targetClaimId,
        String targetLogicGap,
        String targetConsistencyIssueId,
        String targetDeferredProbeId,
        String objective,
        ProbeStrategy strategy,
        PressureLevel pressureLevel,
        String reason,
        List<String> expectedEvidence,
        boolean shouldInjectScenario
) implements Serializable {
    public ProbePlan {
        targetClaimId = targetClaimId == null ? "" : targetClaimId;
        targetLogicGap = targetLogicGap == null ? "" : targetLogicGap;
        targetConsistencyIssueId = targetConsistencyIssueId == null ? "" : targetConsistencyIssueId;
        targetDeferredProbeId = targetDeferredProbeId == null ? "" : targetDeferredProbeId;
        objective = objective == null ? "" : objective.strip();
        pressureLevel = pressureLevel == null ? PressureLevel.STANDARD : pressureLevel;
        reason = reason == null ? "" : reason.strip();
        expectedEvidence = expectedEvidence == null ? List.of() : List.copyOf(expectedEvidence);
    }

    public ProbePlan(
            String targetClaimId,
            String targetLogicGap,
            String targetConsistencyIssueId,
            String objective,
            ProbeStrategy strategy,
            PressureLevel pressureLevel,
            String reason,
            List<String> expectedEvidence,
            boolean shouldInjectScenario
    ) {
        this(targetClaimId, targetLogicGap, targetConsistencyIssueId, "", objective,
                strategy, pressureLevel, reason, expectedEvidence, shouldInjectScenario);
    }

    public ProbePlan(
            String targetClaimId,
            String targetLogicGap,
            String objective,
            ProbeStrategy strategy,
            PressureLevel pressureLevel,
            String reason,
            List<String> expectedEvidence,
            boolean shouldInjectScenario
    ) {
        this(targetClaimId, targetLogicGap, "", "", objective, strategy, pressureLevel,
                reason, expectedEvidence, shouldInjectScenario);
    }

    public ProbePlan(
            String targetClaimId,
            String objective,
            ProbeStrategy strategy,
            PressureLevel pressureLevel,
            String reason,
            List<String> expectedEvidence,
            boolean shouldInjectScenario
    ) {
        this(targetClaimId, "", "", "", objective, strategy, pressureLevel, reason,
                expectedEvidence, shouldInjectScenario);
    }

    public static ProbePlan stageOpening(String objective) {
        return new ProbePlan(
                "", "", "", "", objective, ProbeStrategy.CLARIFY_CONCEPT, PressureLevel.STANDARD,
                "进入新阶段并覆盖尚未验证的岗位能力", List.of("具体经历", "个人行动", "可验证结果"), false);
    }

    public boolean targetsClaim() {
        return !targetClaimId.isBlank();
    }

    public boolean targetsLogicGap() {
        return !targetLogicGap.isBlank();
    }

    public boolean targetsConsistencyIssue() {
        return !targetConsistencyIssueId.isBlank();
    }

    public boolean targetsDeferredProbe() {
        return !targetDeferredProbeId.isBlank();
    }

    public ProbePlan withPressureLevel(PressureLevel level) {
        return new ProbePlan(
                targetClaimId, targetLogicGap, targetConsistencyIssueId, targetDeferredProbeId,
                objective, strategy, level, reason, expectedEvidence, shouldInjectScenario);
    }

    public ProbePlan withSafetyFallback(String safeObjective, String safeReason) {
        return new ProbePlan(
                targetClaimId, targetLogicGap, targetConsistencyIssueId, targetDeferredProbeId,
                safeObjective, ProbeStrategy.CLARIFY_CONCEPT, PressureLevel.RELAXED,
                safeReason, expectedEvidence, false);
    }
}
