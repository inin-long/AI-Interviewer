package com.inin.aiinterviewer.agent.model;

import com.inin.aiinterviewer.domain.model.ConsistencyIssue;
import com.inin.aiinterviewer.domain.model.InterviewClaim;

import java.io.Serializable;
import java.util.List;

public record ConsistencyContext(
        boolean runRequested,
        String reason,
        List<InterviewClaim> currentClaims,
        List<InterviewClaim> historicalClaims,
        List<ConsistencyIssue> openIssues
) implements Serializable {
    public ConsistencyContext {
        reason = reason == null ? "" : reason;
        currentClaims = currentClaims == null ? List.of() : List.copyOf(currentClaims);
        historicalClaims = historicalClaims == null ? List.of() : List.copyOf(historicalClaims);
        openIssues = openIssues == null ? List.of() : List.copyOf(openIssues);
    }

    public static ConsistencyContext skipped(String reason) {
        return new ConsistencyContext(false, reason, List.of(), List.of(), List.of());
    }
}
