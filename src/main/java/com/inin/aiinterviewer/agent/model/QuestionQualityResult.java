package com.inin.aiinterviewer.agent.model;

import java.util.List;

public record QuestionQualityResult(boolean approved, List<QuestionQualityIssue> issues) {

    public QuestionQualityResult {
        issues = issues == null ? List.of() : List.copyOf(issues);
        approved = approved && issues.isEmpty();
    }

    public static QuestionQualityResult pass() {
        return new QuestionQualityResult(true, List.of());
    }

    public static QuestionQualityResult rejected(List<QuestionQualityIssue> issues) {
        return new QuestionQualityResult(false, issues);
    }
}
