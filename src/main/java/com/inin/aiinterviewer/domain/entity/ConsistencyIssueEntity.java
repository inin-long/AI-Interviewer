package com.inin.aiinterviewer.domain.entity;

import com.inin.aiinterviewer.domain.enums.ConsistencyIssueStatus;
import com.inin.aiinterviewer.domain.enums.ConsistencyIssueType;

import java.time.LocalDateTime;

public class ConsistencyIssueEntity {
    private String id;
    private Long userId;
    private Long sessionId;
    private ConsistencyIssueType issueType;
    private ConsistencyIssueStatus status;
    private String description;
    private String relatedClaimIdsJson;
    private Long clarificationMessageId;
    private String clarificationQuestion;
    private String resolution;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
    public ConsistencyIssueType getIssueType() { return issueType; }
    public void setIssueType(ConsistencyIssueType issueType) { this.issueType = issueType; }
    public ConsistencyIssueStatus getStatus() { return status; }
    public void setStatus(ConsistencyIssueStatus status) { this.status = status; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getRelatedClaimIdsJson() { return relatedClaimIdsJson; }
    public void setRelatedClaimIdsJson(String relatedClaimIdsJson) { this.relatedClaimIdsJson = relatedClaimIdsJson; }
    public Long getClarificationMessageId() { return clarificationMessageId; }
    public void setClarificationMessageId(Long clarificationMessageId) {
        this.clarificationMessageId = clarificationMessageId;
    }
    public String getClarificationQuestion() { return clarificationQuestion; }
    public void setClarificationQuestion(String clarificationQuestion) {
        this.clarificationQuestion = clarificationQuestion;
    }
    public String getResolution() { return resolution; }
    public void setResolution(String resolution) { this.resolution = resolution; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
