package com.inin.aiinterviewer.agent.node;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inin.aiinterviewer.agent.model.AgentAction;
import com.inin.aiinterviewer.agent.model.ProbePlan;
import com.inin.aiinterviewer.agent.state.InterviewGraphState;
import com.inin.aiinterviewer.domain.enums.ClaimType;
import com.inin.aiinterviewer.domain.enums.LogicGapType;
import com.inin.aiinterviewer.domain.enums.PressureLevel;
import com.inin.aiinterviewer.domain.enums.ProbeStrategy;
import com.inin.aiinterviewer.domain.model.InterviewClaim;
import com.inin.aiinterviewer.domain.model.LogicGap;
import org.bsc.langgraph4j.action.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Component
public class ProbePlannerNode implements NodeAction<InterviewGraphState> {

    private static final Logger log = LoggerFactory.getLogger(ProbePlannerNode.class);

    private final ObjectMapper objectMapper;

    public ProbePlannerNode(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Map<String, Object> apply(InterviewGraphState state) {
        ProbePlan plan;
        var consistencyIssue = state.consistencyCheckResult().issues().stream()
                .filter(issue -> !issue.issueId().isBlank())
                .findFirst();
        if (consistencyIssue.isPresent()) {
            plan = planForConsistency(consistencyIssue.get());
        } else if (!state.data().containsKey(InterviewGraphState.DECISION)
                || state.decision().action() == AgentAction.NEXT_STAGE) {
            plan = ProbePlan.stageOpening("验证 " + state.stage().name() + " 阶段的岗位核心能力");
        } else {
            var importantGap = state.logicChainResult().gaps().stream()
                    .filter(gap -> gap.severity() >= 0.65)
                    .max(Comparator.comparingDouble(LogicGap::severity));
            plan = importantGap.map(this::planForGap).orElseGet(() -> pendingClaims(state).stream()
                    .max(Comparator.comparingDouble(this::priority))
                    .map(this::planFor)
                    .orElseGet(() -> fallback(state)));
        }
        return Map.of(InterviewGraphState.PROBE_PLAN, plan);
    }

    private ProbePlan planForConsistency(
            com.inin.aiinterviewer.agent.model.ConsistencyCheckResult.IssueCandidate issue
    ) {
        String claimId = issue.relatedClaimIds().isEmpty() ? "" : issue.relatedClaimIds().getFirst();
        return new ProbePlan(
                claimId, "", issue.issueId(), issue.clarificationQuestion(),
                ProbeStrategy.CROSS_CHECK_HISTORY, PressureLevel.STANDARD,
                "发现需要候选人澄清的潜在陈述差异：“" + abbreviate(issue.description()) + "”",
                List.of("两次陈述的适用范围", "各自发生的时间或条件", "职责与决策边界"), false);
    }

    private List<InterviewClaim> pendingClaims(InterviewGraphState state) {
        if (state.claimLedgerContext().isBlank()) return List.of();
        try {
            List<InterviewClaim> claims = objectMapper.readValue(
                    state.claimLedgerContext(), new TypeReference<>() { });
            return claims == null ? List.of() : claims;
        } catch (Exception exception) {
            log.warn("Ignoring invalid claim ledger context while planning a probe");
            return List.of();
        }
    }

    private double priority(InterviewClaim claim) {
        double credibilityGap = 1.25 - (claim.credibility() * 0.75);
        double evidenceGap = claim.missingEvidence().isEmpty() ? 0.8 : 1.0;
        return claim.importance() * credibilityGap * evidenceGap;
    }

    private ProbePlan planFor(InterviewClaim claim) {
        ProbeStrategy strategy = strategy(claim.type(), claim.missingEvidence());
        List<String> expected = claim.missingEvidence().isEmpty()
                ? defaultEvidence(strategy)
                : claim.missingEvidence();
        String objective = "验证主张“%s”是否有充分、可归因的事实依据".formatted(abbreviate(claim.content()));
        String reason = "该主张重要度 %.2f、当前可信度 %.2f，且仍处于 %s 状态"
                .formatted(claim.importance(), claim.credibility(), claim.status());
        return new ProbePlan(
                claim.id(), objective, strategy, PressureLevel.STANDARD,
                reason, expected, false);
    }

    private ProbePlan planForGap(LogicGap gap) {
        ProbeStrategy strategy = strategy(gap.type());
        String claimId = gap.relatedClaimIds().isEmpty() ? "current-answer" : gap.relatedClaimIds().getFirst();
        return new ProbePlan(
                claimId, gap.type().name(), "补全逻辑缺口：“" + abbreviate(gap.description()) + "”",
                strategy, PressureLevel.STANDARD,
                "该逻辑缺口严重度为 %.2f，需要在继续覆盖新主题前验证".formatted(gap.severity()),
                defaultEvidence(strategy), false);
    }

    private ProbePlan fallback(InterviewGraphState state) {
        var claims = state.claimExtraction().claims();
        if (!claims.isEmpty()) {
            var claim = claims.stream()
                    .max(Comparator.comparingDouble(candidate -> candidate.importance()
                            * (1.25 - candidate.credibility() * 0.75)))
                    .orElseThrow();
            ProbeStrategy strategy = strategy(claim.type(), claim.missingEvidence());
            return new ProbePlan(
                    "current-answer", "验证主张“%s”的事实依据".formatted(abbreviate(claim.content())),
                    strategy, PressureLevel.STANDARD, "当前回答产生了尚未落账的待验证主张",
                    claim.missingEvidence().isEmpty() ? defaultEvidence(strategy) : claim.missingEvidence(), false);
        }
        return new ProbePlan(
                "current-answer", "澄清本轮回答中的关键事实和候选人的实际行动",
                ProbeStrategy.ASK_IMPLEMENTATION_DETAIL, PressureLevel.STANDARD,
                "本轮没有提取到完整原子主张，需要先获得可验证细节",
                List.of("具体场景", "个人行动", "结果或反馈"), false);
    }

    private ProbeStrategy strategy(ClaimType type, List<String> missingEvidence) {
        String missing = String.join(" ", missingEvidence);
        if (missing.contains("基线") || missing.contains("优化前")) return ProbeStrategy.REQUEST_BASELINE;
        if (missing.contains("数据源") || missing.contains("监控") || missing.contains("测量")) {
            return ProbeStrategy.VERIFY_DATA_SOURCE;
        }
        return switch (type) {
            case METRIC, RESULT -> ProbeStrategy.REQUEST_METRIC_BREAKDOWN;
            case OWNERSHIP -> ProbeStrategy.VERIFY_PERSONAL_OWNERSHIP;
            case CAUSALITY -> ProbeStrategy.TRACE_CAUSAL_CHAIN;
            case DECISION -> ProbeStrategy.ASK_TRADE_OFF;
            case CONSTRAINT -> ProbeStrategy.INTRODUCE_CONSTRAINT;
            case FAILURE -> ProbeStrategy.INTRODUCE_FAILURE;
            case OPINION -> ProbeStrategy.CHALLENGE_ASSUMPTION;
            case FACT -> ProbeStrategy.ASK_IMPLEMENTATION_DETAIL;
        };
    }

    private ProbeStrategy strategy(LogicGapType type) {
        return switch (type) {
            case MISSING_BASELINE -> ProbeStrategy.REQUEST_BASELINE;
            case MISSING_MECHANISM, CAUSALITY_JUMP -> ProbeStrategy.TRACE_CAUSAL_CHAIN;
            case MISSING_EXECUTION_PATH -> ProbeStrategy.ASK_IMPLEMENTATION_DETAIL;
            case MISSING_ALTERNATIVES -> ProbeStrategy.ASK_ALTERNATIVE;
            case MISSING_TRADE_OFF -> ProbeStrategy.ASK_TRADE_OFF;
            case MISSING_VALIDATION, RESULT_WITHOUT_EVIDENCE -> ProbeStrategy.VERIFY_DATA_SOURCE;
            case MISSING_PERSONAL_CONTRIBUTION -> ProbeStrategy.VERIFY_PERSONAL_OWNERSHIP;
            case MISSING_FAILURE_HANDLING -> ProbeStrategy.INTRODUCE_FAILURE;
        };
    }

    private List<String> defaultEvidence(ProbeStrategy strategy) {
        return switch (strategy) {
            case REQUEST_BASELINE -> List.of("优化前基线", "优化后指标", "统计区间");
            case REQUEST_METRIC_BREAKDOWN -> List.of("指标口径", "分项贡献", "测量结果");
            case VERIFY_PERSONAL_OWNERSHIP -> List.of("个人职责", "亲自完成的行动", "协作边界");
            case VERIFY_DATA_SOURCE -> List.of("数据来源", "测量方式", "统计时间范围");
            case TRACE_CAUSAL_CHAIN -> List.of("关键行动", "中间结果", "其他变量排除方式");
            case ASK_TRADE_OFF, ASK_ALTERNATIVE -> List.of("备选方案", "选择标准", "代价与风险");
            case INTRODUCE_CONSTRAINT, INTRODUCE_FAILURE -> List.of("约束条件", "失效表现", "恢复措施");
            case CROSS_CHECK_HISTORY -> List.of("时间线", "历史说法", "差异原因");
            case CHALLENGE_ASSUMPTION -> List.of("前提假设", "反例", "适用边界");
            case REQUEST_PRIORITIZATION -> List.of("排序标准", "依赖关系", "取舍理由");
            case CLARIFY_CONCEPT, ASK_IMPLEMENTATION_DETAIL -> List.of("具体步骤", "关键细节", "验证结果");
        };
    }

    private String abbreviate(String content) {
        String compact = content == null ? "" : content.replaceAll("\\s+", " ").strip();
        return compact.length() <= 80 ? compact : compact.substring(0, 80) + "…";
    }
}
