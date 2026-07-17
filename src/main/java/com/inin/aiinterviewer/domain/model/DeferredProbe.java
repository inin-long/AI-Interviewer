package com.inin.aiinterviewer.domain.model;

import com.inin.aiinterviewer.domain.enums.InterviewStage;
import com.inin.aiinterviewer.domain.enums.ProbeStrategy;

import java.io.Serializable;
import java.time.LocalDateTime;

public record DeferredProbe(
        String id,
        long sessionId,
        String targetClaimId,
        InterviewStage preferredStage,
        ProbeStrategy strategy,
        String reason,
        boolean completed,
        LocalDateTime createTime,
        LocalDateTime updateTime
) implements Serializable {
    public DeferredProbe {
        id = id == null ? "" : id;
        targetClaimId = targetClaimId == null ? "" : targetClaimId;
        reason = reason == null ? "" : reason;
    }

    public boolean dueAt(InterviewStage stage) {
        return !completed && (preferredStage == null || preferredStage == stage);
    }
}
