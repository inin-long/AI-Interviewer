package com.inin.aiinterviewer.ui.state;

import com.inin.aiinterviewer.application.event.BackgroundTaskCompletedEvent;
import com.inin.aiinterviewer.application.event.BackgroundTaskFailedEvent;
import com.inin.aiinterviewer.application.event.BackgroundTaskQueuedEvent;
import com.inin.aiinterviewer.application.event.BackgroundTaskStartedEvent;
import com.inin.aiinterviewer.domain.enums.BackgroundTaskType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

class TaskNotificationCenterTest {

    @Test
    void filtersByUserAndSupportsUnsubscribe() {
        TaskNotificationCenter center = new TaskNotificationCenter();
        ArrayList<TaskNotificationCenter.TaskNotification> received = new ArrayList<>();
        var registration = center.subscribe(7L, received::add);
        center.taskCompleted(new BackgroundTaskCompletedEvent(
                11L, 8L, BackgroundTaskType.DOCUMENT_PARSE));
        center.taskQueued(new BackgroundTaskQueuedEvent(
                10L, 7L, BackgroundTaskType.RESUME_PARSE));
        center.taskStarted(new BackgroundTaskStartedEvent(
                10L, 7L, BackgroundTaskType.RESUME_PARSE));
        center.taskCompleted(new BackgroundTaskCompletedEvent(
                12L, 7L, BackgroundTaskType.REPORT_GENERATE));
        center.taskFailed(new BackgroundTaskFailedEvent(
                13L, 7L, BackgroundTaskType.PROFILE_GENERATE, " provider unavailable "));

        assertThat(received).hasSize(4);
        assertThat(received.getFirst()).satisfies(notification -> {
            assertThat(notification.taskId()).isEqualTo(10L);
            assertThat(notification.outcome()).isEqualTo(TaskNotificationCenter.Outcome.QUEUED);
        });
        assertThat(received.get(1).outcome()).isEqualTo(TaskNotificationCenter.Outcome.RUNNING);
        assertThat(received.get(2)).satisfies(notification -> {
            assertThat(notification.taskId()).isEqualTo(12L);
            assertThat(notification.outcome()).isEqualTo(TaskNotificationCenter.Outcome.COMPLETED);
            assertThat(notification.errorMessage()).isEmpty();
        });
        assertThat(received.getLast()).satisfies(notification -> {
            assertThat(notification.taskId()).isEqualTo(13L);
            assertThat(notification.outcome()).isEqualTo(TaskNotificationCenter.Outcome.FAILED);
            assertThat(notification.errorMessage()).isEqualTo("provider unavailable");
        });

        registration.close();
        center.taskCompleted(new BackgroundTaskCompletedEvent(
                14L, 7L, BackgroundTaskType.RESUME_PARSE));
        assertThat(received).hasSize(4);
    }
}
