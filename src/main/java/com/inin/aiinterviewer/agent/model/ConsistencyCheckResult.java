package com.inin.aiinterviewer.agent.model;

import com.inin.aiinterviewer.domain.enums.ConsistencyIssueStatus;
import com.inin.aiinterviewer.domain.enums.ConsistencyIssueType;

import java.io.Serializable;
import java.util.List;

public record ConsistencyCheckResult(
        List<IssueCandidate> issues,
        List<ResolutionCandidate> resolutions,
        boolean skipped,
        boolean degraded,
        String failureReason
) implements Serializable {
    public ConsistencyCheckResult {
        issues = issues == null ? List.of() : List.copyOf(issues);
        resolutions = resolutions == null ? List.of() : List.copyOf(resolutions);
        failureReason = failureReason == null ? "" : failureReason;
    }

    public ConsistencyCheckResult(List<IssueCandidate> issues, List<ResolutionCandidate> resolutions) {
        this(issues, resolutions, false, false, "");
    }

    public static ConsistencyCheckResult skipped(String reason) {
        return new ConsistencyCheckResult(List.of(), List.of(), true, false, reason);
    }

    public static ConsistencyCheckResult degraded(String reason) {
        return new ConsistencyCheckResult(List.of(), List.of(), false, true, reason);
    }

    public boolean requiresClarification() {
        return issues.stream().anyMatch(issue -> !issue.issueId().isBlank());
    }

    public record IssueCandidate(
            String issueId,
            ConsistencyIssueType type,
            String description,
            List<String> relatedClaimIds,
            String clarificationQuestion,
            double confidence
    ) implements Serializable {
        public IssueCandidate {
            issueId = issueId == null ? "" : issueId.strip();
            description = description == null ? "" : description.strip();
            relatedClaimIds = relatedClaimIds == null ? List.of() : List.copyOf(relatedClaimIds);
            clarificationQuestion = clarificationQuestion == null ? "" : clarificationQuestion.strip();
        }
    }

    public record ResolutionCandidate(
            String issueId,
            ConsistencyIssueStatus status,
            String resolution,
            double confidence
    ) implements Serializable {
        public ResolutionCandidate {
            issueId = issueId == null ? "" : issueId.strip();
            resolution = resolution == null ? "" : resolution.strip();
        }
    }
}
