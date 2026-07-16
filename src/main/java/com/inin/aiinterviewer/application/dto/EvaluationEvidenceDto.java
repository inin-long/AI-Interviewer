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
        List<String> relatedClaimIds
) {
    public EvaluationEvidenceDto {
        relatedClaimIds = relatedClaimIds == null ? List.of() : List.copyOf(relatedClaimIds);
    }

    public static EvaluationEvidenceDto from(EvaluationEvidence evidence) {
        return new EvaluationEvidenceDto(
                evidence.id(), evidence.messageId(), evidence.competencyCode(), evidence.signal(),
                evidence.strength(), evidence.confidence(), evidence.reason(), evidence.relatedClaimIds());
    }
}
