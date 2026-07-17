package com.inin.aiinterviewer.agent.node;

import com.inin.aiinterviewer.agent.model.LogicChainResult;
import com.inin.aiinterviewer.agent.prompt.AgentPrompts;
import com.inin.aiinterviewer.agent.state.InterviewGraphState;
import com.inin.aiinterviewer.agent.support.StructuredAiResponseParser;
import com.inin.aiinterviewer.domain.model.LogicGap;
import com.inin.aiinterviewer.infrastructure.ai.ChatService;
import org.bsc.langgraph4j.action.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Component
public class LogicChainEvaluatorNode implements NodeAction<InterviewGraphState> {

    private static final Logger log = LoggerFactory.getLogger(LogicChainEvaluatorNode.class);
    private static final int MAX_ITEMS = 12;

    private final ChatService chatService;
    private final StructuredAiResponseParser parser;

    public LogicChainEvaluatorNode(ChatService chatService, StructuredAiResponseParser parser) {
        this.chatService = chatService;
        this.parser = parser;
    }

    @Override
    public Map<String, Object> apply(InterviewGraphState state) {
        if (state.data().containsKey(InterviewGraphState.LOGIC_CHAIN_RESULT)) {
            return output(state.logicChainResult());
        }
        if (!important(state)) return output(LogicChainResult.skippedResult());

        String response = "";
        RuntimeException firstFailure;
        try {
            response = chatService.chat(AgentPrompts.logicChainEvaluation(state));
            return output(validate(parser.parse(response, LogicChainResult.class)));
        } catch (RuntimeException exception) {
            firstFailure = exception;
        }
        try {
            String repaired = chatService.chat(AgentPrompts.repairLogicChainEvaluation(state, response));
            return output(validate(parser.parse(repaired, LogicChainResult.class)));
        } catch (RuntimeException repairFailure) {
            log.warn("Logic-chain evaluation degraded after one repair attempt: first={}, repair={}",
                    firstFailure.getClass().getSimpleName(), repairFailure.getClass().getSimpleName());
            return output(LogicChainResult.degraded("logic_chain_evaluation_failed"));
        }
    }

    private boolean important(InterviewGraphState state) {
        return state.claimExtraction().claims().stream().anyMatch(claim -> claim.importance() >= 0.65)
                || state.answer().length() >= 160;
    }

    private LogicChainResult validate(LogicChainResult result) {
        if (result == null || result.skipped() || result.degraded()
                || result.premises().size() > MAX_ITEMS || result.alternatives().size() > MAX_ITEMS
                || result.actions().size() > MAX_ITEMS || result.gaps().size() > MAX_ITEMS) {
            throw new IllegalArgumentException("Invalid logic-chain envelope");
        }
        List<LogicGap> gaps = result.gaps().stream().map(this::validateGap).toList();
        return new LogicChainResult(
                normalized(result.premises()), result.problemDiagnosis(), normalized(result.alternatives()),
                result.decision(), result.reasoning(), normalized(result.actions()), result.outcome(),
                result.validation(), result.reflection(), gaps, false, false, "");
    }

    private LogicGap validateGap(LogicGap gap) {
        if (gap == null || gap.type() == null || gap.description().isBlank()
                || gap.description().length() > 1_000 || !Double.isFinite(gap.severity())
                || gap.severity() < 0 || gap.severity() > 1 || gap.relatedClaimIds().size() > MAX_ITEMS) {
            throw new IllegalArgumentException("Invalid logic gap");
        }
        return new LogicGap(
                gap.type(), gap.description(), gap.severity(), normalized(gap.relatedClaimIds()));
    }

    private List<String> normalized(List<String> values) {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String value : values) {
            if (value != null && !value.isBlank()) normalized.add(value.strip());
        }
        return List.copyOf(normalized);
    }

    private Map<String, Object> output(LogicChainResult result) {
        return Map.of(InterviewGraphState.LOGIC_CHAIN_RESULT, result);
    }
}
