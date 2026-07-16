package com.inin.aiinterviewer.agent.model;

import com.inin.aiinterviewer.domain.enums.EvidenceSignal;

import java.io.Serializable;
import java.util.List;

public record EvidenceCollectionResult(
        List<EvidenceCandidate> evidence,
        boolean degraded,
        String failureReason
) implements Serializable {
    public EvidenceCollectionResult {
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
        failureReason = failureReason == null ? "" : failureReason;
    }

    public EvidenceCollectionResult(List<EvidenceCandidate> evidence) {
        this(evidence, false, "");
    }

    public static EvidenceCollectionResult degraded(String reason) {
        return new EvidenceCollectionResult(List.of(), true, reason);
    }

    public record EvidenceCandidate(
            String competencyCode,
            EvidenceSignal signal,
            double strength,
            double confidence,
            String reason,
            List<String> relatedClaimIds
    ) implements Serializable {
        public EvidenceCandidate {
            competencyCode = competencyCode == null ? "" : competencyCode.strip();
            reason = reason == null ? "" : reason.strip();
            relatedClaimIds = relatedClaimIds == null ? List.of() : List.copyOf(relatedClaimIds);
        }
    }
}
