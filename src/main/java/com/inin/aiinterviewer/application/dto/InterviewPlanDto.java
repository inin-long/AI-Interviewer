package com.inin.aiinterviewer.application.dto;

import com.inin.aiinterviewer.domain.enums.InterviewDifficulty;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record InterviewPlanDto(
        Long id,
        String name,
        String jobTitle,
        String jobDescription,
        InterviewDifficulty difficulty,
        int durationMinutes,
        int questionCount,
        Long resumeId,
        Map<String, Object> rules,
        List<String> stages,
        boolean defaultPlan,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
}

