package com.inin.aiinterviewer.agent.node;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.inin.aiinterviewer.agent.state.InterviewGraphState;
import com.inin.aiinterviewer.agent.support.StructuredAiResponseParser;
import com.inin.aiinterviewer.domain.enums.ClaimType;
import com.inin.aiinterviewer.domain.enums.InterviewStage;
import com.inin.aiinterviewer.infrastructure.ai.ChatService;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.Queue;

import static org.assertj.core.api.Assertions.assertThat;

class ClaimExtractorNodeTest {

    @Test
    void extractsAndNormalizesValidAtomicClaims() throws Exception {
        QueueChatService chat = new QueueChatService("""
                {"claims":[
                  {"type":"METRIC","content":" 将接口耗时从 800ms 降到 120ms ",
                   "importance":0.95,"credibility":0.75,"missingEvidence":["监控截图","监控截图"]},
                  {"type":"METRIC","content":"将接口耗时从 800ms 降到 120ms",
                   "importance":0.8,"credibility":0.7,"missingEvidence":[]}
                ]}
                """);
        ClaimExtractorNode node = node(chat);

        var result = node.apply(state()).get(InterviewGraphState.CLAIM_EXTRACTION);

        assertThat(result).isInstanceOfSatisfying(
                com.inin.aiinterviewer.agent.model.ClaimExtractionResult.class,
                extraction -> {
                    assertThat(extraction.degraded()).isFalse();
                    assertThat(extraction.claims()).singleElement().satisfies(claim -> {
                        assertThat(claim.type()).isEqualTo(ClaimType.METRIC);
                        assertThat(claim.content()).isEqualTo("将接口耗时从 800ms 降到 120ms");
                        assertThat(claim.missingEvidence()).containsExactly("监控截图");
                    });
                });
        assertThat(chat.calls()).isEqualTo(1);
    }

    @Test
    void retriesOnceWithRepairPrompt() throws Exception {
        QueueChatService chat = new QueueChatService(
                "not-json",
                """
                {"claims":[{"type":"OWNERSHIP","content":"负责订单核心链路",
                "importance":0.9,"credibility":0.7,"missingEvidence":["职责边界"]}]}
                """);

        var extraction = (com.inin.aiinterviewer.agent.model.ClaimExtractionResult)
                node(chat).apply(state()).get(InterviewGraphState.CLAIM_EXTRACTION);

        assertThat(extraction.degraded()).isFalse();
        assertThat(extraction.claims()).singleElement();
        assertThat(chat.calls()).isEqualTo(2);
        assertThat(chat.lastPrompt()).contains("JSON 格式修复器", "not-json");
    }

    @Test
    void degradesWithoutBlockingAfterRepairAlsoFails() throws Exception {
        QueueChatService chat = new QueueChatService(
                "{\"claims\":[{\"type\":\"FACT\",\"content\":\"x\",\"importance\":2,\"credibility\":1}]}",
                "still-invalid");

        var extraction = (com.inin.aiinterviewer.agent.model.ClaimExtractionResult)
                node(chat).apply(state()).get(InterviewGraphState.CLAIM_EXTRACTION);

        assertThat(extraction.degraded()).isTrue();
        assertThat(extraction.claims()).isEmpty();
        assertThat(extraction.failureReason()).isEqualTo("claim_extraction_failed");
        assertThat(chat.calls()).isEqualTo(2);
    }

    private ClaimExtractorNode node(ChatService chatService) {
        return new ClaimExtractorNode(chatService,
                new StructuredAiResponseParser(JsonMapper.builder().findAndAddModules().build()));
    }

    private InterviewGraphState state() {
        return new InterviewGraphState(Map.of(
                InterviewGraphState.STAGE, InterviewStage.PROJECT_EXPERIENCE,
                InterviewGraphState.CURRENT_QUESTION, "你做过哪些性能优化？",
                InterviewGraphState.ANSWER, "我负责优化订单接口，将耗时从 800ms 降到 120ms。",
                InterviewGraphState.DOMAIN_PACK_CONTEXT, "Java 后端领域包",
                InterviewGraphState.CLAIM_LEDGER_CONTEXT, "[]"));
    }

    private static class QueueChatService implements ChatService {
        private final Queue<String> responses = new ArrayDeque<>();
        private int calls;
        private String lastPrompt;

        QueueChatService(String... responses) {
            this.responses.addAll(java.util.List.of(responses));
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

        int calls() {
            return calls;
        }

        String lastPrompt() {
            return lastPrompt;
        }
    }
}
