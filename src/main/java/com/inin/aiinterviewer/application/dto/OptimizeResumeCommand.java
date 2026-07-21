package com.inin.aiinterviewer.application.dto;

public record OptimizeResumeCommand(
        String originalText,
        String resumeTarget,
        String optimizeDirection
) {
}
