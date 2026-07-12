package com.inin.aiinterviewer.agent.model;

import com.inin.aiinterviewer.domain.enums.InterviewStage;
import com.inin.aiinterviewer.domain.model.AnswerAnalysis;

public record InterviewTurnPlan(
        AnswerAnalysis analysis,
        AgentDecision decision,
        InterviewStage stage,
        String questionPrompt
) {
}
