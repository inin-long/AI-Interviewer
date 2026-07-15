package com.inin.aiinterviewer.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inin.aiinterviewer.application.event.BackgroundTaskCompletedEvent;
import com.inin.aiinterviewer.application.event.BackgroundTaskFailedEvent;
import com.inin.aiinterviewer.application.event.BackgroundTaskQueuedEvent;
import com.inin.aiinterviewer.application.event.BackgroundTaskStartedEvent;
import com.inin.aiinterviewer.application.dto.BackgroundTaskDto;
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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class BackgroundTaskService {
    private static final Logger log = LoggerFactory.getLogger(BackgroundTaskService.class);
    private static final int MAX_ERROR_LENGTH = 1_000;
    private static final int MAX_DEDUPLICATION_KEY_LENGTH = 255;
    private static final Pattern BEARER_TOKEN = Pattern.compile("(?i)(Bearer\\s+)[^\\s,;]+", Pattern.CASE_INSENSITIVE);
    private static final Pattern API_KEY = Pattern.compile("(?i)sk-[a-z0-9_-]{8,}");

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
        BackgroundTaskEntity task = newTask(userId, type, payload, null);
        mapper.insert(task);
        publishAfterCommit(new BackgroundTaskQueuedEvent(task.getId(), userId, type));
        return task.getId();
    }

    @Transactional
    public long enqueueUnique(long userId, BackgroundTaskType type, String deduplicationKey, Object payload) {
        String normalizedKey = normalizeDeduplicationKey(deduplicationKey);
        BackgroundTaskEntity task = newTask(userId, type, payload, normalizedKey);
        if (mapper.insertUnique(task) == 1) {
            publishAfterCommit(new BackgroundTaskQueuedEvent(task.getId(), userId, type));
            return task.getId();
        }
        return mapper.findActiveByDeduplicationKey(userId, type, normalizedKey)
                .map(BackgroundTaskEntity::getId)
                .orElseThrow(() -> new TaskException(
                        ErrorCode.TASK_FAILED,
                        new IllegalStateException("Cannot resolve the active background task")));
    }

    public Optional<BackgroundTaskEntity> claimNext(String workerId) {
        if (workerId == null || workerId.isBlank()) throw new IllegalArgumentException("workerId is required");
        Optional<BackgroundTaskEntity> claimed = mapper.claimNext(workerId);
        claimed.ifPresent(task -> publishAfterCommit(new BackgroundTaskStartedEvent(
                task.getId(), task.getUserId(), task.getTaskType())));
        return claimed;
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

    @Transactional(readOnly = true)
    public Optional<BackgroundTaskEntity> findLatestByDeduplicationKey(
            long userId, BackgroundTaskType type, String deduplicationKey
    ) {
        return mapper.findLatestByDeduplicationKey(userId, type,
                normalizeDeduplicationKey(deduplicationKey));
    }

    @Transactional(readOnly = true)
    public BackgroundTaskDto requireDto(long userId, long taskId) {
        return toDto(require(userId, taskId));
    }

    @Transactional(readOnly = true)
    public List<BackgroundTaskDto> listDtos(long userId) {
        return mapper.findAll(userId).stream().map(this::toDto).toList();
    }

    @Transactional
    public BackgroundTaskDto retryFailed(long userId, long taskId) {
        if (mapper.retryFailed(taskId, userId) != 1) {
            throw new BusinessException(ErrorCode.INVALID_STATE);
        }
        BackgroundTaskDto task = requireDto(userId, taskId);
        publishAfterCommit(new BackgroundTaskQueuedEvent(task.id(), userId, task.type()));
        return task;
    }

    @Transactional
    public boolean retryFailedIfCurrent(long userId, long taskId) {
        if (mapper.retryFailed(taskId, userId) != 1) return false;
        BackgroundTaskEntity task = require(userId, taskId);
        publishAfterCommit(new BackgroundTaskQueuedEvent(taskId, userId, task.getTaskType()));
        return true;
    }

    private void handleFailure(BackgroundTaskEntity task, String workerId, Exception exception) {
        String message = safeMessage(exception);
        if (task.getAttemptCount() < properties.retryCount()) {
            if (mapper.scheduleRetry(task.getId(), workerId, message, properties.retryDelay().toSeconds()) == 1) {
                publishAfterCommit(new BackgroundTaskQueuedEvent(
                        task.getId(), task.getUserId(), task.getTaskType()));
            }
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

    private BackgroundTaskEntity newTask(
            long userId, BackgroundTaskType type, Object payload, String deduplicationKey
    ) {
        BackgroundTaskEntity task = new BackgroundTaskEntity();
        task.setUserId(userId);
        task.setTaskType(type);
        task.setPayloadJson(writePayload(payload));
        task.setDeduplicationKey(deduplicationKey);
        return task;
    }

    private String normalizeDeduplicationKey(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("deduplicationKey is required");
        }
        String normalized = key.strip();
        if (normalized.length() > MAX_DEDUPLICATION_KEY_LENGTH) {
            throw new IllegalArgumentException("deduplicationKey is too long");
        }
        return normalized;
    }

    private void publishAfterCommit(Object event) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            events.publishEvent(event);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                events.publishEvent(event);
            }
        });
    }

    private String safeMessage(Exception exception) {
        String outerMessage = nonBlankMessage(exception);
        String detail = outerMessage;
        int detailScore = diagnosticScore(detail);
        Throwable cause = exception;
        while (cause != null) {
            String candidate = nonBlankMessage(cause);
            int candidateScore = diagnosticScore(candidate);
            if (candidate != null && candidateScore >= detailScore) {
                detail = candidate;
                detailScore = candidateScore;
            }
            cause = cause.getCause();
        }
        String message = outerMessage == null ? detail : outerMessage;
        if (message == null) message = exception.getClass().getSimpleName();
        if (detail != null && !detail.equals(message)) message = message + "：" + detail;
        message = BEARER_TOKEN.matcher(message).replaceAll("$1***");
        message = API_KEY.matcher(message).replaceAll("sk-***");
        return message.substring(0, Math.min(MAX_ERROR_LENGTH, message.length()));
    }

    private int diagnosticScore(String message) {
        if (message == null) return -1;
        String normalized = message.toLowerCase(java.util.Locale.ROOT);
        if (normalized.contains("timed out") || normalized.contains("timeout")) return 100;
        if (normalized.contains("unknownhost") || normalized.contains("name resolution")) return 95;
        if (normalized.matches(".*\\b(401|403|408|429|500|502|503|504)\\b.*")) return 90;
        if (normalized.contains("stream was reset") || normalized.contains("cancel")) return 10;
        return 50;
    }

    private String nonBlankMessage(Throwable throwable) {
        if (throwable == null || throwable.getMessage() == null || throwable.getMessage().isBlank()) return null;
        return throwable.getMessage().strip();
    }

    private BackgroundTaskDto toDto(BackgroundTaskEntity task) {
        return new BackgroundTaskDto(task.getId(), task.getTaskType(), task.getStatus(),
                task.getProgress(), task.getAttemptCount(), task.getErrorMessage(),
                task.getAvailableTime(), task.getStartedTime(), task.getFinishedTime(),
                task.getCreateTime(), task.getUpdateTime());
    }
}
