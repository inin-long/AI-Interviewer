package com.inin.aiinterviewer.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "task")
public record TaskProperties(
        boolean enabled,
        int workerCount,
        int retryCount,
        Duration pollInterval,
        Duration retryDelay
) {
    public TaskProperties {
        if (workerCount < 1) throw new IllegalArgumentException("task.worker-count must be at least 1");
        if (retryCount < 1) throw new IllegalArgumentException("task.retry-count must be at least 1");
        if (pollInterval == null || pollInterval.isNegative() || pollInterval.isZero()) {
            throw new IllegalArgumentException("task.poll-interval must be positive");
        }
        if (retryDelay == null || retryDelay.isNegative()) {
            throw new IllegalArgumentException("task.retry-delay cannot be negative");
        }
    }
}
