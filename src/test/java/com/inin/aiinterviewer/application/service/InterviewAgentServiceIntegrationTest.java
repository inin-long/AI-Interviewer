package com.inin.aiinterviewer.application.service;

import com.inin.aiinterviewer.application.dto.SaveInterviewPlanCommand;
import com.inin.aiinterviewer.application.dto.InterviewMessageDto;
import com.inin.aiinterviewer.application.dto.KnowledgeCitationDto;
import com.inin.aiinterviewer.application.exception.AIException;
import com.inin.aiinterviewer.application.exception.BusinessException;
import com.inin.aiinterviewer.application.exception.ErrorCode;
import com.inin.aiinterviewer.domain.enums.InterviewDifficulty;
import com.inin.aiinterviewer.domain.enums.InterviewStage;
import com.inin.aiinterviewer.domain.enums.InterviewStatus;
import com.inin.aiinterviewer.domain.enums.BackgroundTaskStatus;
import com.inin.aiinterviewer.domain.enums.ReportStatus;
import com.inin.aiinterviewer.domain.model.Message;
import com.inin.aiinterviewer.domain.model.CandidateProfileContent;
import com.inin.aiinterviewer.infrastructure.ai.ChatService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
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
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(InterviewAgentServiceIntegrationTest.FakeAiConfiguration.class)
class InterviewAgentServiceIntegrationTest {

    @TempDir
    static Path applicationHome;

    @DynamicPropertySource
    static void applicationProperties(DynamicPropertyRegistry registry) {
        registry.add("ai.interviewer.home", () -> applicationHome.toString());
        registry.add("task.enabled", () -> false);
        registry.add("task.retry-count", () -> 1);
        registry.add("task.retry-delay", () -> "0s");
    }

    @Autowired private UserService userService;
    @Autowired private InterviewPlanService planService;
    @Autowired private InterviewSessionService sessionService;
    @Autowired private InterviewResultService interviewResultService;
    @Autowired private InterviewCompletionService completionService;
    @Autowired private ReportGenerationTaskService reportTaskService;
    @Autowired private BackgroundTaskService backgroundTaskService;
    @Autowired private InterviewAgentService agentService;
    @Autowired private FakeChatService chatService;
    @Autowired private ResumeService resumeService;
    @Autowired private CandidateProfileService profileService;
    @Autowired private ClaimLedgerService claimLedgerService;

    @BeforeEach
    void resetFakeProvider() {
        chatService.clear();
    }

    @Test
    void streamsQuestionsRunsGraphAndPreservesInputAndPartialOutputOnFailure() throws Exception {
        var user = userService.register("agent-owner", "Agent Owner", "safe-password");
        Path source = applicationHome.resolve("agent-profile.md");
        Files.writeString(source, "李明，熟悉 Java、Spring Boot 和订单系统。");
        var resume = resumeService.uploadAndParse(user.id(), source);
        profileService.saveManual(user.id(), resume.id(), new CandidateProfileContent(
                "李明", "Java 工程师", "4 年", "本科", List.of("Java", "Spring Boot"),
                List.of("订单系统"), List.of("后端开发"), List.of("业务理解"),
                List.of("性能调优待验证"), "具备订单系统研发经验。"));
        var profile = profileService.confirm(user.id(), resume.id());
        var plan = planService.create(user.id(), new SaveInterviewPlanCommand(
                "Java Agent 面试", "Java 工程师", "核心服务开发", InterviewDifficulty.MEDIUM,
                45, 10, resume.id(), profile.id(), Map.of("focus", "Spring"),
                List.of("INTRODUCTION", "RESUME_REVIEW", "SUMMARY")));
        var session = sessionService.create(user.id(), plan.id());

        chatService.enqueueStream(Flux.just("请简要介绍", "你的项目经历。"));
        assertThat(agentService.generateInitialQuestion(user.id(), session.id()).collectList().block())
                .containsExactly("请简要介绍", "你的项目经历。");
        assertThat(chatService.lastStreamPrompt()).contains("已确认候选人画像快照", "李明", "Spring Boot");
        assertThat(sessionService.messages(user.id(), session.id()))
                .singleElement().satisfies(message -> {
                    assertThat(message.role()).isEqualTo(Message.Role.ASSISTANT);
                    assertThat(message.content()).isEqualTo("请简要介绍你的项目经历。");
                });

        chatService.enqueueChat("""
                {"correctness":82,"depth":76,"missingPoints":["量化结果"],"feedback":"项目脉络清楚"}
                """);
        chatService.enqueueChat("""
                {"action":"NEXT_STAGE","nextStage":"RESUME_REVIEW","reason":"进入简历回顾"}
                """);
        chatService.enqueueStream(Flux.just("你提到订单系统，", "请说明其中最难的技术决策。"));
        agentService.answer(user.id(), session.id(), "我负责订单系统的核心链路。")
                .collectList().block();

        assertThat(sessionService.require(user.id(), session.id()).stage())
                .isEqualTo(InterviewStage.RESUME_REVIEW);
        assertThat(sessionService.messages(user.id(), session.id()))
                .extracting(message -> message.role())
                .containsExactly(Message.Role.ASSISTANT, Message.Role.USER, Message.Role.ASSISTANT);
        assertThat(sessionService.loadLatestState(user.id(), session.id()))
                .get().satisfies(state -> {
                    assertThat(state.analysis().correctness()).isEqualTo(82);
                    assertThat(state.currentQuestion()).contains("最难的技术决策");
                    assertThat(state.claimLedger().claims()).singleElement();
                    assertThat(state.probePlan().targetsClaim()).isFalse();
                });
        assertThat(claimLedgerService.ledger(user.id(), session.id()).claims())
                .singleElement().satisfies(claim -> assertThat(claim.content()).contains("订单系统"));
        assertThat(chatService.lastStreamPrompt()).contains(
                "结构化追问计划", "负责订单系统核心链路", "\"targetClaimId\":\"\"");

        chatService.enqueueChat("invalid-json");
        assertThatThrownBy(() -> agentService.answer(
                user.id(), session.id(), "这条回答必须先保存。周边再重试。")
                .collectList().block())
                .satisfies(throwable -> assertThat(hasCause(throwable, AIException.class)).isTrue());
        assertThat(sessionService.messages(user.id(), session.id()))
                .extracting(message -> message.content())
                .contains("这条回答必须先保存。周边再重试。");
        assertThat(claimLedgerService.ledger(user.id(), session.id()).claims()).hasSize(2);

        chatService.enqueueChat("""
                {"correctness":60,"depth":55,"missingPoints":[],"feedback":"继续追问"}
                """);
        chatService.enqueueChat("""
                {"action":"FOLLOW_UP","nextStage":null,"reason":"补充细节"}
                """);
        chatService.enqueueStream(Flux.concat(
                Flux.just("这是已生成的半个问题"),
                Flux.error(new AIException(ErrorCode.AI_CALL_FAILED, new RuntimeException("stream interrupted")))));
        assertThatThrownBy(() -> agentService.answer(
                user.id(), session.id(), "用于验证流式中断的回答。")
                .collectList().block())
                .satisfies(throwable -> assertThat(hasCause(throwable, AIException.class)).isTrue());
        assertThat(sessionService.messages(user.id(), session.id()).getLast().content())
                .isEqualTo("这是已生成的半个问题");
        assertThat(sessionService.loadLatestState(user.id(), session.id()))
                .get().satisfies(state -> {
                    assertThat(state.currentQuestion()).isEqualTo("这是已生成的半个问题");
                    assertThat(state.probePlan().targetsClaim()).isTrue();
                });
        assertThat(chatService.lastStreamPrompt()).contains("结构化追问计划", "targetClaimId")
                .doesNotContain("\"targetClaimId\":\"\"");
    }

    @Test
    void completesAtQuestionLimitAndPersistsSixDimensionReport() {
        var user = userService.register("completion-owner", "Completion Owner", "safe-password");
        var plan = planService.create(user.id(), new SaveInterviewPlanCommand(
                "一题结束验证", "Java 工程师", "核心服务开发", InterviewDifficulty.MEDIUM,
                30, 1, null, Map.of(), List.of("INTRODUCTION", "SUMMARY")));
        var session = sessionService.create(user.id(), plan.id());

        chatService.enqueueStream(Flux.just("请介绍你解决过的一个技术难题。"));
        agentService.generateInitialQuestion(user.id(), session.id()).collectList().block();
        chatService.enqueueChat("""
                {"overallScore":78,"technicalScore":80,"problemSolvingScore":82,
                "projectScore":76,"systemDesignScore":70,"communicationScore":84,
                "comprehensiveScore":77,"summary":"回答结构清楚，技术决策仍可进一步量化。"}
                """);

        assertThat(agentService.answer(user.id(), session.id(), "我通过拆分事务边界解决了长事务问题。")
                .collectList().block()).isEmpty();

        assertThat(sessionService.require(user.id(), session.id()).status())
                .isEqualTo(InterviewStatus.RUNNING);
        assertThat(reportTaskService.state(user.id(), session.id())).satisfies(state -> {
            assertThat(state.completion().finalAnswerSaved()).isTrue();
            assertThat(state.completion().reportStatus()).isEqualTo(ReportStatus.GENERATING);
            assertThat(state.taskStatus()).isEqualTo(BackgroundTaskStatus.PENDING);
        });
        assertThat(backgroundTaskService.executeNext("report-success-worker")).isTrue();

        assertThat(sessionService.require(user.id(), session.id()).status()).isEqualTo(InterviewStatus.COMPLETED);
        assertThat(sessionService.require(user.id(), session.id()).stage()).isEqualTo(InterviewStage.COMPLETED);
        assertThat(interviewResultService.find(user.id(), session.id()))
                .get().satisfies(report -> {
                    assertThat(report.overallScore()).isEqualTo(78);
                    assertThat(report.dimensions()).hasSize(6);
                    assertThat(report.contentMarkdown()).contains(
                            "技术基础", "综合评价", "问答摘要", "参考依据",
                            "本次面试未使用知识库片段作为提问依据");
                });
        assertThat(sessionService.loadLatestState(user.id(), session.id()))
                .get().satisfies(state -> {
                    assertThat(state.stage()).isEqualTo(InterviewStage.COMPLETED);
                    assertThat(state.evaluation().overallScore()).isEqualTo(78);
                });
    }

    @Test
    void deduplicatesConcurrentReportGenerationRequests() throws Exception {
        var user = userService.register("report-dedup-owner", "Report Dedup", "safe-password");
        var plan = planService.create(user.id(), new SaveInterviewPlanCommand(
                "报告并发验证", "Java 工程师", "核心服务开发", InterviewDifficulty.MEDIUM,
                30, 1, null, Map.of(), List.of("INTRODUCTION", "SUMMARY")));
        var session = sessionService.create(user.id(), plan.id());

        chatService.enqueueStream(Flux.just("请介绍一次性能优化实践。"));
        agentService.generateInitialQuestion(user.id(), session.id()).collectList().block();
        sessionService.appendUserAnswer(user.id(), session.id(), "我通过调用链分析定位并消除了重复查询。");

        ArrayList<Callable<Long>> requests = new ArrayList<>();
        for (int index = 0; index < 8; index++) {
            requests.add(() -> reportTaskService.enqueue(user.id(), session.id()));
        }
        List<Long> taskIds;
        try (var executor = Executors.newFixedThreadPool(4)) {
            taskIds = executor.invokeAll(requests).stream().map(future -> {
                try { return future.get(); }
                catch (Exception exception) { throw new AssertionError(exception); }
            }).toList();
        }

        assertThat(new HashSet<>(taskIds)).hasSize(1);
        assertThat(reportTaskService.state(user.id(), session.id()).taskStatus())
                .isEqualTo(BackgroundTaskStatus.PENDING);

        chatService.enqueueChat("""
                {"overallScore":79,"technicalScore":82,"problemSolvingScore":83,
                "projectScore":78,"systemDesignScore":73,"communicationScore":80,
                "comprehensiveScore":78,"summary":"性能定位思路清晰。"}
                """);
        assertThat(backgroundTaskService.executeNext("concurrent-report-worker")).isTrue();
        assertThat(sessionService.require(user.id(), session.id()).status()).isEqualTo(InterviewStatus.COMPLETED);
    }

    @Test
    void recoversAnInterruptedReportTaskAfterApplicationRestart() {
        var user = userService.register("report-recovery-owner", "Report Recovery", "safe-password");
        var plan = planService.create(user.id(), new SaveInterviewPlanCommand(
                "报告恢复验证", "Java 工程师", "核心服务开发", InterviewDifficulty.MEDIUM,
                30, 1, null, Map.of(), List.of("INTRODUCTION", "SUMMARY")));
        var session = sessionService.create(user.id(), plan.id());

        chatService.enqueueStream(Flux.just("请说明你如何设计可恢复的后台任务。"));
        agentService.generateInitialQuestion(user.id(), session.id()).collectList().block();
        sessionService.appendUserAnswer(user.id(), session.id(), "任务状态持久化，并在启动时回收 RUNNING 状态。");
        long taskId = reportTaskService.enqueue(user.id(), session.id());

        var claimed = backgroundTaskService.claimNext("crashed-report-worker").orElseThrow();
        assertThat(claimed.getId()).isEqualTo(taskId);
        assertThat(backgroundTaskService.require(user.id(), taskId).getStatus())
                .isEqualTo(BackgroundTaskStatus.RUNNING);

        assertThat(backgroundTaskService.recoverInterruptedTasks()).isEqualTo(1);
        assertThat(backgroundTaskService.require(user.id(), taskId).getStatus())
                .isEqualTo(BackgroundTaskStatus.PENDING);
        chatService.enqueueChat("""
                {"overallScore":84,"technicalScore":86,"problemSolvingScore":85,
                "projectScore":82,"systemDesignScore":88,"communicationScore":81,
                "comprehensiveScore":83,"summary":"具备清晰的任务恢复设计。"}
                """);
        assertThat(backgroundTaskService.executeNext("recovered-report-worker")).isTrue();
        assertThat(sessionService.require(user.id(), session.id()).status()).isEqualTo(InterviewStatus.COMPLETED);
        assertThat(reportTaskService.state(user.id(), session.id()).taskStatus())
                .isEqualTo(BackgroundTaskStatus.SUCCESS);
    }

    @Test
    void preservesFinalAnswerRecordsFailureAndRetriesReportWithoutDuplicateMessage() {
        var user = userService.register("report-retry-owner", "Report Retry", "safe-password");
        var other = userService.register("report-retry-other", "Other", "safe-password");
        var plan = planService.create(user.id(), new SaveInterviewPlanCommand(
                "报告重试验证", "Java 工程师", "核心服务开发", InterviewDifficulty.MEDIUM,
                30, 1, null, Map.of(), List.of("INTRODUCTION", "SUMMARY")));
        var session = sessionService.create(user.id(), plan.id());

        chatService.enqueueStream(Flux.just("请说明一次线上故障的排查过程。"));
        agentService.generateInitialQuestion(user.id(), session.id()).collectList().block();
        chatService.enqueueChat("invalid-json");

        assertThat(agentService.answer(
                user.id(), session.id(), "我先确认监控异常，再结合日志定位根因。")
                .collectList().block()).isEmpty();

        var queued = reportTaskService.state(user.id(), session.id());
        assertThat(queued.taskStatus()).isEqualTo(BackgroundTaskStatus.PENDING);
        assertThat(queued.completion().reportStatus()).isEqualTo(ReportStatus.GENERATING);
        assertThat(backgroundTaskService.executeNext("report-failure-worker")).isTrue();

        assertThat(sessionService.require(user.id(), session.id()).status()).isEqualTo(InterviewStatus.RUNNING);
        assertThat(sessionService.messages(user.id(), session.id()))
                .extracting(InterviewMessageDto::role)
                .containsExactly(Message.Role.ASSISTANT, Message.Role.USER);
        assertThat(completionService.state(user.id(), session.id())).satisfies(state -> {
            assertThat(state.finalAnswerSaved()).isTrue();
            assertThat(state.reportStatus()).isEqualTo(ReportStatus.FAILED);
            assertThat(state.failureMessage()).contains("AI 返回格式无效");
            assertThat(state.retryable()).isTrue();
        });
        assertThat(interviewResultService.find(user.id(), session.id())).isEmpty();

        assertThatThrownBy(() -> agentService.answer(user.id(), session.id(), "不应重复保存的回答")
                .collectList().block())
                .satisfies(throwable -> assertThat(hasCause(throwable, BusinessException.class)).isTrue());
        assertThat(sessionService.messages(user.id(), session.id())).hasSize(2);
        assertThatThrownBy(() -> completionService.state(other.id(), session.id()))
                .isInstanceOf(BusinessException.class);

        sessionService.pause(user.id(), session.id());
        chatService.enqueueChat("""
                {"overallScore":81,"technicalScore":82,"problemSolvingScore":86,
                "projectScore":80,"systemDesignScore":75,"communicationScore":84,
                "comprehensiveScore":80,"summary":"排查过程完整，具备较好的故障定位思路。"}
                """);
        long retriedTaskId = reportTaskService.enqueue(user.id(), session.id());
        assertThat(retriedTaskId).isEqualTo(queued.taskId());
        assertThat(reportTaskService.state(user.id(), session.id()).taskStatus())
                .isEqualTo(BackgroundTaskStatus.PENDING);
        assertThat(backgroundTaskService.executeNext("report-retry-worker")).isTrue();
        var report = interviewResultService.find(user.id(), session.id()).orElseThrow();

        assertThat(report.overallScore()).isEqualTo(81);
        assertThat(sessionService.require(user.id(), session.id()).status()).isEqualTo(InterviewStatus.COMPLETED);
        assertThat(sessionService.messages(user.id(), session.id())).hasSize(2);
        assertThat(completionService.state(user.id(), session.id()).reportStatus())
                .isEqualTo(ReportStatus.COMPLETED);
    }

    @Test
    void formatsQuestionScopedKnowledgeCitationsForReport() {
        var message = new InterviewMessageDto(
                3, Message.Role.ASSISTANT, "请说明 Redis 缓存穿透的处理方式。", LocalDateTime.now(), false,
                List.of(new KnowledgeCitationDto(
                        9L, "Redis 设计说明.md", 2,
                        "缓存穿透可以使用布隆过滤器，并为不存在的数据设置短期空值缓存。", 0.91)));

        assertThat(completionService.citationMarkdown(List.of(message)))
                .contains("第 1 题", "Redis 设计说明.md", "片段 3", "布隆过滤器");
    }

    private boolean hasCause(Throwable throwable, Class<? extends Throwable> type) {
        Throwable current = throwable;
        while (current != null) {
            if (type.isInstance(current)) return true;
            current = current.getCause();
        }
        return false;
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FakeAiConfiguration {
        @Bean
        @Primary
        FakeChatService fakeChatService() {
            return new FakeChatService();
        }
    }

    static class FakeChatService implements ChatService {
        private final Queue<String> chats = new ArrayDeque<>();
        private final Queue<Flux<String>> streams = new ArrayDeque<>();
        private String lastStreamPrompt;

        synchronized void enqueueChat(String response) {
            chats.add(response);
        }

        synchronized void enqueueStream(Flux<String> response) {
            streams.add(response);
        }

        @Override
        public synchronized String chat(String prompt) {
            if (prompt.contains("候选人主张提取器")) {
                return """
                        {"claims":[{"type":"OWNERSHIP","content":"负责订单系统核心链路",
                        "importance":0.9,"credibility":0.7,"missingEvidence":["职责边界"]}]}
                        """;
            }
            return chats.remove();
        }

        @Override
        public synchronized Flux<String> stream(String prompt) {
            lastStreamPrompt = prompt;
            return streams.remove();
        }

        synchronized String lastStreamPrompt() {
            return lastStreamPrompt;
        }

        synchronized void clear() {
            chats.clear();
            streams.clear();
            lastStreamPrompt = null;
        }
    }
}
