package com.inin.aiinterviewer.application.dto;

import com.inin.aiinterviewer.domain.enums.EvidenceSignal;
import com.inin.aiinterviewer.domain.model.EvaluationEvidence;

import java.util.List;

public record EvaluationEvidenceDto(
        String id,
        long messageId,
        String competencyCode,
        EvidenceSignal signal,
        double strength,
        double confidence,
        String reason,
        List<String> relatedClaimIds,
        int questionNumber
) {
    public EvaluationEvidenceDto {
        relatedClaimIds = relatedClaimIds == null ? List.of() : List.copyOf(relatedClaimIds);
    }

    public EvaluationEvidenceDto(
            String id,
            long messageId,
            String competencyCode,
            EvidenceSignal signal,
            double strength,
            double confidence,
            String reason,
            List<String> relatedClaimIds
    ) {
        this(id, messageId, competencyCode, signal, strength, confidence, reason,
                relatedClaimIds, 0);
    }

    public static EvaluationEvidenceDto from(EvaluationEvidence evidence, int questionNumber) {
        return new EvaluationEvidenceDto(
                evidence.id(), evidence.messageId(), evidence.competencyCode(), evidence.signal(),
                evidence.strength(), evidence.confidence(), evidence.reason(),
                evidence.relatedClaimIds(), questionNumber);
    }
}
