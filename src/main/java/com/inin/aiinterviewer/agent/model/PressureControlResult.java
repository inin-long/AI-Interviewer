package com.inin.aiinterviewer.agent.model;

import com.inin.aiinterviewer.domain.model.PressureState;

public record PressureControlResult(
        ProbePlan probePlan,
        PressureState pressureState
) {
}
