package com.inin.aiinterviewer.infrastructure.vector;

import java.util.Map;

public record VectorDocument(String id, String content, float[] embedding, Map<String, Object> metadata) {
    public VectorDocument {
        embedding = embedding == null ? new float[0] : embedding.clone();
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}

