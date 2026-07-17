package com.inin.aiinterviewer.application.dto;

import java.time.LocalDateTime;
import java.util.List;

public record TrainingScoreChangeDto(
        long sessionId,
        String title,
        int sourceScore,
        boolean sourceScored,
        int retestScore,
        boolean retestScored,
        Integer overallDelta,
        List<DimensionChange> dimensions,
        LocalDateTime completedTime
) {
    public TrainingScoreChangeDto {
        title = title == null ? "专项复试" : title.strip();
        dimensions = dimensions == null ? List.of() : List.copyOf(dimensions);
    }

    public record DimensionChange(
            String key,
            String label,
            int sourceScore,
            int retestScore,
            int delta
    ) {
    }
}
