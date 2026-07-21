package com.inin.aiinterviewer.agent.node;

import com.inin.aiinterviewer.agent.stage.StageManager;
import com.inin.aiinterviewer.agent.state.InterviewGraphState;
import com.inin.aiinterviewer.domain.enums.InterviewStage;
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
        InterviewStage next = state.decision().nextStage();
        // COMPLETED 为终态，允许从任意阶段直接结束面试（不走 stageManager 校验）。
        if (next == InterviewStage.COMPLETED) {
            return Map.of(InterviewGraphState.STAGE, InterviewStage.COMPLETED);
        }
        return Map.of(InterviewGraphState.STAGE,
                stageManager.transition(state.stage(), next));
    }
}
