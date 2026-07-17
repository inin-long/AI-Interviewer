package com.inin.aiinterviewer.application.dto;

import java.util.List;

public record SaveSkillArticleCommand(
        String category,
        String title,
        String summary,
        String contentMarkdown,
        List<String> tags
) {
}
