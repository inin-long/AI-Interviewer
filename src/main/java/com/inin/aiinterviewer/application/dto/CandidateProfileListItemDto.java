package com.inin.aiinterviewer.application.dto;

import com.inin.aiinterviewer.domain.enums.ProfileSource;
import com.inin.aiinterviewer.domain.enums.ProfileStatus;

import java.time.LocalDateTime;
import java.util.List;

public record CandidateProfileListItemDto(
        Long id,
        Long resumeId,
        String resumeName,
        String candidateName,
        String targetRole,
        List<String> skills,
        ProfileSource source,
        ProfileStatus status,
        boolean confirmed,
        LocalDateTime updateTime
) {
}
