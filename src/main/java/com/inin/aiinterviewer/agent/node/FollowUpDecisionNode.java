package com.inin.aiinterviewer.agent.node;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inin.aiinterviewer.agent.model.AgentAction;
import com.inin.aiinterviewer.agent.model.AgentDecision;
import com.inin.aiinterviewer.agent.prompt.AgentPrompts;
import com.inin.aiinterviewer.domain.enums.InterviewStage;
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
        return Map.of(InterviewGraphState.DECISION, resolveDecision(state));
    }

    private AgentDecision resolveDecision(InterviewGraphState state) {
        String response = chatService.chat(AgentPrompts.decision(state, objectMapper));
        AgentDecision decision = tryParse(response);
        if (decision != null && isValid(decision, state)) {
            return decision;
        }
        // 修复一次：让模型再决策一次，避免单条畸形 JSON 直接终结整轮面试。
        response = chatService.chat(AgentPrompts.decision(state, objectMapper));
        decision = tryParse(response);
        if (decision != null && isValid(decision, state)) {
            return decision;
        }
        // 安全降级：默认继续追问，保证面试不中断。
        return new AgentDecision(AgentAction.FOLLOW_UP, null, "模型决策解析失败，已默认继续追问");
    }

    private AgentDecision tryParse(String response) {
        try {
            AgentDecision decision = parser.parse(response, AgentDecision.class);
            if (decision != null && decision.action() != null) {
                return decision;
            }
        } catch (Exception ignored) {
            // 解析失败则进入重试 / 降级分支
        }
        return null;
    }

    private boolean isValid(AgentDecision decision, InterviewGraphState state) {
        if (decision.action() == null) {
            return false;
        }
        if (decision.action() != AgentAction.NEXT_STAGE) {
            return true;
        }
        InterviewStage next = decision.nextStage();
        if (next == null) {
            return false;
        }
        // COMPLETED 为终态，允许从任意阶段提前结束面试。
        if (next == InterviewStage.COMPLETED) {
            return true;
        }
        return state.plan().stages().contains(next.name())
                && stageManager.canTransition(state.stage(), next);
    }
}
