package com.inin.aiinterviewer.agent.node;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.inin.aiinterviewer.agent.model.ClaimExtractionResult;
import com.inin.aiinterviewer.agent.model.LogicChainResult;
import com.inin.aiinterviewer.agent.state.InterviewGraphState;
import com.inin.aiinterviewer.agent.support.StructuredAiResponseParser;
import com.inin.aiinterviewer.domain.enums.ClaimType;
import com.inin.aiinterviewer.domain.enums.LogicGapType;
import com.inin.aiinterviewer.infrastructure.ai.ChatService;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import static org.assertj.core.api.Assertions.assertThat;

class LogicChainEvaluatorNodeTest {

    @Test
    void evaluatesImportantAnswerAndNormalizesItsLogicGaps() throws Exception {
        QueueChatService chat = new QueueChatService("""
                {"premises":["数据库写入成为瓶颈","数据库写入成为瓶颈"],
                "problemDiagnosis":"高峰期同步写入超时","alternatives":["扩容","消息队列"],
                "decision":"引入消息队列","reasoning":"异步削峰","actions":["写入 Kafka"],
                "outcome":"P99 降低 40%","validation":"通过监控验证","reflection":"补充降级",
                "gaps":[{"type":"MISSING_BASELINE","description":"未给出优化前 P99",
                "severity":0.82,"relatedClaimIds":["claim-1","claim-1"]}]}
                """);

        LogicChainResult result = result(node(chat).apply(importantState()));

        assertThat(result.degraded()).isFalse();
        assertThat(result.skipped()).isFalse();
        assertThat(result.premises()).containsExactly("数据库写入成为瓶颈");
        assertThat(result.gaps()).singleElement().satisfies(gap -> {
            assertThat(gap.type()).isEqualTo(LogicGapType.MISSING_BASELINE);
            assertThat(gap.severity()).isEqualTo(0.82);
            assertThat(gap.relatedClaimIds()).containsExactly("claim-1");
        });
        assertThat(chat.calls).isEqualTo(1);
    }

    @Test
    void skipsShortLowImportanceAnswerWithoutCallingAi() throws Exception {
        QueueChatService chat = new QueueChatService();
        InterviewGraphState state = state(new ClaimExtractionResult(List.of(
                new ClaimExtractionResult.ClaimCandidate(
                        ClaimType.OPINION, "我觉得可以", 0.2, 0.4, List.of()))), "可以。");

        LogicChainResult result = result(node(chat).apply(state));

        assertThat(result.skipped()).isTrue();
        assertThat(chat.calls).isZero();
    }

    @Test
    void repairsOnceThenDegradesWithoutBlocking() throws Exception {
        QueueChatService repairedChat = new QueueChatService("invalid", validEmptyResult());
        assertThat(result(node(repairedChat).apply(importantState())).degraded()).isFalse();
        assertThat(repairedChat.calls).isEqualTo(2);
        assertThat(repairedChat.lastPrompt).contains("JSON 格式修复器", "invalid");

        QueueChatService failedChat = new QueueChatService("invalid", "still-invalid");
        LogicChainResult degraded = result(node(failedChat).apply(importantState()));
        assertThat(degraded.degraded()).isTrue();
        assertThat(degraded.failureReason()).isEqualTo("logic_chain_evaluation_failed");
        assertThat(failedChat.calls).isEqualTo(2);
    }

    private LogicChainEvaluatorNode node(ChatService chatService) {
        return new LogicChainEvaluatorNode(chatService,
                new StructuredAiResponseParser(JsonMapper.builder().findAndAddModules().build()));
    }

    private InterviewGraphState importantState() {
        return state(new ClaimExtractionResult(List.of(
                new ClaimExtractionResult.ClaimCandidate(
                        ClaimType.METRIC, "P99 降低 40%", 0.95, 0.65, List.of("优化前基线")))),
                "我发现数据库同步写入导致高峰期接口超时，于是选择 Kafka 异步削峰，P99 降低了 40%。");
    }

    private InterviewGraphState state(ClaimExtractionResult extraction, String answer) {
        return new InterviewGraphState(Map.of(
                InterviewGraphState.CURRENT_QUESTION, "请说明一次性能优化。",
                InterviewGraphState.ANSWER, answer,
                InterviewGraphState.CLAIM_EXTRACTION, extraction,
                InterviewGraphState.CLAIM_LEDGER_CONTEXT, "[]",
                InterviewGraphState.DOMAIN_PACK_CONTEXT, "Java 后端"));
    }

    private LogicChainResult result(Map<String, Object> output) {
        return (LogicChainResult) output.get(InterviewGraphState.LOGIC_CHAIN_RESULT);
    }

    private String validEmptyResult() {
        return """
                {"premises":[],"problemDiagnosis":"","alternatives":[],"decision":"",
                "reasoning":"","actions":[],"outcome":"","validation":"","reflection":"","gaps":[]}
                """;
    }

    private static class QueueChatService implements ChatService {
        private final Queue<String> responses = new ArrayDeque<>();
        private int calls;
        private String lastPrompt;

        QueueChatService(String... responses) {
            this.responses.addAll(List.of(responses));
        }

        @Override
        public String chat(String prompt) {
            calls++;
            lastPrompt = prompt;
            return responses.remove();
        }

        @Override
        public Flux<String> stream(String prompt) {
            return Flux.error(new UnsupportedOperationException());
        }
    }
}
