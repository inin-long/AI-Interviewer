package com.inin.aiinterviewer.agent.state;

import com.inin.aiinterviewer.agent.model.ProbePlan;
import com.inin.aiinterviewer.agent.model.LogicChainResult;
import com.inin.aiinterviewer.domain.enums.InterviewStage;
import com.inin.aiinterviewer.domain.model.AnswerAnalysis;
import com.inin.aiinterviewer.domain.model.CandidateProfile;
import com.inin.aiinterviewer.domain.model.ClaimLedger;
import com.inin.aiinterviewer.domain.model.EvaluationResult;
import com.inin.aiinterviewer.domain.model.EvidenceLedger;
import com.inin.aiinterviewer.domain.model.Message;
import com.inin.aiinterviewer.domain.model.DeferredProbe;
import com.inin.aiinterviewer.domain.model.PressureState;
import com.inin.aiinterviewer.domain.model.ScenarioState;

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
        ClaimLedger claimLedger,
        EvidenceLedger evidenceLedger,
        LogicChainResult logicChainResult,
        ProbePlan probePlan,
        List<DeferredProbe> deferredProbes,
        PressureState pressureState,
        ScenarioState activeScenario
) {
    public static final String CURRENT_VERSION = "2.7";

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
            String summary,
            ClaimLedger claimLedger,
            EvidenceLedger evidenceLedger,
            LogicChainResult logicChainResult,
            ProbePlan probePlan,
            List<DeferredProbe> deferredProbes,
            PressureState pressureState
    ) {
        this(stateVersion, sessionId, userId, stage, messages, currentQuestion, latestAnswer,
                analysis, evaluation, profile, rules, summary, claimLedger, evidenceLedger,
                logicChainResult, probePlan, deferredProbes, pressureState, null);
    }

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
            String summary,
            ClaimLedger claimLedger,
            EvidenceLedger evidenceLedger,
            LogicChainResult logicChainResult,
            ProbePlan probePlan,
            List<DeferredProbe> deferredProbes
    ) {
        this(stateVersion, sessionId, userId, stage, messages, currentQuestion, latestAnswer,
                analysis, evaluation, profile, rules, summary, claimLedger, evidenceLedger,
                logicChainResult, probePlan, deferredProbes, PressureState.initial());
    }

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
            String summary,
            ClaimLedger claimLedger,
            EvidenceLedger evidenceLedger,
            LogicChainResult logicChainResult,
            ProbePlan probePlan
    ) {
        this(stateVersion, sessionId, userId, stage, messages, currentQuestion, latestAnswer,
                analysis, evaluation, profile, rules, summary, claimLedger, evidenceLedger,
                logicChainResult, probePlan, List.of());
    }

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
                analysis, evaluation, profile, rules, summary, ClaimLedger.empty(),
                EvidenceLedger.empty(), LogicChainResult.skippedResult(), null);
    }

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
            String summary,
            ClaimLedger claimLedger
    ) {
        this(stateVersion, sessionId, userId, stage, messages, currentQuestion, latestAnswer,
                analysis, evaluation, profile, rules, summary, claimLedger,
                EvidenceLedger.empty(), LogicChainResult.skippedResult(), null);
    }

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
            String summary,
            ClaimLedger claimLedger,
            ProbePlan probePlan
    ) {
        this(stateVersion, sessionId, userId, stage, messages, currentQuestion, latestAnswer,
                analysis, evaluation, profile, rules, summary, claimLedger,
                EvidenceLedger.empty(), LogicChainResult.skippedResult(), probePlan);
    }

    public InterviewState {
        stateVersion = stateVersion == null ? CURRENT_VERSION : stateVersion;
        messages = messages == null ? List.of() : List.copyOf(messages);
        rules = rules == null ? Map.of() : Map.copyOf(rules);
        summary = summary == null ? "" : summary;
        claimLedger = claimLedger == null ? ClaimLedger.empty() : claimLedger;
        evidenceLedger = evidenceLedger == null ? EvidenceLedger.empty() : evidenceLedger;
        logicChainResult = logicChainResult == null ? LogicChainResult.skippedResult() : logicChainResult;
        deferredProbes = deferredProbes == null ? List.of() : List.copyOf(deferredProbes);
        pressureState = pressureState == null ? PressureState.initial() : pressureState;
    }
}
