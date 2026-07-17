package com.inin.aiinterviewer.domain.entity;

import java.time.LocalDateTime;

public class AssessmentResultEntity {
    private Long id;
    private Long userId;
    private String templateCode;
    private String resultCode;
    private String scoresJson;
    private String reportMarkdown;
    private LocalDateTime createTime;
    private boolean deleted;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getTemplateCode() { return templateCode; }
    public void setTemplateCode(String templateCode) { this.templateCode = templateCode; }
    public String getResultCode() { return resultCode; }
    public void setResultCode(String resultCode) { this.resultCode = resultCode; }
    public String getScoresJson() { return scoresJson; }
    public void setScoresJson(String scoresJson) { this.scoresJson = scoresJson; }
    public String getReportMarkdown() { return reportMarkdown; }
    public void setReportMarkdown(String reportMarkdown) { this.reportMarkdown = reportMarkdown; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
}
