package com.inin.aiinterviewer.domain.model;

import java.util.List;

public record AnswerAnalysis(int correctness, int depth, List<String> missingPoints, String feedback) {
    public AnswerAnalysis {
        missingPoints = missingPoints == null ? List.of() : List.copyOf(missingPoints);
    }
}

