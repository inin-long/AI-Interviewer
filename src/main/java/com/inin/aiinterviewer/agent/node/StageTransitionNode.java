package com.inin.aiinterviewer.agent.node;

import com.inin.aiinterviewer.agent.stage.StageManager;
import com.inin.aiinterviewer.agent.state.InterviewGraphState;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class StageTransitionNode implements NodeAction<InterviewGraphState> {

    private final StageManager stageManager;

    public StageTransitionNode(StageManager stageManager) {
        this.stageManager = stageManager;
    }

    @Override
    public Map<String, Object> apply(InterviewGraphState state) {
        return Map.of(InterviewGraphState.STAGE,
                stageManager.transition(state.stage(), state.decision().nextStage()));
    }
}
