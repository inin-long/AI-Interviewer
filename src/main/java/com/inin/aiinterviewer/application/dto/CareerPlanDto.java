package com.inin.aiinterviewer.application.dto;

import java.time.LocalDateTime;

public record CareerPlanDto(
        Long id,
        String currentRole,
        String targetRole,
        String industry,
        String experienceYears,
        String planMarkdown,
        LocalDateTime createTime
) {
}
