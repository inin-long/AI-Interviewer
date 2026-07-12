package com.inin.aiinterviewer.agent.node;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inin.aiinterviewer.agent.prompt.AgentPrompts;
import com.inin.aiinterviewer.agent.state.InterviewGraphState;
import com.inin.aiinterviewer.infrastructure.ai.ChatService;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Map;

@Component
public class QuestionGeneratorNode implements NodeAction<InterviewGraphState> {

    private final ChatService chatService;
    private final ObjectMapper objectMapper;

    public QuestionGeneratorNode(ChatService chatService, ObjectMapper objectMapper) {
        this.chatService = chatService;
        this.objectMapper = objectMapper;
    }

    @Override
    public Map<String, Object> apply(InterviewGraphState state) {
        return Map.of(InterviewGraphState.QUESTION_PROMPT,
                AgentPrompts.question(state, objectMapper));
    }

    public Flux<String> stream(String prompt) {
        return chatService.stream(prompt);
    }
}
