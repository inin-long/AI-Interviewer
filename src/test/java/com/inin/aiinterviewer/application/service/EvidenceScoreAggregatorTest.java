package com.inin.aiinterviewer.application.service;

import com.inin.aiinterviewer.domain.enums.ConsistencyIssueStatus;
import com.inin.aiinterviewer.domain.enums.ConsistencyIssueType;
import com.inin.aiinterviewer.domain.enums.EvidenceSignal;
import com.inin.aiinterviewer.domain.model.ClaimLedger;
import com.inin.aiinterviewer.domain.model.ConsistencyIssue;
import com.inin.aiinterviewer.domain.model.EvaluationEvidence;
import com.inin.aiinterviewer.domain.model.EvidenceLedger;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EvidenceScoreAggregatorTest {

    private final EvidenceScoreAggregator aggregator = new EvidenceScoreAggregator();

    @Test
    void scoresOnlyDimensionsBackedByDecisiveEvidence() {
        EvaluationEvidence positive = evidence(
                "e-positive", "SYSTEM_DESIGN", EvidenceSignal.POSITIVE,
                0.9, 0.8, List.of("claim-1"));
        EvaluationEvidence insufficient = evidence(
                "e-insufficient", "SYSTEM_DESIGN", EvidenceSignal.INSUFFICIENT,
                0.7, 0.7, List.of("claim-1"));

        var payload = aggregator.aggregate(
                new EvidenceLedger(List.of(positive, insufficient)),
                ClaimLedger.empty(), "基于证据的评价");

        assertThat(payload.systemDesignScore()).isEqualTo(86);
        assertThat(payload.technicalScore()).isEqualTo(86);
        assertThat(payload.problemSolvingScore()).isEqualTo(50);
        assertThat(payload.overallScore()).isEqualTo(86);
        assertThat(payload.overallScored()).isTrue();
        assertThat(payload.scoreEvidence().get(EvidenceScoreAggregator.SYSTEM_DESIGN))
                .satisfies(trace -> {
                    assertThat(trace.scored()).isTrue();
                    assertThat(trace.evidenceIds()).containsExactly("e-positive", "e-insufficient");
                    assertThat(trace.messageIds()).containsExactly(101L);
                    assertThat(trace.claimIds()).containsExactly("claim-1");
                    assertThat(trace.confidence()).isLessThan(0.45);
                });
        assertThat(payload.scoreEvidence().get(EvidenceScoreAggregator.PROBLEM_SOLVING).scored())
                .isFalse();
    }

    @Test
    void keepsInsufficientEvidenceSeparateFromLowAbility() {
        var payload = aggregator.aggregate(
                new EvidenceLedger(List.of(evidence(
                        "e-only-insufficient", "COMMUNICATION", EvidenceSignal.INSUFFICIENT,
                        0.8, 0.9, List.of()))),
                ClaimLedger.empty(), "沟通证据不足");

        assertThat(payload.overallScored()).isFalse();
        assertThat(payload.overallScore()).isEqualTo(50);
        assertThat(payload.communicationScore()).isEqualTo(50);
        assertThat(payload.scoreEvidence().get(EvidenceScoreAggregator.COMMUNICATION))
                .satisfies(trace -> {
                    assertThat(trace.scored()).isFalse();
                    assertThat(trace.evidenceIds()).containsExactly("e-only-insufficient");
                    assertThat(trace.rationale()).contains("不作能力强弱结论");
                });
    }

    @Test
    void doesNotScoreUnlinkedEvidenceAsACompetencyJudgment() {
        var payload = aggregator.aggregate(
                new EvidenceLedger(List.of(evidence(
                        "e-unlinked", "SYSTEM_DESIGN", EvidenceSignal.POSITIVE,
                        0.9, 0.9, List.of()))),
                ClaimLedger.empty(), "缺少主张关联");

        assertThat(payload.overallScored()).isFalse();
        assertThat(payload.scoreEvidence().get(EvidenceScoreAggregator.SYSTEM_DESIGN))
                .satisfies(trace -> {
                    assertThat(trace.scored()).isFalse();
                    assertThat(trace.rationale()).contains("缺少主张关联");
                });
    }

    @Test
    void appliesConfirmedConflictOnlyThroughRelatedEvidence() {
        EvaluationEvidence evidence = evidence(
                "e-conflict", "PROBLEM_SOLVING", EvidenceSignal.POSITIVE,
                0.8, 0.8, List.of("claim-conflict"));
        ConsistencyIssue conflict = new ConsistencyIssue(
                "issue-1", 1, ConsistencyIssueType.FACT_CONFLICT,
                ConsistencyIssueStatus.CONFIRMED_CONFLICT, "关键事实已确认冲突",
                List.of("claim-conflict"), 102L, "请澄清", "解释未能消除冲突",
                LocalDateTime.now(), LocalDateTime.now());

        var withoutConflict = aggregator.aggregate(
                new EvidenceLedger(List.of(evidence)), ClaimLedger.empty(), "评价");
        var withConflict = aggregator.aggregate(
                new EvidenceLedger(List.of(evidence)), new ClaimLedger(List.of(), List.of(conflict)), "评价");

        assertThat(withConflict.problemSolvingScore())
                .isEqualTo(withoutConflict.problemSolvingScore() - 5);
        assertThat(withConflict.scoreEvidence().get(EvidenceScoreAggregator.PROBLEM_SOLVING)
                .rationale()).contains("已确认矛盾");
    }

    private EvaluationEvidence evidence(
            String id,
            String competency,
            EvidenceSignal signal,
            double strength,
            double confidence,
            List<String> claimIds
    ) {
        return new EvaluationEvidence(
                id, 1, 101, competency, signal, strength, confidence,
                "可追溯的面试证据", claimIds, LocalDateTime.now());
    }
}
