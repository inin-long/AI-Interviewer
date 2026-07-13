package com.inin.aiinterviewer.application.dto;

import com.inin.aiinterviewer.domain.enums.ReportStatus;

public record InterviewCompletionStateDto(
        boolean finalAnswerSaved,
        ReportStatus reportStatus,
        String failureMessage
) {
    public InterviewCompletionStateDto {
        reportStatus = reportStatus == null ? ReportStatus.NOT_STARTED : reportStatus;
        failureMessage = failureMessage == null ? "" : failureMessage.strip();
    }

    public boolean retryable() {
        return finalAnswerSaved && reportStatus != ReportStatus.COMPLETED;
    }
}
