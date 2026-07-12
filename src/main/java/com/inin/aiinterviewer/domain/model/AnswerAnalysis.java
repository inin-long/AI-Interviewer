package com.inin.aiinterviewer.domain.model;

import java.util.List;
import java.io.Serializable;

public record AnswerAnalysis(int correctness, int depth, List<String> missingPoints, String feedback) implements Serializable {
    public AnswerAnalysis {
        missingPoints = missingPoints == null ? List.of() : List.copyOf(missingPoints);
    }
}
