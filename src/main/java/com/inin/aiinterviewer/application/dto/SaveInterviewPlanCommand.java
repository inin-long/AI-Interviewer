package com.inin.aiinterviewer.application.dto;

import com.inin.aiinterviewer.domain.enums.InterviewDifficulty;

import java.util.List;
import java.util.Map;

public record SaveInterviewPlanCommand(
        String name,
        String jobTitle,
        String jobDescription,
        InterviewDifficulty difficulty,
        int durationMinutes,
        int questionCount,
        Long resumeId,
        Long profileId,
        Map<String, Object> rules,
        List<String> stages
) {
    public SaveInterviewPlanCommand(
            String name,
            String jobTitle,
            String jobDescription,
            InterviewDifficulty difficulty,
            int durationMinutes,
            int questionCount,
            Long resumeId,
            Map<String, Object> rules,
            List<String> stages
    ) {
        this(name, jobTitle, jobDescription, difficulty, durationMinutes, questionCount,
                resumeId, null, rules, stages);
    }
}
