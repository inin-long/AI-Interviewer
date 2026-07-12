package com.inin.aiinterviewer.application.task;

import com.inin.aiinterviewer.domain.enums.BackgroundTaskType;

public interface BackgroundTaskHandler {
    BackgroundTaskType taskType();

    void handle(BackgroundTaskContext context) throws Exception;
}
