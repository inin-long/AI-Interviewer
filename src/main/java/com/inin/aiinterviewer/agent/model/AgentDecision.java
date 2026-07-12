package com.inin.aiinterviewer.agent.model;

import com.inin.aiinterviewer.domain.enums.InterviewStage;
import java.io.Serializable;

public record AgentDecision(
        AgentAction action,
        InterviewStage nextStage,
        String reason
) implements Serializable {
}
