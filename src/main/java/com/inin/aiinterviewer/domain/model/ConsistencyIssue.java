package com.inin.aiinterviewer.domain.model;

import com.inin.aiinterviewer.domain.enums.ConsistencyIssueStatus;
import com.inin.aiinterviewer.domain.enums.ConsistencyIssueType;

import java.time.LocalDateTime;
import java.util.List;

public record ConsistencyIssue(
        String id,
        long sessionId,
        ConsistencyIssueType type,
        ConsistencyIssueStatus status,
        String description,
        List<String> relatedClaimIds,
        Long clarificationMessageId,
        String clarificationQuestion,
        String resolution,
        LocalDateTime createTime,
        LocalDateTime updateTime
) implements java.io.Serializable {
    public ConsistencyIssue {
        relatedClaimIds = relatedClaimIds == null ? List.of() : List.copyOf(relatedClaimIds);
        clarificationQuestion = clarificationQuestion == null ? "" : clarificationQuestion;
        resolution = resolution == null ? "" : resolution;
    }

    public boolean open() {
        return status == ConsistencyIssueStatus.POTENTIAL
                || status == ConsistencyIssueStatus.CLARIFIED;
    }
}
