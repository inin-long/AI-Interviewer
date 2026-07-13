package com.inin.aiinterviewer.application.task;

import com.inin.aiinterviewer.application.service.ResumeService;
import com.inin.aiinterviewer.application.service.ResumeTaskService.ResumeParseTaskPayload;
import com.inin.aiinterviewer.domain.enums.BackgroundTaskType;
import org.springframework.stereotype.Component;

@Component
public class ResumeParseTaskHandler implements BackgroundTaskHandler {
    private final ResumeService resumeService;

    public ResumeParseTaskHandler(ResumeService resumeService) {
        this.resumeService = resumeService;
    }

    @Override
    public BackgroundTaskType taskType() {
        return BackgroundTaskType.RESUME_PARSE;
    }

    @Override
    public void handle(BackgroundTaskContext context) {
        ResumeParseTaskPayload payload = context.payload(ResumeParseTaskPayload.class);
        resumeService.processResume(context.userId(), payload.resumeId());
    }
}
