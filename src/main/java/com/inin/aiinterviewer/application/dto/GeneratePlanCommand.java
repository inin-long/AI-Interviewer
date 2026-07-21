package com.inin.aiinterviewer.application.dto;

public record GeneratePlanCommand(
        String currentRole,
        String targetRole,
        String industry,
        String experienceYears,
        String careerGoal
) {
}
