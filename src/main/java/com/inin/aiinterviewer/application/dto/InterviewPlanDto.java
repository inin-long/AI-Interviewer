package com.inin.aiinterviewer.application.dto;

import com.inin.aiinterviewer.domain.enums.InterviewDifficulty;

import java.time.LocalDateTime;
import java.io.Serializable;
import java.util.LinkedHashMap;
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
        List<Long> knowledgeDocumentIds,
        Map<String, Object> rules,
        List<String> stages,
        boolean defaultPlan,
        LocalDateTime createTime,
        LocalDateTime updateTime,
        String domainPackId,
        List<String> knowledgeCategories
) implements Serializable {
    public InterviewPlanDto {
        knowledgeDocumentIds = knowledgeDocumentIds == null ? List.of() : List.copyOf(knowledgeDocumentIds);
        knowledgeCategories = knowledgeCategories == null ? List.of() : List.copyOf(knowledgeCategories);
        rules = rules == null ? Map.of() : new LinkedHashMap<>(rules);
        stages = stages == null ? List.of() : List.copyOf(stages);
    }

    public InterviewPlanDto(
            Long id,
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
            boolean defaultPlan,
            LocalDateTime createTime,
            LocalDateTime updateTime,
            String domainPackId
    ) {
        this(id, name, jobTitle, jobDescription, difficulty, durationMinutes, questionCount,
                resumeId, profileId, knowledgeDocumentIds, rules, stages, defaultPlan,
                createTime, updateTime, domainPackId, List.of());
    }

    public InterviewPlanDto(
            Long id,
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
            boolean defaultPlan,
            LocalDateTime createTime,
            LocalDateTime updateTime
    ) {
        this(id, name, jobTitle, jobDescription, difficulty, durationMinutes, questionCount,
                resumeId, profileId, knowledgeDocumentIds, rules, stages, defaultPlan,
                createTime, updateTime, null, List.of());
    }

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
                resumeId, null, List.of(), rules, stages, defaultPlan, createTime, updateTime, null, List.of());
    }

    public InterviewPlanDto(
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
    ) {
        this(id, name, jobTitle, jobDescription, difficulty, durationMinutes, questionCount,
                resumeId, profileId, List.of(), rules, stages, defaultPlan, createTime, updateTime, null, List.of());
    }
}
