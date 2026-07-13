package com.inin.aiinterviewer.infrastructure.vector;

import java.util.Collection;
import java.util.List;

public interface VectorStorePort {
    void upsert(long userId, Collection<VectorDocument> documents);

    List<VectorSearchResult> search(long userId, float[] queryEmbedding, int limit, double minimumScore);

    List<VectorSearchResult> search(
            long userId,
            float[] queryEmbedding,
            int limit,
            double minimumScore,
            Collection<Long> allowedDocumentIds
    );

    void delete(long userId, Collection<String> documentIds);
}
