package com.inin.aiinterviewer.domain.entity;

import com.inin.aiinterviewer.domain.enums.InterviewDifficulty;
import com.inin.aiinterviewer.domain.enums.QuestionCategory;
import java.time.LocalDateTime;

public class InterviewQuestionEntity {
    private Long id;
    private Long userId;
    private Long jobId;
    private QuestionCategory category;
    private String title;
    private String content;
    private String referenceAnswer;
    private InterviewDifficulty difficulty;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private boolean deleted;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }
    public QuestionCategory getCategory() { return category; }
    public void setCategory(QuestionCategory category) { this.category = category; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getReferenceAnswer() { return referenceAnswer; }
    public void setReferenceAnswer(String referenceAnswer) { this.referenceAnswer = referenceAnswer; }
    public InterviewDifficulty getDifficulty() { return difficulty; }
    public void setDifficulty(InterviewDifficulty difficulty) { this.difficulty = difficulty; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
}
