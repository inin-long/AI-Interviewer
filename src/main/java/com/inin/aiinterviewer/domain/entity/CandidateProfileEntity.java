package com.inin.aiinterviewer.domain.entity;

import com.inin.aiinterviewer.domain.enums.ProfileSource;
import com.inin.aiinterviewer.domain.enums.ProfileStatus;

import java.time.LocalDateTime;

public class CandidateProfileEntity {
    private Long id;
    private Long resumeId;
    private Long userId;
    private String contentJson;
    private ProfileSource source;
    private ProfileStatus status;
    private String errorMessage;
    private boolean confirmed;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private boolean deleted;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getResumeId() { return resumeId; }
    public void setResumeId(Long resumeId) { this.resumeId = resumeId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getContentJson() { return contentJson; }
    public void setContentJson(String contentJson) { this.contentJson = contentJson; }
    public ProfileSource getSource() { return source; }
    public void setSource(ProfileSource source) { this.source = source; }
    public ProfileStatus getStatus() { return status; }
    public void setStatus(ProfileStatus status) { this.status = status; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public boolean isConfirmed() { return confirmed; }
    public void setConfirmed(boolean confirmed) { this.confirmed = confirmed; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
}

