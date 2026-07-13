package com.inin.aiinterviewer.application.service;

import com.inin.aiinterviewer.application.task.BackgroundTaskContext;
import com.inin.aiinterviewer.application.task.BackgroundTaskHandler;
import com.inin.aiinterviewer.application.exception.BusinessException;
import com.inin.aiinterviewer.domain.enums.BackgroundTaskStatus;
import com.inin.aiinterviewer.domain.enums.BackgroundTaskType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(BackgroundTaskServiceIntegrationTest.TaskHandlerConfiguration.class)
class BackgroundTaskServiceIntegrationTest {
    @TempDir static Path applicationHome;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("ai.interviewer.home", () -> applicationHome.toString());
        registry.add("task.enabled", () -> false);
        registry.add("task.retry-count", () -> 3);
        registry.add("task.retry-delay", () -> "0s");
    }

    @Autowired private UserService userService;
    @Autowired private BackgroundTaskService taskService;
    @Autowired private ControllableTaskHandler taskHandler;

    @Test
    void deduplicatesActiveTasksAndAllowsANewTaskAfterCompletion() throws Exception {
        var owner = userService.register("dedup-task-owner", "Dedup Owner", "safe-password");

        ArrayList<Callable<Long>> enqueues = new ArrayList<>();
        for (int index = 0; index < 10; index++) {
            enqueues.add(() -> taskService.enqueueUnique(
                    owner.id(), BackgroundTaskType.VECTOR_UPDATE, "document:42", Map.of("documentId", 42)));
        }
        java.util.List<Long> taskIds;
        try (var executor = Executors.newFixedThreadPool(5)) {
            taskIds = executor.invokeAll(enqueues).stream().map(future -> {
                try { return future.get(); }
                catch (Exception exception) { throw new AssertionError(exception); }
            }).toList();
        }

        assertThat(new HashSet<>(taskIds)).hasSize(1);
        assertThat(taskService.list(owner.id())).hasSize(1);
        long firstTaskId = taskIds.getFirst();
        assertThat(taskService.executeNext("dedup-worker")).isTrue();
        assertThat(taskService.require(owner.id(), firstTaskId).getStatus())
                .isEqualTo(BackgroundTaskStatus.SUCCESS);

        long nextTaskId = taskService.enqueueUnique(
                owner.id(), BackgroundTaskType.VECTOR_UPDATE, "document:42", Map.of("documentId", 42));
        assertThat(nextTaskId).isNotEqualTo(firstTaskId);
        assertThat(taskService.executeNext("dedup-second-worker")).isTrue();
        assertThatThrownBy(() -> taskService.enqueueUnique(
                owner.id(), BackgroundTaskType.VECTOR_UPDATE, " ", Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void atomicallyClaimsRecoversRetriesAndRecordsTerminalFailure() throws Exception {
        var owner = userService.register("task-owner", "Task Owner", "safe-password");
        var other = userService.register("task-other", "Other", "safe-password");

        ArrayList<Long> queuedIds = new ArrayList<>();
        for (int index = 0; index < 12; index++) {
            queuedIds.add(taskService.enqueue(owner.id(), BackgroundTaskType.VECTOR_UPDATE,
                    Map.of("sequence", index)));
        }

        try (var executor = Executors.newFixedThreadPool(6)) {
            ArrayList<Callable<Long>> claims = new ArrayList<>();
            for (int index = 0; index < 12; index++) {
                String workerId = "claim-worker-" + index;
                claims.add(() -> taskService.claimNext(workerId).orElseThrow().getId());
            }
            var claimed = executor.invokeAll(claims).stream()
                    .map(future -> {
                        try { return future.get(); }
                        catch (Exception exception) { throw new AssertionError(exception); }
                    }).toList();
            assertThat(new HashSet<>(claimed)).hasSize(12);
            assertThat(claimed).containsExactlyInAnyOrderElementsOf(queuedIds);
        }

        assertThat(taskService.recoverInterruptedTasks()).isEqualTo(12);
        assertThat(taskService.list(owner.id()))
                .allSatisfy(task -> assertThat(task.getStatus()).isEqualTo(BackgroundTaskStatus.PENDING));
        for (int index = 0; index < 12; index++) {
            assertThat(taskService.executeNext("recovery-worker")).isTrue();
        }
        assertThat(taskService.list(owner.id()))
                .allSatisfy(task -> assertThat(task.getStatus()).isEqualTo(BackgroundTaskStatus.SUCCESS));
        assertThat(taskService.list(other.id())).isEmpty();

        taskHandler.failNext(2);
        long retryTaskId = taskService.enqueue(owner.id(), BackgroundTaskType.VECTOR_UPDATE, Map.of());
        assertThat(taskService.executeNext("retry-worker")).isTrue();
        assertThat(taskService.require(owner.id(), retryTaskId).getStatus()).isEqualTo(BackgroundTaskStatus.PENDING);
        assertThat(taskService.executeNext("retry-worker")).isTrue();
        assertThat(taskService.require(owner.id(), retryTaskId).getStatus()).isEqualTo(BackgroundTaskStatus.PENDING);
        assertThat(taskService.executeNext("retry-worker")).isTrue();
        var succeeded = taskService.require(owner.id(), retryTaskId);
        assertThat(succeeded.getStatus()).isEqualTo(BackgroundTaskStatus.SUCCESS);
        assertThat(succeeded.getAttemptCount()).isEqualTo(3);

        taskHandler.failNext(4);
        long failedTaskId = taskService.enqueue(owner.id(), BackgroundTaskType.VECTOR_UPDATE, Map.of());
        for (int index = 0; index < 3; index++) {
            assertThat(taskService.executeNext("failure-worker")).isTrue();
        }
        var failed = taskService.require(owner.id(), failedTaskId);
        assertThat(failed.getStatus()).isEqualTo(BackgroundTaskStatus.FAILED);
        assertThat(failed.getAttemptCount()).isEqualTo(3);
        assertThat(failed.getErrorMessage()).contains("planned failure");

        assertThatThrownBy(() -> taskService.retryFailed(other.id(), failedTaskId))
                .isInstanceOf(BusinessException.class);
        var retried = taskService.retryFailed(owner.id(), failedTaskId);
        assertThat(retried.status()).isEqualTo(BackgroundTaskStatus.PENDING);
        assertThat(retried.attemptCount()).isZero();
        assertThat(retried.errorMessage()).isNull();
        assertThatThrownBy(() -> taskService.retryFailed(owner.id(), failedTaskId))
                .isInstanceOf(BusinessException.class);

        taskHandler.failNext(0);
        assertThat(taskService.executeNext("manual-retry-worker")).isTrue();
        assertThat(taskService.requireDto(owner.id(), failedTaskId).status())
                .isEqualTo(BackgroundTaskStatus.SUCCESS);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TaskHandlerConfiguration {
        @Bean
        ControllableTaskHandler controllableTaskHandler() {
            return new ControllableTaskHandler();
        }
    }

    static class ControllableTaskHandler implements BackgroundTaskHandler {
        private final AtomicInteger failuresRemaining = new AtomicInteger();

        void failNext(int count) { failuresRemaining.set(count); }

        @Override public BackgroundTaskType taskType() { return BackgroundTaskType.VECTOR_UPDATE; }

        @Override
        public void handle(BackgroundTaskContext context) {
            if (failuresRemaining.getAndUpdate(value -> Math.max(0, value - 1)) > 0) {
                throw new IllegalStateException("planned failure on attempt " + context.attempt());
            }
        }
    }
}
