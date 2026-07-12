package com.inin.aiinterviewer.infrastructure.task;

import com.inin.aiinterviewer.application.service.BackgroundTaskService;
import com.inin.aiinterviewer.config.properties.TaskProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class BackgroundTaskWorker implements SmartLifecycle {
    private static final Logger log = LoggerFactory.getLogger(BackgroundTaskWorker.class);

    private final BackgroundTaskService taskService;
    private final TaskProperties properties;
    private final AtomicBoolean running = new AtomicBoolean();
    private ExecutorService executor;

    public BackgroundTaskWorker(BackgroundTaskService taskService, TaskProperties properties) {
        this.taskService = taskService;
        this.properties = properties;
    }

    @Override
    public synchronized void start() {
        if (!properties.enabled() || !running.compareAndSet(false, true)) return;
        taskService.recoverInterruptedTasks();
        executor = Executors.newFixedThreadPool(properties.workerCount(),
                Thread.ofPlatform().daemon(true).name("background-task-", 0).factory());
        List<String> workerIds = new ArrayList<>();
        for (int index = 0; index < properties.workerCount(); index++) {
            String workerId = UUID.randomUUID() + ":" + index;
            workerIds.add(workerId);
            executor.submit(() -> runLoop(workerId));
        }
        log.info("Started {} background task worker(s)", workerIds.size());
    }

    private void runLoop(String workerId) {
        while (running.get() && !Thread.currentThread().isInterrupted()) {
            try {
                boolean handled = taskService.executeNext(workerId);
                if (!handled) Thread.sleep(properties.pollInterval().toMillis());
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } catch (RuntimeException exception) {
                log.warn("Background task worker {} encountered a transient error", workerId, exception);
                try {
                    Thread.sleep(properties.pollInterval().toMillis());
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    @Override
    public synchronized void stop() {
        if (!running.compareAndSet(true, false)) return;
        if (executor != null) {
            executor.shutdownNow();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    log.warn("Background task workers did not stop within timeout");
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
            executor = null;
        }
    }

    @Override public boolean isRunning() { return running.get(); }
    @Override public boolean isAutoStartup() { return true; }
    @Override public int getPhase() { return Integer.MAX_VALUE - 100; }
}
