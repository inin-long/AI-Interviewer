package com.inin.aiinterviewer.application.service;

import com.inin.aiinterviewer.application.dto.SaveInterviewPlanCommand;
import com.inin.aiinterviewer.application.exception.BusinessException;
import com.inin.aiinterviewer.application.exception.ErrorCode;
import com.inin.aiinterviewer.domain.enums.InterviewDifficulty;
import com.inin.aiinterviewer.domain.enums.SessionBranchStatus;
import com.inin.aiinterviewer.infrastructure.ai.ChatService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import reactor.core.publisher.Flux;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(SessionBranchServiceIntegrationTest.FakeAiConfiguration.class)
class SessionBranchServiceIntegrationTest {

    @TempDir
    static Path applicationHome;

    @DynamicPropertySource
    static void applicationProperties(DynamicPropertyRegistry registry) {
        registry.add("ai.interviewer.home", () -> applicationHome.toString());
        registry.add("task.enabled", () -> false);
    }

    @Autowired private UserService userService;
    @Autowired private InterviewPlanService planService;
    @Autowired private InterviewSessionService sessionService;
    @Autowired private SessionBranchService branchService;

    @Test
    void replaysAFrozenQuestionComparesAnswersAndNeverMutatesTheSourceSession() {
        var owner = userService.register("branch-owner", "Branch Owner", "safe-password");
        var other = userService.register("branch-other", "Branch Other", "safe-password");
        var plan = planService.create(owner.id(), new SaveInterviewPlanCommand(
                "缓存专项面试", "Java 工程师", "负责高并发服务设计",
                InterviewDifficulty.MEDIUM, 45, 6, null, null, List.of(), Map.of(),
                List.of("INTRODUCTION", "TECHNICAL_DEEP_DIVE", "SUMMARY"),
                "java-backend-1.0.0"));
        var session = sessionService.create(owner.id(), plan.id());

        sessionService.saveAssistantOutput(
                owner.id(), session.id(), "你如何证明缓存优化确实有效？", null, false);
        sessionService.appendUserAnswer(
                owner.id(), session.id(), "我只加了缓存，感觉接口变快了。");
        sessionService.saveAssistantOutput(
                owner.id(), session.id(), "你如何排除流量变化造成的影响？", null, false);

        var sourceMessages = sessionService.messages(owner.id(), session.id());
        var sourceCheckpoint = sessionService.loadLatestState(owner.id(), session.id()).orElseThrow();
        var draft = branchService.create(owner.id(), session.id(), 1, null);

        assertThat(draft.status()).isEqualTo(SessionBranchStatus.DRAFT);
        assertThat(draft.sourceQuestionNumber()).isEqualTo(1);
        assertThat(draft.sourceCheckpointId()).isPositive();
        assertThat(draft.originalQuestion()).contains("证明缓存优化");
        assertThat(draft.originalAnswer()).contains("感觉接口变快");
        assertThat(branchService.list(owner.id(), session.id()))
                .extracting(value -> value.id()).containsExactly(draft.id());

        var completed = branchService.submitAnswer(owner.id(), draft.id(),
                "我修正之前的判断：先记录未启用缓存时的延迟基线和流量，"
                        + "再用对照组灰度启用缓存，比较 P95 延迟和命中率，最后通过监控验证收益。");

        assertThat(completed.status()).isEqualTo(SessionBranchStatus.COMPLETED);
        assertThat(completed.comparison()).isNotNull();
        assertThat(completed.comparison().newLogicCompleteness())
                .isGreaterThan(completed.comparison().originalLogicCompleteness());
        assertThat(completed.comparison().newEvidenceCount()).isGreaterThan(0);
        assertThat(completed.comparison().newEvidenceScore())
                .isGreaterThan(completed.comparison().originalEvidenceScore());
        assertThat(completed.comparison().viewpointRevised()).isTrue();
        assertThat(completed.comparison().resolvedGapTypes())
                .contains("MISSING_BASELINE", "MISSING_VALIDATION");
        assertThat(completed.comparison().branchFollowUp()).isNotBlank();
        assertThat(completed.comparisonMarkdown())
                .contains("局部对比", "逻辑链完整度", "证据数量", "证据质量分", "追问变化", "缺口");

        assertThat(sessionService.messages(owner.id(), session.id())).isEqualTo(sourceMessages);
        assertThat(sessionService.loadLatestState(owner.id(), session.id()).orElseThrow())
                .usingRecursiveComparison().isEqualTo(sourceCheckpoint);

        var child = branchService.create(owner.id(), session.id(), 1, completed.id());
        assertThat(child.parentBranchId()).isEqualTo(completed.id());
        assertThatThrownBy(() -> branchService.list(other.id(), session.id()))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> branchService.require(other.id(), completed.id()))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.BRANCH_NOT_FOUND));
        assertThatThrownBy(() -> branchService.submitAnswer(
                owner.id(), completed.id(), "不能重复覆盖已经完成的分支"))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.INVALID_STATE));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FakeAiConfiguration {
        @Bean
        @Primary
        ChatService branchComparisonChatService() {
            return new BranchComparisonChatService();
        }
    }

    static class BranchComparisonChatService implements ChatService {

        @Override
        public String chat(String prompt) {
            boolean improved = prompt.contains("延迟基线") && prompt.contains("对照组");
            if (prompt.contains("候选人主张提取器")) {
                return improved ? """
                        {"claims":[
                          {"type":"DECISION","content":"通过基线和对照组验证缓存收益", "importance":0.9,
                           "credibility":0.9,"missingEvidence":[]},
                          {"type":"METRIC","content":"比较 P95 延迟与命中率", "importance":0.8,
                           "credibility":0.85,"missingEvidence":[]}
                        ]}
                        """ : """
                        {"claims":[{"type":"OPINION","content":"缓存让接口更快", "importance":0.8,
                        "credibility":0.3,"missingEvidence":["性能基线","验证数据"]}]}
                        """;
            }
            if (prompt.contains("逻辑链评估器")) {
                return improved ? """
                        {"premises":["需要排除流量变化"],"problemDiagnosis":"缺少可归因的性能数据",
                        "alternatives":["直接全量上线","使用灰度对照组"],"decision":"采用灰度对照组",
                        "reasoning":"对照实验可以隔离流量变量","actions":["采集基线","灰度启用缓存"],
                        "outcome":"P95 延迟下降","validation":"比较 P95 延迟与命中率",
                        "reflection":"持续监控收益","gaps":[]}
                        """ : """
                        {"premises":[],"problemDiagnosis":"接口较慢","alternatives":[],"decision":"增加缓存",
                        "reasoning":"","actions":["增加缓存"],"outcome":"感觉变快","validation":"","reflection":"",
                        "gaps":[
                          {"type":"MISSING_BASELINE","description":"缺少优化前基线","severity":0.9,"relatedClaimIds":[]},
                          {"type":"MISSING_VALIDATION","description":"缺少对照和监控验证","severity":0.9,"relatedClaimIds":[]}
                        ]}
                        """;
            }
            if (prompt.contains("逐轮面试证据收集器")) {
                return """
                        {"evidence":[
                          {"competencyCode":"PROBLEM_SOLVING","signal":"POSITIVE","strength":0.9,
                           "confidence":0.9,"reason":"给出了基线、对照组和验证指标","relatedClaimIds":[]},
                          {"competencyCode":"SYSTEM_DESIGN","signal":"POSITIVE","strength":0.8,
                           "confidence":0.85,"reason":"使用灰度降低变更风险","relatedClaimIds":[]}
                        ]}
                        """;
            }
            throw new AssertionError("Unexpected branch comparison prompt: " + prompt);
        }

        @Override
        public Flux<String> stream(String prompt) {
            return Flux.error(new AssertionError("Branch comparison must not stream"));
        }
    }
}
