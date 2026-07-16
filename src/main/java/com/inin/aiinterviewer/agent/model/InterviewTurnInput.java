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
        ClaimExtractionResult claimExtraction
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
                "", "", "", null);
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
                candidateProfileContext, "", "", null);
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
                candidateProfileContext, domainPackContext, claimLedgerContext, null);
    }

    public InterviewTurnInput {
        messages = messages == null ? List.of() : List.copyOf(messages);
        summary = summary == null ? "" : summary;
        retrievedContext = retrievedContext == null ? "" : retrievedContext;
        candidateProfileContext = candidateProfileContext == null ? "" : candidateProfileContext;
        domainPackContext = domainPackContext == null ? "" : domainPackContext;
        claimLedgerContext = claimLedgerContext == null ? "" : claimLedgerContext;
    }

    public InterviewTurnInput withClaimExtraction(ClaimExtractionResult extraction) {
        return new InterviewTurnInput(
                stage, currentQuestion, answer, plan, messages, summary, retrievedContext,
                candidateProfileContext, domainPackContext, claimLedgerContext, extraction);
    }
}
