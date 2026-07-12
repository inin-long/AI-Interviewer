package com.inin.aiinterviewer.application.dto;

import com.inin.aiinterviewer.domain.enums.InterviewStage;
import com.inin.aiinterviewer.domain.enums.InterviewStatus;

import java.time.LocalDateTime;

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
        Integer score
) {
}
