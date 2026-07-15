package com.inin.aiinterviewer.infrastructure.ai;

import com.inin.aiinterviewer.agent.graph.InterviewGraph;
import com.inin.aiinterviewer.agent.model.InterviewTurnInput;
import com.inin.aiinterviewer.application.dto.InterviewPlanDto;
import com.inin.aiinterviewer.domain.enums.InterviewDifficulty;
import com.inin.aiinterviewer.domain.enums.InterviewStage;
import com.inin.aiinterviewer.domain.model.Message;
import com.inin.aiinterviewer.application.service.UserService;
import com.inin.aiinterviewer.application.service.ResumeService;
import com.inin.aiinterviewer.application.service.CandidateProfileService;
import com.inin.aiinterviewer.application.service.CandidateProfileTaskService;
import com.inin.aiinterviewer.application.service.BackgroundTaskService;
import com.inin.aiinterviewer.domain.enums.ProfileSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Path;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "AI_LLM_LIVE_TEST", matches = "true")
class LiveAiProviderIntegrationTest {

    @TempDir
    static Path applicationHome;

    @DynamicPropertySource
    static void applicationProperties(DynamicPropertyRegistry registry) {
        registry.add("ai.interviewer.home", () -> applicationHome.toString());
        registry.add("task.retry-count", () -> "1");
        registry.add("llm.timeout", () -> "300s");
        registry.add("llm.max-retries", () -> "0");
        registry.add("llm.max-tokens", () -> "2048");
    }

    @Autowired private ChatService chatService;
    @Autowired private EmbeddingService embeddingService;
    @Autowired private InterviewGraph interviewGraph;
    @Autowired private UserService userService;
    @Autowired private ResumeService resumeService;
    @Autowired private CandidateProfileService profileService;
    @Autowired private CandidateProfileTaskService profileTaskService;
    @Autowired private BackgroundTaskService backgroundTaskService;

    @Test
    void validatesSynchronousStreamingEmbeddingAgentAndProfileCalls() throws Exception {
        String response = chatService.chat("只回复四个字：连接成功");
        assertThat(response).isNotBlank();

        String streamed = chatService.stream("只回复四个字：流式成功")
                .collectList().map(parts -> String.join("", parts)).block();
        assertThat(streamed).isNotBlank();

        float[] embedding = embeddingService.embed("AI 技术面试向量验证");
        assertThat(embedding).isNotEmpty();
        for (float value : embedding) {
            assertThat(Float.isFinite(value)).isTrue();
        }

        var turn = interviewGraph.plan(new InterviewTurnInput(
                InterviewStage.INTRODUCTION,
                "请简要说明你如何定位一次线上性能问题。",
                "我先确认监控指标和影响范围，再结合日志、链路追踪和火焰图定位热点。",
                new InterviewPlanDto(1L, "真实模型验证", "Java 工程师", "负责后端服务稳定性",
                        InterviewDifficulty.MEDIUM, 30, 3, null, Map.of("focus", "问题定位"),
                        List.of("INTRODUCTION", "RESUME_REVIEW", "SUMMARY"), false,
                        LocalDateTime.now(), LocalDateTime.now()),
                List.of(new Message(Message.Role.ASSISTANT, "请简要说明你如何定位一次线上性能问题。",
                                LocalDateTime.now()),
                        new Message(Message.Role.USER, "我先确认指标，再结合日志和链路追踪定位。",
                                LocalDateTime.now())), "", ""));
        assertThat(turn.analysis().correctness()).isBetween(0, 100);
        assertThat(turn.questionPrompt()).isNotBlank();

        var user = userService.register("live-profile-user", "Live Profile", "safe-password");
        Path resumeFile = applicationHome.resolve("live-profile-resume.md");
        Files.writeString(resumeFile, """
                # 测试候选人
                4 年 Java 后端开发经验，熟悉 Spring Boot、Redis、MySQL 和 Docker。
                负责过订单系统性能优化与稳定性建设，目标岗位为 Java 后端工程师。
                """);
        var resume = resumeService.uploadAndParse(user.id(), resumeFile);
        long taskId = profileTaskService.enqueue(user.id(), resume.id());
        assertThat(backgroundTaskService.executeNext("live-profile-worker")).isTrue();
        assertThat(backgroundTaskService.require(user.id(), taskId).getStatus().name()).isEqualTo("SUCCESS");
        assertThat(profileService.find(user.id(), resume.id()))
                .get().satisfies(profile -> {
                    assertThat(profile.source()).isEqualTo(ProfileSource.AI);
                    assertThat(profile.content().skills()).isNotEmpty();
                    assertThat(profile.content().summary()).isNotBlank();
                });
    }
}
