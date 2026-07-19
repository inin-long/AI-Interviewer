package com.inin.aiinterviewer.application.dto;

public record DomainPackDto(
        String id,
        String roleCode,
        String industryCode,
        String version,
        String displayName,
        String source
) {
}
