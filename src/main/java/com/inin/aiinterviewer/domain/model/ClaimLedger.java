package com.inin.aiinterviewer.domain.model;

import com.inin.aiinterviewer.domain.enums.ClaimStatus;

import java.util.List;

public record ClaimLedger(List<InterviewClaim> claims, List<ConsistencyIssue> issues) {
    public ClaimLedger {
        claims = claims == null ? List.of() : List.copyOf(claims);
        issues = issues == null ? List.of() : List.copyOf(issues);
    }

    public ClaimLedger(List<InterviewClaim> claims) {
        this(claims, List.of());
    }

    public static ClaimLedger empty() {
        return new ClaimLedger(List.of(), List.of());
    }

    public List<InterviewClaim> pendingVerification() {
        return claims.stream().filter(claim -> claim.status() == ClaimStatus.UNVERIFIED
                || claim.status() == ClaimStatus.PARTIALLY_VERIFIED
                || claim.status() == ClaimStatus.DISPUTED).toList();
    }

    public List<ConsistencyIssue> openConsistencyIssues() {
        return issues.stream().filter(ConsistencyIssue::open).toList();
    }
}
