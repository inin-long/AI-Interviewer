package com.inin.aiinterviewer.domain.model;

import com.inin.aiinterviewer.domain.enums.EvidenceSignal;

import java.time.LocalDateTime;
import java.util.List;

public record EvaluationEvidence(
        String id,
        long sessionId,
        long messageId,
        String competencyCode,
        EvidenceSignal signal,
        double strength,
        double confidence,
        String reason,
        List<String> relatedClaimIds,
        LocalDateTime createTime
) {
    public EvaluationEvidence {
        relatedClaimIds = relatedClaimIds == null ? List.of() : List.copyOf(relatedClaimIds);
    }
}
