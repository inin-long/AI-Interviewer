package com.inin.aiinterviewer.application.event;

import com.inin.aiinterviewer.domain.enums.BackgroundTaskType;

public record BackgroundTaskStartedEvent(long taskId, long userId, BackgroundTaskType taskType) {
}
