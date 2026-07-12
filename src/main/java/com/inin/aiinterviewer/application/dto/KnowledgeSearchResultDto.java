package com.inin.aiinterviewer.application.dto;

public record KnowledgeSearchResultDto(
        long documentId,
        int chunkIndex,
        String documentName,
        String content,
        double score
) {
}
