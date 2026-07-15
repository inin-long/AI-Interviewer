package com.inin.aiinterviewer.infrastructure.ai;

import com.inin.aiinterviewer.application.exception.AIException;
import com.inin.aiinterviewer.application.exception.BusinessException;
import com.inin.aiinterviewer.application.exception.ErrorCode;
import com.inin.aiinterviewer.config.properties.LlmProperties;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.stereotype.Service;

@Service
public class OpenAiEmbeddingService implements EmbeddingService {

    private final LlmProperties properties;
    private final OpenAiEmbeddingModel embeddingModel;

    public OpenAiEmbeddingService(LlmProperties properties) {
        this.properties = properties;
        this.embeddingModel = properties.isEmbeddingConfigured() ? createModel(properties) : null;
    }

    @Override
    public float[] embed(String text) {
        requireConfigured();
        if (text == null || text.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        try {
            return embeddingModel.embed(text);
        } catch (RuntimeException exception) {
            throw new AIException(ErrorCode.AI_CALL_FAILED, exception);
        }
    }

    private OpenAiEmbeddingModel createModel(LlmProperties llm) {
        OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder()
                .apiKey(llm.apiKey())
                .baseUrl(llm.baseUrl())
                .model(llm.embeddingModel())
                .timeout(llm.effectiveTimeout())
                .maxRetries(llm.effectiveMaxRetries())
                .build();
        return OpenAiEmbeddingModel.builder().options(options).build();
    }

    private void requireConfigured() {
        if (embeddingModel == null || !properties.isEmbeddingConfigured()) {
            throw new AIException(ErrorCode.AI_NOT_CONFIGURED, null);
        }
    }
}
