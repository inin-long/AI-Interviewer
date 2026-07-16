package com.inin.aiinterviewer.domain.model;

import java.util.List;
import java.util.Map;

public record DomainPack(
        String id,
        String roleCode,
        String industryCode,
        String version,
        String displayName,
        List<CompetencyDefinition> competencies,
        List<MetricDefinition> metrics,
        List<FailurePattern> failurePatterns,
        List<ProbePlaybook> probePlaybooks,
        List<ScenarioTemplate> scenarios,
        List<EvaluationRubric> rubrics
) {
    public DomainPack {
        competencies = immutable(competencies);
        metrics = immutable(metrics);
        failurePatterns = immutable(failurePatterns);
        probePlaybooks = immutable(probePlaybooks);
        scenarios = immutable(scenarios);
        rubrics = immutable(rubrics);
    }

    private static <T> List<T> immutable(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    public record CompetencyDefinition(
            String code, String name, String description, double importance, List<String> indicators
    ) {
        public CompetencyDefinition {
            indicators = immutable(indicators);
        }
    }

    public record MetricDefinition(String code, String name, String description) {
    }

    public record FailurePattern(
            String code, String name, String description, List<String> symptoms, List<String> probes
    ) {
        public FailurePattern {
            symptoms = immutable(symptoms);
            probes = immutable(probes);
        }
    }

    public record ProbePlaybook(
            String code, String objective, List<String> expectedEvidence, List<String> templates
    ) {
        public ProbePlaybook {
            expectedEvidence = immutable(expectedEvidence);
            templates = immutable(templates);
        }
    }

    public record ScenarioTemplate(
            String id,
            String type,
            String objective,
            String background,
            Map<String, Object> variables,
            List<String> constraints,
            List<String> competencies,
            List<Map<String, Object>> events,
            List<String> endConditions
    ) {
        public ScenarioTemplate {
            variables = variables == null ? Map.of() : Map.copyOf(variables);
            constraints = immutable(constraints);
            competencies = immutable(competencies);
            events = immutable(events);
            endConditions = immutable(endConditions);
        }
    }

    public record EvaluationRubric(
            String competencyCode,
            List<String> positiveSignals,
            List<String> negativeSignals,
            List<String> insufficientEvidenceSignals
    ) {
        public EvaluationRubric {
            positiveSignals = immutable(positiveSignals);
            negativeSignals = immutable(negativeSignals);
            insufficientEvidenceSignals = immutable(insufficientEvidenceSignals);
        }
    }
}
