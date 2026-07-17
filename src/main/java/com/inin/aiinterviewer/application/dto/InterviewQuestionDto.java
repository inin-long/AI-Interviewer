package com.inin.aiinterviewer.application.dto;

import com.inin.aiinterviewer.domain.enums.InterviewDifficulty;
import com.inin.aiinterviewer.domain.enums.QuestionCategory;

import java.time.LocalDateTime;
import java.util.List;

public record InterviewQuestionDto(
        Long id,
        Long jobId,
        String jobTitle,
        QuestionCategory category,
        String title,
        String content,
        String referenceAnswer,
        InterviewDifficulty difficulty,
        List<String> tags,
        LocalDateTime createTime
) {
}
