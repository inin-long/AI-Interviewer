package com.inin.aiinterviewer.application.event;

import com.inin.aiinterviewer.domain.enums.BackgroundTaskType;

public record BackgroundTaskQueuedEvent(long taskId, long userId, BackgroundTaskType taskType) {
}
