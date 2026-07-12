package com.inin.aiinterviewer.application.dto;

import java.util.Map;

public record InterviewReportDto(
        long id,
        long interviewId,
        String title,
        int overallScore,
        Map<String, Integer> dimensions,
        String summary,
        String contentMarkdown
) {
}
