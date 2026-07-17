package com.inin.aiinterviewer.application.dto;

import java.util.List;

public record CoachingFeedbackDto(
        boolean available,
        int sourceQuestionNumber,
        List<String> coveredContent,
        List<String> missingContent,
        List<String> logicGaps,
        List<String> referenceStructure,
        String hint,
        boolean canReanswer
) {
    public CoachingFeedbackDto {
        coveredContent = immutable(coveredContent);
        missingContent = immutable(missingContent);
        logicGaps = immutable(logicGaps);
        referenceStructure = immutable(referenceStructure);
        hint = hint == null ? "" : hint.strip();
    }

    public static CoachingFeedbackDto unavailable() {
        return new CoachingFeedbackDto(
                false, 0, List.of(), List.of(), List.of(), List.of(), "", false);
    }

    private static List<String> immutable(List<String> values) {
        return values == null ? List.of() : List.copyOf(values);
    }
}
