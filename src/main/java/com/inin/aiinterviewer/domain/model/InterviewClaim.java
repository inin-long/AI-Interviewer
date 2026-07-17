package com.inin.aiinterviewer.domain.model;

import com.inin.aiinterviewer.domain.enums.ClaimStatus;
import com.inin.aiinterviewer.domain.enums.ClaimType;

import java.time.LocalDateTime;
import java.util.List;

public record InterviewClaim(
        String id,
        long sessionId,
        long sourceMessageId,
        ClaimType type,
        String content,
        double importance,
        double credibility,
        ClaimStatus status,
        List<String> missingEvidence,
        List<String> supportingEvidenceIds,
        List<String> conflictingEvidenceIds,
        LocalDateTime createTime,
        LocalDateTime updateTime
) implements java.io.Serializable {
    public InterviewClaim {
        missingEvidence = missingEvidence == null ? List.of() : List.copyOf(missingEvidence);
        supportingEvidenceIds = supportingEvidenceIds == null ? List.of() : List.copyOf(supportingEvidenceIds);
        conflictingEvidenceIds = conflictingEvidenceIds == null ? List.of() : List.copyOf(conflictingEvidenceIds);
    }
}
