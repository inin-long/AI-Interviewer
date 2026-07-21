package com.inin.aiinterviewer.infrastructure.ai;

import com.inin.aiinterviewer.application.exception.AIException;
import com.inin.aiinterviewer.application.exception.ErrorCode;
import com.inin.aiinterviewer.config.properties.LlmProperties;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Objects;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OpenAiChatService implements ChatService {

    private final LlmProperties properties;
    private final ChatClient chatClient;
    private final OpenAiChatOptions defaultOptions;

    public OpenAiChatService(LlmProperties properties) {
        this.properties = properties;
        this.defaultOptions = properties.isConfigured() ? createOptions(properties) : null;
        this.chatClient = defaultOptions == null ? null : createClient(defaultOptions);
    }

    @Override
    public String chat(String prompt) {
        requireConfigured();
        try {
            return chatClient.prompt().user(prompt).call().content();
        } catch (RuntimeException exception) {
            throw new AIException(ErrorCode.AI_CALL_FAILED, exception);
        }
    }

    @Override
    public String chatJson(String prompt) {
        requireConfigured();
        try {
            OpenAiChatModel.ResponseFormat responseFormat = OpenAiChatModel.ResponseFormat.builder()
                    .type(OpenAiChatModel.ResponseFormat.Type.JSON_OBJECT)
                    .build();
            String content = chatClient.prompt()
                    .user(prompt)
                    .options(defaultOptions.mutate()
                            .responseFormat(responseFormat)
                            .maxTokens(properties.effectiveMaxTokens()))
                    .stream()
                    .content()
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining())
                    .block(properties.effectiveTimeout().plusSeconds(5));
            if (content == null || content.isBlank()) {
                throw new IllegalStateException("AI response is empty");
            }
            return content;
        } catch (RuntimeException exception) {
            throw new AIException(ErrorCode.AI_CALL_FAILED, exception);
        }
    }

    @Override
    public Flux<String> stream(String prompt) {
        requireConfigured();
        return chatClient.prompt().user(prompt).stream().content()
                .onErrorMap(exception -> new AIException(ErrorCode.AI_CALL_FAILED, exception));
    }

    private OpenAiChatOptions createOptions(LlmProperties llm) {
        OpenAiChatOptions.Builder builder = OpenAiChatOptions.builder();
        builder
                .apiKey(llm.apiKey())
                .baseUrl(llm.baseUrl())
                .model(llm.chatModel())
                .timeout(llm.effectiveTimeout())
                .maxRetries(llm.effectiveMaxRetries())
                .maxTokens(llm.effectiveMaxTokens())
                .temperature(llm.effectiveTemperature());
        if (llm.effectiveThinkingEnabled() != null) {
            builder.extraBody(Map.of("enable_thinking", llm.effectiveThinkingEnabled()));
        }
        return builder.build();
    }

    private ChatClient createClient(OpenAiChatOptions options) {
        OpenAiChatModel model = OpenAiChatModel.builder().options(options).build();
        return ChatClient.create(model);
    }

    private void requireConfigured() {
        if (chatClient == null || !properties.isConfigured()) {
            throw new AIException(ErrorCode.AI_NOT_CONFIGURED, null);
        }
    }
}
