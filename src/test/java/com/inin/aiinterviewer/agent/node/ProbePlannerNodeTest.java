package com.inin.aiinterviewer.agent.node;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.inin.aiinterviewer.agent.model.AgentAction;
import com.inin.aiinterviewer.agent.model.AgentDecision;
import com.inin.aiinterviewer.agent.model.ClaimExtractionResult;
import com.inin.aiinterviewer.agent.model.LogicChainResult;
import com.inin.aiinterviewer.agent.model.ProbePlan;
import com.inin.aiinterviewer.agent.state.InterviewGraphState;
import com.inin.aiinterviewer.domain.enums.ClaimStatus;
import com.inin.aiinterviewer.domain.enums.ClaimType;
import com.inin.aiinterviewer.domain.enums.InterviewStage;
import com.inin.aiinterviewer.domain.enums.ProbeStrategy;
import com.inin.aiinterviewer.domain.model.InterviewClaim;
import com.inin.aiinterviewer.domain.model.DeferredProbe;
import com.inin.aiinterviewer.domain.model.LogicGap;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ProbePlannerNodeTest {

    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
    private final ProbePlannerNode planner = new ProbePlannerNode(objectMapper);

    @Test
    void selectsTheHighestPriorityPendingClaimAndItsMissingEvidence() throws Exception {
        InterviewClaim ownership = claim(
                "claim-owner", ClaimType.OWNERSHIP, "负责订单核心链路", 0.7, 0.8, List.of("职责边界"));
        InterviewClaim metric = claim(
                "claim-metric", ClaimType.METRIC, "将 P99 延迟降低 40%", 1.0, 0.45,
                List.of("监控数据", "统计区间"));
        InterviewGraphState state = state(Map.of(
                InterviewGraphState.CLAIM_LEDGER_CONTEXT,
                objectMapper.writeValueAsString(List.of(ownership, metric)),
                InterviewGraphState.DECISION,
                new AgentDecision(AgentAction.FOLLOW_UP, null, "继续验证")));

        ProbePlan plan = plan(state);

        assertThat(plan.targetClaimId()).isEqualTo("claim-metric");
        assertThat(plan.strategy()).isEqualTo(ProbeStrategy.VERIFY_DATA_SOURCE);
        assertThat(plan.objective()).contains("P99 延迟降低 40%");
        assertThat(plan.expectedEvidence()).containsExactly("监控数据", "统计区间");
        assertThat(plan.reason()).contains("1.00", "0.45", "UNVERIFIED");
    }

    @Test
    void createsAStageOpeningPlanAfterLegalTransition() throws Exception {
        InterviewGraphState state = state(Map.of(
                InterviewGraphState.STAGE, InterviewStage.RESUME_REVIEW,
                InterviewGraphState.DECISION,
                new AgentDecision(AgentAction.NEXT_STAGE, InterviewStage.RESUME_REVIEW, "进入下一阶段")));

        ProbePlan plan = plan(state);

        assertThat(plan.targetsClaim()).isFalse();
        assertThat(plan.objective()).contains("RESUME_REVIEW");
    }

    @Test
    void fallsBackToTheCurrentAtomicClaimWhenLedgerContextIsUnavailable() throws Exception {
        ClaimExtractionResult extraction = new ClaimExtractionResult(List.of(
                new ClaimExtractionResult.ClaimCandidate(
                        ClaimType.DECISION, "选择 Outbox 而不是分布式事务", 0.95, 0.65,
                        List.of("备选方案", "取舍依据"))));
        InterviewGraphState state = state(Map.of(
                InterviewGraphState.CLAIM_LEDGER_CONTEXT, "invalid-json",
                InterviewGraphState.CLAIM_EXTRACTION, extraction,
                InterviewGraphState.DECISION,
                new AgentDecision(AgentAction.FOLLOW_UP, null, "继续验证")));

        ProbePlan plan = plan(state);

        assertThat(plan.targetClaimId()).isEqualTo("current-answer");
        assertThat(plan.strategy()).isEqualTo(ProbeStrategy.ASK_TRADE_OFF);
        assertThat(plan.objective()).contains("Outbox");
    }

    @Test
    void prioritizesASevereLogicGapOverARegularPendingClaim() throws Exception {
        InterviewClaim claim = claim(
                "claim-logic", ClaimType.RESULT, "吞吐量提升 30%", 0.8, 0.6, List.of());
        LogicChainResult logic = new LogicChainResult(
                List.of(), "", List.of(), "", "", List.of(), "吞吐量提升 30%", "", "",
                List.of(new LogicGap(
                        com.inin.aiinterviewer.domain.enums.LogicGapType.MISSING_BASELINE,
                        "没有提供优化前吞吐量基线", 0.9, List.of("claim-logic"))),
                false, false, "");
        InterviewGraphState state = state(Map.of(
                InterviewGraphState.CLAIM_LEDGER_CONTEXT,
                objectMapper.writeValueAsString(List.of(claim)),
                InterviewGraphState.LOGIC_CHAIN_RESULT, logic,
                InterviewGraphState.DECISION,
                new AgentDecision(AgentAction.FOLLOW_UP, null, "继续验证")));

        ProbePlan plan = plan(state);

        assertThat(plan.targetClaimId()).isEqualTo("claim-logic");
        assertThat(plan.targetLogicGap()).isEqualTo("MISSING_BASELINE");
        assertThat(plan.strategy()).isEqualTo(ProbeStrategy.REQUEST_BASELINE);
        assertThat(plan.objective()).contains("吞吐量基线");
    }

    @Test
    void consumesOnlyDeferredProbesDueInTheCurrentStage() throws Exception {
        InterviewClaim decision = claim(
                "claim-decision", ClaimType.DECISION, "选择 Outbox 保证最终一致性",
                0.95, 0.6, List.of("备选方案"));
        DeferredProbe future = new DeferredProbe(
                "probe-future", 20, decision.id(), InterviewStage.SYSTEM_DESIGN,
                ProbeStrategy.ASK_TRADE_OFF, "在系统设计阶段验证方案取舍", false,
                LocalDateTime.now(), LocalDateTime.now());
        DeferredProbe due = new DeferredProbe(
                "probe-due", 20, decision.id(), InterviewStage.PROJECT_EXPERIENCE,
                ProbeStrategy.CROSS_CHECK_HISTORY, "交叉验证历史主张", false,
                LocalDateTime.now(), LocalDateTime.now());
        InterviewGraphState state = state(Map.of(
                InterviewGraphState.CLAIM_LEDGER_CONTEXT,
                objectMapper.writeValueAsString(List.of(decision)),
                InterviewGraphState.DEFERRED_PROBES, List.of(future, due),
                InterviewGraphState.DECISION,
                new AgentDecision(AgentAction.FOLLOW_UP, null, "继续验证")));

        ProbePlan plan = plan(state);

        assertThat(plan.targetDeferredProbeId()).isEqualTo("probe-due");
        assertThat(plan.targetClaimId()).isEqualTo("claim-decision");
        assertThat(plan.strategy()).isEqualTo(ProbeStrategy.CROSS_CHECK_HISTORY);
        assertThat(plan.objective()).contains("Outbox");
    }

    @Test
    void ignoresCompletedDeferredProbe() throws Exception {
        DeferredProbe completed = new DeferredProbe(
                "probe-completed", 20, "claim-completed", InterviewStage.PROJECT_EXPERIENCE,
                ProbeStrategy.ASK_TRADE_OFF, "已经完成", true,
                LocalDateTime.now(), LocalDateTime.now());
        InterviewGraphState state = state(Map.of(
                InterviewGraphState.DEFERRED_PROBES, List.of(completed),
                InterviewGraphState.DECISION,
                new AgentDecision(AgentAction.NEXT_STAGE, InterviewStage.PROJECT_EXPERIENCE, "进入阶段")));

        ProbePlan plan = plan(state);

        assertThat(plan.targetDeferredProbeId()).isBlank();
        assertThat(plan.targetsDeferredProbe()).isFalse();
    }

    private ProbePlan plan(InterviewGraphState state) throws Exception {
        return (ProbePlan) planner.apply(state).get(InterviewGraphState.PROBE_PLAN);
    }

    private InterviewGraphState state(Map<String, Object> overrides) {
        java.util.HashMap<String, Object> values = new java.util.HashMap<>();
        values.put(InterviewGraphState.STAGE, InterviewStage.PROJECT_EXPERIENCE);
        values.putAll(overrides);
        return new InterviewGraphState(values);
    }

    private InterviewClaim claim(
            String id,
            ClaimType type,
            String content,
            double importance,
            double credibility,
            List<String> missingEvidence
    ) {
        return new InterviewClaim(
                id, 10, 20, type, content, importance, credibility, ClaimStatus.UNVERIFIED,
                missingEvidence, List.of(), List.of(), LocalDateTime.now(), LocalDateTime.now());
    }
}
