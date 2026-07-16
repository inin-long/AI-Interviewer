package com.inin.aiinterviewer.domain.entity;

import com.inin.aiinterviewer.domain.enums.EvidenceSignal;

import java.time.LocalDateTime;

public class EvaluationEvidenceEntity {
    private String id;
    private Long userId;
    private Long sessionId;
    private Long messageId;
    private String competencyCode;
    private EvidenceSignal signal;
    private double strength;
    private double confidence;
    private String reason;
    private String relatedClaimIdsJson;
    private LocalDateTime createTime;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
    public Long getMessageId() { return messageId; }
    public void setMessageId(Long messageId) { this.messageId = messageId; }
    public String getCompetencyCode() { return competencyCode; }
    public void setCompetencyCode(String competencyCode) { this.competencyCode = competencyCode; }
    public EvidenceSignal getSignal() { return signal; }
    public void setSignal(EvidenceSignal signal) { this.signal = signal; }
    public double getStrength() { return strength; }
    public void setStrength(double strength) { this.strength = strength; }
    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getRelatedClaimIdsJson() { return relatedClaimIdsJson; }
    public void setRelatedClaimIdsJson(String relatedClaimIdsJson) { this.relatedClaimIdsJson = relatedClaimIdsJson; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
