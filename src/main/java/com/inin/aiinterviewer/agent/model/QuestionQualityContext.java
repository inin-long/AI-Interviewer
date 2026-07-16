package com.inin.aiinterviewer.agent.model;

import com.inin.aiinterviewer.application.dto.InterviewPlanDto;
import com.inin.aiinterviewer.domain.enums.InterviewStage;
import com.inin.aiinterviewer.domain.model.Message;
import com.inin.aiinterviewer.domain.model.PressureState;
import com.inin.aiinterviewer.domain.model.ScenarioState;

import java.util.List;

public record QuestionQualityContext(
        InterviewStage stage,
        InterviewPlanDto plan,
        ProbePlan probePlan,
        PressureState pressureState,
        ScenarioState activeScenario,
        List<Message> messages,
        String candidateProfileContext,
        String domainPackContext
) {
    public QuestionQualityContext {
        stage = stage == null ? InterviewStage.INTRODUCTION : stage;
        messages = messages == null ? List.of() : List.copyOf(messages);
        candidateProfileContext = candidateProfileContext == null ? "" : candidateProfileContext;
        domainPackContext = domainPackContext == null ? "" : domainPackContext;
        pressureState = pressureState == null ? PressureState.initial() : pressureState;
    }
}
