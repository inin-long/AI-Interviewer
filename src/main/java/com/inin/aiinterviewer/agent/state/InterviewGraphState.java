package com.inin.aiinterviewer.agent.state;

import com.inin.aiinterviewer.agent.model.AgentDecision;
import com.inin.aiinterviewer.application.dto.InterviewPlanDto;
import com.inin.aiinterviewer.domain.enums.InterviewStage;
import com.inin.aiinterviewer.domain.model.AnswerAnalysis;
import com.inin.aiinterviewer.domain.model.Message;
import org.bsc.langgraph4j.state.AgentState;

import java.util.List;
import java.util.Map;

public class InterviewGraphState extends AgentState {

    public static final String STAGE = "stage";
    public static final String CURRENT_QUESTION = "currentQuestion";
    public static final String ANSWER = "answer";
    public static final String PLAN = "plan";
    public static final String MESSAGES = "messages";
    public static final String ANALYSIS = "analysis";
    public static final String DECISION = "decision";
    public static final String QUESTION_PROMPT = "questionPrompt";
    public static final String SUMMARY = "summary";
    public static final String RETRIEVED_CONTEXT = "retrievedContext";
    public static final String CANDIDATE_PROFILE_CONTEXT = "candidateProfileContext";

    public InterviewGraphState(Map<String, Object> data) {
        super(data);
    }

    public InterviewStage stage() {
        return this.<InterviewStage>value(STAGE).orElse(InterviewStage.INTRODUCTION);
    }

    public String currentQuestion() {
        return this.<String>value(CURRENT_QUESTION).orElse("");
    }

    public String answer() {
        return this.<String>value(ANSWER).orElse("");
    }

    public InterviewPlanDto plan() {
        return this.<InterviewPlanDto>value(PLAN).orElseThrow();
    }

    public List<Message> messages() {
        return this.<List<Message>>value(MESSAGES).orElseGet(List::of);
    }

    public AnswerAnalysis analysis() {
        return this.<AnswerAnalysis>value(ANALYSIS).orElseThrow();
    }

    public AgentDecision decision() {
        return this.<AgentDecision>value(DECISION).orElseThrow();
    }

    public String questionPrompt() {
        return this.<String>value(QUESTION_PROMPT).orElse("");
    }

    public String summary() {
        return this.<String>value(SUMMARY).orElse("");
    }

    public String retrievedContext() {
        return this.<String>value(RETRIEVED_CONTEXT).orElse("");
    }

    public String candidateProfileContext() {
        return this.<String>value(CANDIDATE_PROFILE_CONTEXT).orElse("");
    }
}
