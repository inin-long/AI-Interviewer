package com.inin.aiinterviewer.infrastructure.ai;

import com.inin.aiinterviewer.application.exception.AIException;
import com.inin.aiinterviewer.application.exception.BusinessException;
import com.inin.aiinterviewer.config.properties.LlmProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OpenAiEmbeddingServiceTest {

    @Test
    void startsWithoutEmbeddingConfigurationAndFailsOnlyWhenCalled() {
        OpenAiEmbeddingService service = assertDoesNotThrow(() -> new OpenAiEmbeddingService(
                new LlmProperties("https://example.invalid/v1", "", "", "", Duration.ofSeconds(30), 0, 2048, null, null)));
        assertThrows(AIException.class, () -> service.embed("hello"));
    }

    @Test
    void buildsCompatibleClientAndRejectsBlankInputLocally() {
        OpenAiEmbeddingService service = assertDoesNotThrow(() -> new OpenAiEmbeddingService(
                new LlmProperties("https://example.invalid/v1", "test-key", "chat", "embedding",
                        Duration.ofSeconds(30), 0, 2048, null, null)));
        assertThrows(BusinessException.class, () -> service.embed("  "));
    }
}
