package com.inin.aiinterviewer.application.service;

import com.inin.aiinterviewer.agent.model.EvidenceCollectionResult;
import com.inin.aiinterviewer.domain.enums.EvidenceSignal;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CollaborationEvidenceCollectorTest {

    private final CollaborationEvidenceCollector collector = new CollaborationEvidenceCollector();

    @Test
    void recordsAllExplicitCollaborationBehaviorsAsTraceableEvidence() {
        String answer = "我想确认这里指的是发布窗口吗？目前信息不足，我会修正刚才的说法。"
                + "这个质疑有道理，我会考虑这个反对意见。我们可以一起先对齐约束并共同推进。";
        EvidenceCollectionResult result = collector.enrich(answer, baseEvidence());

        assertThat(result.evidence()).extracting(EvidenceCollectionResult.EvidenceCandidate::reason)
                .anyMatch(reason -> reason.contains("ACTIVE_CLARIFICATION"))
                .anyMatch(reason -> reason.contains("ACKNOWLEDGES_UNCERTAINTY"))
                .anyMatch(reason -> reason.contains("REVISES_VIEW"))
                .anyMatch(reason -> reason.contains("INTEGRATES_OPPOSITION"))
                .anyMatch(reason -> reason.contains("JOINT_PROBLEM_SOLVING"));
        assertThat(result.evidence().stream()
                .filter(evidence -> evidence.competencyCode().equals(
                        CollaborationEvidenceCollector.COMPETENCY_CODE)))
                .allMatch(evidence -> evidence.signal() == EvidenceSignal.POSITIVE);
    }

    @Test
    void distinguishesEvidenceBasedDissentFromUncriticalAgreement() {
        EvidenceCollectionResult result = collector.enrich(
                "我不同意这个结论，因为监控数据不支持；但如果你说什么都对，我无条件同意。",
                baseEvidence());

        assertThat(result.evidence())
                .anySatisfy(evidence -> {
                    assertThat(evidence.reason()).contains("EVIDENCE_BASED_DISSENT");
                    assertThat(evidence.signal()).isEqualTo(EvidenceSignal.POSITIVE);
                })
                .anySatisfy(evidence -> {
                    assertThat(evidence.reason()).contains("UNCRITICAL_AGREEMENT");
                    assertThat(evidence.signal()).isEqualTo(EvidenceSignal.NEGATIVE);
                });
    }

    private EvidenceCollectionResult baseEvidence() {
        return new EvidenceCollectionResult(List.of(new EvidenceCollectionResult.EvidenceCandidate(
                "PROBLEM_SOLVING", EvidenceSignal.POSITIVE, 0.8, 0.7,
                "能够解释技术决策", List.of("claim-1"))));
    }
}
