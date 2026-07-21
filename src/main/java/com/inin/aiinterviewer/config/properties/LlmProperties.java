package com.inin.aiinterviewer.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "llm")
public record LlmProperties(
        String baseUrl,
        String apiKey,
        String chatModel,
        String embeddingModel,
        Duration timeout,
        Integer maxRetries,
        Integer maxTokens,
        Boolean thinkingEnabled,
        Double temperature
) {
    private static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(5);
    private static final int DEFAULT_MAX_RETRIES = 0;
    private static final int DEFAULT_MAX_TOKENS = 2_048;
    private static final double DEFAULT_TEMPERATURE = 0.1;

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank()
                && chatModel != null && !chatModel.isBlank();
    }

    public boolean isEmbeddingConfigured() {
        return apiKey != null && !apiKey.isBlank()
                && embeddingModel != null && !embeddingModel.isBlank();
    }

    public Duration effectiveTimeout() {
        return timeout == null || timeout.isZero() || timeout.isNegative()
                ? DEFAULT_TIMEOUT : timeout;
    }

    public int effectiveMaxRetries() {
        return maxRetries == null ? DEFAULT_MAX_RETRIES : Math.max(0, maxRetries);
    }

    public int effectiveMaxTokens() {
        return maxTokens == null || maxTokens <= 0 ? DEFAULT_MAX_TOKENS : maxTokens;
    }

    public double effectiveTemperature() {
        return temperature == null ? DEFAULT_TEMPERATURE : temperature;
    }

    /**
     * SiliconFlow DeepSeek V4 enables expensive reasoning by default. Desktop
     * interview requests favor bounded latency unless the user opts in.
     */
    public Boolean effectiveThinkingEnabled() {
        if (thinkingEnabled != null) return thinkingEnabled;
        String provider = baseUrl == null ? "" : baseUrl.toLowerCase(java.util.Locale.ROOT);
        String model = chatModel == null ? "" : chatModel.toLowerCase(java.util.Locale.ROOT);
        if (provider.contains("siliconflow.cn") && model.contains("deepseek-v4")) return false;
        return null;
    }
}
