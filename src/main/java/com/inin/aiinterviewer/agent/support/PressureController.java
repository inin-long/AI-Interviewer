package com.inin.aiinterviewer.agent.support;

import com.inin.aiinterviewer.agent.model.EvidenceCollectionResult;
import com.inin.aiinterviewer.agent.model.PressureControlResult;
import com.inin.aiinterviewer.agent.model.ProbePlan;
import com.inin.aiinterviewer.application.dto.InterviewPlanDto;
import com.inin.aiinterviewer.domain.enums.EvidenceSignal;
import com.inin.aiinterviewer.domain.enums.InterviewDifficulty;
import com.inin.aiinterviewer.domain.enums.PressureLevel;
import com.inin.aiinterviewer.domain.enums.ProbeStrategy;
import com.inin.aiinterviewer.domain.model.PressureState;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
public class PressureController {

    private static final int MAX_CONSECUTIVE_PRESSURE_TURNS = 2;
    private static final int MAX_REPEATED_TARGET_TURNS = 2;
    private static final List<String> UNSAFE_TERMS = List.of(
            "蠢", "愚蠢", "撒谎", "骗子", "无能", "垃圾", "闭嘴",
            "stupid", "idiot", "liar", "incompetent");

    public PressureControlResult control(
            InterviewPlanDto interviewPlan,
            ProbePlan probePlan,
            PressureState previous,
            EvidenceCollectionResult evidence
    ) {
        PressureState safePrevious = previous == null ? PressureState.initial() : previous;
        ProbePlan safePlan = probePlan == null
                ? ProbePlan.stageOpening("验证当前阶段的岗位核心能力") : probePlan;
        String targetKey = targetKey(safePlan);
        boolean sameTarget = !targetKey.isBlank() && targetKey.equals(safePrevious.lastTargetKey());
        int repeatedTurns = sameTarget ? safePrevious.repeatedTargetTurns() + 1 : 1;
        boolean sufficientEvidence = hasSufficientEvidence(safePlan, evidence);

        if (containsUnsafeLanguage(safePlan)) {
            ProbePlan adjusted = safePlan.withSafetyFallback(
                    "请基于已知事实，中性说明该主张的适用条件和可验证依据。",
                    "原追问包含不安全表达，已替换为中性验证目标");
            return new PressureControlResult(adjusted, new PressureState(
                    PressureLevel.RELAXED, 0, targetKey, repeatedTurns,
                    sufficientEvidence, true, true, "检测到攻击性或人格判断表达，已安全降压"));
        }

        PressureLevel desired = lowerOf(levelFor(safePlan.strategy()), maximumLevel(interviewPlan));
        boolean lowered = false;
        String reason = "依据追问策略和方案上限设置压力等级";
        int consecutive = desired.ordinal() > PressureLevel.STANDARD.ordinal()
                ? safePrevious.consecutivePressureTurns() + 1 : 0;

        if (sufficientEvidence) {
            desired = PressureLevel.RELAXED;
            consecutive = 0;
            lowered = true;
            reason = "已获得高强度且高置信度的正向证据，停止继续施压";
        } else if (repeatedTurns > MAX_REPEATED_TARGET_TURNS) {
            desired = lowerOf(desired, PressureLevel.STANDARD);
            consecutive = 0;
            lowered = true;
            reason = "同一验证目标已连续追问，降低压力以避免无意义重复";
        } else if (consecutive > MAX_CONSECUTIVE_PRESSURE_TURNS) {
            desired = PressureLevel.STANDARD;
            consecutive = 0;
            lowered = true;
            reason = "连续高强度追问已达到上限，本轮自动降压";
        }

        ProbePlan adjusted = safePlan.withPressureLevel(desired);
        return new PressureControlResult(adjusted, new PressureState(
                desired, consecutive, targetKey, repeatedTurns, sufficientEvidence,
                lowered, false, reason));
    }

    private PressureLevel levelFor(ProbeStrategy strategy) {
        if (strategy == null) return PressureLevel.RELAXED;
        return switch (strategy) {
            case CLARIFY_CONCEPT -> PressureLevel.RELAXED;
            case REQUEST_BASELINE, REQUEST_METRIC_BREAKDOWN, VERIFY_PERSONAL_OWNERSHIP,
                    VERIFY_DATA_SOURCE, TRACE_CAUSAL_CHAIN, ASK_IMPLEMENTATION_DETAIL,
                    CROSS_CHECK_HISTORY -> PressureLevel.STANDARD;
            case ASK_TRADE_OFF, ASK_ALTERNATIVE, CHALLENGE_ASSUMPTION,
                    REQUEST_PRIORITIZATION -> PressureLevel.CHALLENGING;
            case INTRODUCE_CONSTRAINT, INTRODUCE_FAILURE -> PressureLevel.HIGH_PRESSURE;
        };
    }

    private PressureLevel maximumLevel(InterviewPlanDto plan) {
        PressureLevel configured = configuredLevel(plan);
        PressureLevel maximum = configured == null ? levelForDifficulty(
                plan == null ? null : plan.difficulty()) : configured;
        if (plan != null && plan.rules() != null) {
            String mode = String.valueOf(plan.rules().getOrDefault("interviewMode", "FORMAL_SIMULATION"));
            if ("COACHING".equalsIgnoreCase(mode)) {
                maximum = lowerOf(maximum, PressureLevel.CHALLENGING);
            }
        }
        return maximum;
    }

    private PressureLevel configuredLevel(InterviewPlanDto plan) {
        if (plan == null || plan.rules() == null) return null;
        Object value = plan.rules().get("pressureLevel");
        if (value == null) return null;
        try {
            return PressureLevel.valueOf(String.valueOf(value).strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private PressureLevel levelForDifficulty(InterviewDifficulty difficulty) {
        if (difficulty == null) return PressureLevel.STANDARD;
        return switch (difficulty) {
            case JUNIOR -> PressureLevel.STANDARD;
            case MEDIUM -> PressureLevel.CHALLENGING;
            case SENIOR, EXPERT -> PressureLevel.HIGH_PRESSURE;
        };
    }

    private boolean hasSufficientEvidence(
            ProbePlan plan,
            EvidenceCollectionResult result
    ) {
        if (result == null || result.degraded()) return false;
        return result.evidence().stream().anyMatch(candidate ->
                candidate.signal() == EvidenceSignal.POSITIVE
                        && candidate.strength() >= 0.75
                        && candidate.confidence() >= 0.65
                        && (plan.targetClaimId().isBlank()
                        || candidate.relatedClaimIds().contains(plan.targetClaimId())));
    }

    private boolean containsUnsafeLanguage(ProbePlan plan) {
        String content = (plan.objective() + " " + plan.reason()).toLowerCase(Locale.ROOT);
        return UNSAFE_TERMS.stream().anyMatch(content::contains);
    }

    private String targetKey(ProbePlan plan) {
        if (!plan.targetConsistencyIssueId().isBlank()) return "consistency:" + plan.targetConsistencyIssueId();
        if (!plan.targetDeferredProbeId().isBlank()) return "deferred:" + plan.targetDeferredProbeId();
        if (!plan.targetClaimId().isBlank()) return "claim:" + plan.targetClaimId();
        if (!plan.targetLogicGap().isBlank()) return "logic:" + plan.targetLogicGap();
        return "stage:" + plan.objective();
    }

    private PressureLevel lowerOf(PressureLevel first, PressureLevel second) {
        return first.ordinal() <= second.ordinal() ? first : second;
    }
}
