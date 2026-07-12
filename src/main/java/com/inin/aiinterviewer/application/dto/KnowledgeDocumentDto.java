package com.inin.aiinterviewer.application.dto;

import com.inin.aiinterviewer.domain.enums.KnowledgeStatus;

import java.time.LocalDateTime;

public record KnowledgeDocumentDto(
        long id,
        String name,
        String originalName,
        String fileType,
        long fileSize,
        String category,
        KnowledgeStatus status,
        String errorMessage,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
}
