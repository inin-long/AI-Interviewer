package com.inin.aiinterviewer.domain.model;

import com.inin.aiinterviewer.domain.enums.EvidenceSignal;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record EvidenceLedger(List<EvaluationEvidence> evidence) {
    public EvidenceLedger {
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
    }

    public static EvidenceLedger empty() {
        return new EvidenceLedger(List.of());
    }

    public Map<String, CompetencyEvidenceSummary> summaries() {
        Map<String, List<EvaluationEvidence>> grouped = evidence.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        EvaluationEvidence::competencyCode, LinkedHashMap::new,
                        java.util.stream.Collectors.toList()));
        LinkedHashMap<String, CompetencyEvidenceSummary> summaries = new LinkedHashMap<>();
        grouped.forEach((code, values) -> summaries.put(code, summarize(code, values)));
        return Map.copyOf(summaries);
    }

    private CompetencyEvidenceSummary summarize(String code, List<EvaluationEvidence> values) {
        double positive = values.stream().filter(value -> value.signal() == EvidenceSignal.POSITIVE)
                .mapToDouble(EvaluationEvidence::strength).sum();
        double negative = values.stream().filter(value -> value.signal() == EvidenceSignal.NEGATIVE)
                .mapToDouble(EvaluationEvidence::strength).sum();
        double weight = values.stream().mapToDouble(value -> Math.max(0.1, value.strength())).sum();
        double weightedConfidence = values.stream()
                .mapToDouble(value -> value.confidence() * Math.max(0.1, value.strength())).sum()
                / Math.max(0.1, weight);
        double breadth = 1.0 - Math.exp(-values.size() / 2.0);
        double confidence = Math.min(1.0, weightedConfidence * breadth);
        return new CompetencyEvidenceSummary(
                code, values.size(), positive, negative, confidence);
    }
}
