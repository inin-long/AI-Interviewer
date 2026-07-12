package com.inin.aiinterviewer.infrastructure.vector;

import java.util.Map;

public record VectorSearchResult(String id, String content, double score, Map<String, Object> metadata) {
}

