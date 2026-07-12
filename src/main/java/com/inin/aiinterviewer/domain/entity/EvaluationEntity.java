package com.inin.aiinterviewer.domain.entity;

public class EvaluationEntity {
    private Long id;
    private Long userId;
    private Long interviewId;
    private int overallScore;
    private int technicalScore;
    private int problemSolvingScore;
    private int projectScore;
    private int systemDesignScore;
    private int communicationScore;
    private int comprehensiveScore;
    private String contentJson;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getInterviewId() { return interviewId; }
    public void setInterviewId(Long interviewId) { this.interviewId = interviewId; }
    public int getOverallScore() { return overallScore; }
    public void setOverallScore(int overallScore) { this.overallScore = overallScore; }
    public int getTechnicalScore() { return technicalScore; }
    public void setTechnicalScore(int technicalScore) { this.technicalScore = technicalScore; }
    public int getProblemSolvingScore() { return problemSolvingScore; }
    public void setProblemSolvingScore(int problemSolvingScore) { this.problemSolvingScore = problemSolvingScore; }
    public int getProjectScore() { return projectScore; }
    public void setProjectScore(int projectScore) { this.projectScore = projectScore; }
    public int getSystemDesignScore() { return systemDesignScore; }
    public void setSystemDesignScore(int systemDesignScore) { this.systemDesignScore = systemDesignScore; }
    public int getCommunicationScore() { return communicationScore; }
    public void setCommunicationScore(int communicationScore) { this.communicationScore = communicationScore; }
    public int getComprehensiveScore() { return comprehensiveScore; }
    public void setComprehensiveScore(int comprehensiveScore) { this.comprehensiveScore = comprehensiveScore; }
    public String getContentJson() { return contentJson; }
    public void setContentJson(String contentJson) { this.contentJson = contentJson; }
}
