package com.inin.aiinterviewer.application.dto;

import com.inin.aiinterviewer.domain.enums.BackgroundTaskStatus;

public record ReportGenerationTaskStateDto(
        InterviewCompletionStateDto completion,
        Long taskId,
        BackgroundTaskStatus taskStatus,
        int attemptCount,
        String taskErrorMessage
) {
    public ReportGenerationTaskStateDto {
        taskErrorMessage = taskErrorMessage == null ? "" : taskErrorMessage.strip();
    }

    public boolean active() {
        return taskStatus == BackgroundTaskStatus.PENDING
                || taskStatus == BackgroundTaskStatus.RUNNING;
    }

    public boolean manualRetryAvailable() {
        return completion.retryable() && !active();
    }
}
