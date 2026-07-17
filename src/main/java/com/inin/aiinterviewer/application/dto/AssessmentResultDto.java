package com.inin.aiinterviewer.application.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record AssessmentResultDto(
        Long id,
        String templateCode,
        String resultCode,
        Map<String, Integer> scores,
        String reportMarkdown,
        LocalDateTime createTime
) {
}
