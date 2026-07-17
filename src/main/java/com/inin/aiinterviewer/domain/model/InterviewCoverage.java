package com.inin.aiinterviewer.domain.model;

import com.inin.aiinterviewer.agent.model.EvidenceCollectionResult;
import com.inin.aiinterviewer.domain.enums.EvidenceSignal;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record InterviewCoverage(
        Map<String, CompetencyCoverage> competencies
) implements Serializable {

    public InterviewCoverage {
        competencies = competencies == null ? Map.of() : Map.copyOf(competencies);
    }

    public static InterviewCoverage empty() {
        return new InterviewCoverage(Map.of());
    }

    public static InterviewCoverage fromDomainPack(DomainPack pack) {
        if (pack == null) return empty();
        LinkedHashMap<String, CompetencyCoverage> values = new LinkedHashMap<>();
        for (DomainPack.CompetencyDefinition competency : pack.competencies()) {
            values.put(competency.code(), new CompetencyCoverage(
                    competency.importance(), 0, 0, 0, true));
        }
        return new InterviewCoverage(values);
    }

    public InterviewCoverage ensureDomainPack(DomainPack pack) {
        if (pack == null) return this;
        LinkedHashMap<String, CompetencyCoverage> values = new LinkedHashMap<>();
        for (DomainPack.CompetencyDefinition competency : pack.competencies()) {
            CompetencyCoverage current = competencies.get(competency.code());
            values.put(competency.code(), current == null
                    ? new CompetencyCoverage(competency.importance(), 0, 0, 0, true)
                    : current.withImportance(competency.importance()));
        }
        competencies.forEach(values::putIfAbsent);
        return new InterviewCoverage(values);
    }

    public InterviewCoverage update(EvidenceCollectionResult result) {
        if (result == null || result.degraded() || result.evidence().isEmpty()) return this;
        LinkedHashMap<String, CompetencyCoverage> values = new LinkedHashMap<>(competencies);
        Map<String, List<EvidenceCollectionResult.EvidenceCandidate>> grouped = result.evidence().stream()
                .filter(candidate -> !candidate.competencyCode().isBlank())
                .collect(java.util.stream.Collectors.groupingBy(
                        EvidenceCollectionResult.EvidenceCandidate::competencyCode,
                        LinkedHashMap::new, java.util.stream.Collectors.toList()));
        grouped.forEach((code, evidence) -> values.put(code,
                values.getOrDefault(code, CompetencyCoverage.unmapped()).add(evidence)));
        return new InterviewCoverage(values);
    }

    public record CompetencyCoverage(
            double importance,
            int evidenceCount,
            double confidence,
            double coverage,
            boolean needsVerification
    ) implements Serializable {

        public CompetencyCoverage {
            importance = bounded(importance);
            evidenceCount = Math.max(0, evidenceCount);
            confidence = bounded(confidence);
            coverage = bounded(coverage);
        }

        static CompetencyCoverage unmapped() {
            return new CompetencyCoverage(0.5, 0, 0, 0, true);
        }

        CompetencyCoverage withImportance(double value) {
            return new CompetencyCoverage(value, evidenceCount, confidence, coverage, needsVerification);
        }

        CompetencyCoverage add(List<EvidenceCollectionResult.EvidenceCandidate> evidence) {
            int added = evidence.size();
            int nextCount = evidenceCount + added;
            double addedConfidence = evidence.stream()
                    .mapToDouble(EvidenceCollectionResult.EvidenceCandidate::confidence).sum();
            double nextConfidence = (confidence * evidenceCount + addedConfidence)
                    / Math.max(1, nextCount);
            double usefulBreadth = evidence.stream().mapToDouble(item ->
                    item.signal() == EvidenceSignal.INSUFFICIENT ? 0.25 : 1.0).sum();
            double previousBreadth = evidenceCount == 0 || confidence == 0
                    ? 0 : Math.min(3, coverage / confidence * 3);
            double nextCoverage = Math.min(1, (previousBreadth + usefulBreadth) / 3.0)
                    * nextConfidence;
            boolean verify = nextCoverage < 0.65 || nextConfidence < 0.7
                    || evidence.stream().anyMatch(item -> item.signal() == EvidenceSignal.INSUFFICIENT);
            return new CompetencyCoverage(
                    importance, nextCount, nextConfidence, nextCoverage, verify);
        }

        public double priority() {
            return importance * (1.0 - coverage) * (1.15 - confidence * 0.5);
        }

        private static double bounded(double value) {
            if (!Double.isFinite(value)) return 0;
            return Math.max(0, Math.min(1, value));
        }
    }
}
