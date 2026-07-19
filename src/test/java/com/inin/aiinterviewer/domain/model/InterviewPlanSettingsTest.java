package com.inin.aiinterviewer.domain.model;

import com.inin.aiinterviewer.domain.enums.InterviewMode;
import com.inin.aiinterviewer.domain.enums.InterviewerPersona;
import com.inin.aiinterviewer.domain.enums.PressureLevel;
import com.inin.aiinterviewer.domain.enums.VerificationStrictness;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InterviewPlanSettingsTest {

    @Test
    void appliesStableDefaultsAndPreservesExtensionRules() {
        InterviewPlanSettings settings = InterviewPlanSettings.fromRules(
                Map.of("focus", "数据库与故障处理"));

        assertThat(settings).isEqualTo(InterviewPlanSettings.defaults());
        assertThat(settings.mergeInto(Map.of("focus", "数据库与故障处理")))
                .containsEntry("focus", "数据库与故障处理")
                .containsEntry(InterviewPlanSettings.MODE_KEY, "FORMAL_SIMULATION")
                .containsEntry(InterviewPlanSettings.PERSONA_KEY, "FRIENDLY")
                .containsEntry(InterviewPlanSettings.PRESSURE_KEY, "STANDARD")
                .containsEntry(InterviewPlanSettings.STRICTNESS_KEY, "STANDARD")
                .containsEntry(InterviewPlanSettings.SCENARIO_RATIO_KEY, 0);
    }

    @Test
    void parsesTypedPlanControlsAndRejectsUnsupportedValues() {
        InterviewPlanSettings settings = InterviewPlanSettings.fromRules(Map.of(
                InterviewPlanSettings.MODE_KEY, "scenario_simulation",
                InterviewPlanSettings.PERSONA_KEY, "pressure",
                InterviewPlanSettings.PRESSURE_KEY, "challenging",
                InterviewPlanSettings.STRICTNESS_KEY, "strict",
                InterviewPlanSettings.SCENARIO_RATIO_KEY, "30"));

        assertThat(settings.mode()).isEqualTo(InterviewMode.SCENARIO_SIMULATION);
        assertThat(settings.persona()).isEqualTo(InterviewerPersona.PRESSURE);
        assertThat(settings.pressureLevel()).isEqualTo(PressureLevel.CHALLENGING);
        assertThat(settings.strictness()).isEqualTo(VerificationStrictness.STRICT);
        assertThat(settings.scenarioRatio()).isEqualTo(30);
        assertThatThrownBy(() -> InterviewPlanSettings.fromRules(Map.of(
                InterviewPlanSettings.SCENARIO_RATIO_KEY, 40)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> InterviewPlanSettings.fromRules(Map.of(
                InterviewPlanSettings.MODE_KEY, "UNSUPPORTED")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
