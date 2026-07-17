package com.inin.aiinterviewer.domain.entity;

import java.time.LocalDateTime;

public class ResumeOptimizationEntity {
    private Long id;
    private Long userId;
    private String originalText;
    private String optimizedText;
    private String highlightsJson;
    private LocalDateTime createTime;
    private boolean deleted;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getOriginalText() { return originalText; }
    public void setOriginalText(String originalText) { this.originalText = originalText; }
    public String getOptimizedText() { return optimizedText; }
    public void setOptimizedText(String optimizedText) { this.optimizedText = optimizedText; }
    public String getHighlightsJson() { return highlightsJson; }
    public void setHighlightsJson(String highlightsJson) { this.highlightsJson = highlightsJson; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
}
