package com.inin.aiinterviewer.agent.node;

import com.inin.aiinterviewer.agent.state.InterviewGraphState;
import com.inin.aiinterviewer.agent.support.PressureController;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PressureControllerNode implements NodeAction<InterviewGraphState> {

    private final PressureController pressureController;

    public PressureControllerNode(PressureController pressureController) {
        this.pressureController = pressureController;
    }

    @Override
    public Map<String, Object> apply(InterviewGraphState state) {
        var result = pressureController.control(
                state.plan(), state.probePlan(), state.pressureState(), state.evidenceCollectionResult());
        return Map.of(
                InterviewGraphState.PROBE_PLAN, result.probePlan(),
                InterviewGraphState.PRESSURE_STATE, result.pressureState());
    }
}
