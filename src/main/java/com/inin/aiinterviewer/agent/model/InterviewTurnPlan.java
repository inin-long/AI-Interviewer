package com.inin.aiinterviewer.agent.model;

import com.inin.aiinterviewer.domain.enums.InterviewStage;
import com.inin.aiinterviewer.domain.model.AnswerAnalysis;
import com.inin.aiinterviewer.domain.model.PressureState;
import com.inin.aiinterviewer.domain.model.InterviewCoverage;
import com.inin.aiinterviewer.domain.model.InterviewStrategy;

public record InterviewTurnPlan(
        AnswerAnalysis analysis,
        AgentDecision decision,
        InterviewStage stage,
        String questionPrompt,
        ClaimExtractionResult claimExtraction,
        LogicChainResult logicChainResult,
        EvidenceCollectionResult evidenceCollectionResult,
        ConsistencyCheckResult consistencyCheckResult,
        ProbePlan probePlan,
        PressureState pressureState,
        ScenarioDirectionResult scenarioDirectionResult,
        InterviewCoverage coverage,
        InterviewStrategy strategy
) {
    public InterviewTurnPlan(
            AnswerAnalysis analysis,
            AgentDecision decision,
            InterviewStage stage,
            String questionPrompt,
            ClaimExtractionResult claimExtraction,
            LogicChainResult logicChainResult,
            EvidenceCollectionResult evidenceCollectionResult,
            ConsistencyCheckResult consistencyCheckResult,
            ProbePlan probePlan,
            PressureState pressureState,
            ScenarioDirectionResult scenarioDirectionResult
    ) {
        this(analysis, decision, stage, questionPrompt, claimExtraction, logicChainResult,
                evidenceCollectionResult, consistencyCheckResult, probePlan, pressureState,
                scenarioDirectionResult, InterviewCoverage.empty(), InterviewStrategy.empty());
    }
    public InterviewTurnPlan(
            AnswerAnalysis analysis,
            AgentDecision decision,
            InterviewStage stage,
            String questionPrompt
    ) {
        this(analysis, decision, stage, questionPrompt,
                new ClaimExtractionResult(java.util.List.of()), LogicChainResult.skippedResult(),
                EvidenceCollectionResult.degraded("not_collected"),
                ConsistencyCheckResult.skipped("not_collected"), null, PressureState.initial(),
                ScenarioDirectionResult.skipped("not_directed"), InterviewCoverage.empty(),
                InterviewStrategy.empty());
    }
}
