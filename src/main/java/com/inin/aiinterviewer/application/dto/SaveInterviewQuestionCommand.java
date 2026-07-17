package com.inin.aiinterviewer.application.dto;

import com.inin.aiinterviewer.domain.enums.InterviewDifficulty;
import com.inin.aiinterviewer.domain.enums.QuestionCategory;

import java.util.List;

public record SaveInterviewQuestionCommand(
        Long jobId,
        QuestionCategory category,
        String title,
        String content,
        String referenceAnswer,
        InterviewDifficulty difficulty,
        List<String> tags
) {
}
