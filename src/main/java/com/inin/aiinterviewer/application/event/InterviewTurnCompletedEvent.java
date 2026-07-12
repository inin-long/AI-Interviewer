package com.inin.aiinterviewer.application.event;

import com.inin.aiinterviewer.domain.enums.InterviewStage;

public record InterviewTurnCompletedEvent(
        long userId,
        long sessionId,
        InterviewStage stage,
        boolean partial
) {
}
