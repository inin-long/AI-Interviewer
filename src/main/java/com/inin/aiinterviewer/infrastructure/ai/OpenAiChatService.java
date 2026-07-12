package com.inin.aiinterviewer.infrastructure.ai;

import com.inin.aiinterviewer.application.exception.AIException;
import com.inin.aiinterviewer.application.exception.ErrorCode;
import com.inin.aiinterviewer.config.properties.LlmProperties;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class OpenAiChatService implements ChatService {

    private final LlmProperties properties;
    private final ChatClient chatClient;

    public OpenAiChatService(LlmProperties properties) {
        this.properties = properties;
        this.chatClient = properties.isConfigured() ? createClient(properties) : null;
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
    public Flux<String> stream(String prompt) {
        requireConfigured();
        return chatClient.prompt().user(prompt).stream().content()
                .onErrorMap(exception -> new AIException(ErrorCode.AI_CALL_FAILED, exception));
    }

    private ChatClient createClient(LlmProperties llm) {
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .apiKey(llm.apiKey())
                .baseUrl(llm.baseUrl())
                .model(llm.chatModel())
                .timeout(llm.timeout())
                .temperature(0.1)
                .build();
        OpenAiChatModel model = OpenAiChatModel.builder().options(options).build();
        return ChatClient.create(model);
    }

    private void requireConfigured() {
        if (chatClient == null || !properties.isConfigured()) {
            throw new AIException(ErrorCode.AI_NOT_CONFIGURED, null);
        }
    }
}

