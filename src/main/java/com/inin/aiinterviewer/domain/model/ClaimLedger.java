package com.inin.aiinterviewer.domain.model;

import com.inin.aiinterviewer.domain.enums.ClaimStatus;

import java.util.List;

public record ClaimLedger(List<InterviewClaim> claims) {
    public ClaimLedger {
        claims = claims == null ? List.of() : List.copyOf(claims);
    }

    public static ClaimLedger empty() {
        return new ClaimLedger(List.of());
    }

    public List<InterviewClaim> pendingVerification() {
        return claims.stream().filter(claim -> claim.status() == ClaimStatus.UNVERIFIED
                || claim.status() == ClaimStatus.PARTIALLY_VERIFIED
                || claim.status() == ClaimStatus.DISPUTED).toList();
    }
}
