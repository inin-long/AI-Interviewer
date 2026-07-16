package com.inin.aiinterviewer.domain.entity;

import com.inin.aiinterviewer.domain.enums.SessionBranchStatus;

import java.time.LocalDateTime;

public class SessionBranchEntity {
    private String id;
    private Long userId;
    private Long sourceSessionId;
    private Long sourceCheckpointId;
    private String parentBranchId;
    private Integer sourceQuestionNumber;
    private String title;
    private SessionBranchStatus status;
    private String sourceStateJson;
    private String originalQuestion;
    private String originalAnswer;
    private String newAnswer;
    private String comparisonJson;
    private String comparisonMarkdown;
    private String errorMessage;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private boolean deleted;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getSourceSessionId() { return sourceSessionId; }
    public void setSourceSessionId(Long sourceSessionId) { this.sourceSessionId = sourceSessionId; }
    public Long getSourceCheckpointId() { return sourceCheckpointId; }
    public void setSourceCheckpointId(Long sourceCheckpointId) { this.sourceCheckpointId = sourceCheckpointId; }
    public String getParentBranchId() { return parentBranchId; }
    public void setParentBranchId(String parentBranchId) { this.parentBranchId = parentBranchId; }
    public Integer getSourceQuestionNumber() { return sourceQuestionNumber; }
    public void setSourceQuestionNumber(Integer sourceQuestionNumber) { this.sourceQuestionNumber = sourceQuestionNumber; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public SessionBranchStatus getStatus() { return status; }
    public void setStatus(SessionBranchStatus status) { this.status = status; }
    public String getSourceStateJson() { return sourceStateJson; }
    public void setSourceStateJson(String sourceStateJson) { this.sourceStateJson = sourceStateJson; }
    public String getOriginalQuestion() { return originalQuestion; }
    public void setOriginalQuestion(String originalQuestion) { this.originalQuestion = originalQuestion; }
    public String getOriginalAnswer() { return originalAnswer; }
    public void setOriginalAnswer(String originalAnswer) { this.originalAnswer = originalAnswer; }
    public String getNewAnswer() { return newAnswer; }
    public void setNewAnswer(String newAnswer) { this.newAnswer = newAnswer; }
    public String getComparisonJson() { return comparisonJson; }
    public void setComparisonJson(String comparisonJson) { this.comparisonJson = comparisonJson; }
    public String getComparisonMarkdown() { return comparisonMarkdown; }
    public void setComparisonMarkdown(String comparisonMarkdown) { this.comparisonMarkdown = comparisonMarkdown; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
}
