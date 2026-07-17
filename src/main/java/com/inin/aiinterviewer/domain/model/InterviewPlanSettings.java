package com.inin.aiinterviewer.domain.model;

import com.inin.aiinterviewer.domain.enums.InterviewMode;
import com.inin.aiinterviewer.domain.enums.InterviewerPersona;
import com.inin.aiinterviewer.domain.enums.PressureLevel;
import com.inin.aiinterviewer.domain.enums.VerificationStrictness;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public record InterviewPlanSettings(
        InterviewMode mode,
        InterviewerPersona persona,
        PressureLevel pressureLevel,
        VerificationStrictness strictness,
        int scenarioRatio
) implements Serializable {

    public static final String MODE_KEY = "interviewMode";
    public static final String PERSONA_KEY = "interviewerPersona";
    public static final String PRESSURE_KEY = "pressureLevel";
    public static final String STRICTNESS_KEY = "verificationStrictness";
    public static final String SCENARIO_RATIO_KEY = "scenarioRatio";
    public static final Set<Integer> ALLOWED_SCENARIO_RATIOS = Set.of(0, 20, 30, 50);

    public InterviewPlanSettings {
        mode = mode == null ? InterviewMode.FORMAL_SIMULATION : mode;
        persona = persona == null ? InterviewerPersona.PROFESSIONAL_INTERVIEWER : persona;
        pressureLevel = pressureLevel == null ? PressureLevel.STANDARD : pressureLevel;
        strictness = strictness == null ? VerificationStrictness.STANDARD : strictness;
        if (!ALLOWED_SCENARIO_RATIOS.contains(scenarioRatio)) {
            throw new IllegalArgumentException("Unsupported scenario ratio: " + scenarioRatio);
        }
    }

    public static InterviewPlanSettings defaults() {
        return new InterviewPlanSettings(
                InterviewMode.FORMAL_SIMULATION,
                InterviewerPersona.PROFESSIONAL_INTERVIEWER,
                PressureLevel.STANDARD,
                VerificationStrictness.STANDARD,
                0);
    }

    public static InterviewPlanSettings fromRules(Map<String, Object> rules) {
        Map<String, Object> safeRules = rules == null ? Map.of() : rules;
        InterviewPlanSettings defaults = defaults();
        return new InterviewPlanSettings(
                enumValue(safeRules, MODE_KEY, InterviewMode.class, defaults.mode()),
                enumValue(safeRules, PERSONA_KEY, InterviewerPersona.class, defaults.persona()),
                enumValue(safeRules, PRESSURE_KEY, PressureLevel.class, defaults.pressureLevel()),
                enumValue(safeRules, STRICTNESS_KEY, VerificationStrictness.class, defaults.strictness()),
                intValue(safeRules, SCENARIO_RATIO_KEY, defaults.scenarioRatio()));
    }

    public Map<String, Object> mergeInto(Map<String, Object> rules) {
        LinkedHashMap<String, Object> normalized = new LinkedHashMap<>();
        if (rules != null) normalized.putAll(rules);
        normalized.put(MODE_KEY, mode.name());
        normalized.put(PERSONA_KEY, persona.name());
        normalized.put(PRESSURE_KEY, pressureLevel.name());
        normalized.put(STRICTNESS_KEY, strictness.name());
        normalized.put(SCENARIO_RATIO_KEY, scenarioRatio);
        return Map.copyOf(normalized);
    }

    private static <E extends Enum<E>> E enumValue(
            Map<String, Object> rules,
            String key,
            Class<E> type,
            E defaultValue
    ) {
        Object value = rules.get(key);
        if (value == null || String.valueOf(value).isBlank()) return defaultValue;
        return Enum.valueOf(type, String.valueOf(value).strip().toUpperCase(Locale.ROOT));
    }

    private static int intValue(Map<String, Object> rules, String key, int defaultValue) {
        Object value = rules.get(key);
        if (value == null || String.valueOf(value).isBlank()) return defaultValue;
        if (value instanceof Number number) return number.intValue();
        return Integer.parseInt(String.valueOf(value).strip());
    }
}
