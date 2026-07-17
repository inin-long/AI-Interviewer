package com.inin.aiinterviewer.domain.enums;

public enum AssessmentType {
    HOLLAND("霍兰德职业兴趣"),
    MBTI("MBTI 性格类型");

    private final String label;

    AssessmentType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
