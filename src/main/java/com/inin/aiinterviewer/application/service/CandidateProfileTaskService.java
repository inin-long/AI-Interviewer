package com.inin.aiinterviewer.application.service;

import com.inin.aiinterviewer.domain.enums.BackgroundTaskType;
import org.springframework.stereotype.Service;

@Service
public class CandidateProfileTaskService {
    private final BackgroundTaskService taskService;

    public CandidateProfileTaskService(BackgroundTaskService taskService) {
        this.taskService = taskService;
    }

    public long enqueue(long userId, long resumeId) {
        return taskService.enqueue(userId, BackgroundTaskType.PROFILE_GENERATE,
                new CandidateProfileTaskPayload(resumeId));
    }

    public record CandidateProfileTaskPayload(long resumeId) {
    }
}
