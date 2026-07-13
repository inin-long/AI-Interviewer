package com.inin.aiinterviewer.application.dto;

public record KnowledgeCitationDto(
        long documentId,
        String documentName,
        int chunkIndex,
        String excerpt,
        double score
) {
    private static final int MAX_EXCERPT_LENGTH = 320;

    public KnowledgeCitationDto {
        documentName = documentName == null || documentName.isBlank()
                ? "未命名文档" : documentName.strip();
        excerpt = excerpt == null ? "" : excerpt.replaceAll("\\s+", " ").strip();
        if (excerpt.length() > MAX_EXCERPT_LENGTH) {
            excerpt = excerpt.substring(0, MAX_EXCERPT_LENGTH) + "…";
        }
    }
}
