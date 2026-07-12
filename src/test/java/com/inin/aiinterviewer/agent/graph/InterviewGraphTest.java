package com.inin.aiinterviewer.agent.graph;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.inin.aiinterviewer.agent.model.AgentAction;
import com.inin.aiinterviewer.agent.model.InterviewTurnInput;
import com.inin.aiinterviewer.agent.node.AnswerAnalyzerNode;
import com.inin.aiinterviewer.agent.node.FollowUpDecisionNode;
import com.inin.aiinterviewer.agent.node.QuestionGeneratorNode;
import com.inin.aiinterviewer.agent.node.StageTransitionNode;
import com.inin.aiinterviewer.agent.stage.StageManager;
import com.inin.aiinterviewer.agent.support.StructuredAiResponseParser;
import com.inin.aiinterviewer.application.dto.InterviewPlanDto;
import com.inin.aiinterviewer.application.exception.AIException;
import com.inin.aiinterviewer.application.exception.BusinessException;
import com.inin.aiinterviewer.domain.enums.InterviewDifficulty;
import com.inin.aiinterviewer.domain.enums.InterviewStage;
import com.inin.aiinterviewer.domain.model.Message;
import com.inin.aiinterviewer.infrastructure.ai.ChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InterviewGraphTest {

    private QueueChatService chatService;
    private InterviewGraph graph;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
        StageManager stageManager = new StageManager();
        StructuredAiResponseParser parser = new StructuredAiResponseParser(objectMapper);
        chatService = new QueueChatService();
        graph = new InterviewGraph(
                new AnswerAnalyzerNode(chatService, parser),
                new FollowUpDecisionNode(chatService, parser, stageManager, objectMapper),
                new StageTransitionNode(stageManager),
                new QuestionGeneratorNode(chatService, objectMapper));
    }

    @Test
    void analyzesAnswerAndPreparesFollowUpQuestion() {
        chatService.enqueue("""
                {"correctness":72,"depth":65,"missingPoints":["事务传播"],"feedback":"基础清楚"}
                """);
        chatService.enqueue("""
                {"action":"FOLLOW_UP","nextStage":null,"reason":"需要继续深挖"}
                """);

        var result = graph.plan(input());

        assertThat(result.analysis().correctness()).isEqualTo(72);
        assertThat(result.decision().action()).isEqualTo(AgentAction.FOLLOW_UP);
        assertThat(result.stage()).isEqualTo(InterviewStage.INTRODUCTION);
        assertThat(result.questionPrompt()).contains("一次只提出一个清晰问题", "Java 工程师");
    }

    @Test
    void appliesOnlyLegalStageTransition() {
        chatService.enqueue(validAnalysis());
        chatService.enqueue("""
                {"action":"NEXT_STAGE","nextStage":"RESUME_REVIEW","reason":"开场完成"}
                """);

        assertThat(graph.plan(input()).stage()).isEqualTo(InterviewStage.RESUME_REVIEW);
    }

    @Test
    void rejectsMalformedAnalysisAndIllegalStageSkip() {
        chatService.enqueue("not-json");
        assertThatThrownBy(() -> graph.plan(input()))
                .satisfies(throwable -> assertThat(hasCause(throwable, AIException.class)).isTrue());

        chatService.enqueue(validAnalysis());
        chatService.enqueue("""
                {"action":"NEXT_STAGE","nextStage":"COMPLETED","reason":"尝试跳过流程"}
                """);
        assertThatThrownBy(() -> graph.plan(input()))
                .satisfies(throwable -> assertThat(hasCause(throwable, BusinessException.class)).isTrue());
    }

    private InterviewTurnInput input() {
        return new InterviewTurnInput(
                InterviewStage.INTRODUCTION,
                "请介绍 Spring 事务。",
                "事务用于保证一组数据库操作的一致性。",
                new InterviewPlanDto(1L, "Java 面试", "Java 工程师", "后端服务开发",
                        InterviewDifficulty.MEDIUM, 45, 10, null, Map.of("focus", "Spring"),
                        List.of("INTRODUCTION", "RESUME_REVIEW", "SUMMARY"), false,
                        LocalDateTime.now(), LocalDateTime.now()),
                List.of(new Message(Message.Role.ASSISTANT, "请介绍 Spring 事务。", LocalDateTime.now()),
                        new Message(Message.Role.USER, "事务用于保证一致性。", LocalDateTime.now())), "");
    }

    private String validAnalysis() {
        return """
                {"correctness":80,"depth":70,"missingPoints":[],"feedback":"回答有效"}
                """;
    }

    private boolean hasCause(Throwable throwable, Class<? extends Throwable> type) {
        Throwable current = throwable;
        while (current != null) {
            if (type.isInstance(current)) return true;
            current = current.getCause();
        }
        return false;
    }

    private static class QueueChatService implements ChatService {
        private final Queue<String> responses = new ArrayDeque<>();

        void enqueue(String response) {
            responses.add(response);
        }

        @Override
        public String chat(String prompt) {
            return responses.remove();
        }

        @Override
        public Flux<String> stream(String prompt) {
            return Flux.error(new UnsupportedOperationException());
        }
    }
}
