package com.inin.aiinterviewer.agent.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inin.aiinterviewer.application.exception.AIException;
import com.inin.aiinterviewer.application.exception.ErrorCode;
import org.springframework.stereotype.Component;

@Component
public class StructuredAiResponseParser {

    private final ObjectMapper objectMapper;

    public StructuredAiResponseParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public <T> T parse(String response, Class<T> type) {
        try {
            return objectMapper.readValue(extractObject(response), type);
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            throw new AIException(ErrorCode.AI_RESPONSE_INVALID, exception);
        }
    }

    private String extractObject(String response) {
        if (response == null || response.isBlank()) {
            throw new IllegalArgumentException("AI response is empty");
        }
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new IllegalArgumentException("AI response does not contain a JSON object");
        }
        return response.substring(start, end + 1);
    }
}
