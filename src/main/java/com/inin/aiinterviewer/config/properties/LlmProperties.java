package com.inin.aiinterviewer.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "llm")
public record LlmProperties(
        String baseUrl,
        String apiKey,
        String chatModel,
        String embeddingModel,
        Duration timeout
) {
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank()
                && chatModel != null && !chatModel.isBlank();
    }
}

