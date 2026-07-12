package com.inin.aiinterviewer.domain.entity;

import com.inin.aiinterviewer.domain.enums.InterviewDifficulty;

import java.time.LocalDateTime;

public class InterviewPlanEntity {
    private Long id;
    private Long userId;
    private String name;
    private String jobTitle;
    private String jobDescription;
    private InterviewDifficulty difficulty;
    private int durationMinutes;
    private int questionCount;
    private Long resumeId;
    private String rulesJson;
    private String stagesJson;
    private boolean defaultPlan;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private boolean deleted;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }
    public String getJobDescription() { return jobDescription; }
    public void setJobDescription(String jobDescription) { this.jobDescription = jobDescription; }
    public InterviewDifficulty getDifficulty() { return difficulty; }
    public void setDifficulty(InterviewDifficulty difficulty) { this.difficulty = difficulty; }
    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }
    public int getQuestionCount() { return questionCount; }
    public void setQuestionCount(int questionCount) { this.questionCount = questionCount; }
    public Long getResumeId() { return resumeId; }
    public void setResumeId(Long resumeId) { this.resumeId = resumeId; }
    public String getRulesJson() { return rulesJson; }
    public void setRulesJson(String rulesJson) { this.rulesJson = rulesJson; }
    public String getStagesJson() { return stagesJson; }
    public void setStagesJson(String stagesJson) { this.stagesJson = stagesJson; }
    public boolean isDefaultPlan() { return defaultPlan; }
    public void setDefaultPlan(boolean defaultPlan) { this.defaultPlan = defaultPlan; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
}
