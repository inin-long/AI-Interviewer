package com.inin.aiinterviewer.application.service;

import com.inin.aiinterviewer.agent.model.ClaimExtractionResult;
import com.inin.aiinterviewer.application.dto.SaveInterviewPlanCommand;
import com.inin.aiinterviewer.application.exception.BusinessException;
import com.inin.aiinterviewer.domain.enums.ClaimType;
import com.inin.aiinterviewer.domain.enums.InterviewDifficulty;
import com.inin.aiinterviewer.domain.enums.ClaimStatus;
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
class ClaimLedgerServiceIntegrationTest {

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

    @Test
    void persistsIdempotentClaimsAndIsolatesThemByUserAndSession() {
        var owner = userService.register("claim-owner", "Claim Owner", "safe-password");
        var other = userService.register("claim-other", "Claim Other", "safe-password");
        var plan = planService.create(owner.id(), new SaveInterviewPlanCommand(
                "主张账本验证", "Java 工程师", "核心服务开发", InterviewDifficulty.MEDIUM,
                30, 5, null, Map.of(), List.of("INTRODUCTION", "SUMMARY")));
        var session = sessionService.create(owner.id(), plan.id());
        sessionService.appendUserAnswer(owner.id(), session.id(), "我负责订单链路，并将延迟降低了 40%。");

        var first = claimLedgerService.recordLatestAnswer(owner.id(), session.id(), extraction(
                new ClaimExtractionResult.ClaimCandidate(
                        ClaimType.OWNERSHIP, "负责订单链路", 0.9, 0.7, List.of("职责边界")),
                new ClaimExtractionResult.ClaimCandidate(
                        ClaimType.METRIC, "将延迟降低 40%", 0.95, 0.6, List.of("监控数据"))));

        assertThat(first.claims()).hasSize(2).allSatisfy(claim -> {
            assertThat(claim.status()).isEqualTo(ClaimStatus.UNVERIFIED);
            assertThat(claim.sourceMessageId()).isPositive();
        });
        assertThat(claimLedgerService.compactSummary(owner.id(), session.id()))
                .contains("将延迟降低 40%", "监控数据");

        var replaced = claimLedgerService.recordLatestAnswer(owner.id(), session.id(), extraction(
                new ClaimExtractionResult.ClaimCandidate(
                        ClaimType.METRIC, "将 P99 延迟降低 40%", 1.0, 0.8, List.of("监控区间"))));
        assertThat(replaced.claims()).singleElement()
                .satisfies(claim -> assertThat(claim.content()).contains("P99"));

        var afterDegradation = claimLedgerService.recordLatestAnswer(
                owner.id(), session.id(), ClaimExtractionResult.degraded("provider_unavailable"));
        assertThat(afterDegradation).isEqualTo(replaced);
        assertThat(sessionService.updateClaimLedger(owner.id(), session.id(), replaced).claimLedger())
                .isEqualTo(replaced);
        assertThat(sessionService.loadLatestState(owner.id(), session.id())).get()
                .extracting(state -> state.claimLedger().claims().size()).isEqualTo(1);

        assertThatThrownBy(() -> claimLedgerService.ledger(other.id(), session.id()))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> claimLedgerService.recordLatestAnswer(
                other.id(), session.id(), extraction()))
                .isInstanceOf(BusinessException.class);
    }

    private ClaimExtractionResult extraction(ClaimExtractionResult.ClaimCandidate... claims) {
        return new ClaimExtractionResult(List.of(claims));
    }
}
