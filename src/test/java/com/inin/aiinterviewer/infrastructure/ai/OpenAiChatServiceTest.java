package com.inin.aiinterviewer.infrastructure.ai;

import com.inin.aiinterviewer.application.exception.AIException;
import com.inin.aiinterviewer.config.properties.LlmProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OpenAiChatServiceTest {

    @Test
    void startsWithoutAiConfigurationAndFailsOnlyWhenCalled() {
        OpenAiChatService service = assertDoesNotThrow(() -> new OpenAiChatService(
                new LlmProperties("https://example.invalid/v1", "", "", "", Duration.ofSeconds(30), 0, 2048, null)));

        assertThrows(AIException.class, () -> service.chat("hello"));
    }

    @Test
    void buildsAnOpenAiCompatibleClientWhenConfigured() {
        OpenAiChatService service = assertDoesNotThrow(() -> new OpenAiChatService(new LlmProperties(
                "https://example.invalid/v1", "test-key", "test-model", "", Duration.ofSeconds(30), 0, 1234, null)));

        OpenAiChatOptions options = (OpenAiChatOptions) ReflectionTestUtils.getField(service, "defaultOptions");
        assertThat(options).isNotNull();
        assertThat(options.getTimeout()).isEqualTo(Duration.ofSeconds(30));
        assertThat(options.getMaxRetries()).isZero();
        assertThat(options.getMaxTokens()).isEqualTo(1234);
    }

    @Test
    void disablesThinkingForSiliconFlowDeepSeekV4UnlessExplicitlyOverridden() {
        OpenAiChatService service = new OpenAiChatService(new LlmProperties(
                "https://api.siliconflow.cn/v1", "test-key", "deepseek-ai/DeepSeek-V4-Pro", "",
                Duration.ofMinutes(5), 0, 2048, null));

        OpenAiChatOptions options = (OpenAiChatOptions) ReflectionTestUtils.getField(service, "defaultOptions");
        assertThat(options.getExtraBody()).containsEntry("enable_thinking", false);
    }
}
