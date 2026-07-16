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
        List<Long> knowledgeDocumentIds,
        Map<String, Object> rules,
        List<String> stages,
        String domainPackId
) {
    public SaveInterviewPlanCommand {
        knowledgeDocumentIds = knowledgeDocumentIds == null ? List.of() : List.copyOf(knowledgeDocumentIds);
    }

    public SaveInterviewPlanCommand(
            String name,
            String jobTitle,
            String jobDescription,
            InterviewDifficulty difficulty,
            int durationMinutes,
            int questionCount,
            Long resumeId,
            Long profileId,
            List<Long> knowledgeDocumentIds,
            Map<String, Object> rules,
            List<String> stages
    ) {
        this(name, jobTitle, jobDescription, difficulty, durationMinutes, questionCount,
                resumeId, profileId, knowledgeDocumentIds, rules, stages, null);
    }

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
                resumeId, null, List.of(), rules, stages, null);
    }

    public SaveInterviewPlanCommand(
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
        this(name, jobTitle, jobDescription, difficulty, durationMinutes, questionCount,
                resumeId, profileId, List.of(), rules, stages, null);
    }
}
