package com.inin.aiinterviewer.application.service;

import com.inin.aiinterviewer.domain.enums.KnowledgeStatus;
import com.inin.aiinterviewer.infrastructure.ai.EmbeddingService;
import com.inin.aiinterviewer.agent.tool.ToolInput;
import com.inin.aiinterviewer.agent.tool.ToolRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(KnowledgeDocumentServiceIntegrationTest.FakeEmbeddingConfiguration.class)
class KnowledgeDocumentServiceIntegrationTest {

    @TempDir static Path applicationHome;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("ai.interviewer.home", () -> applicationHome.toString());
        registry.add("rag.chunk-size", () -> 120);
        registry.add("rag.overlap", () -> 20);
        registry.add("task.enabled", () -> false);
    }

    @Autowired private UserService userService;
    @Autowired private KnowledgeDocumentService knowledgeService;
    @Autowired private KnowledgeDocumentTaskService knowledgeTaskService;
    @Autowired private BackgroundTaskService backgroundTaskService;
    @Autowired private ToolRegistry toolRegistry;

    @Test
    void uploadsChunksIndexesSearchesAndIsolatesUsers() throws Exception {
        var owner = userService.register("knowledge-owner", "Owner", "safe-password");
        var other = userService.register("knowledge-other", "Other", "safe-password");
        Path source = applicationHome.resolve("redis-notes.md");
        Files.writeString(source, ("Redis 使用缓存穿透保护、互斥锁和过期策略。\n\n"
                + "数据库索引需要关注选择性与回表成本。\n").repeat(6));

        var document = knowledgeService.uploadAndIndex(owner.id(), source, "项目文档");

        assertThat(document.status()).isEqualTo(KnowledgeStatus.READY);
        assertThat(knowledgeService.detail(owner.id(), document.id()).chunks()).hasSizeGreaterThan(1);
        assertThat(knowledgeService.list(other.id())).isEmpty();
        assertThat(knowledgeService.search(owner.id(), "Redis 缓存", 3))
                .isNotEmpty().allSatisfy(result -> assertThat(result.documentId()).isEqualTo(document.id()));
        assertThat(knowledgeService.search(other.id(), "Redis 缓存", 3)).isEmpty();
        var toolResult = toolRegistry.find("knowledge_search").orElseThrow()
                .execute(new ToolInput(owner.id(), 0L, Map.of("query", "Redis 缓存", "limit", 2)));
        assertThat(toolResult.success()).isTrue();
        assertThat((java.util.List<?>) toolResult.data().get("results")).isNotEmpty();

        knowledgeService.delete(owner.id(), document.id());
        assertThat(knowledgeService.list(owner.id())).isEmpty();
        assertThat(knowledgeService.search(owner.id(), "Redis 缓存", 3)).isEmpty();
    }

    @Test
    void queuesDocumentProcessingAndCompletesItThroughPersistentWorkerContract() throws Exception {
        var owner = userService.register("queued-knowledge-owner", "Queue Owner", "safe-password");
        Path source = applicationHome.resolve("queued-notes.txt");
        Files.writeString(source, "Java 虚拟线程适合高并发阻塞式 I/O 场景。\n".repeat(12));

        var queued = knowledgeTaskService.uploadAndEnqueue(owner.id(), source, "并发资料");
        assertThat(queued.document().status()).isEqualTo(KnowledgeStatus.UPLOADED);
        assertThat(backgroundTaskService.executeNext("knowledge-test-worker")).isTrue();

        assertThat(backgroundTaskService.require(owner.id(), queued.taskId()).getStatus().name())
                .isEqualTo("SUCCESS");
        assertThat(knowledgeService.detail(owner.id(), queued.document().id()).document().status())
                .isEqualTo(KnowledgeStatus.READY);
        assertThat(knowledgeService.search(owner.id(), "虚拟线程", 3)).isNotEmpty();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FakeEmbeddingConfiguration {
        @Bean
        @Primary
        EmbeddingService fakeEmbeddingService() {
            return text -> text.toLowerCase().contains("redis")
                    ? new float[]{1, 0, 0}
                    : new float[]{0, 1, 0};
        }
    }
}
