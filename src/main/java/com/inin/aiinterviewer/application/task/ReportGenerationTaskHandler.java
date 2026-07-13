package com.inin.aiinterviewer.application.task;

import com.inin.aiinterviewer.application.service.InterviewCompletionService;
import com.inin.aiinterviewer.application.service.ReportGenerationTaskService.ReportGenerationTaskPayload;
import com.inin.aiinterviewer.domain.enums.BackgroundTaskType;
import org.springframework.stereotype.Component;

@Component
public class ReportGenerationTaskHandler implements BackgroundTaskHandler {
    private final InterviewCompletionService completionService;

    public ReportGenerationTaskHandler(InterviewCompletionService completionService) {
        this.completionService = completionService;
    }

    @Override
    public BackgroundTaskType taskType() {
        return BackgroundTaskType.REPORT_GENERATE;
    }

    @Override
    public void handle(BackgroundTaskContext context) {
        ReportGenerationTaskPayload payload = context.payload(ReportGenerationTaskPayload.class);
        completionService.complete(context.userId(), payload.sessionId());
    }
}
