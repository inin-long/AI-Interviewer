package com.inin.aiinterviewer.domain.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public record CandidateDecision(
        String id,
        int round,
        long sourceMessageId,
        String answer,
        String action,
        String rationale,
        LocalDateTime createTime
) implements Serializable {
    public CandidateDecision {
        id = id == null ? "" : id.strip();
        answer = answer == null ? "" : answer.strip();
        action = action == null ? "" : action.strip();
        rationale = rationale == null ? "" : rationale.strip();
    }
}
