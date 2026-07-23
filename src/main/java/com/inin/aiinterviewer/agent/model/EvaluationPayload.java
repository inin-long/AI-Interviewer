package com.inin.aiinterviewer.agent.model;

import java.util.List;
import java.util.Map;

public record EvaluationPayload(
        int overallScore,
        int technicalScore,
        int problemSolvingScore,
        int projectScore,
        int communicationScore,
        int comprehensiveScore,
        String summary,
        Map<String, EvidenceTrace> scoreEvidence,
        double overallConfidence,
        boolean overallScored
) {
    public EvaluationPayload(
            int overallScore,
            int technicalScore,
            int problemSolvingScore,
            int projectScore,
            int communicationScore,
            int comprehensiveScore,
            String summary
    ) {
        this(overallScore, technicalScore, problemSolvingScore, projectScore,
                communicationScore, comprehensiveScore, summary,
                Map.of(), 0, true);
    }

    public EvaluationPayload {
        summary = summary == null ? "" : summary.strip();
        scoreEvidence = scoreEvidence == null ? Map.of() : Map.copyOf(scoreEvidence);
        overallConfidence = bounded(overallConfidence);
    }

    public record EvidenceTrace(
            boolean scored,
            double confidence,
            List<String> evidenceIds,
            List<Long> messageIds,
            List<String> claimIds,
            String rationale
    ) {
        public EvidenceTrace {
            confidence = bounded(confidence);
            evidenceIds = immutable(evidenceIds);
            messageIds = messageIds == null ? List.of() : List.copyOf(messageIds);
            claimIds = immutable(claimIds);
            rationale = rationale == null ? "" : rationale.strip();
        }
    }

    private static <T> List<T> immutable(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    private static double bounded(double value) {
        if (!Double.isFinite(value)) return 0;
        return Math.max(0, Math.min(1, value));
    }
}
