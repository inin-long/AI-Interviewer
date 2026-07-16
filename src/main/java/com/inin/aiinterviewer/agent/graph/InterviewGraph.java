package com.inin.aiinterviewer.agent.graph;

import com.inin.aiinterviewer.agent.model.AgentAction;
import com.inin.aiinterviewer.agent.model.ClaimExtractionResult;
import com.inin.aiinterviewer.agent.model.InterviewTurnInput;
import com.inin.aiinterviewer.agent.model.InterviewTurnPlan;
import com.inin.aiinterviewer.agent.model.LogicChainResult;
import com.inin.aiinterviewer.agent.model.EvidenceCollectionResult;
import com.inin.aiinterviewer.agent.model.ConsistencyCheckResult;
import com.inin.aiinterviewer.agent.node.AnswerAnalyzerNode;
import com.inin.aiinterviewer.agent.node.ClaimExtractorNode;
import com.inin.aiinterviewer.agent.node.FollowUpDecisionNode;
import com.inin.aiinterviewer.agent.node.LogicChainEvaluatorNode;
import com.inin.aiinterviewer.agent.node.EvidenceCollectorNode;
import com.inin.aiinterviewer.agent.node.ConsistencyCheckNode;
import com.inin.aiinterviewer.agent.node.ProbePlannerNode;
import com.inin.aiinterviewer.agent.node.PressureControllerNode;
import com.inin.aiinterviewer.agent.node.QuestionRendererNode;
import com.inin.aiinterviewer.agent.node.StageTransitionNode;
import com.inin.aiinterviewer.agent.state.InterviewGraphState;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphDefinition;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.action.AsyncEdgeAction;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class InterviewGraph {

    private static final String ANALYZE = "answer_analysis";
    private static final String EXTRACT_CLAIMS = "claim_extractor";
    private static final String EVALUATE_LOGIC = "logic_chain_evaluator";
    private static final String COLLECT_EVIDENCE = "evidence_collector";
    private static final String CHECK_CONSISTENCY = "consistency_check";
    private static final String DECIDE = "follow_up_decision";
    private static final String TRANSITION = "stage_transition";
    private static final String PLAN_PROBE = "probe_planner";
    private static final String CONTROL_PRESSURE = "pressure_controller";
    private static final String RENDER_QUESTION = "question_renderer";

    private final CompiledGraph<InterviewGraphState> graph;
    private final QuestionRendererNode questionRenderer;
    private final ClaimExtractorNode claimExtractor;
    private final ProbePlannerNode probePlanner;
    private final PressureControllerNode pressureController;
    private final LogicChainEvaluatorNode logicChainEvaluator;
    private final EvidenceCollectorNode evidenceCollector;
    private final ConsistencyCheckNode consistencyCheck;

    public InterviewGraph(
            ClaimExtractorNode claimExtractor,
            LogicChainEvaluatorNode logicChainEvaluator,
            EvidenceCollectorNode evidenceCollector,
            ConsistencyCheckNode consistencyCheck,
            AnswerAnalyzerNode answerAnalyzer,
            FollowUpDecisionNode decisionNode,
            StageTransitionNode transitionNode,
            ProbePlannerNode probePlanner,
            PressureControllerNode pressureController,
            QuestionRendererNode questionRenderer
    ) {
        this.questionRenderer = questionRenderer;
        this.claimExtractor = claimExtractor;
        this.probePlanner = probePlanner;
        this.pressureController = pressureController;
        this.logicChainEvaluator = logicChainEvaluator;
        this.evidenceCollector = evidenceCollector;
        this.consistencyCheck = consistencyCheck;
        try {
            this.graph = new StateGraph<>(InterviewGraphState::new)
                    .addNode(EXTRACT_CLAIMS, AsyncNodeAction.node_async(claimExtractor))
                    .addNode(EVALUATE_LOGIC, AsyncNodeAction.node_async(logicChainEvaluator))
                    .addNode(COLLECT_EVIDENCE, AsyncNodeAction.node_async(evidenceCollector))
                    .addNode(CHECK_CONSISTENCY, AsyncNodeAction.node_async(consistencyCheck))
                    .addNode(ANALYZE, AsyncNodeAction.node_async(answerAnalyzer))
                    .addNode(DECIDE, AsyncNodeAction.node_async(decisionNode))
                    .addNode(TRANSITION, AsyncNodeAction.node_async(transitionNode))
                    .addNode(PLAN_PROBE, AsyncNodeAction.node_async(probePlanner))
                    .addNode(CONTROL_PRESSURE, AsyncNodeAction.node_async(pressureController))
                    .addNode(RENDER_QUESTION, AsyncNodeAction.node_async(questionRenderer))
                    .addEdge(GraphDefinition.START, EXTRACT_CLAIMS)
                    .addEdge(EXTRACT_CLAIMS, EVALUATE_LOGIC)
                    .addEdge(EVALUATE_LOGIC, COLLECT_EVIDENCE)
                    .addEdge(COLLECT_EVIDENCE, CHECK_CONSISTENCY)
                    .addEdge(CHECK_CONSISTENCY, ANALYZE)
                    .addEdge(ANALYZE, DECIDE)
                    .addConditionalEdges(DECIDE,
                            AsyncEdgeAction.edge_async(state -> state.consistencyCheckResult()
                                    .requiresClarification() ? "probe"
                                    : state.decision().action() == AgentAction.NEXT_STAGE
                                    ? "transition" : "probe"),
                            Map.of("transition", TRANSITION, "probe", PLAN_PROBE))
                    .addEdge(TRANSITION, PLAN_PROBE)
                    .addEdge(PLAN_PROBE, CONTROL_PRESSURE)
                    .addEdge(CONTROL_PRESSURE, RENDER_QUESTION)
                    .addEdge(RENDER_QUESTION, GraphDefinition.END)
                    .compile();
        } catch (GraphStateException exception) {
            throw new IllegalStateException("Cannot build interview graph", exception);
        }
    }

    public InterviewTurnPlan plan(InterviewTurnInput input) {
        InterviewGraphState state = graph.invoke(input(input))
                .orElseThrow(() -> new IllegalStateException("Interview graph returned no state"));
        return new InterviewTurnPlan(
                state.analysis(), state.decision(), state.stage(), state.questionPrompt(),
                state.claimExtraction(), state.logicChainResult(), state.evidenceCollectionResult(),
                state.consistencyCheckResult(), state.probePlan(), state.pressureState());
    }

    public String initialQuestionPrompt(InterviewTurnInput input) {
        try {
            Map<String, Object> values = input(input);
            InterviewGraphState state = new InterviewGraphState(values);
            values.putAll(probePlanner.apply(state));
            values.putAll(pressureController.apply(new InterviewGraphState(values)));
            return (String) questionRenderer.apply(new InterviewGraphState(values))
                    .get(InterviewGraphState.QUESTION_PROMPT);
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot prepare initial interview question", exception);
        }
    }

    public ClaimExtractionResult extractClaims(InterviewTurnInput input) {
        try {
            InterviewGraphState state = new InterviewGraphState(input(input));
            Object extraction = claimExtractor.apply(state).get(InterviewGraphState.CLAIM_EXTRACTION);
            if (extraction instanceof ClaimExtractionResult result) {
                return result;
            }
            return ClaimExtractionResult.degraded("claim_extraction_returned_no_result");
        } catch (Exception exception) {
            return ClaimExtractionResult.degraded("claim_extraction_failed");
        }
    }

    public LogicChainResult evaluateLogic(InterviewTurnInput input) {
        try {
            InterviewGraphState state = new InterviewGraphState(input(input));
            Object result = logicChainEvaluator.apply(state).get(InterviewGraphState.LOGIC_CHAIN_RESULT);
            return result instanceof LogicChainResult logic
                    ? logic : LogicChainResult.degraded("logic_chain_returned_no_result");
        } catch (Exception exception) {
            return LogicChainResult.degraded("logic_chain_evaluation_failed");
        }
    }

    public EvidenceCollectionResult collectEvidence(InterviewTurnInput input) {
        try {
            InterviewGraphState state = new InterviewGraphState(input(input));
            Object result = evidenceCollector.apply(state)
                    .get(InterviewGraphState.EVIDENCE_COLLECTION_RESULT);
            return result instanceof EvidenceCollectionResult evidence
                    ? evidence : EvidenceCollectionResult.degraded("evidence_collector_returned_no_result");
        } catch (Exception exception) {
            return EvidenceCollectionResult.degraded("evidence_collection_failed");
        }
    }

    public ConsistencyCheckResult checkConsistency(InterviewTurnInput input) {
        try {
            InterviewGraphState state = new InterviewGraphState(input(input));
            Object result = consistencyCheck.apply(state)
                    .get(InterviewGraphState.CONSISTENCY_CHECK_RESULT);
            return result instanceof ConsistencyCheckResult consistency
                    ? consistency : ConsistencyCheckResult.degraded("consistency_check_returned_no_result");
        } catch (Exception exception) {
            return ConsistencyCheckResult.degraded("consistency_check_failed");
        }
    }

    public QuestionRendererNode questionRenderer() {
        return questionRenderer;
    }

    private Map<String, Object> input(InterviewTurnInput input) {
        Map<String, Object> values = new HashMap<>();
        values.put(InterviewGraphState.STAGE, input.stage());
        values.put(InterviewGraphState.CURRENT_QUESTION,
                input.currentQuestion() == null ? "" : input.currentQuestion());
        values.put(InterviewGraphState.ANSWER, input.answer() == null ? "" : input.answer());
        values.put(InterviewGraphState.PLAN, input.plan());
        values.put(InterviewGraphState.MESSAGES, input.messages());
        values.put(InterviewGraphState.SUMMARY, input.summary());
        values.put(InterviewGraphState.RETRIEVED_CONTEXT, input.retrievedContext());
        values.put(InterviewGraphState.CANDIDATE_PROFILE_CONTEXT, input.candidateProfileContext());
        values.put(InterviewGraphState.DOMAIN_PACK_CONTEXT, input.domainPackContext());
        values.put(InterviewGraphState.CLAIM_LEDGER_CONTEXT, input.claimLedgerContext());
        values.put(InterviewGraphState.EVIDENCE_LEDGER_CONTEXT, input.evidenceLedgerContext());
        values.put(InterviewGraphState.CONSISTENCY_CONTEXT, input.consistencyContext());
        values.put(InterviewGraphState.DEFERRED_PROBES, input.deferredProbes());
        values.put(InterviewGraphState.PRESSURE_STATE, input.pressureState());
        if (input.claimExtraction() != null) {
            values.put(InterviewGraphState.CLAIM_EXTRACTION, input.claimExtraction());
        }
        if (input.logicChainResult() != null) {
            values.put(InterviewGraphState.LOGIC_CHAIN_RESULT, input.logicChainResult());
        }
        if (input.evidenceCollectionResult() != null) {
            values.put(InterviewGraphState.EVIDENCE_COLLECTION_RESULT, input.evidenceCollectionResult());
        }
        if (input.consistencyCheckResult() != null) {
            values.put(InterviewGraphState.CONSISTENCY_CHECK_RESULT, input.consistencyCheckResult());
        }
        return values;
    }
}
