package com.inin.aiinterviewer.application.dto;

public record KnowledgeCategoryDto(
        String name,
        long documentCount,
        long readyCount
) {
}
