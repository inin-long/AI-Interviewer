package com.inin.aiinterviewer.agent.state;

import com.inin.aiinterviewer.agent.model.AgentDecision;
import com.inin.aiinterviewer.agent.model.ClaimExtractionResult;
import com.inin.aiinterviewer.agent.model.LogicChainResult;
import com.inin.aiinterviewer.agent.model.EvidenceCollectionResult;
import com.inin.aiinterviewer.agent.model.ConsistencyCheckResult;
import com.inin.aiinterviewer.agent.model.ConsistencyContext;
import com.inin.aiinterviewer.agent.model.ProbePlan;
import com.inin.aiinterviewer.application.dto.InterviewPlanDto;
import com.inin.aiinterviewer.domain.enums.InterviewStage;
import com.inin.aiinterviewer.domain.model.AnswerAnalysis;
import com.inin.aiinterviewer.domain.model.Message;
import com.inin.aiinterviewer.domain.model.DeferredProbe;
import com.inin.aiinterviewer.domain.model.PressureState;
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
    public static final String DOMAIN_PACK_CONTEXT = "domainPackContext";
    public static final String CLAIM_LEDGER_CONTEXT = "claimLedgerContext";
    public static final String CLAIM_EXTRACTION = "claimExtraction";
    public static final String PROBE_PLAN = "probePlan";
    public static final String LOGIC_CHAIN_RESULT = "logicChainResult";
    public static final String EVIDENCE_LEDGER_CONTEXT = "evidenceLedgerContext";
    public static final String EVIDENCE_COLLECTION_RESULT = "evidenceCollectionResult";
    public static final String CONSISTENCY_CONTEXT = "consistencyContext";
    public static final String CONSISTENCY_CHECK_RESULT = "consistencyCheckResult";
    public static final String DEFERRED_PROBES = "deferredProbes";
    public static final String PRESSURE_STATE = "pressureState";

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

    public String domainPackContext() {
        return this.<String>value(DOMAIN_PACK_CONTEXT).orElse("");
    }

    public String claimLedgerContext() {
        return this.<String>value(CLAIM_LEDGER_CONTEXT).orElse("");
    }

    public ClaimExtractionResult claimExtraction() {
        return this.<ClaimExtractionResult>value(CLAIM_EXTRACTION)
                .orElseGet(() -> new ClaimExtractionResult(List.of()));
    }

    public ProbePlan probePlan() {
        return this.<ProbePlan>value(PROBE_PLAN)
                .orElseGet(() -> ProbePlan.stageOpening("验证当前阶段的岗位核心能力"));
    }

    public LogicChainResult logicChainResult() {
        return this.<LogicChainResult>value(LOGIC_CHAIN_RESULT).orElseGet(LogicChainResult::skippedResult);
    }

    public String evidenceLedgerContext() {
        return this.<String>value(EVIDENCE_LEDGER_CONTEXT).orElse("");
    }

    public EvidenceCollectionResult evidenceCollectionResult() {
        return this.<EvidenceCollectionResult>value(EVIDENCE_COLLECTION_RESULT)
                .orElseGet(() -> EvidenceCollectionResult.degraded("not_collected"));
    }

    public ConsistencyContext consistencyContext() {
        return this.<ConsistencyContext>value(CONSISTENCY_CONTEXT)
                .orElseGet(() -> ConsistencyContext.skipped("not_prepared"));
    }

    public ConsistencyCheckResult consistencyCheckResult() {
        return this.<ConsistencyCheckResult>value(CONSISTENCY_CHECK_RESULT)
                .orElseGet(() -> ConsistencyCheckResult.skipped("not_checked"));
    }

    public List<DeferredProbe> deferredProbes() {
        return this.<List<DeferredProbe>>value(DEFERRED_PROBES).orElseGet(List::of);
    }

    public PressureState pressureState() {
        return this.<PressureState>value(PRESSURE_STATE).orElseGet(PressureState::initial);
    }
}
