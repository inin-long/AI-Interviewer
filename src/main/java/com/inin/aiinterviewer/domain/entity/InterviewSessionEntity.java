package com.inin.aiinterviewer.domain.entity;

import com.inin.aiinterviewer.domain.enums.InterviewStage;
import com.inin.aiinterviewer.domain.enums.InterviewStatus;

import java.time.LocalDateTime;

public class InterviewSessionEntity {
    private Long id;
    private Long userId;
    private Long planId;
    private Long resumeId;
    private Long profileId;
    private String title;
    private String jobTitle;
    private String planSnapshotJson;
    private String profileSnapshotJson;
    private String knowledgeSnapshotJson;
    private String domainPackId;
    private String domainPackVersion;
    private String domainPackSnapshotJson;
    private InterviewStage stage;
    private InterviewStatus status;
    private String promptVersion;
    private LocalDateTime startedTime;
    private LocalDateTime completedTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private boolean deleted;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getPlanId() { return planId; }
    public void setPlanId(Long planId) { this.planId = planId; }
    public Long getResumeId() { return resumeId; }
    public void setResumeId(Long resumeId) { this.resumeId = resumeId; }
    public Long getProfileId() { return profileId; }
    public void setProfileId(Long profileId) { this.profileId = profileId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }
    public String getPlanSnapshotJson() { return planSnapshotJson; }
    public void setPlanSnapshotJson(String planSnapshotJson) { this.planSnapshotJson = planSnapshotJson; }
    public String getProfileSnapshotJson() { return profileSnapshotJson; }
    public void setProfileSnapshotJson(String profileSnapshotJson) { this.profileSnapshotJson = profileSnapshotJson; }
    public String getKnowledgeSnapshotJson() { return knowledgeSnapshotJson; }
    public void setKnowledgeSnapshotJson(String knowledgeSnapshotJson) { this.knowledgeSnapshotJson = knowledgeSnapshotJson; }
    public String getDomainPackId() { return domainPackId; }
    public void setDomainPackId(String domainPackId) { this.domainPackId = domainPackId; }
    public String getDomainPackVersion() { return domainPackVersion; }
    public void setDomainPackVersion(String domainPackVersion) { this.domainPackVersion = domainPackVersion; }
    public String getDomainPackSnapshotJson() { return domainPackSnapshotJson; }
    public void setDomainPackSnapshotJson(String domainPackSnapshotJson) { this.domainPackSnapshotJson = domainPackSnapshotJson; }
    public InterviewStage getStage() { return stage; }
    public void setStage(InterviewStage stage) { this.stage = stage; }
    public InterviewStatus getStatus() { return status; }
    public void setStatus(InterviewStatus status) { this.status = status; }
    public String getPromptVersion() { return promptVersion; }
    public void setPromptVersion(String promptVersion) { this.promptVersion = promptVersion; }
    public LocalDateTime getStartedTime() { return startedTime; }
    public void setStartedTime(LocalDateTime startedTime) { this.startedTime = startedTime; }
    public LocalDateTime getCompletedTime() { return completedTime; }
    public void setCompletedTime(LocalDateTime completedTime) { this.completedTime = completedTime; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
}
