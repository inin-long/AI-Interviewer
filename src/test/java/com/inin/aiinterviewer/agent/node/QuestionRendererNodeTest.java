package com.inin.aiinterviewer.agent.node;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.inin.aiinterviewer.agent.model.ProbePlan;
import com.inin.aiinterviewer.agent.state.InterviewGraphState;
import com.inin.aiinterviewer.application.dto.InterviewPlanDto;
import com.inin.aiinterviewer.domain.enums.InterviewDifficulty;
import com.inin.aiinterviewer.domain.enums.InterviewStage;
import com.inin.aiinterviewer.domain.enums.PressureLevel;
import com.inin.aiinterviewer.domain.enums.ProbeStrategy;
import com.inin.aiinterviewer.infrastructure.ai.ChatService;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class QuestionRendererNodeTest {

    @Test
    void rendersOnlyTheStructuredProbeTargetIntoTheQuestionPrompt() throws Exception {
        ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
        EchoChatService chatService = new EchoChatService();
        QuestionRendererNode renderer = new QuestionRendererNode(chatService, objectMapper);
        ProbePlan probe = new ProbePlan(
                "claim-42", "验证主张“将 P99 延迟降低 40%”是否有可靠数据来源",
                ProbeStrategy.VERIFY_DATA_SOURCE, PressureLevel.STANDARD,
                "缺少测量方式", List.of("监控平台", "统计区间"), false);
        InterviewPlanDto interviewPlan = new InterviewPlanDto(
                1L, "后端面试", "Java 后端工程师", "负责高并发订单服务",
                InterviewDifficulty.MEDIUM, 45, 10, null, Map.of(),
                List.of("PROJECT_EXPERIENCE", "SUMMARY"), false,
                LocalDateTime.now(), LocalDateTime.now());
        InterviewGraphState state = new InterviewGraphState(Map.of(
                InterviewGraphState.STAGE, InterviewStage.PROJECT_EXPERIENCE,
                InterviewGraphState.PLAN, interviewPlan,
                InterviewGraphState.PROBE_PLAN, probe));

        String prompt = (String) renderer.apply(state).get(InterviewGraphState.QUESTION_PROMPT);

        assertThat(prompt).contains(
                "结构化追问计划", "claim-42", "P99 延迟降低 40%",
                "监控平台", "统计区间", "禁止改成通用知识题");
        assertThat(renderer.stream(prompt).collectList().block()).containsExactly("渲染后的问题");
        assertThat(chatService.prompt).isEqualTo(prompt);
    }

    private static class EchoChatService implements ChatService {
        private String prompt;

        @Override
        public String chat(String prompt) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Flux<String> stream(String prompt) {
            this.prompt = prompt;
            return Flux.just("渲染后的问题");
        }
    }
}
