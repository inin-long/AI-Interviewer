package com.inin.aiinterviewer.infrastructure.ai;

import com.inin.aiinterviewer.application.exception.AIException;
import com.inin.aiinterviewer.config.properties.LlmProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OpenAiChatServiceTest {

    @Test
    void startsWithoutAiConfigurationAndFailsOnlyWhenCalled() {
        OpenAiChatService service = assertDoesNotThrow(() -> new OpenAiChatService(
                new LlmProperties("https://example.invalid/v1", "", "", "", Duration.ofSeconds(30))));

        assertThrows(AIException.class, () -> service.chat("hello"));
    }

    @Test
    void buildsAnOpenAiCompatibleClientWhenConfigured() {
        assertDoesNotThrow(() -> new OpenAiChatService(new LlmProperties(
                "https://example.invalid/v1", "test-key", "test-model", "", Duration.ofSeconds(30))));
    }
}
