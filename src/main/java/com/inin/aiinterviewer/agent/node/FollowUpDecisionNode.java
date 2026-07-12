package com.inin.aiinterviewer.agent.node;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inin.aiinterviewer.agent.model.AgentAction;
import com.inin.aiinterviewer.agent.model.AgentDecision;
import com.inin.aiinterviewer.agent.prompt.AgentPrompts;
import com.inin.aiinterviewer.agent.stage.StageManager;
import com.inin.aiinterviewer.agent.state.InterviewGraphState;
import com.inin.aiinterviewer.agent.support.StructuredAiResponseParser;
import com.inin.aiinterviewer.application.exception.BusinessException;
import com.inin.aiinterviewer.application.exception.ErrorCode;
import com.inin.aiinterviewer.infrastructure.ai.ChatService;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class FollowUpDecisionNode implements NodeAction<InterviewGraphState> {

    private final ChatService chatService;
    private final StructuredAiResponseParser parser;
    private final StageManager stageManager;
    private final ObjectMapper objectMapper;

    public FollowUpDecisionNode(
            ChatService chatService,
            StructuredAiResponseParser parser,
            StageManager stageManager,
            ObjectMapper objectMapper
    ) {
        this.chatService = chatService;
        this.parser = parser;
        this.stageManager = stageManager;
        this.objectMapper = objectMapper;
    }

    @Override
    public Map<String, Object> apply(InterviewGraphState state) {
        AgentDecision decision = parser.parse(
                chatService.chat(AgentPrompts.decision(state, objectMapper)), AgentDecision.class);
        if (decision.action() == null) {
            throw new BusinessException(ErrorCode.INVALID_STATE);
        }
        if (decision.action() == AgentAction.NEXT_STAGE
                && (decision.nextStage() == null
                || !state.plan().stages().contains(decision.nextStage().name())
                || !stageManager.canTransition(state.stage(), decision.nextStage()))) {
            throw new BusinessException(ErrorCode.INVALID_STATE);
        }
        return Map.of(InterviewGraphState.DECISION, decision);
    }
}
