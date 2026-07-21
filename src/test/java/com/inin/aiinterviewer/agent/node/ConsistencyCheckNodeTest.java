package com.inin.aiinterviewer.agent.node;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.inin.aiinterviewer.agent.model.ConsistencyCheckResult;
import com.inin.aiinterviewer.agent.model.ConsistencyContext;
import com.inin.aiinterviewer.agent.state.InterviewGraphState;
import com.inin.aiinterviewer.agent.support.StructuredAiResponseParser;
import com.inin.aiinterviewer.domain.enums.ClaimStatus;
import com.inin.aiinterviewer.domain.enums.ClaimType;
import com.inin.aiinterviewer.domain.enums.ConsistencyIssueStatus;
import com.inin.aiinterviewer.domain.enums.ConsistencyIssueType;
import com.inin.aiinterviewer.domain.enums.InterviewStage;
import com.inin.aiinterviewer.domain.model.ConsistencyIssue;
import com.inin.aiinterviewer.domain.model.InterviewClaim;
import com.inin.aiinterviewer.infrastructure.ai.ChatService;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import static org.assertj.core.api.Assertions.assertThat;

class ConsistencyCheckNodeTest {

    @Test
    void detectsPotentialConflictAndNormalizesClaimIds() throws Exception {
        QueueChatService chat = new QueueChatService("""
                {"issues":[{"issueId":"invented","type":"OWNERSHIP_CONFLICT",
                "description":"两次陈述对架构选型责任的范围不同",
                "relatedClaimIds":["claim-new","claim-old","claim-old"],
                "clarificationQuestion":"请说明你与架构师分别负责哪些设计决策？","confidence":0.82}],
                "resolutions":[]}
                """);

        ConsistencyCheckResult result = result(node(chat).apply(state(true, List.of())));

        assertThat(result.degraded()).isFalse();
        assertThat(result.issues()).singleElement().satisfies(issue -> {
            // 稳定的 issueId 由 "类型:关联主张" 派生生成，便于图内路由澄清探针
            assertThat(issue.issueId()).isEqualTo("OWNERSHIP_CONFLICT:claim-new,claim-old");
            assertThat(issue.type()).isEqualTo(ConsistencyIssueType.OWNERSHIP_CONFLICT);
            assertThat(issue.relatedClaimIds()).containsExactly("claim-new", "claim-old");
            assertThat(issue.clarificationQuestion()).contains("分别负责");
        });
        assertThat(chat.calls).isEqualTo(1);
    }

    @Test
    void resolvesOnlyThroughStructuredClarificationResult() throws Exception {
        ConsistencyIssue issue = new ConsistencyIssue(
                "issue-1", 1, ConsistencyIssueType.OWNERSHIP_CONFLICT,
                ConsistencyIssueStatus.CLARIFIED, "职责范围不同", List.of("claim-old", "claim-new"),
                9L, "请解释职责边界", "", LocalDateTime.now(), LocalDateTime.now());
        QueueChatService chat = new QueueChatService("""
                {"issues":[],"resolutions":[{"issueId":"issue-1","status":"RESOLVED",
                "resolution":"候选人说明自己主导接口设计，架构师负责总体选型，职责并不冲突。",
                "confidence":0.9}]}
                """);

        ConsistencyCheckResult result = result(node(chat).apply(state(true, List.of(issue))));

        assertThat(result.resolutions()).singleElement().satisfies(resolution -> {
            assertThat(resolution.issueId()).isEqualTo("issue-1");
            assertThat(resolution.status()).isEqualTo(ConsistencyIssueStatus.RESOLVED);
        });
    }

    @Test
    void skipsWhenNotScheduledAndDegradesAfterUnsafeRepair() throws Exception {
        QueueChatService skippedChat = new QueueChatService();
        assertThat(result(node(skippedChat).apply(state(false, List.of()))).skipped()).isTrue();
        assertThat(skippedChat.calls).isZero();

        QueueChatService failed = new QueueChatService("""
                {"issues":[{"type":"FACT_CONFLICT","description":"候选人撒谎",
                "relatedClaimIds":["claim-old","claim-new"],
                "clarificationQuestion":"你为什么撒谎？","confidence":0.9}],"resolutions":[]}
                """, "still-invalid");
        ConsistencyCheckResult degraded = result(node(failed).apply(state(true, List.of())));
        assertThat(degraded.degraded()).isTrue();
        assertThat(degraded.failureReason()).isEqualTo("consistency_check_failed");
        assertThat(failed.calls).isEqualTo(2);
        assertThat(failed.lastPrompt).contains("JSON 格式修复器", "禁止人格判断");
    }

    private ConsistencyCheckNode node(ChatService chatService) {
        return new ConsistencyCheckNode(chatService,
                new StructuredAiResponseParser(JsonMapper.builder().findAndAddModules().build()));
    }

    private InterviewGraphState state(boolean runRequested, List<ConsistencyIssue> issues) {
        return new InterviewGraphState(Map.of(
                InterviewGraphState.STAGE, InterviewStage.PROJECT_EXPERIENCE,
                InterviewGraphState.ANSWER, "架构选型主要由架构师决定。",
                InterviewGraphState.CONSISTENCY_CONTEXT, new ConsistencyContext(
                        runRequested, runRequested ? "related_claim_topic" : "not_due",
                        List.of(claim("claim-new", 2, "架构选型主要由架构师决定")),
                        List.of(claim("claim-old", 1, "我主导了技术方案设计")), issues)));
    }

    private InterviewClaim claim(String id, long messageId, String content) {
        return new InterviewClaim(
                id, 1, messageId, ClaimType.OWNERSHIP, content, 0.9, 0.7,
                ClaimStatus.UNVERIFIED, List.of(), List.of(), List.of(),
                LocalDateTime.now(), LocalDateTime.now());
    }

    private ConsistencyCheckResult result(Map<String, Object> output) {
        return (ConsistencyCheckResult) output.get(InterviewGraphState.CONSISTENCY_CHECK_RESULT);
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
