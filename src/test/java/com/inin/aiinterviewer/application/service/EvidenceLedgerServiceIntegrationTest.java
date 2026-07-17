package com.inin.aiinterviewer.application.service;

import com.inin.aiinterviewer.agent.model.ClaimExtractionResult;
import com.inin.aiinterviewer.agent.model.EvidenceCollectionResult;
import com.inin.aiinterviewer.application.dto.SaveInterviewPlanCommand;
import com.inin.aiinterviewer.application.exception.BusinessException;
import com.inin.aiinterviewer.domain.enums.ClaimType;
import com.inin.aiinterviewer.domain.enums.EvidenceSignal;
import com.inin.aiinterviewer.domain.enums.InterviewDifficulty;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class EvidenceLedgerServiceIntegrationTest {

    @TempDir
    static Path applicationHome;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("ai.interviewer.home", () -> applicationHome.toString());
        registry.add("task.enabled", () -> false);
    }

    @Autowired private UserService userService;
    @Autowired private InterviewPlanService planService;
    @Autowired private InterviewSessionService sessionService;
    @Autowired private ClaimLedgerService claimLedgerService;
    @Autowired private EvidenceLedgerService evidenceLedgerService;

    @Test
    void persistsIdempotentEvidenceFiltersClaimIdsAndIsolatesUsers() {
        var owner = userService.register("evidence-owner", "Evidence Owner", "safe-password");
        var other = userService.register("evidence-other", "Evidence Other", "safe-password");
        var plan = planService.create(owner.id(), new SaveInterviewPlanCommand(
                "证据账本验证", "Java 工程师", "核心服务开发", InterviewDifficulty.MEDIUM,
                30, 5, null, Map.of(), List.of("INTRODUCTION", "SUMMARY")));
        var session = sessionService.create(owner.id(), plan.id());
        sessionService.appendUserAnswer(
                owner.id(), session.id(), "我使用 Outbox 保证订单和事件最终一致。" );
        var claims = claimLedgerService.recordLatestAnswer(owner.id(), session.id(),
                new ClaimExtractionResult(List.of(new ClaimExtractionResult.ClaimCandidate(
                        ClaimType.DECISION, "使用 Outbox 保证最终一致性", 0.95, 0.8,
                        List.of("故障恢复数据")))));
        String claimId = claims.claims().getFirst().id();

        var ledger = evidenceLedgerService.recordLatestAnswer(owner.id(), session.id(), result(
                evidence("SYSTEM_DESIGN", EvidenceSignal.POSITIVE, 0.9, 0.8,
                        "能够说明 Outbox 的事务边界", List.of(claimId, "invented-claim")),
                evidence("SYSTEM_DESIGN", EvidenceSignal.POSITIVE, 0.4, 0.3,
                        "能够说明 Outbox 的事务边界", List.of()),
                evidence("FAILURE_HANDLING", EvidenceSignal.INSUFFICIENT, 0.1, 0.9,
                        "没有说明持续投递失败的恢复路径", List.of())));

        assertThat(ledger.evidence()).hasSize(2);
        assertThat(ledger.evidence()).filteredOn(value -> value.signal() == EvidenceSignal.POSITIVE)
                .singleElement().satisfies(value ->
                        assertThat(value.relatedClaimIds()).containsExactly(claimId));
        assertThat(ledger.evidence()).extracting(value -> value.signal())
                .containsExactlyInAnyOrder(EvidenceSignal.POSITIVE, EvidenceSignal.INSUFFICIENT);
        assertThat(ledger.evidence()).allSatisfy(value ->
                assertThat(value.relatedClaimIds()).containsExactly(claimId));
        assertThat(ledger.summaries().get("SYSTEM_DESIGN")).satisfies(summary -> {
            assertThat(summary.positiveStrength()).isEqualTo(0.9);
            assertThat(summary.negativeStrength()).isZero();
            assertThat(summary.confidence()).isBetween(0.0, 1.0);
        });
        assertThat(ledger.summaries().get("FAILURE_HANDLING")).satisfies(summary -> {
            assertThat(summary.positiveStrength()).isZero();
            assertThat(summary.negativeStrength()).isZero();
        });
        assertThat(evidenceLedgerService.compactSummary(owner.id(), session.id()))
                .contains("INSUFFICIENT", "messageId", claimId)
                .doesNotContain("invented-claim");

        var replaced = evidenceLedgerService.recordLatestAnswer(owner.id(), session.id(), result(
                evidence("SYSTEM_DESIGN", EvidenceSignal.NEGATIVE, 0.6, 0.7,
                        "事务边界与事件投递职责混淆", List.of(claimId))));
        assertThat(replaced.evidence()).singleElement()
                .satisfies(value -> assertThat(value.signal()).isEqualTo(EvidenceSignal.NEGATIVE));
        assertThat(evidenceLedgerService.recordLatestAnswer(
                owner.id(), session.id(), EvidenceCollectionResult.degraded("provider_unavailable")))
                .isEqualTo(replaced);
        assertThat(sessionService.updateEvidenceLedger(owner.id(), session.id(), replaced).evidenceLedger())
                .isEqualTo(replaced);
        assertThat(sessionService.loadLatestState(owner.id(), session.id())).get()
                .extracting(state -> state.evidenceLedger().evidence().size()).isEqualTo(1);

        assertThatThrownBy(() -> evidenceLedgerService.ledger(other.id(), session.id()))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> evidenceLedgerService.recordLatestAnswer(
                other.id(), session.id(), result()))
                .isInstanceOf(BusinessException.class);
    }

    private EvidenceCollectionResult result(EvidenceCollectionResult.EvidenceCandidate... values) {
        return new EvidenceCollectionResult(List.of(values));
    }

    private EvidenceCollectionResult.EvidenceCandidate evidence(
            String competency,
            EvidenceSignal signal,
            double strength,
            double confidence,
            String reason,
            List<String> claimIds
    ) {
        return new EvidenceCollectionResult.EvidenceCandidate(
                competency, signal, strength, confidence, reason, claimIds);
    }
}
