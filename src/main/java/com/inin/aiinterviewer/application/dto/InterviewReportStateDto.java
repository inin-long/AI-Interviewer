package com.inin.aiinterviewer.application.dto;

import com.inin.aiinterviewer.domain.enums.ReportStatus;

public record InterviewReportStateDto(ReportStatus status, String failureMessage) {
    public InterviewReportStateDto {
        status = status == null ? ReportStatus.NOT_STARTED : status;
        failureMessage = failureMessage == null ? "" : failureMessage.strip();
    }
}
