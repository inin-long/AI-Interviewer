package com.inin.aiinterviewer.application.dto;

import com.inin.aiinterviewer.domain.enums.ProfileSource;
import com.inin.aiinterviewer.domain.enums.ProfileStatus;
import com.inin.aiinterviewer.domain.model.CandidateProfileContent;

import java.time.LocalDateTime;

public record CandidateProfileDto(
        Long id,
        Long resumeId,
        CandidateProfileContent content,
        ProfileSource source,
        ProfileStatus status,
        boolean confirmed,
        String errorMessage,
        LocalDateTime updateTime
) {
}

