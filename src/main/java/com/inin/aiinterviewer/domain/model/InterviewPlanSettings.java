package com.inin.aiinterviewer.domain.model;

import com.inin.aiinterviewer.domain.enums.InterviewMode;
import com.inin.aiinterviewer.domain.enums.InterviewerPersona;
import com.inin.aiinterviewer.domain.enums.PressureLevel;
import com.inin.aiinterviewer.domain.enums.VerificationStrictness;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger log = LoggerFactory.getLogger(InterviewPlanSettings.class);

    public static final String MODE_KEY = "interviewMode";
    public static final String PERSONA_KEY = "interviewerPersona";
    public static final String PRESSURE_KEY = "pressureLevel";
    public static final String STRICTNESS_KEY = "verificationStrictness";
    public static final String SCENARIO_RATIO_KEY = "scenarioRatio";
    public static final String SCENARIO_KEY = "scenario";
    public static final String ANSWER_TIME_LIMIT_KEY = "answerTimeLimitSeconds";
    public static final Set<Integer> ALLOWED_SCENARIO_RATIOS = Set.of(0, 20, 30, 50);

    public InterviewPlanSettings {
        mode = mode == null ? InterviewMode.FORMAL_SIMULATION : mode;
        persona = persona == null ? InterviewerPersona.PROFESSIONAL_INTERVIEWER : persona;
        pressureLevel = pressureLevel == null ? PressureLevel.STANDARD : pressureLevel;
        strictness = strictness == null ? VerificationStrictness.STANDARD : strictness;
        scenarioRatio = normalizeScenarioRatio(scenarioRatio);
    }

    private static int normalizeScenarioRatio(int ratio) {
        if (ALLOWED_SCENARIO_RATIOS.contains(ratio)) return ratio;
        int nearest = 0;
        int bestDiff = Integer.MAX_VALUE;
        for (int allowed : ALLOWED_SCENARIO_RATIOS) {
            int diff = Math.abs(allowed - ratio);
            if (diff < bestDiff) {
                bestDiff = diff;
                nearest = allowed;
            }
        }
        log.warn("scenarioRatio {} 不在允许集合，已就近归一到 {}", ratio, nearest);
        return nearest;
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
        return new LinkedHashMap<>(normalized);
    }

    public static String scenarioOf(Map<String, Object> rules) {
        Object value = rules == null ? null : rules.get(SCENARIO_KEY);
        return value == null ? "" : String.valueOf(value).strip();
    }

    public static Integer answerTimeLimitSecondsOf(Map<String, Object> rules) {
        Object value = rules == null ? null : rules.get(ANSWER_TIME_LIMIT_KEY);
        // 默认每题 3 分钟（180 秒），保证每题倒计时默认可见，而非永远隐藏。
        if (value == null || String.valueOf(value).isBlank()) return 180;
        try {
            int seconds = value instanceof Number
                    ? ((Number) value).intValue()
                    : Integer.parseInt(String.valueOf(value).strip());
            return seconds <= 0 ? 180 : seconds;
        } catch (NumberFormatException exception) {
            return 180;
        }
    }

    private static <E extends Enum<E>> E enumValue(
            Map<String, Object> rules,
            String key,
            Class<E> type,
            E defaultValue
    ) {
        Object value = rules.get(key);
        if (value == null || String.valueOf(value).isBlank()) return defaultValue;
        try {
            return Enum.valueOf(type, String.valueOf(value).strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            log.warn("Unknown {} value '{}' in plan rules, fallback to default {}.",
                    type.getSimpleName(), value, defaultValue);
            return defaultValue;
        }
    }

    private static int intValue(Map<String, Object> rules, String key, int defaultValue) {
        Object value = rules.get(key);
        if (value == null || String.valueOf(value).isBlank()) return defaultValue;
        if (value instanceof Number number) return number.intValue();
        try {
            return Integer.parseInt(String.valueOf(value).strip());
        } catch (NumberFormatException exception) {
            log.warn("计划规则 {} 的值 '{}' 不是合法整数，回退到默认值 {}", key, value, defaultValue);
            return defaultValue;
        }
    }
}
