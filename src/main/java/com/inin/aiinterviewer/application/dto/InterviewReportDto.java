package com.inin.aiinterviewer.application.dto;

import com.inin.aiinterviewer.agent.model.EvaluationPayload;

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
        List<EvaluationEvidenceDto> evidence,
        Map<String, EvaluationPayload.EvidenceTrace> scoreEvidence,
        double overallConfidence,
        boolean overallScored
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
                Map.of(), List.of(), Map.of(), 0, true);
    }

    public InterviewReportDto(
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
        this(id, interviewId, title, overallScore, dimensions, summary, contentMarkdown,
                confidence, evidence, Map.of(), 0, true);
    }

    public InterviewReportDto {
        confidence = confidence == null ? Map.of() : Map.copyOf(confidence);
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
        scoreEvidence = scoreEvidence == null ? Map.of() : Map.copyOf(scoreEvidence);
        overallConfidence = Math.max(0, Math.min(1, overallConfidence));
    }
}
