package com.inin.aiinterviewer.application.service;

import com.inin.aiinterviewer.application.dto.SaveInterviewPlanCommand;
import com.inin.aiinterviewer.application.exception.AIException;
import com.inin.aiinterviewer.application.exception.ErrorCode;
import com.inin.aiinterviewer.domain.enums.InterviewDifficulty;
import com.inin.aiinterviewer.domain.enums.InterviewStage;
import com.inin.aiinterviewer.domain.model.Message;
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
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.Queue;

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
    }

    @Autowired private UserService userService;
    @Autowired private InterviewPlanService planService;
    @Autowired private InterviewSessionService sessionService;
    @Autowired private InterviewResultService interviewResultService;
    @Autowired private InterviewAgentService agentService;
    @Autowired private FakeChatService chatService;

    @BeforeEach
    void resetFakeProvider() {
        chatService.clear();
    }

    @Test
    void streamsQuestionsRunsGraphAndPreservesInputAndPartialOutputOnFailure() {
        var user = userService.register("agent-owner", "Agent Owner", "safe-password");
        var plan = planService.create(user.id(), new SaveInterviewPlanCommand(
                "Java Agent 面试", "Java 工程师", "核心服务开发", InterviewDifficulty.MEDIUM,
                45, 10, null, Map.of("focus", "Spring"),
                List.of("INTRODUCTION", "RESUME_REVIEW", "SUMMARY")));
        var session = sessionService.create(user.id(), plan.id());

        chatService.enqueueStream(Flux.just("请简要介绍", "你的项目经历。"));
        assertThat(agentService.generateInitialQuestion(user.id(), session.id()).collectList().block())
                .containsExactly("请简要介绍", "你的项目经历。");
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
                });

        chatService.enqueueChat("invalid-json");
        assertThatThrownBy(() -> agentService.answer(
                user.id(), session.id(), "这条回答必须先保存。周边再重试。")
                .collectList().block())
                .satisfies(throwable -> assertThat(hasCause(throwable, AIException.class)).isTrue());
        assertThat(sessionService.messages(user.id(), session.id()))
                .extracting(message -> message.content())
                .contains("这条回答必须先保存。周边再重试。");

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
                .get().satisfies(state -> assertThat(state.currentQuestion()).isEqualTo("这是已生成的半个问题"));
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
                .isEqualTo(com.inin.aiinterviewer.domain.enums.InterviewStatus.COMPLETED);
        assertThat(sessionService.require(user.id(), session.id()).stage())
                .isEqualTo(InterviewStage.COMPLETED);
        assertThat(interviewResultService.find(user.id(), session.id()))
                .get().satisfies(report -> {
                    assertThat(report.overallScore()).isEqualTo(78);
                    assertThat(report.dimensions()).hasSize(6);
                    assertThat(report.contentMarkdown()).contains("技术基础", "综合评价", "问答摘要");
                });
        assertThat(sessionService.loadLatestState(user.id(), session.id()))
                .get().satisfies(state -> {
                    assertThat(state.stage()).isEqualTo(InterviewStage.COMPLETED);
                    assertThat(state.evaluation().overallScore()).isEqualTo(78);
                });
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

        synchronized void enqueueChat(String response) {
            chats.add(response);
        }

        synchronized void enqueueStream(Flux<String> response) {
            streams.add(response);
        }

        @Override
        public synchronized String chat(String prompt) {
            return chats.remove();
        }

        @Override
        public synchronized Flux<String> stream(String prompt) {
            return streams.remove();
        }

        synchronized void clear() {
            chats.clear();
            streams.clear();
        }
    }
}
