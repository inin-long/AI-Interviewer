package com.inin.aiinterviewer.application.dto;

import com.inin.aiinterviewer.domain.enums.BackgroundTaskStatus;
import com.inin.aiinterviewer.domain.enums.BackgroundTaskType;

import java.time.LocalDateTime;

public record BackgroundTaskDto(
        Long id,
        BackgroundTaskType type,
        BackgroundTaskStatus status,
        int progress,
        int attemptCount,
        String errorMessage,
        LocalDateTime availableTime,
        LocalDateTime startedTime,
        LocalDateTime finishedTime,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
}
