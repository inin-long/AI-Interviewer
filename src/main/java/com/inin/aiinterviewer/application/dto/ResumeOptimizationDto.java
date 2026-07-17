package com.inin.aiinterviewer.application.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ResumeOptimizationDto(
        Long id,
        String originalText,
        String optimizedText,
        List<String> highlights,
        LocalDateTime createTime
) {
}
