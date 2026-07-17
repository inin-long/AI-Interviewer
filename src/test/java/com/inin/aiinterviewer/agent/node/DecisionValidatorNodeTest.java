package com.inin.aiinterviewer.agent.node;

import com.inin.aiinterviewer.agent.model.ProbePlan;
import com.inin.aiinterviewer.agent.state.InterviewGraphState;
import com.inin.aiinterviewer.application.dto.InterviewPlanDto;
import com.inin.aiinterviewer.domain.enums.InterviewDifficulty;
import com.inin.aiinterviewer.domain.enums.InterviewStage;
import com.inin.aiinterviewer.domain.enums.PressureLevel;
import com.inin.aiinterviewer.domain.enums.ProbeStrategy;
import com.inin.aiinterviewer.domain.model.InterviewCoverage;
import com.inin.aiinterviewer.domain.model.InterviewStrategy;
import com.inin.aiinterviewer.domain.model.Message;
import com.inin.aiinterviewer.domain.model.PressureState;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DecisionValidatorNodeTest {

    private final DecisionValidatorNode validator = new DecisionValidatorNode();

    @Test
    void suppliesMissingVerificationTargetAndEnforcesPressureLimit() throws Exception {
        HashMap<String, Object> values = baseState(InterviewStage.INTRODUCTION, 1);
        values.put(InterviewGraphState.PROBE_PLAN, new ProbePlan(
                "", "", "", "", "", ProbeStrategy.INTRODUCE_FAILURE,
                PressureLevel.HIGH_PRESSURE, "", List.of(), false));
        values.put(InterviewGraphState.PRESSURE_STATE, new PressureState(
                PressureLevel.HIGH_PRESSURE, 3, "stage:test", 1,
                false, false, false, "持续施压"));
        values.put(InterviewGraphState.COVERAGE, new InterviewCoverage(Map.of(
                "COMMUNICATION", new InterviewCoverage.CompetencyCoverage(0.4, 2, 0.8, 0.7, false),
                "SYSTEM_DESIGN", new InterviewCoverage.CompetencyCoverage(0.9, 0, 0, 0, true))));

        Map<String, Object> output = validator.apply(new InterviewGraphState(values));
        ProbePlan probe = (ProbePlan) output.get(InterviewGraphState.PROBE_PLAN);
        InterviewStrategy strategy = (InterviewStrategy) output.get(InterviewGraphState.STRATEGY);
        PressureState pressure = (PressureState) output.get(InterviewGraphState.PRESSURE_STATE);

        assertThat(probe.targetCompetencyCode()).isEqualTo("SYSTEM_DESIGN");
        assertThat(probe.objective()).contains("SYSTEM_DESIGN");
        assertThat(probe.pressureLevel()).isEqualTo(PressureLevel.STANDARD);
        assertThat(pressure.level()).isEqualTo(PressureLevel.STANDARD);
        assertThat(pressure.lowered()).isTrue();
        assertThat(strategy.targetCompetencyCode()).isEqualTo("SYSTEM_DESIGN");
        assertThat(strategy.remainingQuestions()).isEqualTo(3);
    }

    @Test
    void preventsEarlyCompletionButAllowsItAtQuestionLimit() throws Exception {
        Map<String, Object> early = validator.apply(new InterviewGraphState(
                baseState(InterviewStage.COMPLETED, 1)));
        Map<String, Object> complete = validator.apply(new InterviewGraphState(
                baseState(InterviewStage.COMPLETED, 4)));

        assertThat(early.get(InterviewGraphState.STAGE)).isEqualTo(InterviewStage.SUMMARY);
        assertThat(complete.get(InterviewGraphState.STAGE)).isEqualTo(InterviewStage.COMPLETED);
    }

    private HashMap<String, Object> baseState(InterviewStage stage, int askedQuestions) {
        HashMap<String, Object> values = new HashMap<>();
        values.put(InterviewGraphState.STAGE, stage);
        values.put(InterviewGraphState.PLAN, plan());
        values.put(InterviewGraphState.MESSAGES, java.util.stream.IntStream.range(0, askedQuestions)
                .mapToObj(index -> new Message(Message.Role.ASSISTANT, "问题 " + index, LocalDateTime.now()))
                .toList());
        values.put(InterviewGraphState.PROBE_PLAN,
                ProbePlan.stageOpening("验证系统设计能力", "SYSTEM_DESIGN"));
        values.put(InterviewGraphState.COVERAGE, InterviewCoverage.empty());
        values.put(InterviewGraphState.PRESSURE_STATE, PressureState.initial());
        return values;
    }

    private InterviewPlanDto plan() {
        return new InterviewPlanDto(
                1L, "Java 面试", "Java 工程师", "服务端开发",
                InterviewDifficulty.SENIOR, 45, 4, null,
                Map.of("pressureLevel", "HIGH_PRESSURE"),
                List.of("INTRODUCTION", "SUMMARY"), false,
                LocalDateTime.now(), LocalDateTime.now());
    }
}
