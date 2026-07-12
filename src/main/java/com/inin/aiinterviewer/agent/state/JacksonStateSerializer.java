package com.inin.aiinterviewer.agent.state;

import com.inin.aiinterviewer.application.exception.ErrorCode;
import com.inin.aiinterviewer.application.exception.SystemException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class JacksonStateSerializer implements StateSerializer {

    private final ObjectMapper objectMapper;

    public JacksonStateSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String serialize(InterviewState state) {
        try {
            return objectMapper.writeValueAsString(state);
        } catch (JsonProcessingException exception) {
            throw new SystemException(ErrorCode.SYSTEM_ERROR, exception);
        }
    }

    @Override
    public InterviewState deserialize(String json) {
        try {
            InterviewState state = objectMapper.readValue(json, InterviewState.class);
            if (!InterviewState.CURRENT_VERSION.equals(state.stateVersion())) {
                throw new IllegalStateException("Unsupported interview state version: " + state.stateVersion());
            }
            return state;
        } catch (JsonProcessingException exception) {
            throw new SystemException(ErrorCode.SYSTEM_ERROR, exception);
        }
    }
}

