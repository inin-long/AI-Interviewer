package com.inin.aiinterviewer.application.dto;

import com.inin.aiinterviewer.domain.enums.InterviewStage;
import com.inin.aiinterviewer.domain.enums.InterviewStatus;

import java.time.LocalDateTime;

public record InterviewSessionDto(
        long id,
        Long planId,
        Long resumeId,
        String title,
        String jobTitle,
        InterviewPlanDto planSnapshot,
        InterviewStage stage,
        InterviewStatus status,
        String promptVersion,
        LocalDateTime startedTime,
        LocalDateTime completedTime,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
}
