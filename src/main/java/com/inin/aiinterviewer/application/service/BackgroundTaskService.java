package com.inin.aiinterviewer.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inin.aiinterviewer.application.event.BackgroundTaskCompletedEvent;
import com.inin.aiinterviewer.application.event.BackgroundTaskFailedEvent;
import com.inin.aiinterviewer.application.exception.BusinessException;
import com.inin.aiinterviewer.application.exception.ErrorCode;
import com.inin.aiinterviewer.application.exception.TaskException;
import com.inin.aiinterviewer.application.task.BackgroundTaskContext;
import com.inin.aiinterviewer.application.task.BackgroundTaskHandlerRegistry;
import com.inin.aiinterviewer.config.properties.TaskProperties;
import com.inin.aiinterviewer.domain.entity.BackgroundTaskEntity;
import com.inin.aiinterviewer.domain.enums.BackgroundTaskType;
import com.inin.aiinterviewer.infrastructure.database.mapper.BackgroundTaskMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class BackgroundTaskService {
    private static final Logger log = LoggerFactory.getLogger(BackgroundTaskService.class);
    private static final int MAX_ERROR_LENGTH = 1_000;

    private final BackgroundTaskMapper mapper;
    private final BackgroundTaskHandlerRegistry handlers;
    private final TaskProperties properties;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher events;

    public BackgroundTaskService(
            BackgroundTaskMapper mapper,
            BackgroundTaskHandlerRegistry handlers,
            TaskProperties properties,
            ObjectMapper objectMapper,
            ApplicationEventPublisher events
    ) {
        this.mapper = mapper;
        this.handlers = handlers;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.events = events;
    }

    @Transactional
    public long enqueue(long userId, BackgroundTaskType type, Object payload) {
        BackgroundTaskEntity task = new BackgroundTaskEntity();
        task.setUserId(userId);
        task.setTaskType(type);
        task.setPayloadJson(writePayload(payload));
        mapper.insert(task);
        return task.getId();
    }

    public Optional<BackgroundTaskEntity> claimNext(String workerId) {
        if (workerId == null || workerId.isBlank()) throw new IllegalArgumentException("workerId is required");
        return mapper.claimNext(workerId);
    }

    public boolean executeNext(String workerId) {
        Optional<BackgroundTaskEntity> claimed = claimNext(workerId);
        if (claimed.isEmpty()) return false;
        executeClaimed(claimed.get(), workerId);
        return true;
    }

    public void executeClaimed(BackgroundTaskEntity task, String workerId) {
        try {
            handlers.require(task.getTaskType()).handle(new BackgroundTaskContext(task, objectMapper));
            if (mapper.markSuccess(task.getId(), workerId) != 1) {
                throw new IllegalStateException("Background task ownership was lost: " + task.getId());
            }
        } catch (Exception exception) {
            handleFailure(task, workerId, exception);
            return;
        }
        events.publishEvent(new BackgroundTaskCompletedEvent(
                task.getId(), task.getUserId(), task.getTaskType()));
    }

    public int recoverInterruptedTasks() {
        int recovered = mapper.recoverInterrupted();
        if (recovered > 0) log.info("Recovered {} interrupted background task(s)", recovered);
        return recovered;
    }

    @Transactional(readOnly = true)
    public BackgroundTaskEntity require(long userId, long taskId) {
        return mapper.findById(taskId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TASK_FAILED));
    }

    @Transactional(readOnly = true)
    public List<BackgroundTaskEntity> list(long userId) {
        return mapper.findAll(userId);
    }

    private void handleFailure(BackgroundTaskEntity task, String workerId, Exception exception) {
        String message = safeMessage(exception);
        if (task.getAttemptCount() < properties.retryCount()) {
            mapper.scheduleRetry(task.getId(), workerId, message, properties.retryDelay().toSeconds());
            log.warn("Background task {} failed on attempt {}/{} and will retry: {}",
                    task.getId(), task.getAttemptCount(), properties.retryCount(), message);
            return;
        }
        mapper.markFailed(task.getId(), workerId, message);
        log.error("Background task {} failed after {} attempt(s): {}",
                task.getId(), task.getAttemptCount(), message);
        events.publishEvent(new BackgroundTaskFailedEvent(
                task.getId(), task.getUserId(), task.getTaskType(), message));
    }

    private String writePayload(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload == null ? java.util.Map.of() : payload);
        } catch (JsonProcessingException exception) {
            throw new TaskException(ErrorCode.TASK_FAILED, exception);
        }
    }

    private String safeMessage(Exception exception) {
        Throwable cause = exception;
        while (cause.getCause() != null && cause.getMessage() == null) cause = cause.getCause();
        String message = cause.getMessage();
        if (message == null || message.isBlank()) message = exception.getClass().getSimpleName();
        return message.substring(0, Math.min(MAX_ERROR_LENGTH, message.length()));
    }
}
