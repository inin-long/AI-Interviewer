package com.inin.aiinterviewer.application.dto;

import com.inin.aiinterviewer.domain.enums.InterviewDifficulty;

import java.time.LocalDateTime;
import java.io.Serializable;
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
        Long profileId,
        Map<String, Object> rules,
        List<String> stages,
        boolean defaultPlan,
        LocalDateTime createTime,
        LocalDateTime updateTime
) implements Serializable {
    public InterviewPlanDto(
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
        this(id, name, jobTitle, jobDescription, difficulty, durationMinutes, questionCount,
                resumeId, null, rules, stages, defaultPlan, createTime, updateTime);
    }
}
