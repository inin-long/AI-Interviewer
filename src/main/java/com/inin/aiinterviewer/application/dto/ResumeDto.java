package com.inin.aiinterviewer.application.dto;

import com.inin.aiinterviewer.domain.enums.ResumeStatus;

import java.time.LocalDateTime;

public record ResumeDto(
        Long id,
        String originalName,
        String fileType,
        long fileSize,
        ResumeStatus status,
        String errorMessage,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
}

