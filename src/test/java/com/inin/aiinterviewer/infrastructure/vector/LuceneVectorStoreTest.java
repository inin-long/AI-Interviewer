package com.inin.aiinterviewer.infrastructure.vector;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.inin.aiinterviewer.config.properties.AppProperties;
import com.inin.aiinterviewer.infrastructure.file.DefaultPathService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class LuceneVectorStoreTest {

    @TempDir Path root;

    @Test
    void upsertsSearchesDeletesAndIsolatesUserIndexes() {
        var pathService = new DefaultPathService(new AppProperties("test", "1", root.toString()));
        var store = new LuceneVectorStore(pathService, JsonMapper.builder().build());
        store.upsert(1L, List.of(
                new VectorDocument("a", "Redis 缓存", new float[]{1, 0, 0}, Map.of("documentId", 1)),
                new VectorDocument("b", "MySQL 索引", new float[]{0, 1, 0}, Map.of("documentId", 2))));
        store.upsert(2L, List.of(
                new VectorDocument("private", "其他用户", new float[]{1, 0, 0}, Map.of("documentId", 9))));

        assertThat(store.search(1L, new float[]{1, 0, 0}, 2, 0.0))
                .extracting(VectorSearchResult::id).containsExactly("a", "b");
        assertThat(store.search(1L, new float[]{1, 0, 0}, 2, 0.0))
                .extracting(VectorSearchResult::id).doesNotContain("private");
        assertThat(store.search(1L, new float[]{1, 0, 0}, 5, 0.0, List.of(2L)))
                .extracting(VectorSearchResult::id).containsExactly("b");
        assertThat(store.search(1L, new float[]{1, 0, 0}, 5, 0.0, List.of()))
                .isEmpty();

        store.upsert(1L, List.of(new VectorDocument(
                "a", "更新后的 Redis 内容", new float[]{1, 0, 0}, Map.of("documentId", 1))));
        assertThat(store.search(1L, new float[]{1, 0, 0}, 1, 0.0).getFirst().content())
                .isEqualTo("更新后的 Redis 内容");

        store.delete(1L, List.of("a"));
        assertThat(store.search(1L, new float[]{1, 0, 0}, 5, 0.0))
                .extracting(VectorSearchResult::id).containsExactly("b");
    }
}
