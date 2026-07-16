package com.inin.aiinterviewer.agent.state;

import com.inin.aiinterviewer.domain.enums.InterviewStage;
import com.inin.aiinterviewer.domain.model.AnswerAnalysis;
import com.inin.aiinterviewer.domain.model.CandidateProfile;
import com.inin.aiinterviewer.domain.model.ClaimLedger;
import com.inin.aiinterviewer.domain.model.EvaluationResult;
import com.inin.aiinterviewer.domain.model.Message;

import java.util.List;
import java.util.Map;

public record InterviewState(
        String stateVersion,
        long sessionId,
        long userId,
        InterviewStage stage,
        List<Message> messages,
        String currentQuestion,
        String latestAnswer,
        AnswerAnalysis analysis,
        EvaluationResult evaluation,
        CandidateProfile profile,
        Map<String, Object> rules,
        String summary,
        ClaimLedger claimLedger
) {
    public static final String CURRENT_VERSION = "2.0";

    public InterviewState(
            String stateVersion,
            long sessionId,
            long userId,
            InterviewStage stage,
            List<Message> messages,
            String currentQuestion,
            String latestAnswer,
            AnswerAnalysis analysis,
            EvaluationResult evaluation,
            CandidateProfile profile,
            Map<String, Object> rules,
            String summary
    ) {
        this(stateVersion, sessionId, userId, stage, messages, currentQuestion, latestAnswer,
                analysis, evaluation, profile, rules, summary, ClaimLedger.empty());
    }

    public InterviewState {
        stateVersion = stateVersion == null ? CURRENT_VERSION : stateVersion;
        messages = messages == null ? List.of() : List.copyOf(messages);
        rules = rules == null ? Map.of() : Map.copyOf(rules);
        summary = summary == null ? "" : summary;
        claimLedger = claimLedger == null ? ClaimLedger.empty() : claimLedger;
    }
}
