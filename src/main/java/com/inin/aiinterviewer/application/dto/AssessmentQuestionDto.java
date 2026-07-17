package com.inin.aiinterviewer.application.dto;

import java.util.List;

public record AssessmentQuestionDto(
        Long id,
        Long templateId,
        String dimension,
        String content,
        List<AssessmentOption> options,
        int sortOrder
) {
}
