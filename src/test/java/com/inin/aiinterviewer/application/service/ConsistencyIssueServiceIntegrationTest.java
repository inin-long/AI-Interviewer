package com.inin.aiinterviewer.application.service;

import com.inin.aiinterviewer.agent.model.ClaimExtractionResult;
import com.inin.aiinterviewer.agent.model.ConsistencyCheckResult;
import com.inin.aiinterviewer.application.dto.SaveInterviewPlanCommand;
import com.inin.aiinterviewer.application.exception.BusinessException;
import com.inin.aiinterviewer.domain.enums.ClaimType;
import com.inin.aiinterviewer.domain.enums.ConsistencyIssueStatus;
import com.inin.aiinterviewer.domain.enums.ConsistencyIssueType;
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
class ConsistencyIssueServiceIntegrationTest {

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
    @Autowired private ConsistencyIssueService consistencyIssueService;

    @Test
    void detectsClarifiesAndResolvesPotentialConflictWithoutPersonalityJudgment() {
        var owner = userService.register("consistency-owner", "Consistency Owner", "safe-password");
        var other = userService.register("consistency-other", "Consistency Other", "safe-password");
        var plan = planService.create(owner.id(), new SaveInterviewPlanCommand(
                "一致性验证", "Java 工程师", "核心服务开发", InterviewDifficulty.MEDIUM,
                30, 5, null, Map.of(), List.of("PROJECT_EXPERIENCE", "SUMMARY")));
        var session = sessionService.create(owner.id(), plan.id());

        sessionService.appendUserAnswer(owner.id(), session.id(), "我主导了订单系统技术方案设计。");
        var firstLedger = claimLedgerService.recordLatestAnswer(owner.id(), session.id(), extraction(
                "我主导了订单系统技术方案设计"));
        String firstClaimId = firstLedger.claims().stream()
                .filter(claim -> claim.content().contains("主导了订单系统"))
                .findFirst().orElseThrow().id();
        assertThat(consistencyIssueService.prepareContext(owner.id(), session.id()).runRequested()).isFalse();

        sessionService.appendUserAnswer(owner.id(), session.id(), "架构选型主要由架构师决定。");
        var secondLedger = claimLedgerService.recordLatestAnswer(owner.id(), session.id(), extraction(
                "架构选型主要由架构师决定"));
        String secondClaimId = secondLedger.claims().stream()
                .filter(claim -> claim.content().contains("架构师决定"))
                .findFirst().orElseThrow().id();
        assertThat(consistencyIssueService.prepareContext(owner.id(), session.id())).satisfies(context -> {
            assertThat(context.runRequested()).isTrue();
            assertThat(context.reason()).isEqualTo("related_claim_topic");
            assertThat(context.currentClaims()).extracting(claim -> claim.id()).containsExactly(secondClaimId);
            assertThat(context.historicalClaims()).extracting(claim -> claim.id()).contains(firstClaimId);
        });

        var applied = consistencyIssueService.apply(owner.id(), session.id(), check(
                issue(firstClaimId, secondClaimId, "两次陈述对架构决策的职责范围不同",
                        "请说明你与架构师分别负责哪些架构决策？")));
        assertThat(applied.result().requiresClarification()).isTrue();
        assertThat(applied.ledger().issues()).singleElement().satisfies(issue -> {
            assertThat(issue.status()).isEqualTo(ConsistencyIssueStatus.POTENTIAL);
            assertThat(issue.description()).doesNotContain("撒谎", "不诚实");
        });
        String issueId = applied.ledger().issues().getFirst().id();

        consistencyIssueService.apply(owner.id(), session.id(), new ConsistencyCheckResult(
                List.of(), List.of(new ConsistencyCheckResult.ResolutionCandidate(
                issueId, ConsistencyIssueStatus.CONFIRMED_CONFLICT,
                "尚未经过澄清就确认冲突", 0.9))));
        assertThat(consistencyIssueService.ledger(owner.id(), session.id()).issues().getFirst().status())
                .isEqualTo(ConsistencyIssueStatus.POTENTIAL);

        sessionService.saveAssistantOutput(owner.id(), session.id(),
                "请说明你与架构师分别负责哪些架构决策？", null, false);
        var clarified = consistencyIssueService.markClarificationAsked(owner.id(), session.id(), issueId);
        assertThat(clarified.issues()).singleElement().satisfies(issue -> {
            assertThat(issue.status()).isEqualTo(ConsistencyIssueStatus.CLARIFIED);
            assertThat(issue.clarificationMessageId()).isPositive();
        });

        sessionService.appendUserAnswer(owner.id(), session.id(),
                "我主导接口与数据模型设计，架构师负责公司级技术栈选型。");
        claimLedgerService.recordLatestAnswer(owner.id(), session.id(), extraction(
                "我负责接口与数据模型设计，架构师负责技术栈选型"));
        assertThat(consistencyIssueService.prepareContext(owner.id(), session.id()).reason())
                .isEqualTo("clarification_answered");
        var resolved = consistencyIssueService.apply(owner.id(), session.id(),
                new ConsistencyCheckResult(List.of(), List.of(
                        new ConsistencyCheckResult.ResolutionCandidate(
                                issueId, ConsistencyIssueStatus.RESOLVED,
                                "候选人澄清了方案细化与总体技术栈选型的职责边界，两次陈述不冲突。", 0.92))));
        assertThat(resolved.ledger().issues()).singleElement().satisfies(issue -> {
            assertThat(issue.status()).isEqualTo(ConsistencyIssueStatus.RESOLVED);
            assertThat(issue.resolution()).contains("职责边界", "不冲突");
        });
        assertThat(claimLedgerService.ledger(owner.id(), session.id()).issues())
                .singleElement().satisfies(issue ->
                        assertThat(issue.status()).isEqualTo(ConsistencyIssueStatus.RESOLVED));

        assertThatThrownBy(() -> consistencyIssueService.ledger(other.id(), session.id()))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> consistencyIssueService.apply(owner.id(), session.id(), check(
                issue(firstClaimId, secondClaimId, "候选人撒谎", "为什么说谎？"))))
                .isInstanceOf(BusinessException.class);
    }

    private ClaimExtractionResult extraction(String content) {
        return new ClaimExtractionResult(List.of(new ClaimExtractionResult.ClaimCandidate(
                ClaimType.OWNERSHIP, content, 0.9, 0.7, List.of("职责边界"))));
    }

    private ConsistencyCheckResult check(ConsistencyCheckResult.IssueCandidate issue) {
        return new ConsistencyCheckResult(List.of(issue), List.of());
    }

    private ConsistencyCheckResult.IssueCandidate issue(
            String firstClaimId,
            String secondClaimId,
            String description,
            String question
    ) {
        return new ConsistencyCheckResult.IssueCandidate(
                "", ConsistencyIssueType.OWNERSHIP_CONFLICT, description,
                List.of(firstClaimId, secondClaimId), question, 0.86);
    }
}
