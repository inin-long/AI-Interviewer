package com.inin.aiinterviewer.application.service;

import com.inin.aiinterviewer.application.dto.ReportGenerationTaskStateDto;
import com.inin.aiinterviewer.application.exception.BusinessException;
import com.inin.aiinterviewer.application.exception.ErrorCode;
import com.inin.aiinterviewer.domain.enums.BackgroundTaskType;
import com.inin.aiinterviewer.domain.enums.BackgroundTaskStatus;
import com.inin.aiinterviewer.domain.enums.ReportStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class ReportGenerationTaskService {
    private static final String DEDUPLICATION_PREFIX = "interview-report:";

    private final BackgroundTaskService taskService;
    private final InterviewCompletionService completionService;
    private final InterviewSessionService sessionService;
    private final InterviewResultService resultService;
    private final TransactionTemplate transactionTemplate;

    public ReportGenerationTaskService(
            BackgroundTaskService taskService,
            InterviewCompletionService completionService,
            InterviewSessionService sessionService,
            InterviewResultService resultService,
            PlatformTransactionManager transactionManager
    ) {
        this.taskService = taskService;
        this.completionService = completionService;
        this.sessionService = sessionService;
        this.resultService = resultService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public long enqueue(long userId, long sessionId) {
        var completion = completionService.state(userId, sessionId);
        if (!completion.finalAnswerSaved() || completion.reportStatus() == ReportStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.INVALID_STATE);
        }

        var latest = taskService.findLatestByDeduplicationKey(
                userId, BackgroundTaskType.REPORT_GENERATE, deduplicationKey(sessionId));
        Long taskId = transactionTemplate.execute(status -> {
            long id;
            if (latest.isPresent()
                    && latest.get().getStatus() == BackgroundTaskStatus.FAILED
                    && taskService.retryFailedIfCurrent(userId, latest.get().getId())) {
                id = latest.get().getId();
            } else {
                id = taskService.enqueueUnique(
                        userId,
                        BackgroundTaskType.REPORT_GENERATE,
                        deduplicationKey(sessionId),
                        new ReportGenerationTaskPayload(sessionId));
            }
            resultService.beginGeneration(userId, sessionService.require(userId, sessionId));
            return id;
        });
        if (taskId == null) throw new BusinessException(ErrorCode.TASK_FAILED);
        return taskId;
    }

    @Transactional(readOnly = true)
    public ReportGenerationTaskStateDto state(long userId, long sessionId) {
        var completion = completionService.state(userId, sessionId);
        return taskService.findLatestByDeduplicationKey(
                        userId, BackgroundTaskType.REPORT_GENERATE, deduplicationKey(sessionId))
                .map(task -> new ReportGenerationTaskStateDto(
                        completion, task.getId(), task.getStatus(), task.getAttemptCount(), task.getErrorMessage()))
                .orElseGet(() -> new ReportGenerationTaskStateDto(completion, null, null, 0, ""));
    }

    private String deduplicationKey(long sessionId) {
        return DEDUPLICATION_PREFIX + sessionId;
    }

    public record ReportGenerationTaskPayload(long sessionId) {
        public ReportGenerationTaskPayload {
            if (sessionId <= 0) throw new IllegalArgumentException("sessionId must be positive");
        }
    }
}
