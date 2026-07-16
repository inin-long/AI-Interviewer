package com.inin.aiinterviewer.application.dto;

import java.util.Map;
import java.util.List;

public record InterviewReportDto(
        long id,
        long interviewId,
        String title,
        int overallScore,
        Map<String, Integer> dimensions,
        String summary,
        String contentMarkdown,
        Map<String, Double> confidence,
        List<EvaluationEvidenceDto> evidence
) {
    public InterviewReportDto(
            long id,
            long interviewId,
            String title,
            int overallScore,
            Map<String, Integer> dimensions,
            String summary,
            String contentMarkdown
    ) {
        this(id, interviewId, title, overallScore, dimensions, summary, contentMarkdown,
                Map.of(), List.of());
    }

    public InterviewReportDto {
        confidence = confidence == null ? Map.of() : Map.copyOf(confidence);
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
    }
}
