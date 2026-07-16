package com.inin.aiinterviewer.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inin.aiinterviewer.application.dto.SaveInterviewPlanCommand;
import com.inin.aiinterviewer.application.exception.BusinessException;
import com.inin.aiinterviewer.application.exception.ErrorCode;
import com.inin.aiinterviewer.domain.enums.InterviewDifficulty;
import com.inin.aiinterviewer.domain.enums.ScenarioEventType;
import com.inin.aiinterviewer.domain.enums.ScenarioStatus;
import com.inin.aiinterviewer.domain.enums.SimulationType;
import com.inin.aiinterviewer.domain.model.ScenarioAdvanceCommand;
import com.inin.aiinterviewer.domain.model.ScenarioConstraint;
import com.inin.aiinterviewer.domain.model.ScenarioDefinition;
import com.inin.aiinterviewer.domain.model.ScenarioState;
import com.inin.aiinterviewer.infrastructure.database.mapper.ScenarioSessionMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class ScenarioEngineIntegrationTest {

    @TempDir
    static Path applicationHome;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("ai.interviewer.home", () -> applicationHome.toString());
        registry.add("task.enabled", () -> false);
    }

    @Autowired private UserService userService;
    @Autowired private InterviewPlanService planService;
    @Autowired private InterviewSessionService sessionService;
    @Autowired private ScenarioEngine scenarioEngine;
    @Autowired private ScenarioSessionMapper scenarioMapper;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void advancesDecisionDrivenEventsWithoutBreakingTheVariableTimeline() {
        var owner = userService.register("scenario-owner", "Scenario Owner", "safe-password");
        var other = userService.register("scenario-other", "Scenario Other", "safe-password");
        var session = session(owner.id(), "场景引擎验证");
        ScenarioState started = scenarioEngine.start(owner.id(), session.id(), definition(2));

        assertThat(started.status()).isEqualTo(ScenarioStatus.ACTIVE);
        assertThat(started.currentRound()).isZero();
        assertThat(started.initialVariables()).isEqualTo(started.variables());
        assertThatThrownBy(() -> scenarioEngine.start(owner.id(), session.id(), definition(2)))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> scenarioEngine.require(other.id(), session.id(), started.id()))
                .isInstanceOf(BusinessException.class);

        sessionService.appendUserAnswer(owner.id(), session.id(),
                "我先熔断 Redis 依赖并把非核心请求降级，避免流量全部回源数据库。");
        ScenarioState first = scenarioEngine.advance(owner.id(), session.id(), started.id(),
                new ScenarioAdvanceCommand(
                        "熔断缓存依赖并降级非核心请求", "优先保护数据库和核心下单链路",
                        ScenarioEventType.RESOURCE_SHOCK, "Redis 集群不可用且数据库 CPU 升高",
                        Map.of("redisAvailable", false, "databaseCpu", 85),
                        "数据库连接池使用率继续升至 90%，你下一步如何处理？", false));

        assertThat(first.currentRound()).isEqualTo(1);
        assertThat(first.variables()).containsEntry("redisAvailable", false)
                .containsEntry("databaseCpu", 85).containsEntry("connectionPoolUsage", 55);
        assertThat(first.events()).singleElement().satisfies(event -> {
            assertThat(event.triggeredByDecisionId()).isEqualTo(first.decisions().getFirst().id());
            assertThat(event.variablesBefore()).isEqualTo(started.variables());
            assertThat(event.variablesAfter()).isEqualTo(first.variables());
        });
        assertThat(scenarioEngine.findActive(owner.id(), session.id())).contains(first);
        assertThatThrownBy(() -> scenarioEngine.advance(owner.id(), session.id(), started.id(),
                new ScenarioAdvanceCommand(
                        "重复消费回答", "不允许", ScenarioEventType.TRAFFIC_SPIKE, "重复事件",
                        Map.of("connectionPoolUsage", 90), "重复问题", false)))
                .isInstanceOf(BusinessException.class);

        sessionService.appendUserAnswer(owner.id(), session.id(),
                "我会按租户限流并关闭报表查询，再逐步恢复缓存和数据库容量。");
        ScenarioState completed = scenarioEngine.advance(owner.id(), session.id(), started.id(),
                new ScenarioAdvanceCommand(
                        "按租户限流并关闭非核心查询", "保住下单写路径并控制恢复节奏",
                        ScenarioEventType.RECOVERY_SIGNAL, "限流生效，核心链路错误率开始下降",
                        Map.of("connectionPoolUsage", 70, "errorRate", 2.5),
                        "请总结你的恢复顺序和复盘项。", false));

        assertThat(completed.status()).isEqualTo(ScenarioStatus.COMPLETED);
        assertThat(completed.currentRound()).isEqualTo(2);
        assertThat(completed.events()).hasSize(2);
        assertThat(completed.events().get(1).variablesBefore()).isEqualTo(first.variables());
        assertThat(completed.variables()).containsEntry("redisAvailable", false)
                .containsEntry("databaseCpu", 85)
                .containsEntry("connectionPoolUsage", 70)
                .containsEntry("errorRate", 2.5);
        assertThat(scenarioEngine.findActive(owner.id(), session.id())).isEmpty();
        assertThat(scenarioEngine.require(owner.id(), session.id(), started.id())).isEqualTo(completed);

        ScenarioState second = scenarioEngine.start(owner.id(), session.id(), definition(3));
        ScenarioState failed = scenarioEngine.failActive(
                owner.id(), session.id(), "场景导演输出无效，安全返回普通面试");
        assertThat(failed.id()).isEqualTo(second.id());
        assertThat(failed.status()).isEqualTo(ScenarioStatus.FAILED);
        assertThat(failed.terminationReason()).contains("普通面试");

        assertThatCode(() -> sessionService.delete(owner.id(), session.id())).doesNotThrowAnyException();
    }

    @Test
    void rejectsUnknownVariablesAndDetectsPersistedTimelineTampering() throws Exception {
        var owner = userService.register("scenario-timeline", "Scenario Timeline", "safe-password");
        var session = session(owner.id(), "时间线校验");
        ScenarioState started = scenarioEngine.start(owner.id(), session.id(), definition(3));
        sessionService.appendUserAnswer(owner.id(), session.id(), "我先确认影响范围。");

        assertThatThrownBy(() -> scenarioEngine.advance(owner.id(), session.id(), started.id(),
                new ScenarioAdvanceCommand(
                        "确认影响", "避免误操作", ScenarioEventType.DEPENDENCY_FAILURE, "发现新依赖故障",
                        Map.of("unknownVariable", true), "你如何处置？", false)))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).getErrorCode())
                        .isEqualTo(ErrorCode.VALIDATION_FAILED));

        Map<String, Object> tamperedVariables = new LinkedHashMap<>(started.variables());
        tamperedVariables.put("databaseCpu", 99);
        ScenarioState tampered = new ScenarioState(
                started.id(), started.sessionId(), started.type(), started.objective(), started.background(),
                started.candidateRole(), started.knownFacts(), started.assumptions(),
                started.hiddenInformation(), started.initialVariables(), tamperedVariables,
                started.constraints(), started.events(), started.decisions(),
                started.evaluatedCompetencies(), started.endConditions(), started.maxRounds(),
                started.currentRound(), started.status(), started.terminationReason(),
                started.createTime(), started.updateTime());
        assertThat(scenarioMapper.updateState(
                started.id(), owner.id(), session.id(), ScenarioStatus.ACTIVE,
                objectMapper.writeValueAsString(tampered), 0, 0)).isEqualTo(1);

        assertThatThrownBy(() -> scenarioEngine.require(owner.id(), session.id(), started.id()))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).getErrorCode())
                        .isEqualTo(ErrorCode.SCENARIO_STATE_INVALID));
    }

    private com.inin.aiinterviewer.application.dto.InterviewSessionDto session(
            long userId,
            String name
    ) {
        var plan = planService.create(userId, new SaveInterviewPlanCommand(
                name, "Java 工程师", "核心交易系统", InterviewDifficulty.SENIOR,
                45, 6, null, Map.of(), List.of("SYSTEM_DESIGN", "SUMMARY")));
        return sessionService.create(userId, plan.id());
    }

    private ScenarioDefinition definition(int maxRounds) {
        return new ScenarioDefinition(
                SimulationType.INCIDENT_RESPONSE,
                "在缓存与数据库双重压力下保护核心下单链路",
                "促销开始后错误率上升，候选人担任事故指挥者。",
                "事故指挥者",
                List.of("峰值 QPS 为 20000", "Redis 和 MySQL 均为现有基础设施"),
                List.of("不允许立即扩容", "允许关闭非核心能力"),
                Map.of("rootCause", "cache_hot_key"),
                Map.of(
                        "redisAvailable", true,
                        "databaseCpu", 40,
                        "connectionPoolUsage", 55,
                        "errorRate", 8.0),
                List.of(
                        new ScenarioConstraint("NO_SCALE", "事故前十分钟不能扩容", true, true),
                        new ScenarioConstraint("NO_OVERSOLD", "不能产生超卖", true, true)),
                List.of("INCIDENT_RESPONSE", "PRIORITIZATION", "COMMUNICATION"),
                List.of("核心链路恢复", "给出复盘与预防措施"),
                maxRounds);
    }
}
