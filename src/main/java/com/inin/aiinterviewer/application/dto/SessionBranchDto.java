package com.inin.aiinterviewer.application.dto;

import com.inin.aiinterviewer.domain.enums.SessionBranchStatus;
import com.inin.aiinterviewer.domain.model.BranchComparison;

import java.time.LocalDateTime;

public record SessionBranchDto(
        String id,
        long sourceSessionId,
        long sourceCheckpointId,
        String parentBranchId,
        int sourceQuestionNumber,
        String title,
        SessionBranchStatus status,
        String originalQuestion,
        String originalAnswer,
        String newAnswer,
        BranchComparison comparison,
        String comparisonMarkdown,
        String errorMessage,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
    public SessionBranchDto {
        parentBranchId = parentBranchId == null ? "" : parentBranchId;
        originalQuestion = originalQuestion == null ? "" : originalQuestion;
        originalAnswer = originalAnswer == null ? "" : originalAnswer;
        newAnswer = newAnswer == null ? "" : newAnswer;
        comparisonMarkdown = comparisonMarkdown == null ? "" : comparisonMarkdown;
        errorMessage = errorMessage == null ? "" : errorMessage;
    }
}
