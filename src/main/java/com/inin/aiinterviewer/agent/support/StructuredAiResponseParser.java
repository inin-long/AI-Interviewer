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
        if (start < 0) {
            throw new IllegalArgumentException("AI response does not contain a JSON object");
        }
        // 括号配平：从第一个 '{' 起，按深度匹配到对应的 '}'，
        // 忽略字符串内的括号，避免尾随文本（如"仅供参考 {...}"）污染 JSON。
        int depth = 0;
        boolean inString = false;
        for (int i = start; i < response.length(); i++) {
            char c = response.charAt(i);
            if (inString) {
                if (c == '\\') {
                    i++; // 跳过被转义的字符
                    continue;
                }
                if (c == '"') {
                    inString = false;
                }
            } else {
                if (c == '"') {
                    inString = true;
                } else if (c == '{') {
                    depth++;
                } else if (c == '}') {
                    depth--;
                    if (depth == 0) {
                        return response.substring(start, i + 1);
                    }
                }
            }
        }
        throw new IllegalArgumentException("AI response does not contain a balanced JSON object");
    }
}
