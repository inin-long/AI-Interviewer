package com.inin.aiinterviewer.agent.node;

import com.inin.aiinterviewer.agent.state.InterviewGraphState;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class CoverageUpdaterNode implements NodeAction<InterviewGraphState> {

    @Override
    public Map<String, Object> apply(InterviewGraphState state) {
        return Map.of(InterviewGraphState.COVERAGE,
                state.coverage().update(state.evidenceCollectionResult()));
    }
}
