package com.inin.aiinterviewer.domain.model;

import java.util.List;

public record BranchComparison(
        double originalLogicCompleteness,
        double newLogicCompleteness,
        int originalEvidenceCount,
        int newEvidenceCount,
        int originalEvidenceScore,
        int newEvidenceScore,
        String originalFollowUp,
        String branchFollowUp,
        boolean viewpointRevised,
        List<String> resolvedGapTypes,
        List<String> remainingGapTypes,
        String summary
) {
    public BranchComparison {
        originalFollowUp = text(originalFollowUp);
        branchFollowUp = text(branchFollowUp);
        resolvedGapTypes = resolvedGapTypes == null ? List.of() : List.copyOf(resolvedGapTypes);
        remainingGapTypes = remainingGapTypes == null ? List.of() : List.copyOf(remainingGapTypes);
        summary = text(summary);
    }

    private static String text(String value) {
        return value == null ? "" : value.strip();
    }
}
