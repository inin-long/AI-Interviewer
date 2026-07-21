package com.inin.aiinterviewer.application.service;

import com.inin.aiinterviewer.application.dto.InterviewPlanDto;
import com.inin.aiinterviewer.domain.enums.InterviewDifficulty;
import com.inin.aiinterviewer.domain.enums.InterviewStage;
import com.inin.aiinterviewer.domain.enums.SimulationType;
import com.inin.aiinterviewer.domain.model.DomainPack;
import com.inin.aiinterviewer.domain.model.DomainPackSnapshot;
import com.inin.aiinterviewer.domain.model.InterviewPlanSettings;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ScenarioSchedulingServiceTest {

    private final ScenarioSchedulingService service = new ScenarioSchedulingService();

    @Test
    void schedulesACompleteBuiltInScenarioAtTheConfiguredQuestionRatio() {
        InterviewPlanDto plan = plan(30);

        assertThat(service.select(7, plan, snapshot(), InterviewStage.SYSTEM_DESIGN, 6)).isEmpty();
        var selected = service.select(
                7, plan, snapshot(), InterviewStage.SYSTEM_DESIGN, 7).orElseThrow();

        assertThat(selected.type()).isEqualTo(SimulationType.INCIDENT_RESPONSE);
        assertThat(selected.candidateRole()).isEqualTo("当班技术负责人");
        assertThat(selected.knownFacts()).containsExactly("流量翻倍");
        assertThat(selected.assumptions()).containsExactly("监控可信");
        assertThat(selected.hiddenInformation())
                .containsEntry("rootCause", "retry_storm")
                .containsEntry("templateId", "incident-template")
                .containsKey("injectableEvents");
        assertThat(selected.constraints()).singleElement()
                .satisfies(value -> assertThat(value.description()).isEqualTo("不能立即扩容"));
        assertThat(selected.maxRounds()).isEqualTo(2);
    }

    @Test
    void disablesScenariosForZeroRatioSummaryAndInsufficientRemainingQuestions() {
        assertThat(service.select(
                7, plan(0), snapshot(), InterviewStage.SYSTEM_DESIGN, 7)).isEmpty();
        assertThat(service.select(
                7, plan(50), snapshot(), InterviewStage.SUMMARY, 5)).isEmpty();
        assertThat(service.select(
                7, plan(50), snapshot(), InterviewStage.SYSTEM_DESIGN, 9)).isEmpty();
    }

    private InterviewPlanDto plan(int ratio) {
        Map<String, Object> rules = new InterviewPlanSettings(
                null, null, null, null, ratio).mergeInto(Map.of("focus", "故障处理"));
        LocalDateTime now = LocalDateTime.of(2026, 7, 17, 0, 0);
        return new InterviewPlanDto(
                1L, "场景方案", "Java 工程师", "核心服务", InterviewDifficulty.SENIOR,
                45, 10, null, null, List.of(), rules,
                List.of("SYSTEM_DESIGN", "SUMMARY"), false, now, now, "test-pack-1.0.0");
    }

    private DomainPackSnapshot snapshot() {
        DomainPack.ScenarioTemplate scenario = new DomainPack.ScenarioTemplate(
                "incident-template", "INCIDENT_RESPONSE", "保护核心链路", "查询服务延迟升高",
                "当班技术负责人", List.of("流量翻倍"), List.of("监控可信"),
                Map.of("rootCause", "retry_storm"), Map.of("databaseCpu", 70),
                List.of("不能立即扩容"), List.of("INCIDENT_RESPONSE"),
                List.of(Map.of(
                        "round", 1, "type", "RESOURCE_SHOCK",
                        "changes", Map.of("databaseCpu", 90))),
                List.of("核心链路恢复"), 3);
        DomainPack pack = new DomainPack(
                "test-pack-1.0.0", "test-role", "test-industry", "1.0.0", "测试知识包",
                List.of(), List.of(), List.of(), List.of(), List.of(scenario), List.of());
        return DomainPackSnapshot.from(pack);
    }
}
