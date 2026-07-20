package com.inin.aiinterviewer.application.dto;

import com.inin.aiinterviewer.domain.enums.InterviewStage;
import com.inin.aiinterviewer.domain.enums.InterviewStatus;

import java.time.LocalDateTime;
import java.util.List;

public record InterviewHistoryItemDto(
        long sessionId,
        String title,
        String jobTitle,
        InterviewStatus status,
        InterviewStage stage,
        LocalDateTime startedTime,
        LocalDateTime completedTime,
        LocalDateTime updateTime,
        int messageCount,
        boolean reportAvailable,
        Integer score,
        String planIconPath,
        String resumeName,
        List<String> tags,
        List<String> sessionStages,
        String interviewSummary,
        String reportStatusText
) {
    public InterviewHistoryItemDto {
        tags = tags == null ? List.of() : List.copyOf(tags);
        sessionStages = sessionStages == null ? List.of() : List.copyOf(sessionStages);
    }
}
