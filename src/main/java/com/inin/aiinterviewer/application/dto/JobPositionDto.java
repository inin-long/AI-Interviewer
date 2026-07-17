package com.inin.aiinterviewer.application.dto;

import java.time.LocalDateTime;

public record JobPositionDto(
        Long id,
        String title,
        String department,
        String description,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
}
