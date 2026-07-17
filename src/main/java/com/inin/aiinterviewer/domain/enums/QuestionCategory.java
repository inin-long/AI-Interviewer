package com.inin.aiinterviewer.domain.enums;

public enum QuestionCategory {
    TECHNICAL("技术题"),
    BEHAVIORAL("行为题"),
    SCENARIO("场景题");

    private final String label;

    QuestionCategory(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
