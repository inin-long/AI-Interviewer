package com.inin.aiinterviewer.infrastructure.ai;

import com.inin.aiinterviewer.agent.graph.InterviewGraph;
import com.inin.aiinterviewer.agent.model.InterviewTurnInput;
import com.inin.aiinterviewer.application.dto.InterviewPlanDto;
import com.inin.aiinterviewer.domain.enums.InterviewDifficulty;
import com.inin.aiinterviewer.domain.enums.InterviewStage;
import com.inin.aiinterviewer.domain.model.Message;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Path;
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
    }

    @Autowired private ChatService chatService;
    @Autowired private EmbeddingService embeddingService;
    @Autowired private InterviewGraph interviewGraph;

    @Test
    void validatesSynchronousStreamingAndEmbeddingCalls() {
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
                                LocalDateTime.now())), ""));
        assertThat(turn.analysis().correctness()).isBetween(0, 100);
        assertThat(turn.questionPrompt()).isNotBlank();
    }
}
