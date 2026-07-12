package com.inin.aiinterviewer.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "task")
public record TaskProperties(int workerCount, int retryCount) {
}

