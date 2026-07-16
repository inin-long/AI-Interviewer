package com.inin.aiinterviewer.agent.model;

import com.inin.aiinterviewer.domain.enums.ClaimType;

import java.util.List;
import java.io.Serializable;

public record ClaimExtractionResult(
        List<ClaimCandidate> claims,
        boolean degraded,
        String failureReason
) implements Serializable {
    public ClaimExtractionResult {
        claims = claims == null ? List.of() : List.copyOf(claims);
        failureReason = failureReason == null ? "" : failureReason;
    }

    public ClaimExtractionResult(List<ClaimCandidate> claims) {
        this(claims, false, "");
    }

    public static ClaimExtractionResult degraded(String reason) {
        return new ClaimExtractionResult(List.of(), true, reason);
    }

    public record ClaimCandidate(
            ClaimType type,
            String content,
            double importance,
            double credibility,
            List<String> missingEvidence
    ) implements Serializable {
        public ClaimCandidate {
            missingEvidence = missingEvidence == null ? List.of() : List.copyOf(missingEvidence);
        }
    }
}
