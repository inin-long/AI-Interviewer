package com.inin.aiinterviewer.agent.node;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.inin.aiinterviewer.agent.model.ProbePlan;
import com.inin.aiinterviewer.agent.model.ScenarioDirectionResult;
import com.inin.aiinterviewer.agent.state.InterviewGraphState;
import com.inin.aiinterviewer.agent.support.StructuredAiResponseParser;
import com.inin.aiinterviewer.domain.enums.PressureLevel;
import com.inin.aiinterviewer.domain.enums.ProbeStrategy;
import com.inin.aiinterviewer.domain.enums.ScenarioEventType;
import com.inin.aiinterviewer.domain.enums.ScenarioStatus;
import com.inin.aiinterviewer.domain.enums.SimulationType;
import com.inin.aiinterviewer.domain.model.ScenarioState;
import com.inin.aiinterviewer.infrastructure.ai.ChatService;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import static org.assertj.core.api.Assertions.assertThat;

class ScenarioDirectorNodeTest {

    @Test
    void turnsCandidateDecisionIntoCausalScenarioEvent() throws Exception {
        QueueChatService chat = new QueueChatService("""
                {"decisionAction":"启用熔断并将读流量切到降级缓存",
                "decisionRationale":"先保护主数据库并维持核心查询可用",
                "eventType":"DEPENDENCY_FAILURE",
                "eventDescription":"缓存依赖完全不可用，降级读流量回源导致数据库 CPU 上升",
                "changes":{"cacheAvailable":false,"databaseCpu":86},
                "nextQuestion":"缓存完全不可用且数据库 CPU 已升至 86%，你接下来如何保障核心查询？",
                "completeAfterEvent":false}
                """);

        Map<String, Object> output = node(chat).apply(state(activeScenario(), "先启用熔断并切换到降级缓存。"));

        ScenarioDirectionResult result = direction(output);
        ProbePlan plan = (ProbePlan) output.get(InterviewGraphState.PROBE_PLAN);
        assertThat(result.handled()).isTrue();
        assertThat(result.eventType()).isEqualTo(ScenarioEventType.DEPENDENCY_FAILURE);
        assertThat(result.changes()).containsEntry("cacheAvailable", false)
                .containsEntry("databaseCpu", 86);
        assertThat(plan.shouldInjectScenario()).isTrue();
        assertThat(plan.strategy()).isEqualTo(ProbeStrategy.INTRODUCE_FAILURE);
        assertThat(plan.objective()).isEqualTo(result.nextQuestion());
        assertThat(plan.expectedEvidence()).containsExactly("故障处置", "权衡分析");
        assertThat(chat.calls).isEqualTo(1);
        assertThat(chat.lastPrompt).contains("databasePrimaryLag", "仅供导演推演");
    }

    @Test
    void repairsInvalidVariableUpdateOnce() throws Exception {
        QueueChatService chat = new QueueChatService(
                """
                        {"decisionAction":"扩容","decisionRationale":"降低负载",
                        "eventType":"RESOURCE_SHOCK","eventDescription":"容量不足",
                        "changes":{"undeclaredCapacity":2},"nextQuestion":"如何处理？",
                        "completeAfterEvent":false}
                        """,
                """
                        {"decisionAction":"扩容数据库只读实例","decisionRationale":"分担查询负载",
                        "eventType":"RESOURCE_SHOCK","eventDescription":"新增实例仍在预热，数据库 CPU 暂时升至 92%",
                        "changes":{"databaseCpu":92},
                        "nextQuestion":"只读实例尚未完成预热且数据库 CPU 达到 92%，你如何安排流量切换？",
                        "completeAfterEvent":false}
                        """);

        ScenarioDirectionResult result = direction(node(chat).apply(
                state(activeScenario(), "我会先扩容只读实例。")));

        assertThat(result.handled()).isTrue();
        assertThat(result.changes()).containsOnlyKeys("databaseCpu");
        assertThat(chat.calls).isEqualTo(2);
        assertThat(chat.lastPrompt).contains("修复", "只能使用当前 variables");
    }

    @Test
    void degradesSafelyAfterUnsafeRepairAndKeepsRegularProbe() throws Exception {
        QueueChatService chat = new QueueChatService(
                "not-json",
                """
                        {"decisionAction":"质疑候选人","decisionRationale":"施加压力",
                        "eventType":"STAKEHOLDER_ESCALATION","eventDescription":"负责人指责候选人无能",
                        "changes":{"databaseCpu":90},"nextQuestion":"你为什么这么无能？",
                        "completeAfterEvent":false}
                        """);

        Map<String, Object> output = node(chat).apply(state(activeScenario(), "我会观察指标。"));

        ScenarioDirectionResult result = direction(output);
        assertThat(result.degraded()).isTrue();
        assertThat(result.failureReason()).isEqualTo("scenario_direction_failed");
        assertThat(output).doesNotContainKey(InterviewGraphState.PROBE_PLAN);
        assertThat(chat.calls).isEqualTo(2);
    }

    @Test
    void skipsWithoutActiveScenarioOrCandidateDecision() throws Exception {
        QueueChatService chat = new QueueChatService();

        assertThat(direction(node(chat).apply(state(null, "正常回答"))).skipped()).isTrue();
        assertThat(direction(node(chat).apply(state(activeScenario(), ""))).skipped()).isTrue();
        assertThat(chat.calls).isZero();
    }

    @Test
    void rendersSafeKickoffBeforeTreatingAnswersAsScenarioDecisions() throws Exception {
        QueueChatService chat = new QueueChatService();

        Map<String, Object> output = node(chat).apply(
                state(activeScenario(false), "这是上一道普通问题的回答。"));

        ScenarioDirectionResult result = direction(output);
        ProbePlan plan = (ProbePlan) output.get(InterviewGraphState.PROBE_PLAN);
        assertThat(result.kickoff()).isTrue();
        assertThat(result.handled()).isFalse();
        assertThat(plan.shouldInjectScenario()).isTrue();
        assertThat(plan.objective()).contains(
                        "核心查询服务突发延迟", "当班技术负责人", "你首先会采取什么行动")
                .doesNotContain("databasePrimaryLag", "rootCause");
        assertThat(chat.calls).isZero();
    }

    private ScenarioDirectorNode node(ChatService chatService) {
        return new ScenarioDirectorNode(chatService,
                new StructuredAiResponseParser(JsonMapper.builder().findAndAddModules().build()));
    }

    private InterviewGraphState state(ScenarioState scenario, String answer) {
        Map<String, Object> data = new java.util.HashMap<>();
        data.put(InterviewGraphState.ANSWER, answer);
        data.put(InterviewGraphState.PROBE_PLAN, new ProbePlan(
                "claim-1", "验证故障处理依据", ProbeStrategy.REQUEST_BASELINE,
                PressureLevel.STANDARD, "验证候选人决策", List.of("处置步骤"), false));
        if (scenario != null) data.put(InterviewGraphState.ACTIVE_SCENARIO, scenario);
        return new InterviewGraphState(data);
    }

    private ScenarioState activeScenario() {
        return activeScenario(true);
    }

    private ScenarioState activeScenario(boolean introduced) {
        LocalDateTime now = LocalDateTime.of(2026, 7, 17, 0, 0);
        return new ScenarioState(
                "scenario-1", 12, SimulationType.INCIDENT_RESPONSE,
                "验证故障处置与取舍", "核心查询服务突发延迟", "当班技术负责人",
                List.of("流量较平时增加一倍"), List.of("数据库可扩容"),
                Map.of("rootCause", "databasePrimaryLag"),
                Map.of("cacheAvailable", true, "databaseCpu", 68),
                Map.of("cacheAvailable", true, "databaseCpu", 68),
                List.of(), List.of(), List.of(), List.of("故障处置", "权衡分析"),
                List.of("核心查询恢复稳定"), introduced, 3, 0, ScenarioStatus.ACTIVE, "", now, now);
    }

    private ScenarioDirectionResult direction(Map<String, Object> output) {
        return (ScenarioDirectionResult) output.get(InterviewGraphState.SCENARIO_DIRECTION_RESULT);
    }

    private static final class QueueChatService implements ChatService {
        private final Queue<String> responses = new ArrayDeque<>();
        private int calls;
        private String lastPrompt = "";

        private QueueChatService(String... responses) {
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
