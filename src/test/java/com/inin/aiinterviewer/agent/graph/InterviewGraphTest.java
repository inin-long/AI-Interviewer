package com.inin.aiinterviewer.agent.graph;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.inin.aiinterviewer.agent.model.AgentAction;
import com.inin.aiinterviewer.agent.model.InterviewTurnInput;
import com.inin.aiinterviewer.agent.node.AnswerAnalyzerNode;
import com.inin.aiinterviewer.agent.node.ClaimExtractorNode;
import com.inin.aiinterviewer.agent.node.FollowUpDecisionNode;
import com.inin.aiinterviewer.agent.node.LogicChainEvaluatorNode;
import com.inin.aiinterviewer.agent.node.EvidenceCollectorNode;
import com.inin.aiinterviewer.agent.node.ConsistencyCheckNode;
import com.inin.aiinterviewer.agent.node.ProbePlannerNode;
import com.inin.aiinterviewer.agent.node.PressureControllerNode;
import com.inin.aiinterviewer.agent.node.ScenarioDirectorNode;
import com.inin.aiinterviewer.agent.node.QuestionRendererNode;
import com.inin.aiinterviewer.agent.node.StageTransitionNode;
import com.inin.aiinterviewer.agent.stage.StageManager;
import com.inin.aiinterviewer.agent.support.StructuredAiResponseParser;
import com.inin.aiinterviewer.agent.support.PressureController;
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
                new ClaimExtractorNode(chatService, parser),
                new LogicChainEvaluatorNode(chatService, parser),
                new EvidenceCollectorNode(chatService, parser),
                new ConsistencyCheckNode(chatService, parser),
                new AnswerAnalyzerNode(chatService, parser),
                new FollowUpDecisionNode(chatService, parser, stageManager, objectMapper),
                new StageTransitionNode(stageManager),
                new ProbePlannerNode(objectMapper),
                new ScenarioDirectorNode(chatService, parser),
                new PressureControllerNode(new PressureController()),
                new QuestionRendererNode(chatService, objectMapper));
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
        assertThat(result.claimExtraction().claims()).singleElement()
                .satisfies(claim -> assertThat(claim.content()).contains("事务"));
        assertThat(result.probePlan().targetClaimId()).isEqualTo("current-answer");
        assertThat(result.logicChainResult().gaps()).singleElement();
        assertThat(result.questionPrompt()).contains("一次只提出一个清晰的中文问题", "Java 工程师",
                "结构化追问计划", "使用事务保证数据库操作一致性");
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
                        new Message(Message.Role.USER, "事务用于保证一致性。", LocalDateTime.now())), "", "");
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
            if (prompt.contains("候选人主张提取器")) {
                return """
                        {"claims":[{"type":"FACT","content":"使用事务保证数据库操作一致性",
                        "importance":0.8,"credibility":0.7,"missingEvidence":["具体事务边界"]}]}
                        """;
            }
            if (prompt.contains("逻辑链评估器")) {
                return """
                        {"premises":[],"problemDiagnosis":"需要保证数据库操作一致性","alternatives":[],
                        "decision":"使用事务","reasoning":"将操作作为整体提交或回滚","actions":[],
                        "outcome":"保持一致性","validation":"","reflection":"",
                        "gaps":[{"type":"MISSING_EXECUTION_PATH","description":"未说明具体事务边界",
                        "severity":0.7,"relatedClaimIds":[]}]}
                        """;
            }
            if (prompt.contains("逐轮面试证据收集器")) {
                return """
                        {"evidence":[{"competencyCode":"PROBLEM_SOLVING","signal":"POSITIVE",
                        "strength":0.75,"confidence":0.7,"reason":"能够说明事务的一致性价值",
                        "relatedClaimIds":[]}]}
                        """;
            }
            return responses.remove();
        }

        @Override
        public Flux<String> stream(String prompt) {
            return Flux.error(new UnsupportedOperationException());
        }
    }
}
