package com.inin.aiinterviewer.agent.node;

import com.inin.aiinterviewer.agent.prompt.AgentPrompts;
import com.inin.aiinterviewer.agent.state.InterviewGraphState;
import com.inin.aiinterviewer.agent.support.StructuredAiResponseParser;
import com.inin.aiinterviewer.domain.model.AnswerAnalysis;
import com.inin.aiinterviewer.application.exception.AIException;
import com.inin.aiinterviewer.application.exception.ErrorCode;
import com.inin.aiinterviewer.infrastructure.ai.ChatService;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class AnswerAnalyzerNode implements NodeAction<InterviewGraphState> {

    private final ChatService chatService;
    private final StructuredAiResponseParser parser;

    public AnswerAnalyzerNode(ChatService chatService, StructuredAiResponseParser parser) {
        this.chatService = chatService;
        this.parser = parser;
    }

    @Override
    public Map<String, Object> apply(InterviewGraphState state) {
        AnswerAnalysis analysis = parser.parse(
                chatService.chat(AgentPrompts.analysis(state)), AnswerAnalysis.class);
        if (analysis.correctness() < 0 || analysis.correctness() > 100
                || analysis.depth() < 0 || analysis.depth() > 100) {
            throw new AIException(ErrorCode.AI_RESPONSE_INVALID,
                    new IllegalArgumentException("AI analysis score is outside 0..100"));
        }
        return Map.of(InterviewGraphState.ANALYSIS, analysis);
    }
}
