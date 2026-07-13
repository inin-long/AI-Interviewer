package com.inin.aiinterviewer.application.dto;

import java.time.LocalDateTime;

public record KnowledgeDocumentSnapshotDto(
        long id,
        String name,
        String originalName,
        String category,
        LocalDateTime updateTime
) {
    public static KnowledgeDocumentSnapshotDto from(KnowledgeDocumentDto document) {
        return new KnowledgeDocumentSnapshotDto(document.id(), document.name(), document.originalName(),
                document.category(), document.updateTime());
    }
}
