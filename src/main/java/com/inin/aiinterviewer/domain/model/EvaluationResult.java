package com.inin.aiinterviewer.domain.model;

import java.util.Map;

public record EvaluationResult(int overallScore, Map<String, Integer> dimensions, String summary) {
    public EvaluationResult {
        dimensions = dimensions == null ? Map.of() : Map.copyOf(dimensions);
    }
}

