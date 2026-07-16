package com.inin.aiinterviewer.agent.model;

import com.inin.aiinterviewer.application.dto.InterviewPlanDto;
import com.inin.aiinterviewer.domain.enums.InterviewStage;
import com.inin.aiinterviewer.domain.model.Message;

import java.util.List;

public record InterviewTurnInput(
        InterviewStage stage,
        String currentQuestion,
        String answer,
        InterviewPlanDto plan,
        List<Message> messages,
        String summary,
        String retrievedContext,
        String candidateProfileContext,
        String domainPackContext,
        String claimLedgerContext,
        String evidenceLedgerContext,
        ConsistencyContext consistencyContext,
        ClaimExtractionResult claimExtraction,
        LogicChainResult logicChainResult,
        EvidenceCollectionResult evidenceCollectionResult,
        ConsistencyCheckResult consistencyCheckResult
) {
    public InterviewTurnInput(
            InterviewStage stage,
            String currentQuestion,
            String answer,
            InterviewPlanDto plan,
            List<Message> messages,
            String summary,
            String retrievedContext
    ) {
        this(stage, currentQuestion, answer, plan, messages, summary, retrievedContext,
                "", "", "", "", ConsistencyContext.skipped("not_prepared"),
                null, null, null, null);
    }

    public InterviewTurnInput(
            InterviewStage stage,
            String currentQuestion,
            String answer,
            InterviewPlanDto plan,
            List<Message> messages,
            String summary,
            String retrievedContext,
            String candidateProfileContext
    ) {
        this(stage, currentQuestion, answer, plan, messages, summary, retrievedContext,
                candidateProfileContext, "", "", "", ConsistencyContext.skipped("not_prepared"),
                null, null, null, null);
    }

    public InterviewTurnInput(
            InterviewStage stage,
            String currentQuestion,
            String answer,
            InterviewPlanDto plan,
            List<Message> messages,
            String summary,
            String retrievedContext,
            String candidateProfileContext,
            String domainPackContext,
            String claimLedgerContext
    ) {
        this(stage, currentQuestion, answer, plan, messages, summary, retrievedContext,
                candidateProfileContext, domainPackContext, claimLedgerContext, "",
                ConsistencyContext.skipped("not_prepared"), null, null, null, null);
    }

    public InterviewTurnInput(
            InterviewStage stage,
            String currentQuestion,
            String answer,
            InterviewPlanDto plan,
            List<Message> messages,
            String summary,
            String retrievedContext,
            String candidateProfileContext,
            String domainPackContext,
            String claimLedgerContext,
            String evidenceLedgerContext
    ) {
        this(stage, currentQuestion, answer, plan, messages, summary, retrievedContext,
                candidateProfileContext, domainPackContext, claimLedgerContext,
                evidenceLedgerContext, ConsistencyContext.skipped("not_prepared"),
                null, null, null, null);
    }

    public InterviewTurnInput {
        messages = messages == null ? List.of() : List.copyOf(messages);
        summary = summary == null ? "" : summary;
        retrievedContext = retrievedContext == null ? "" : retrievedContext;
        candidateProfileContext = candidateProfileContext == null ? "" : candidateProfileContext;
        domainPackContext = domainPackContext == null ? "" : domainPackContext;
        claimLedgerContext = claimLedgerContext == null ? "" : claimLedgerContext;
        evidenceLedgerContext = evidenceLedgerContext == null ? "" : evidenceLedgerContext;
        consistencyContext = consistencyContext == null
                ? ConsistencyContext.skipped("not_prepared") : consistencyContext;
    }

    public InterviewTurnInput withClaimExtraction(ClaimExtractionResult extraction) {
        return new InterviewTurnInput(
                stage, currentQuestion, answer, plan, messages, summary, retrievedContext,
                candidateProfileContext, domainPackContext, claimLedgerContext, evidenceLedgerContext,
                consistencyContext, extraction, logicChainResult, evidenceCollectionResult,
                consistencyCheckResult);
    }

    public InterviewTurnInput withClaimContext(
            ClaimExtractionResult extraction,
            String updatedClaimLedgerContext
    ) {
        return new InterviewTurnInput(
                stage, currentQuestion, answer, plan, messages, summary, retrievedContext,
                candidateProfileContext, domainPackContext, updatedClaimLedgerContext, evidenceLedgerContext,
                consistencyContext, extraction, logicChainResult, evidenceCollectionResult,
                consistencyCheckResult);
    }

    public InterviewTurnInput withLogicChainResult(LogicChainResult result) {
        return new InterviewTurnInput(
                stage, currentQuestion, answer, plan, messages, summary, retrievedContext,
                candidateProfileContext, domainPackContext, claimLedgerContext, evidenceLedgerContext,
                consistencyContext, claimExtraction, result, evidenceCollectionResult,
                consistencyCheckResult);
    }

    public InterviewTurnInput withEvidenceContext(
            EvidenceCollectionResult result,
            String updatedEvidenceLedgerContext
    ) {
        return new InterviewTurnInput(
                stage, currentQuestion, answer, plan, messages, summary, retrievedContext,
                candidateProfileContext, domainPackContext, claimLedgerContext,
                updatedEvidenceLedgerContext, consistencyContext, claimExtraction, logicChainResult,
                result, consistencyCheckResult);
    }

    public InterviewTurnInput withConsistencyContext(
            ConsistencyContext context,
            ConsistencyCheckResult result,
            String updatedClaimLedgerContext
    ) {
        return new InterviewTurnInput(
                stage, currentQuestion, answer, plan, messages, summary, retrievedContext,
                candidateProfileContext, domainPackContext, updatedClaimLedgerContext,
                evidenceLedgerContext, context, claimExtraction, logicChainResult,
                evidenceCollectionResult, result);
    }
}
