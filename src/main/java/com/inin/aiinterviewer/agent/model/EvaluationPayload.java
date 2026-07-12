package com.inin.aiinterviewer.agent.model;

public record EvaluationPayload(
        int overallScore,
        int technicalScore,
        int problemSolvingScore,
        int projectScore,
        int systemDesignScore,
        int communicationScore,
        int comprehensiveScore,
        String summary
) {
}
