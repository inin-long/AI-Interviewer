package com.inin.aiinterviewer.agent.support;

import com.inin.aiinterviewer.agent.model.EvidenceCollectionResult;
import com.inin.aiinterviewer.agent.model.ProbePlan;
import com.inin.aiinterviewer.application.dto.InterviewPlanDto;
import com.inin.aiinterviewer.domain.enums.EvidenceSignal;
import com.inin.aiinterviewer.domain.enums.InterviewDifficulty;
import com.inin.aiinterviewer.domain.enums.PressureLevel;
import com.inin.aiinterviewer.domain.enums.ProbeStrategy;
import com.inin.aiinterviewer.domain.model.PressureState;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PressureControllerTest {

    private final PressureController controller = new PressureController();

    @Test
    void mapsRealValidationStrategiesToAConfiguredPressureLadder() {
        var high = controller.control(
                plan(InterviewDifficulty.SENIOR, Map.of()),
                probe("claim-1", ProbeStrategy.INTRODUCE_FAILURE, "处理 Redis 故障"),
                PressureState.initial(), EvidenceCollectionResult.degraded("not_collected"));
        var capped = controller.control(
                plan(InterviewDifficulty.JUNIOR, Map.of()),
                probe("claim-1", ProbeStrategy.INTRODUCE_FAILURE, "处理 Redis 故障"),
                PressureState.initial(), EvidenceCollectionResult.degraded("not_collected"));
        var coaching = controller.control(
                plan(InterviewDifficulty.EXPERT, Map.of("interviewMode", "COACHING")),
                probe("claim-1", ProbeStrategy.INTRODUCE_CONSTRAINT, "预算缩减一半"),
                PressureState.initial(), EvidenceCollectionResult.degraded("not_collected"));

        assertThat(high.probePlan().pressureLevel()).isEqualTo(PressureLevel.HIGH_PRESSURE);
        assertThat(high.pressureState().consecutivePressureTurns()).isEqualTo(1);
        assertThat(capped.probePlan().pressureLevel()).isEqualTo(PressureLevel.STANDARD);
        assertThat(coaching.probePlan().pressureLevel()).isEqualTo(PressureLevel.CHALLENGING);
    }

    @Test
    void stopsPressureAfterSufficientTargetedEvidence() {
        ProbePlan probe = probe("claim-1", ProbeStrategy.CHALLENGE_ASSUMPTION, "排除流量下降影响");
        EvidenceCollectionResult evidence = new EvidenceCollectionResult(List.of(
                new EvidenceCollectionResult.EvidenceCandidate(
                        "PERFORMANCE", EvidenceSignal.POSITIVE, 0.9, 0.8,
                        "给出了监控数据和统计区间", List.of("claim-1"))));

        var result = controller.control(
                plan(InterviewDifficulty.SENIOR, Map.of()), probe,
                new PressureState(PressureLevel.CHALLENGING, 1, "claim:claim-1", 1,
                        false, false, false, ""), evidence);

        assertThat(result.probePlan().pressureLevel()).isEqualTo(PressureLevel.RELAXED);
        assertThat(result.pressureState().sufficientEvidence()).isTrue();
        assertThat(result.pressureState().consecutivePressureTurns()).isZero();
        assertThat(result.pressureState().reason()).contains("停止继续施压");
    }

    @Test
    void lowersRepeatedAndContinuousPressure() {
        ProbePlan probe = probe("claim-1", ProbeStrategy.INTRODUCE_FAILURE, "注入依赖故障");
        var first = controller.control(
                plan(InterviewDifficulty.EXPERT, Map.of()), probe,
                PressureState.initial(), EvidenceCollectionResult.degraded("none"));
        var second = controller.control(
                plan(InterviewDifficulty.EXPERT, Map.of()), probe,
                first.pressureState(), EvidenceCollectionResult.degraded("none"));
        var third = controller.control(
                plan(InterviewDifficulty.EXPERT, Map.of()), probe,
                second.pressureState(), EvidenceCollectionResult.degraded("none"));

        assertThat(first.pressureState().consecutivePressureTurns()).isEqualTo(1);
        assertThat(second.pressureState().consecutivePressureTurns()).isEqualTo(2);
        assertThat(third.probePlan().pressureLevel()).isEqualTo(PressureLevel.STANDARD);
        assertThat(third.pressureState().lowered()).isTrue();
        assertThat(third.pressureState().reason()).contains("无意义重复");
    }

    @Test
    void replacesAttackingLanguageWithANeutralClarification() {
        ProbePlan unsafe = probe("claim-1", ProbeStrategy.CHALLENGE_ASSUMPTION,
                "你是不是在撒谎，为什么这么无能？");

        var result = controller.control(
                plan(InterviewDifficulty.EXPERT, Map.of("pressureLevel", "HIGH_PRESSURE")),
                unsafe, PressureState.initial(), EvidenceCollectionResult.degraded("none"));

        assertThat(result.probePlan().objective()).doesNotContain("撒谎", "无能").contains("中性");
        assertThat(result.probePlan().strategy()).isEqualTo(ProbeStrategy.CLARIFY_CONCEPT);
        assertThat(result.probePlan().pressureLevel()).isEqualTo(PressureLevel.RELAXED);
        assertThat(result.pressureState().safetyAdjusted()).isTrue();
    }

    @Test
    void doesNotTreatUnrelatedEvidenceAsSufficient() {
        EvidenceCollectionResult unrelated = new EvidenceCollectionResult(List.of(
                new EvidenceCollectionResult.EvidenceCandidate(
                        "PERFORMANCE", EvidenceSignal.POSITIVE, 0.95, 0.95,
                        "属于另一个主张", List.of("claim-other"))));

        var result = controller.control(
                plan(InterviewDifficulty.SENIOR, Map.of("pressureLevel", "CHALLENGING")),
                probe("claim-1", ProbeStrategy.ASK_TRADE_OFF, "说明取舍"),
                PressureState.initial(), unrelated);

        assertThat(result.pressureState().sufficientEvidence()).isFalse();
        assertThat(result.probePlan().pressureLevel()).isEqualTo(PressureLevel.CHALLENGING);
    }

    private ProbePlan probe(String claimId, ProbeStrategy strategy, String objective) {
        return new ProbePlan(
                claimId, objective, strategy, PressureLevel.STANDARD,
                "需要验证", List.of("事实依据"), false);
    }

    private InterviewPlanDto plan(InterviewDifficulty difficulty, Map<String, Object> rules) {
        return new InterviewPlanDto(
                null, "压力控制", "Java 工程师", "核心服务开发", difficulty,
                45, 8, null, rules, List.of("PROJECT_EXPERIENCE", "SUMMARY"),
                false, null, null);
    }
}
