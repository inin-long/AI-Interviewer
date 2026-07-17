package com.inin.aiinterviewer.application.dto;

import java.time.LocalDateTime;
import java.util.List;

public record SkillArticleDto(
        Long id,
        Long userId,
        String category,
        String title,
        String summary,
        String contentMarkdown,
        List<String> tags,
        LocalDateTime createTime
) {
}
