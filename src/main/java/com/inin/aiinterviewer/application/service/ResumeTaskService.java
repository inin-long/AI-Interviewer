package com.inin.aiinterviewer.application.service;

import com.inin.aiinterviewer.application.dto.ResumeDto;
import com.inin.aiinterviewer.domain.enums.BackgroundTaskType;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

@Service
public class ResumeTaskService {
    private final ResumeService resumeService;
    private final BackgroundTaskService taskService;

    public ResumeTaskService(ResumeService resumeService, BackgroundTaskService taskService) {
        this.resumeService = resumeService;
        this.taskService = taskService;
    }

    public QueuedResume uploadAndEnqueue(long userId, Path source) {
        ResumeDto resume = resumeService.upload(userId, source);
        try {
            long taskId = taskService.enqueue(userId, BackgroundTaskType.RESUME_PARSE,
                    new ResumeParseTaskPayload(resume.id()));
            return new QueuedResume(resume, taskId);
        } catch (RuntimeException exception) {
            resumeService.delete(userId, resume.id());
            throw exception;
        }
    }

    public record QueuedResume(ResumeDto resume, long taskId) {
    }

    public record ResumeParseTaskPayload(long resumeId) {
    }
}
