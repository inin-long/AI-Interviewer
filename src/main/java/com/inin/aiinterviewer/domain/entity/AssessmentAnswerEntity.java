package com.inin.aiinterviewer.domain.entity;

public class AssessmentAnswerEntity {
    private Long id;
    private Long resultId;
    private Long questionId;
    private int optionIndex;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getResultId() { return resultId; }
    public void setResultId(Long resultId) { this.resultId = resultId; }
    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }
    public int getOptionIndex() { return optionIndex; }
    public void setOptionIndex(int optionIndex) { this.optionIndex = optionIndex; }
}
