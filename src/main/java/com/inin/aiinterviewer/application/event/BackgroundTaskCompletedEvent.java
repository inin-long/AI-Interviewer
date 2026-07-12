package com.inin.aiinterviewer.application.event;

import com.inin.aiinterviewer.domain.enums.BackgroundTaskType;

public record BackgroundTaskCompletedEvent(long taskId, long userId, BackgroundTaskType taskType) {
}
