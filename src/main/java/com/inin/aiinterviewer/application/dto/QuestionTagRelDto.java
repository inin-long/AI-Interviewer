package com.inin.aiinterviewer.application.dto;

public class QuestionTagRelDto {
    private Long questionId;
    private String tagName;

    public QuestionTagRelDto() {
    }

    public QuestionTagRelDto(Long questionId, String tagName) {
        this.questionId = questionId;
        this.tagName = tagName;
    }

    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }
    public String getTagName() { return tagName; }
    public void setTagName(String tagName) { this.tagName = tagName; }
}
