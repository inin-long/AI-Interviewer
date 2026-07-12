package com.inin.aiinterviewer.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rag")
public record RagProperties(int chunkSize, int overlap) {
    public RagProperties {
        if (chunkSize < 100) throw new IllegalArgumentException("rag.chunk-size must be at least 100");
        if (overlap < 0 || overlap >= chunkSize) {
            throw new IllegalArgumentException("rag.overlap must be between 0 and chunk-size");
        }
    }
}
