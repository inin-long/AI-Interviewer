package com.inin.aiinterviewer.domain.entity;

import java.time.LocalDateTime;

public class CareerPlanEntity {
    private Long id;
    private Long userId;
    private String currentRole;
    private String targetRole;
    private String industry;
    private String experienceYears;
    private String planMarkdown;
    private LocalDateTime createTime;
    private boolean deleted;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getCurrentRole() { return currentRole; }
    public void setCurrentRole(String currentRole) { this.currentRole = currentRole; }
    public String getTargetRole() { return targetRole; }
    public void setTargetRole(String targetRole) { this.targetRole = targetRole; }
    public String getIndustry() { return industry; }
    public void setIndustry(String industry) { this.industry = industry; }
    public String getExperienceYears() { return experienceYears; }
    public void setExperienceYears(String experienceYears) { this.experienceYears = experienceYears; }
    public String getPlanMarkdown() { return planMarkdown; }
    public void setPlanMarkdown(String planMarkdown) { this.planMarkdown = planMarkdown; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
}
