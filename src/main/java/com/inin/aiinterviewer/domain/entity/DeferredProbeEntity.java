package com.inin.aiinterviewer.domain.entity;

import com.inin.aiinterviewer.domain.enums.InterviewStage;
import com.inin.aiinterviewer.domain.enums.ProbeStrategy;

import java.time.LocalDateTime;

public class DeferredProbeEntity {
    private String id;
    private Long userId;
    private Long sessionId;
    private String targetClaimId;
    private InterviewStage preferredStage;
    private ProbeStrategy strategy;
    private String reason;
    private Boolean completed;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
    public String getTargetClaimId() { return targetClaimId; }
    public void setTargetClaimId(String targetClaimId) { this.targetClaimId = targetClaimId; }
    public InterviewStage getPreferredStage() { return preferredStage; }
    public void setPreferredStage(InterviewStage preferredStage) { this.preferredStage = preferredStage; }
    public ProbeStrategy getStrategy() { return strategy; }
    public void setStrategy(ProbeStrategy strategy) { this.strategy = strategy; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Boolean getCompleted() { return completed; }
    public void setCompleted(Boolean completed) { this.completed = completed; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
