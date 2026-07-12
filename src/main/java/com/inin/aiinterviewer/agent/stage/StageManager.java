package com.inin.aiinterviewer.agent.stage;

import com.inin.aiinterviewer.application.exception.BusinessException;
import com.inin.aiinterviewer.application.exception.ErrorCode;
import com.inin.aiinterviewer.domain.enums.InterviewStage;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Component
public class StageManager {

    private final Map<InterviewStage, Set<InterviewStage>> allowedTransitions =
            new EnumMap<>(InterviewStage.class);

    public StageManager() {
        allow(InterviewStage.INTRODUCTION, InterviewStage.RESUME_REVIEW, InterviewStage.SUMMARY);
        allow(InterviewStage.RESUME_REVIEW, InterviewStage.PROJECT_EXPERIENCE,
                InterviewStage.TECHNICAL_DEEP_DIVE, InterviewStage.SUMMARY);
        allow(InterviewStage.PROJECT_EXPERIENCE, InterviewStage.TECHNICAL_DEEP_DIVE,
                InterviewStage.SYSTEM_DESIGN, InterviewStage.CODING, InterviewStage.SUMMARY);
        allow(InterviewStage.TECHNICAL_DEEP_DIVE, InterviewStage.SYSTEM_DESIGN,
                InterviewStage.CODING, InterviewStage.BEHAVIORAL, InterviewStage.SUMMARY);
        allow(InterviewStage.SYSTEM_DESIGN, InterviewStage.CODING,
                InterviewStage.BEHAVIORAL, InterviewStage.SUMMARY);
        allow(InterviewStage.CODING, InterviewStage.BEHAVIORAL, InterviewStage.SUMMARY);
        allow(InterviewStage.BEHAVIORAL, InterviewStage.SUMMARY);
        allow(InterviewStage.SUMMARY, InterviewStage.COMPLETED);
        allowedTransitions.put(InterviewStage.COMPLETED, EnumSet.noneOf(InterviewStage.class));
    }

    public boolean canTransition(InterviewStage from, InterviewStage to) {
        if (from == to) {
            return true;
        }
        return allowedTransitions.getOrDefault(from, Set.of()).contains(to);
    }

    public InterviewStage transition(InterviewStage from, InterviewStage to) {
        if (!canTransition(from, to)) {
            throw new BusinessException(ErrorCode.INVALID_STATE);
        }
        return to;
    }

    private void allow(InterviewStage from, InterviewStage... destinations) {
        allowedTransitions.put(from, EnumSet.of(destinations[0], destinations));
    }
}

