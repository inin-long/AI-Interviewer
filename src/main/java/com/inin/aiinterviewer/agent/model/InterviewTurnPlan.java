package com.inin.aiinterviewer.agent.model;

import com.inin.aiinterviewer.domain.enums.InterviewStage;
import com.inin.aiinterviewer.domain.model.AnswerAnalysis;

public record InterviewTurnPlan(
        AnswerAnalysis analysis,
        AgentDecision decision,
        InterviewStage stage,
        String questionPrompt,
        ClaimExtractionResult claimExtraction,
        LogicChainResult logicChainResult,
        EvidenceCollectionResult evidenceCollectionResult,
        ConsistencyCheckResult consistencyCheckResult,
        ProbePlan probePlan
) {
    public InterviewTurnPlan(
            AnswerAnalysis analysis,
            AgentDecision decision,
            InterviewStage stage,
            String questionPrompt
    ) {
        this(analysis, decision, stage, questionPrompt,
                new ClaimExtractionResult(java.util.List.of()), LogicChainResult.skippedResult(),
                EvidenceCollectionResult.degraded("not_collected"),
                ConsistencyCheckResult.skipped("not_collected"), null);
    }
}
