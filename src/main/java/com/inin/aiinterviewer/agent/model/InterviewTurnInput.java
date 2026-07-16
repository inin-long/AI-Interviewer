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
        ClaimExtractionResult claimExtraction,
        LogicChainResult logicChainResult,
        EvidenceCollectionResult evidenceCollectionResult
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
                "", "", "", "", null, null, null);
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
                candidateProfileContext, "", "", "", null, null, null);
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
                candidateProfileContext, domainPackContext, claimLedgerContext, "", null, null, null);
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
                evidenceLedgerContext, null, null, null);
    }

    public InterviewTurnInput {
        messages = messages == null ? List.of() : List.copyOf(messages);
        summary = summary == null ? "" : summary;
        retrievedContext = retrievedContext == null ? "" : retrievedContext;
        candidateProfileContext = candidateProfileContext == null ? "" : candidateProfileContext;
        domainPackContext = domainPackContext == null ? "" : domainPackContext;
        claimLedgerContext = claimLedgerContext == null ? "" : claimLedgerContext;
        evidenceLedgerContext = evidenceLedgerContext == null ? "" : evidenceLedgerContext;
    }

    public InterviewTurnInput withClaimExtraction(ClaimExtractionResult extraction) {
        return new InterviewTurnInput(
                stage, currentQuestion, answer, plan, messages, summary, retrievedContext,
                candidateProfileContext, domainPackContext, claimLedgerContext, evidenceLedgerContext,
                extraction, logicChainResult, evidenceCollectionResult);
    }

    public InterviewTurnInput withClaimContext(
            ClaimExtractionResult extraction,
            String updatedClaimLedgerContext
    ) {
        return new InterviewTurnInput(
                stage, currentQuestion, answer, plan, messages, summary, retrievedContext,
                candidateProfileContext, domainPackContext, updatedClaimLedgerContext, evidenceLedgerContext,
                extraction, logicChainResult, evidenceCollectionResult);
    }

    public InterviewTurnInput withLogicChainResult(LogicChainResult result) {
        return new InterviewTurnInput(
                stage, currentQuestion, answer, plan, messages, summary, retrievedContext,
                candidateProfileContext, domainPackContext, claimLedgerContext, evidenceLedgerContext,
                claimExtraction, result, evidenceCollectionResult);
    }

    public InterviewTurnInput withEvidenceContext(
            EvidenceCollectionResult result,
            String updatedEvidenceLedgerContext
    ) {
        return new InterviewTurnInput(
                stage, currentQuestion, answer, plan, messages, summary, retrievedContext,
                candidateProfileContext, domainPackContext, claimLedgerContext,
                updatedEvidenceLedgerContext, claimExtraction, logicChainResult, result);
    }
}
