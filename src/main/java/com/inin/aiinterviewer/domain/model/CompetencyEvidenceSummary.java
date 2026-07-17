package com.inin.aiinterviewer.domain.model;

public record CompetencyEvidenceSummary(
        String competencyCode,
        int evidenceCount,
        double positiveStrength,
        double negativeStrength,
        double confidence
) {
}
