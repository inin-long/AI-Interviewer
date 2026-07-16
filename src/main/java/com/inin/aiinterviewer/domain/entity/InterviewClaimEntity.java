package com.inin.aiinterviewer.domain.entity;

import com.inin.aiinterviewer.domain.enums.ClaimStatus;
import com.inin.aiinterviewer.domain.enums.ClaimType;

import java.time.LocalDateTime;

public class InterviewClaimEntity {
    private String id;
    private Long userId;
    private Long sessionId;
    private Long sourceMessageId;
    private ClaimType claimType;
    private String content;
    private double importance;
    private double credibility;
    private ClaimStatus status;
    private String missingEvidenceJson;
    private String supportingEvidenceIdsJson;
    private String conflictingEvidenceIdsJson;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
    public Long getSourceMessageId() { return sourceMessageId; }
    public void setSourceMessageId(Long sourceMessageId) { this.sourceMessageId = sourceMessageId; }
    public ClaimType getClaimType() { return claimType; }
    public void setClaimType(ClaimType claimType) { this.claimType = claimType; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public double getImportance() { return importance; }
    public void setImportance(double importance) { this.importance = importance; }
    public double getCredibility() { return credibility; }
    public void setCredibility(double credibility) { this.credibility = credibility; }
    public ClaimStatus getStatus() { return status; }
    public void setStatus(ClaimStatus status) { this.status = status; }
    public String getMissingEvidenceJson() { return missingEvidenceJson; }
    public void setMissingEvidenceJson(String missingEvidenceJson) { this.missingEvidenceJson = missingEvidenceJson; }
    public String getSupportingEvidenceIdsJson() { return supportingEvidenceIdsJson; }
    public void setSupportingEvidenceIdsJson(String supportingEvidenceIdsJson) { this.supportingEvidenceIdsJson = supportingEvidenceIdsJson; }
    public String getConflictingEvidenceIdsJson() { return conflictingEvidenceIdsJson; }
    public void setConflictingEvidenceIdsJson(String conflictingEvidenceIdsJson) { this.conflictingEvidenceIdsJson = conflictingEvidenceIdsJson; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
