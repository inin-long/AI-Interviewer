package com.inin.aiinterviewer.ui.state;

import com.inin.aiinterviewer.application.event.BackgroundTaskCompletedEvent;
import com.inin.aiinterviewer.application.event.BackgroundTaskDeletedEvent;
import com.inin.aiinterviewer.application.event.BackgroundTaskFailedEvent;
import com.inin.aiinterviewer.application.event.BackgroundTaskQueuedEvent;
import com.inin.aiinterviewer.application.event.BackgroundTaskStartedEvent;
import com.inin.aiinterviewer.domain.enums.BackgroundTaskType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

@Component
public class TaskNotificationCenter {
    private static final Logger log = LoggerFactory.getLogger(TaskNotificationCenter.class);

    private final CopyOnWriteArrayList<Subscriber> subscribers = new CopyOnWriteArrayList<>();

    public Registration subscribe(long userId, Consumer<TaskNotification> listener) {
        if (userId <= 0) throw new IllegalArgumentException("userId must be positive");
        if (listener == null) throw new IllegalArgumentException("listener is required");
        Subscriber subscriber = new Subscriber(userId, listener);
        subscribers.add(subscriber);
        return () -> subscribers.remove(subscriber);
    }

    @EventListener
    public void taskQueued(BackgroundTaskQueuedEvent event) {
        dispatch(new TaskNotification(
                event.taskId(), event.userId(), event.taskType(), Outcome.QUEUED, ""));
    }

    @EventListener
    public void taskStarted(BackgroundTaskStartedEvent event) {
        dispatch(new TaskNotification(
                event.taskId(), event.userId(), event.taskType(), Outcome.RUNNING, ""));
    }

    @EventListener
    public void taskCompleted(BackgroundTaskCompletedEvent event) {
        dispatch(new TaskNotification(
                event.taskId(), event.userId(), event.taskType(), Outcome.COMPLETED, ""));
    }

    @EventListener
    public void taskFailed(BackgroundTaskFailedEvent event) {
        dispatch(new TaskNotification(
                event.taskId(), event.userId(), event.taskType(), Outcome.FAILED, event.errorMessage()));
    }

    @EventListener
    public void taskDeleted(BackgroundTaskDeletedEvent event) {
        dispatch(new TaskNotification(
                event.taskId(), event.userId(), event.taskType(), Outcome.DELETED, ""));
    }

    private void dispatch(TaskNotification notification) {
        for (Subscriber subscriber : subscribers) {
            if (subscriber.userId() != notification.userId()) continue;
            try {
                subscriber.listener().accept(notification);
            } catch (RuntimeException exception) {
                log.warn("Task notification listener failed for task {}", notification.taskId(), exception);
            }
        }
    }

    public enum Outcome {
        QUEUED,
        RUNNING,
        COMPLETED,
        FAILED,
        DELETED
    }

    public record TaskNotification(
            long taskId,
            long userId,
            BackgroundTaskType taskType,
            Outcome outcome,
            String errorMessage
    ) {
        public TaskNotification {
            errorMessage = errorMessage == null ? "" : errorMessage.strip();
        }
    }

    @FunctionalInterface
    public interface Registration extends AutoCloseable {
        @Override
        void close();
    }

    private record Subscriber(long userId, Consumer<TaskNotification> listener) {
    }
}
