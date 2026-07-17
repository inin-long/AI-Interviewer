package com.inin.aiinterviewer.domain.model;

import com.inin.aiinterviewer.agent.model.EvidenceCollectionResult;
import com.inin.aiinterviewer.domain.enums.EvidenceSignal;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InterviewCoverageTest {

    @Test
    void initializesFromDomainPackAndAccumulatesEvidence() {
        DomainPack pack = new DomainPack(
                "java-backend", "JAVA_BACKEND", "", "1.0", "Java 后端",
                List.of(new DomainPack.CompetencyDefinition(
                        "SYSTEM_DESIGN", "系统设计", "设计可用系统", 0.9, List.of("取舍"))),
                List.of(), List.of(), List.of(), List.of(), List.of());
        InterviewCoverage initial = InterviewCoverage.fromDomainPack(pack);

        InterviewCoverage updated = initial.update(new EvidenceCollectionResult(List.of(
                evidence(EvidenceSignal.POSITIVE, 0.9),
                evidence(EvidenceSignal.POSITIVE, 0.8),
                evidence(EvidenceSignal.POSITIVE, 0.85))));

        var competency = updated.competencies().get("SYSTEM_DESIGN");
        assertThat(competency.importance()).isEqualTo(0.9);
        assertThat(competency.evidenceCount()).isEqualTo(3);
        assertThat(competency.confidence()).isEqualTo(0.85);
        assertThat(competency.coverage()).isEqualTo(0.85);
        assertThat(competency.needsVerification()).isFalse();
    }

    @Test
    void keepsInsufficientEvidenceOpenAndIgnoresInvalidUpdates() {
        InterviewCoverage initial = new InterviewCoverage(java.util.Map.of(
                "SYSTEM_DESIGN", new InterviewCoverage.CompetencyCoverage(
                        0.9, 0, 0, 0, true)));
        InterviewCoverage insufficient = initial.update(new EvidenceCollectionResult(List.of(
                evidence(EvidenceSignal.INSUFFICIENT, 0.9),
                new EvidenceCollectionResult.EvidenceCandidate(
                        "", EvidenceSignal.POSITIVE, 1, 1, "缺少能力编码", List.of()))));

        assertThat(insufficient.competencies()).containsOnlyKeys("SYSTEM_DESIGN");
        assertThat(insufficient.competencies().get("SYSTEM_DESIGN").needsVerification()).isTrue();
        assertThat(insufficient.update(EvidenceCollectionResult.degraded("timeout")))
                .isEqualTo(insufficient);
    }

    private EvidenceCollectionResult.EvidenceCandidate evidence(
            EvidenceSignal signal,
            double confidence
    ) {
        return new EvidenceCollectionResult.EvidenceCandidate(
                "SYSTEM_DESIGN", signal, 0.8, confidence,
                "候选人给出可验证的系统设计依据", List.of("claim-1"));
    }
}
