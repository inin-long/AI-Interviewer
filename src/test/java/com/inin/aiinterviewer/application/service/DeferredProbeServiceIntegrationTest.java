package com.inin.aiinterviewer.application.service;

import com.inin.aiinterviewer.agent.model.ClaimExtractionResult;
import com.inin.aiinterviewer.application.dto.SaveInterviewPlanCommand;
import com.inin.aiinterviewer.application.exception.BusinessException;
import com.inin.aiinterviewer.domain.enums.ClaimType;
import com.inin.aiinterviewer.domain.enums.InterviewDifficulty;
import com.inin.aiinterviewer.domain.enums.InterviewStage;
import com.inin.aiinterviewer.domain.enums.ProbeStrategy;
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
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class DeferredProbeServiceIntegrationTest {

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
    @Autowired private DeferredProbeService deferredProbeService;

    @Test
    void schedulesFutureStageProbesIdempotentlyAndPersistsTheirLifecycle() {
        var owner = userService.register("deferred-owner", "Deferred Owner", "safe-password");
        var other = userService.register("deferred-other", "Deferred Other", "safe-password");
        var plan = planService.create(owner.id(), new SaveInterviewPlanCommand(
                "延迟验证", "Java 工程师", "核心服务开发", InterviewDifficulty.SENIOR,
                45, 8, null, Map.of(), List.of(
                "INTRODUCTION", "PROJECT_EXPERIENCE", "TECHNICAL_DEEP_DIVE",
                "SYSTEM_DESIGN", "BEHAVIORAL", "SUMMARY")));
        var session = sessionService.create(owner.id(), plan.id());
        sessionService.appendUserAnswer(owner.id(), session.id(), "我主导订单系统并决定使用 Outbox，吞吐提升了 40%。");
        claimLedgerService.recordLatestAnswer(owner.id(), session.id(), new ClaimExtractionResult(List.of(
                candidate(ClaimType.OWNERSHIP, "我主导订单系统", 0.90, "职责边界"),
                candidate(ClaimType.DECISION, "我决定使用 Outbox", 0.95, "备选方案"),
                candidate(ClaimType.METRIC, "吞吐提升了 40%", 0.85, "监控数据"),
                candidate(ClaimType.FACT, "系统使用 Java", 0.99, "版本信息"),
                candidate(ClaimType.OPINION, "我认为 Outbox 最好", 0.70, "反例"))));

        var scheduled = deferredProbeService.scheduleLatestAnswer(
                owner.id(), session.id(), plan, InterviewStage.INTRODUCTION, false);
        var repeated = deferredProbeService.scheduleLatestAnswer(
                owner.id(), session.id(), plan, InterviewStage.INTRODUCTION, false);

        assertThat(scheduled).hasSize(3);
        assertThat(repeated).hasSize(3);
        assertThat(scheduled).anySatisfy(probe -> {
            assertThat(probe.preferredStage()).isEqualTo(InterviewStage.PROJECT_EXPERIENCE);
            assertThat(probe.strategy()).isEqualTo(ProbeStrategy.VERIFY_PERSONAL_OWNERSHIP);
        }).anySatisfy(probe -> {
            assertThat(probe.preferredStage()).isEqualTo(InterviewStage.TECHNICAL_DEEP_DIVE);
            assertThat(probe.strategy()).isEqualTo(ProbeStrategy.REQUEST_METRIC_BREAKDOWN);
        }).anySatisfy(probe -> {
            assertThat(probe.preferredStage()).isEqualTo(InterviewStage.SYSTEM_DESIGN);
            assertThat(probe.strategy()).isEqualTo(ProbeStrategy.ASK_TRADE_OFF);
        });
        assertThat(scheduled).noneMatch(probe -> probe.dueAt(InterviewStage.INTRODUCTION));
        assertThat(scheduled).anyMatch(probe -> probe.dueAt(InterviewStage.PROJECT_EXPERIENCE));

        var withConsistencyRetry = deferredProbeService.scheduleLatestAnswer(
                owner.id(), session.id(), plan, InterviewStage.INTRODUCTION, true);
        assertThat(withConsistencyRetry).hasSize(4)
                .anySatisfy(probe -> {
                    assertThat(probe.preferredStage()).isEqualTo(InterviewStage.PROJECT_EXPERIENCE);
                    assertThat(probe.strategy()).isEqualTo(ProbeStrategy.CROSS_CHECK_HISTORY);
                });

        sessionService.updateDeferredProbes(owner.id(), session.id(), withConsistencyRetry);
        assertThat(sessionService.loadLatestState(owner.id(), session.id())).get()
                .satisfies(state -> assertThat(state.deferredProbes()).hasSize(4));

        String completedId = withConsistencyRetry.stream()
                .filter(probe -> probe.strategy() == ProbeStrategy.ASK_TRADE_OFF)
                .findFirst().orElseThrow().id();
        var completed = deferredProbeService.markCompleted(owner.id(), session.id(), completedId);
        assertThat(deferredProbeService.markCompleted(owner.id(), session.id(), completedId))
                .isEqualTo(completed);
        assertThat(completed).filteredOn(probe -> probe.id().equals(completedId))
                .singleElement().satisfies(probe -> assertThat(probe.completed()).isTrue());
        assertThat(deferredProbeService.pending(owner.id(), session.id())).hasSize(3);

        sessionService.updateDeferredProbes(owner.id(), session.id(), completed);
        sessionService.saveAssistantOutput(owner.id(), session.id(), "请继续说明。", null, false);
        sessionService.appendUserAnswer(owner.id(), session.id(), "这是补充回答。");
        assertThat(sessionService.loadLatestState(owner.id(), session.id())).get()
                .satisfies(state -> assertThat(state.deferredProbes()).isEqualTo(completed));

        assertThatThrownBy(() -> deferredProbeService.all(other.id(), session.id()))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> deferredProbeService.markCompleted(
                owner.id(), session.id(), "missing-probe"))
                .isInstanceOf(BusinessException.class);
        assertThatCode(() -> sessionService.delete(owner.id(), session.id())).doesNotThrowAnyException();
    }

    private ClaimExtractionResult.ClaimCandidate candidate(
            ClaimType type,
            String content,
            double importance,
            String missingEvidence
    ) {
        return new ClaimExtractionResult.ClaimCandidate(
                type, content, importance, 0.65, List.of(missingEvidence));
    }
}
