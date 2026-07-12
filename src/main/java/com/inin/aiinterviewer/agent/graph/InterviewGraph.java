package com.inin.aiinterviewer.agent.graph;

import com.inin.aiinterviewer.agent.model.AgentAction;
import com.inin.aiinterviewer.agent.model.InterviewTurnInput;
import com.inin.aiinterviewer.agent.model.InterviewTurnPlan;
import com.inin.aiinterviewer.agent.node.AnswerAnalyzerNode;
import com.inin.aiinterviewer.agent.node.FollowUpDecisionNode;
import com.inin.aiinterviewer.agent.node.QuestionGeneratorNode;
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
    private static final String DECIDE = "follow_up_decision";
    private static final String TRANSITION = "stage_transition";
    private static final String QUESTION = "question_generator";

    private final CompiledGraph<InterviewGraphState> graph;
    private final QuestionGeneratorNode questionGenerator;

    public InterviewGraph(
            AnswerAnalyzerNode answerAnalyzer,
            FollowUpDecisionNode decisionNode,
            StageTransitionNode transitionNode,
            QuestionGeneratorNode questionGenerator
    ) {
        this.questionGenerator = questionGenerator;
        try {
            this.graph = new StateGraph<>(InterviewGraphState::new)
                    .addNode(ANALYZE, AsyncNodeAction.node_async(answerAnalyzer))
                    .addNode(DECIDE, AsyncNodeAction.node_async(decisionNode))
                    .addNode(TRANSITION, AsyncNodeAction.node_async(transitionNode))
                    .addNode(QUESTION, AsyncNodeAction.node_async(questionGenerator))
                    .addEdge(GraphDefinition.START, ANALYZE)
                    .addEdge(ANALYZE, DECIDE)
                    .addConditionalEdges(DECIDE,
                            AsyncEdgeAction.edge_async(state -> state.decision().action() == AgentAction.NEXT_STAGE
                                    ? "transition" : "question"),
                            Map.of("transition", TRANSITION, "question", QUESTION))
                    .addEdge(TRANSITION, QUESTION)
                    .addEdge(QUESTION, GraphDefinition.END)
                    .compile();
        } catch (GraphStateException exception) {
            throw new IllegalStateException("Cannot build interview graph", exception);
        }
    }

    public InterviewTurnPlan plan(InterviewTurnInput input) {
        InterviewGraphState state = graph.invoke(input(input))
                .orElseThrow(() -> new IllegalStateException("Interview graph returned no state"));
        return new InterviewTurnPlan(
                state.analysis(), state.decision(), state.stage(), state.questionPrompt());
    }

    public String initialQuestionPrompt(InterviewTurnInput input) {
        try {
            InterviewGraphState state = new InterviewGraphState(input(input));
            return (String) questionGenerator.apply(state).get(InterviewGraphState.QUESTION_PROMPT);
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot prepare initial interview question", exception);
        }
    }

    public QuestionGeneratorNode questionGenerator() {
        return questionGenerator;
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
        return values;
    }
}
