package com.inin.aiinterviewer.agent.node;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.inin.aiinterviewer.agent.model.ClaimExtractionResult;
import com.inin.aiinterviewer.agent.model.EvidenceCollectionResult;
import com.inin.aiinterviewer.agent.model.LogicChainResult;
import com.inin.aiinterviewer.agent.state.InterviewGraphState;
import com.inin.aiinterviewer.agent.support.StructuredAiResponseParser;
import com.inin.aiinterviewer.domain.enums.EvidenceSignal;
import com.inin.aiinterviewer.domain.enums.InterviewStage;
import com.inin.aiinterviewer.infrastructure.ai.ChatService;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import static org.assertj.core.api.Assertions.assertThat;

class EvidenceCollectorNodeTest {

    @Test
    void collectsAndDeduplicatesEvidenceWhileKeepingInsufficientDistinct() throws Exception {
        QueueChatService chat = new QueueChatService("""
                {"evidence":[
                {"competencyCode":"SYSTEM_DESIGN","signal":"POSITIVE","strength":0.8,
                "confidence":0.7,"reason":"能够说明拆分事务边界的决策","relatedClaimIds":["claim-1","claim-1"]},
                {"competencyCode":"SYSTEM_DESIGN","signal":"POSITIVE","strength":0.6,
                "confidence":0.5,"reason":"能够说明拆分事务边界的决策","relatedClaimIds":[]},
                {"competencyCode":"FAILURE_HANDLING","signal":"INSUFFICIENT","strength":0.1,
                "confidence":0.9,"reason":"没有说明持续失败时的恢复路径","relatedClaimIds":[]}]}
                """);

        EvidenceCollectionResult result = result(node(chat).apply(state()));

        assertThat(result.degraded()).isFalse();
        assertThat(result.evidence()).hasSize(2);
        assertThat(result.evidence().getFirst().relatedClaimIds()).containsExactly("claim-1");
        assertThat(result.evidence().getLast().signal()).isEqualTo(EvidenceSignal.INSUFFICIENT);
        assertThat(chat.calls).isEqualTo(1);
    }

    @Test
    void repairsInvalidResponseOnceAndThenDegradesWithoutBlocking() throws Exception {
        QueueChatService repaired = new QueueChatService("invalid", validEvidence());
        assertThat(result(node(repaired).apply(state())).degraded()).isFalse();
        assertThat(repaired.calls).isEqualTo(2);
        assertThat(repaired.lastPrompt).contains("JSON 格式修复器", "invalid");

        QueueChatService failed = new QueueChatService(
                "{\"evidence\":[{\"competencyCode\":\"INVALID\",\"signal\":\"NEGATIVE\","
                        + "\"strength\":2,\"confidence\":1,\"reason\":\"越界\",\"relatedClaimIds\":[]}]}",
                "still-invalid");
        EvidenceCollectionResult degraded = result(node(failed).apply(state()));
        assertThat(degraded.degraded()).isTrue();
        assertThat(degraded.failureReason()).isEqualTo("evidence_collection_failed");
        assertThat(failed.calls).isEqualTo(2);
    }

    private EvidenceCollectorNode node(ChatService chatService) {
        return new EvidenceCollectorNode(chatService,
                new StructuredAiResponseParser(JsonMapper.builder().findAndAddModules().build()));
    }

    private InterviewGraphState state() {
        return new InterviewGraphState(Map.of(
                InterviewGraphState.STAGE, InterviewStage.SYSTEM_DESIGN,
                InterviewGraphState.CURRENT_QUESTION, "请说明事务边界设计。",
                InterviewGraphState.ANSWER, "我拆分事务边界并使用 Outbox。",
                InterviewGraphState.CLAIM_EXTRACTION, new ClaimExtractionResult(List.of()),
                InterviewGraphState.LOGIC_CHAIN_RESULT, LogicChainResult.skippedResult(),
                InterviewGraphState.DOMAIN_PACK_CONTEXT, "SYSTEM_DESIGN",
                InterviewGraphState.EVIDENCE_LEDGER_CONTEXT, "[]"));
    }

    private EvidenceCollectionResult result(Map<String, Object> output) {
        return (EvidenceCollectionResult) output.get(InterviewGraphState.EVIDENCE_COLLECTION_RESULT);
    }

    private String validEvidence() {
        return """
                {"evidence":[{"competencyCode":"PROBLEM_SOLVING","signal":"POSITIVE",
                "strength":0.7,"confidence":0.65,"reason":"给出了可执行决策","relatedClaimIds":[]}]}
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
