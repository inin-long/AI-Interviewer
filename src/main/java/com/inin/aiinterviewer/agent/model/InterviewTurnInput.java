package com.inin.aiinterviewer.agent.model;

import com.inin.aiinterviewer.application.dto.InterviewPlanDto;
import com.inin.aiinterviewer.domain.enums.InterviewStage;
import com.inin.aiinterviewer.domain.model.Message;
import com.inin.aiinterviewer.domain.model.DeferredProbe;
import com.inin.aiinterviewer.domain.model.PressureState;
import com.inin.aiinterviewer.domain.model.ScenarioState;
import com.inin.aiinterviewer.domain.model.InterviewCoverage;

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
        List<DeferredProbe> deferredProbes,
        PressureState pressureState,
        ScenarioState activeScenario,
        ClaimExtractionResult claimExtraction,
        LogicChainResult logicChainResult,
        EvidenceCollectionResult evidenceCollectionResult,
        ConsistencyCheckResult consistencyCheckResult,
        InterviewCoverage coverage
) {
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
            String evidenceLedgerContext,
            ConsistencyContext consistencyContext,
            List<DeferredProbe> deferredProbes,
            PressureState pressureState,
            ScenarioState activeScenario,
            ClaimExtractionResult claimExtraction,
            LogicChainResult logicChainResult,
            EvidenceCollectionResult evidenceCollectionResult,
            ConsistencyCheckResult consistencyCheckResult
    ) {
        this(stage, currentQuestion, answer, plan, messages, summary, retrievedContext,
                candidateProfileContext, domainPackContext, claimLedgerContext,
                evidenceLedgerContext, consistencyContext, deferredProbes, pressureState,
                activeScenario, claimExtraction, logicChainResult, evidenceCollectionResult,
                consistencyCheckResult, InterviewCoverage.empty());
    }
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
                "", "", "", "", ConsistencyContext.skipped("not_prepared"), List.of(),
                PressureState.initial(), null, null, null, null, null);
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
                candidateProfileContext, "", "", "", ConsistencyContext.skipped("not_prepared"), List.of(),
                PressureState.initial(), null, null, null, null, null);
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
                ConsistencyContext.skipped("not_prepared"), List.of(), PressureState.initial(),
                null, null, null, null, null);
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
                List.of(), PressureState.initial(), null, null, null, null, null);
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
        deferredProbes = deferredProbes == null ? List.of() : List.copyOf(deferredProbes);
        pressureState = pressureState == null ? PressureState.initial() : pressureState;
        coverage = coverage == null ? InterviewCoverage.empty() : coverage;
    }

    public InterviewTurnInput withClaimExtraction(ClaimExtractionResult extraction) {
        return new InterviewTurnInput(
                stage, currentQuestion, answer, plan, messages, summary, retrievedContext,
                candidateProfileContext, domainPackContext, claimLedgerContext, evidenceLedgerContext,
                consistencyContext, deferredProbes, pressureState, activeScenario,
                extraction, logicChainResult,
                evidenceCollectionResult,
                consistencyCheckResult, coverage);
    }

    public InterviewTurnInput withClaimContext(
            ClaimExtractionResult extraction,
            String updatedClaimLedgerContext
    ) {
        return new InterviewTurnInput(
                stage, currentQuestion, answer, plan, messages, summary, retrievedContext,
                candidateProfileContext, domainPackContext, updatedClaimLedgerContext, evidenceLedgerContext,
                consistencyContext, deferredProbes, pressureState, activeScenario,
                extraction, logicChainResult,
                evidenceCollectionResult,
                consistencyCheckResult, coverage);
    }

    public InterviewTurnInput withLogicChainResult(LogicChainResult result) {
        return new InterviewTurnInput(
                stage, currentQuestion, answer, plan, messages, summary, retrievedContext,
                candidateProfileContext, domainPackContext, claimLedgerContext, evidenceLedgerContext,
                consistencyContext, deferredProbes, pressureState, activeScenario,
                claimExtraction, result,
                evidenceCollectionResult,
                consistencyCheckResult, coverage);
    }

    public InterviewTurnInput withEvidenceContext(
            EvidenceCollectionResult result,
            String updatedEvidenceLedgerContext
    ) {
        return new InterviewTurnInput(
                stage, currentQuestion, answer, plan, messages, summary, retrievedContext,
                candidateProfileContext, domainPackContext, claimLedgerContext,
                updatedEvidenceLedgerContext, consistencyContext, deferredProbes, pressureState,
                activeScenario,
                claimExtraction, logicChainResult,
                result, consistencyCheckResult, coverage);
    }

    public InterviewTurnInput withConsistencyContext(
            ConsistencyContext context,
            ConsistencyCheckResult result,
            String updatedClaimLedgerContext
    ) {
        return new InterviewTurnInput(
                stage, currentQuestion, answer, plan, messages, summary, retrievedContext,
                candidateProfileContext, domainPackContext, updatedClaimLedgerContext,
                evidenceLedgerContext, context, deferredProbes, pressureState, activeScenario,
                claimExtraction,
                logicChainResult,
                evidenceCollectionResult, result, coverage);
    }

    public InterviewTurnInput withDeferredProbes(List<DeferredProbe> probes) {
        return new InterviewTurnInput(
                stage, currentQuestion, answer, plan, messages, summary, retrievedContext,
                candidateProfileContext, domainPackContext, claimLedgerContext,
                evidenceLedgerContext, consistencyContext, probes, pressureState, activeScenario,
                claimExtraction, logicChainResult,
                evidenceCollectionResult, consistencyCheckResult, coverage);
    }

    public InterviewTurnInput withPressureState(PressureState state) {
        return new InterviewTurnInput(
                stage, currentQuestion, answer, plan, messages, summary, retrievedContext,
                candidateProfileContext, domainPackContext, claimLedgerContext,
                evidenceLedgerContext, consistencyContext, deferredProbes, state, activeScenario,
                claimExtraction, logicChainResult, evidenceCollectionResult, consistencyCheckResult,
                coverage);
    }

    public InterviewTurnInput withActiveScenario(ScenarioState scenario) {
        return new InterviewTurnInput(
                stage, currentQuestion, answer, plan, messages, summary, retrievedContext,
                candidateProfileContext, domainPackContext, claimLedgerContext,
                evidenceLedgerContext, consistencyContext, deferredProbes, pressureState, scenario,
                claimExtraction, logicChainResult, evidenceCollectionResult, consistencyCheckResult,
                coverage);
    }

    public InterviewTurnInput withCoverage(InterviewCoverage value) {
        return new InterviewTurnInput(
                stage, currentQuestion, answer, plan, messages, summary, retrievedContext,
                candidateProfileContext, domainPackContext, claimLedgerContext,
                evidenceLedgerContext, consistencyContext, deferredProbes, pressureState,
                activeScenario, claimExtraction, logicChainResult, evidenceCollectionResult,
                consistencyCheckResult, value);
    }
}
