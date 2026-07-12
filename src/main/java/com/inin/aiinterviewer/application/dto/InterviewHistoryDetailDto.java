package com.inin.aiinterviewer.application.dto;

import java.util.List;
import java.util.Optional;

public record InterviewHistoryDetailDto(
        InterviewSessionDto session,
        List<InterviewMessageDto> messages,
        Optional<InterviewReportDto> report
) {
}
