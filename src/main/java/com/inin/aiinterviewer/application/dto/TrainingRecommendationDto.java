package com.inin.aiinterviewer.application.dto;

import java.util.List;

public record TrainingRecommendationDto(
        long sourceSessionId,
        List<TrainingTopic> topics,
        List<TrainingExercise> exercises,
        List<KnowledgeResource> knowledgeResources
) {
    public TrainingRecommendationDto {
        topics = topics == null ? List.of() : List.copyOf(topics);
        exercises = exercises == null ? List.of() : List.copyOf(exercises);
        knowledgeResources = knowledgeResources == null ? List.of() : List.copyOf(knowledgeResources);
    }

    public record TrainingTopic(
            String competencyCode,
            String title,
            String rationale,
            int priority,
            List<String> sourceEvidenceIds,
            List<Integer> sourceQuestionNumbers
    ) {
        public TrainingTopic {
            sourceEvidenceIds = sourceEvidenceIds == null ? List.of() : List.copyOf(sourceEvidenceIds);
            sourceQuestionNumbers = sourceQuestionNumbers == null
                    ? List.of() : List.copyOf(sourceQuestionNumbers);
        }
    }

    public record TrainingExercise(
            String logicGapType,
            String title,
            String instruction,
            double severity,
            List<String> relatedClaimIds
    ) {
        public TrainingExercise {
            relatedClaimIds = relatedClaimIds == null ? List.of() : List.copyOf(relatedClaimIds);
        }
    }

    public record KnowledgeResource(
            long documentId,
            String name,
            String category,
            String reason
    ) {
    }
}
