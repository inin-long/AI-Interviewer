package com.inin.aiinterviewer.application.dto;

import com.inin.aiinterviewer.domain.enums.InterviewStage;
import com.inin.aiinterviewer.domain.enums.InterviewStatus;

import java.time.LocalDateTime;
import java.util.List;

public record InterviewSessionDto(
        long id,
        Long planId,
        Long resumeId,
        Long profileId,
        String title,
        String jobTitle,
        InterviewPlanDto planSnapshot,
        CandidateProfileDto profileSnapshot,
        List<KnowledgeDocumentSnapshotDto> knowledgeSnapshot,
        InterviewStage stage,
        InterviewStatus status,
        String promptVersion,
        LocalDateTime startedTime,
        LocalDateTime completedTime,
        LocalDateTime createTime,
        LocalDateTime updateTime,
        DomainPackDto domainPack
) {
    public InterviewSessionDto {
        knowledgeSnapshot = knowledgeSnapshot == null ? List.of() : List.copyOf(knowledgeSnapshot);
    }

    public InterviewSessionDto(
            long id,
            Long planId,
            Long resumeId,
            Long profileId,
            String title,
            String jobTitle,
            InterviewPlanDto planSnapshot,
            CandidateProfileDto profileSnapshot,
            List<KnowledgeDocumentSnapshotDto> knowledgeSnapshot,
            InterviewStage stage,
            InterviewStatus status,
            String promptVersion,
            LocalDateTime startedTime,
            LocalDateTime completedTime,
            LocalDateTime createTime,
            LocalDateTime updateTime
    ) {
        this(id, planId, resumeId, profileId, title, jobTitle, planSnapshot, profileSnapshot,
                knowledgeSnapshot, stage, status, promptVersion, startedTime, completedTime,
                createTime, updateTime, null);
    }
}
