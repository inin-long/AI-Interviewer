package com.inin.aiinterviewer.agent.stage;

import com.inin.aiinterviewer.application.exception.BusinessException;
import com.inin.aiinterviewer.domain.enums.InterviewStage;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StageManagerTest {

    private final StageManager stageManager = new StageManager();

    @Test
    void acceptsConfiguredTransitionsAndSameStageFollowUps() {
        assertThat(stageManager.transition(
                InterviewStage.PROJECT_EXPERIENCE,
                InterviewStage.TECHNICAL_DEEP_DIVE
        )).isEqualTo(InterviewStage.TECHNICAL_DEEP_DIVE);

        assertThat(stageManager.transition(
                InterviewStage.TECHNICAL_DEEP_DIVE,
                InterviewStage.TECHNICAL_DEEP_DIVE
        )).isEqualTo(InterviewStage.TECHNICAL_DEEP_DIVE);
    }

    @Test
    void rejectsAgentAttemptToSkipToCompleted() {
        assertThatThrownBy(() -> stageManager.transition(
                InterviewStage.INTRODUCTION,
                InterviewStage.COMPLETED
        )).isInstanceOf(BusinessException.class);
    }
}

