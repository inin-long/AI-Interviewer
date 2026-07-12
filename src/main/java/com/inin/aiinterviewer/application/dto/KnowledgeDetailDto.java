package com.inin.aiinterviewer.application.dto;

import java.util.List;

public record KnowledgeDetailDto(KnowledgeDocumentDto document, List<String> chunks) {
    public KnowledgeDetailDto {
        chunks = chunks == null ? List.of() : List.copyOf(chunks);
    }
}
