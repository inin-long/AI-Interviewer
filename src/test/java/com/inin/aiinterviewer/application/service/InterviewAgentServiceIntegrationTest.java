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
import com.inin.aiinterviewer.domain.enums.ConsistencyIssueStatus;
import com.inin.aiinterviewer.domain.enums.ScenarioEventType;
import com.inin.aiinterviewer.domain.enums.ScenarioStatus;
import com.inin.aiinterviewer.domain.enums.SimulationType;
import com.inin.aiinterviewer.domain.model.Message;
import com.inin.aiinterviewer.domain.model.CandidateProfileContent;
import com.inin.aiinterviewer.domain.model.ScenarioConstraint;
import com.inin.aiinterviewer.domain.model.ScenarioDefinition;
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
    @Autowired private EvidenceLedgerService evidenceLedgerService;
    @Autowired private ConsistencyIssueService consistencyIssueService;
    @Autowired private ScenarioEngine scenarioEngine;

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
                    assertThat(state.evidenceLedger().evidence()).singleElement();
                    assertThat(state.logicChainResult().gaps()).singleElement();
                    assertThat(state.probePlan().targetsClaim()).isFalse();
                    assertThat(state.pressureState().level())
                            .isEqualTo(com.inin.aiinterviewer.domain.enums.PressureLevel.RELAXED);
                });
        assertThat(claimLedgerService.ledger(user.id(), session.id()).claims())
                .singleElement().satisfies(claim -> assertThat(claim.content()).contains("订单系统"));
        assertThat(evidenceLedgerService.ledger(user.id(), session.id()).evidence()).singleElement()
                .satisfies(evidence -> assertThat(evidence.competencyCode())
                        .isEqualTo("PROBLEM_SOLVING"));
        assertThat(chatService.lastStreamPrompt()).contains(
                "结构化追问计划", "压力控制状态", "负责订单系统核心链路",
                "\"targetClaimId\":\"\"");

        chatService.enqueueChat("invalid-json");
        assertThatThrownBy(() -> agentService.answer(
                user.id(), session.id(), "这条回答必须先保存。周边再重试。")
                .collectList().block())
                .satisfies(throwable -> assertThat(hasCause(throwable, AIException.class)).isTrue());
        assertThat(sessionService.messages(user.id(), session.id()))
                .extracting(message -> message.content())
                .contains("这条回答必须先保存。周边再重试。");
        assertThat(claimLedgerService.ledger(user.id(), session.id()).claims()).hasSize(2);
        assertThat(evidenceLedgerService.ledger(user.id(), session.id()).evidence()).hasSize(2);
        assertThat(sessionService.loadLatestState(user.id(), session.id())).get()
                .satisfies(state -> assertThat(state.logicChainResult().gaps()).singleElement());

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
                    assertThat(state.probePlan().targetsLogicGap()).isTrue();
                    assertThat(state.probePlan().targetLogicGap())
                            .isEqualTo("MISSING_PERSONAL_CONTRIBUTION");
                });
        assertThat(chatService.lastStreamPrompt()).contains("结构化追问计划", "targetClaimId")
                .doesNotContain("\"targetClaimId\":\"\"");
    }

    @Test
    void asksNeutralClarificationBeforeResolvingCrossTurnConflict() {
        var user = userService.register("agent-consistency", "Agent Consistency", "safe-password");
        var plan = planService.create(user.id(), new SaveInterviewPlanCommand(
                "跨轮一致性面试", "Java 工程师", "架构职责验证", InterviewDifficulty.MEDIUM,
                30, 4, null, Map.of(), List.of("PROJECT_EXPERIENCE", "SUMMARY")));
        var session = sessionService.create(user.id(), plan.id());

        chatService.enqueueStream(Flux.just("请介绍你在订单系统中的架构职责。"));
        agentService.generateInitialQuestion(user.id(), session.id()).collectList().block();
        chatService.enqueueChat("""
                {"correctness":78,"depth":70,"missingPoints":["职责边界"],"feedback":"继续核实"}
                """);
        chatService.enqueueChat("""
                {"action":"FOLLOW_UP","nextStage":null,"reason":"继续核实职责"}
                """);
        chatService.enqueueStream(Flux.just("你具体负责了哪些架构设计工作？"));
        agentService.answer(user.id(), session.id(), "我主导了订单系统技术方案设计。")
                .collectList().block();
        String firstClaimId = claimLedgerService.ledger(user.id(), session.id()).claims().stream()
                .filter(claim -> claim.content().contains("负责订单系统核心链路"))
                .findFirst().orElseThrow().id();

        chatService.enqueueConsistency("""
                {"issues":[{"issueId":"","type":"OWNERSHIP_CONFLICT",
                "description":"技术方案主导权与架构选型责任的范围需要澄清",
                "relatedClaimIds":["%s","CURRENT_CLAIM"],
                "clarificationQuestion":"前面你提到主导技术方案，现在又说架构选型由架构师决定，请说明双方各自负责哪些决策？",
                "confidence":0.86}],"resolutions":[]}
                """.formatted(firstClaimId));
        chatService.enqueueChat("""
                {"correctness":76,"depth":68,"missingPoints":["决策边界"],"feedback":"需要澄清"}
                """);
        chatService.enqueueChat("""
                {"action":"FOLLOW_UP","nextStage":null,"reason":"澄清前后陈述"}
                """);
        chatService.enqueueStream(Flux.just(
                "前面你提到主导技术方案，现在又说架构选型由架构师决定，",
                "请说明双方各自负责哪些决策？"));

        agentService.answer(user.id(), session.id(), "架构选型主要由架构师决定。")
                .collectList().block();

        var clarified = consistencyIssueService.ledger(user.id(), session.id()).issues();
        assertThat(clarified).singleElement().satisfies(issue -> {
            assertThat(issue.status()).isEqualTo(ConsistencyIssueStatus.CLARIFIED);
            assertThat(issue.description()).doesNotContain("撒谎", "不诚实");
            assertThat(issue.clarificationMessageId()).isPositive();
        });
        assertThat(sessionService.messages(user.id(), session.id()).getLast().content())
                .contains("双方各自负责哪些决策").doesNotContain("撒谎", "不诚实");
        assertThat(sessionService.loadLatestState(user.id(), session.id())).get()
                .satisfies(state -> {
                    assertThat(state.probePlan().targetsConsistencyIssue()).isTrue();
                    assertThat(state.claimLedger().issues()).singleElement();
                });

        String issueId = clarified.getFirst().id();
        chatService.enqueueConsistency("""
                {"issues":[],"resolutions":[{"issueId":"%s","status":"RESOLVED",
                "resolution":"候选人说明自己负责接口与数据模型设计，架构师负责公司级技术栈选型，职责边界不冲突。",
                "confidence":0.91}]}
                """.formatted(issueId));
        chatService.enqueueChat("""
                {"correctness":86,"depth":82,"missingPoints":[],"feedback":"职责边界已澄清"}
                """);
        chatService.enqueueChat("""
                {"action":"FOLLOW_UP","nextStage":null,"reason":"继续验证架构能力"}
                """);
        chatService.enqueueStream(Flux.just("请继续说明接口设计中的关键取舍。"));
        agentService.answer(user.id(), session.id(),
                "我负责接口与数据模型设计，架构师负责公司级技术栈选型。")
                .collectList().block();

        assertThat(consistencyIssueService.ledger(user.id(), session.id()).issues())
                .singleElement().satisfies(issue -> {
                    assertThat(issue.status()).isEqualTo(ConsistencyIssueStatus.RESOLVED);
                    assertThat(issue.resolution()).contains("职责边界不冲突");
                });
    }

    @Test
    void directsAndPersistsScenarioOnlyAfterQuestionStreamCompletes() {
        var user = userService.register("agent-scenario", "Agent Scenario", "safe-password");
        var plan = planService.create(user.id(), new SaveInterviewPlanCommand(
                "故障沙盘面试", "Java 工程师", "核心查询服务稳定性", InterviewDifficulty.SENIOR,
                30, 4, null, Map.of("pressureLevel", "CHALLENGING"),
                List.of("SYSTEM_DESIGN", "SUMMARY")));
        var session = sessionService.create(user.id(), plan.id());

        chatService.enqueueStream(Flux.just("核心查询服务出现延迟，你会先检查什么？"));
        agentService.generateInitialQuestion(user.id(), session.id()).collectList().block();
        var started = scenarioEngine.start(user.id(), session.id(), new ScenarioDefinition(
                SimulationType.INCIDENT_RESPONSE, "验证故障处置和取舍",
                "核心查询服务在流量上涨后延迟增加", "当班技术负责人",
                List.of("流量达到平时两倍"), List.of("数据库允许增加只读实例"),
                Map.of("rootCause", "databasePrimaryLag"),
                Map.of("cacheAvailable", true, "databaseCpu", 68),
                List.of(new ScenarioConstraint("preserve_core_queries", "必须保障核心查询可用", true, true)),
                List.of("故障处置", "权衡分析"), List.of("核心查询恢复稳定"), 3));

        chatService.enqueueChat("""
                {"correctness":84,"depth":80,"missingPoints":["回源保护"],"feedback":"处置顺序清晰"}
                """);
        chatService.enqueueChat("""
                {"action":"FOLLOW_UP","nextStage":null,"reason":"继续验证故障处置"}
                """);
        chatService.enqueueChat("""
                {"decisionAction":"启用熔断并将读流量切到降级缓存",
                "decisionRationale":"先保护主数据库并维持核心查询可用",
                "eventType":"DEPENDENCY_FAILURE",
                "eventDescription":"缓存依赖完全不可用，降级读流量回源导致数据库 CPU 上升",
                "changes":{"cacheAvailable":false,"databaseCpu":86},
                "nextQuestion":"缓存完全不可用且数据库 CPU 已升至 86%，你接下来如何保障核心查询？",
                "completeAfterEvent":false}
                """);
        chatService.enqueueStream(Flux.just(
                "缓存完全不可用且数据库 CPU 已升至 86%，",
                "你接下来如何保障核心查询？"));

        assertThat(agentService.answer(
                user.id(), session.id(), "我会先启用熔断，把读流量切到降级缓存。")
                .collectList().block()).containsExactly(
                        "缓存完全不可用且数据库 CPU 已升至 86%，",
                        "你接下来如何保障核心查询？");

        var advanced = scenarioEngine.findActive(user.id(), session.id()).orElseThrow();
        assertThat(advanced.id()).isEqualTo(started.id());
        assertThat(advanced.currentRound()).isEqualTo(1);
        assertThat(advanced.status()).isEqualTo(ScenarioStatus.ACTIVE);
        assertThat(advanced.variables()).containsEntry("cacheAvailable", false)
                .containsEntry("databaseCpu", 86);
        assertThat(advanced.decisions()).singleElement().satisfies(decision -> {
            assertThat(decision.action()).contains("熔断");
            assertThat(decision.sourceMessageId()).isPositive();
        });
        assertThat(advanced.events()).singleElement().satisfies(event -> {
            assertThat(event.type()).isEqualTo(ScenarioEventType.DEPENDENCY_FAILURE);
            assertThat(event.triggeredByDecisionId()).isEqualTo(advanced.decisions().getFirst().id());
        });
        assertThat(sessionService.loadLatestState(user.id(), session.id())).get()
                .satisfies(state -> {
                    assertThat(state.stateVersion()).isEqualTo("2.7");
                    assertThat(state.activeScenario()).isEqualTo(advanced);
                    assertThat(state.probePlan().shouldInjectScenario()).isTrue();
                    assertThat(state.currentQuestion()).contains("数据库 CPU 已升至 86%");
                });
        assertThat(chatService.lastStreamPrompt())
                .contains("当前场景公开状态", "databaseCpu", "场景后果问题")
                .doesNotContain("databasePrimaryLag", "hiddenInformation", "rootCause");

        chatService.enqueueChat("""
                {"correctness":70,"depth":62,"missingPoints":["容量依据"],"feedback":"继续验证"}
                """);
        chatService.enqueueChat("""
                {"action":"FOLLOW_UP","nextStage":null,"reason":"补充容量判断依据"}
                """);
        chatService.enqueueChat("not-json");
        chatService.enqueueChat("still-not-json");
        chatService.enqueueStream(Flux.just("请说明你判断数据库容量是否足够的指标依据。"));

        assertThat(agentService.answer(
                user.id(), session.id(), "我会限制非核心查询并继续观察数据库容量。")
                .collectList().block())
                .containsExactly("请说明你判断数据库容量是否足够的指标依据。");

        assertThat(scenarioEngine.findActive(user.id(), session.id())).isEmpty();
        assertThat(scenarioEngine.require(user.id(), session.id(), started.id())).satisfies(failed -> {
            assertThat(failed.status()).isEqualTo(ScenarioStatus.FAILED);
            assertThat(failed.terminationReason()).contains("场景导演失败");
            assertThat(failed.currentRound()).isEqualTo(1);
        });
        assertThat(sessionService.loadLatestState(user.id(), session.id())).get()
                .satisfies(state -> {
                    assertThat(state.activeScenario().status()).isEqualTo(ScenarioStatus.FAILED);
                    assertThat(state.probePlan().shouldInjectScenario()).isFalse();
                    assertThat(state.currentQuestion()).contains("容量是否足够");
                });
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
                    assertThat(report.confidence()).containsKey("PROBLEM_SOLVING");
                    assertThat(report.evidence()).singleElement();
                    assertThat(report.contentMarkdown()).contains(
                            "技术基础", "综合评价", "问答摘要", "证据与置信度", "证据明细", "参考依据",
                            "本次面试未使用知识库片段作为提问依据");
                });
        assertThat(chatService.lastChatPrompt()).contains(
                "评分必须以证据账本为主要依据", "PROBLEM_SOLVING", "逐条证据");
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
        private final Queue<String> consistencyChats = new ArrayDeque<>();
        private final Queue<Flux<String>> streams = new ArrayDeque<>();
        private String lastStreamPrompt;
        private String lastChatPrompt;

        synchronized void enqueueChat(String response) {
            chats.add(response);
        }

        synchronized void enqueueStream(Flux<String> response) {
            streams.add(response);
        }

        synchronized void enqueueConsistency(String response) {
            consistencyChats.add(response);
        }

        @Override
        public synchronized String chat(String prompt) {
            lastChatPrompt = prompt;
            if (prompt.contains("候选人主张提取器")) {
                if (prompt.contains("接口与数据模型设计")) {
                    return claim("负责接口与数据模型设计，架构师负责技术栈选型");
                }
                if (prompt.contains("架构选型主要由架构师决定")) {
                    return claim("架构选型主要由架构师决定");
                }
                return """
                        {"claims":[{"type":"OWNERSHIP","content":"负责订单系统核心链路",
                        "importance":0.9,"credibility":0.7,"missingEvidence":["职责边界"]}]}
                        """;
            }
            if (prompt.contains("逻辑链评估器")) {
                return """
                        {"premises":["订单核心链路需要稳定交付"],"problemDiagnosis":"核心链路职责复杂",
                        "alternatives":[],"decision":"负责核心链路","reasoning":"","actions":[],
                        "outcome":"","validation":"","reflection":"",
                        "gaps":[{"type":"MISSING_PERSONAL_CONTRIBUTION","description":"缺少个人行动和职责边界",
                        "severity":0.85,"relatedClaimIds":[]}]}
                        """;
            }
            if (prompt.contains("逐轮面试证据收集器")) {
                return """
                        {"evidence":[{"competencyCode":"PROBLEM_SOLVING","signal":"POSITIVE",
                        "strength":0.8,"confidence":0.72,"reason":"回答给出了明确的技术决策",
                        "relatedClaimIds":[]}]}
                        """;
            }
            if (prompt.contains("跨轮面试一致性检查器")) {
                if (consistencyChats.isEmpty()) {
                    return "{\"issues\":[],\"resolutions\":[]}";
                }
                String response = consistencyChats.remove();
                if (response.contains("CURRENT_CLAIM")) {
                    String currentClaimId = extractCurrentClaimId(prompt);
                    return response.replace("CURRENT_CLAIM", currentClaimId);
                }
                return response;
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

        synchronized String lastChatPrompt() {
            return lastChatPrompt;
        }

        synchronized void clear() {
            chats.clear();
            consistencyChats.clear();
            streams.clear();
            lastStreamPrompt = null;
            lastChatPrompt = null;
        }

        private String claim(String content) {
            return """
                    {"claims":[{"type":"OWNERSHIP","content":"%s",
                    "importance":0.9,"credibility":0.7,"missingEvidence":["职责边界"]}]}
                    """.formatted(content);
        }

        private String extractCurrentClaimId(String prompt) {
            java.util.regex.Matcher matcher = java.util.regex.Pattern
                    .compile("本轮主张：\\[InterviewClaim\\[id=([0-9a-f-]{36})")
                    .matcher(prompt);
            if (!matcher.find()) throw new IllegalStateException("Current claim id missing from prompt");
            return matcher.group(1);
        }
    }
}
