package com.inin.aiinterviewer.agent.node;

import com.inin.aiinterviewer.agent.prompt.AgentPrompts;
import com.inin.aiinterviewer.agent.state.InterviewGraphState;
import com.inin.aiinterviewer.agent.support.StructuredAiResponseParser;
import com.inin.aiinterviewer.domain.model.AnswerAnalysis;
import com.inin.aiinterviewer.infrastructure.ai.ChatService;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.stereotype.Component;

import java.util.List;
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
        return Map.of(InterviewGraphState.ANALYSIS, resolveAnalysis(state));
    }

    private AnswerAnalysis resolveAnalysis(InterviewGraphState state) {
        String response = chatService.chat(AgentPrompts.analysis(state));
        AnswerAnalysis analysis = tryParse(response);
        if (analysis != null) {
            return analysis;
        }
        // 修复一次：让模型再答一次，避免单条畸形 JSON 直接终结整轮面试。
        response = chatService.chat(AgentPrompts.analysis(state));
        analysis = tryParse(response);
        if (analysis != null) {
            return analysis;
        }
        // 安全降级：保证面试继续，仅本轮反馈较弱。
        return new AnswerAnalysis(50, 50, List.of(), "（回答分析暂不可用，已继续面试）");
    }

    private AnswerAnalysis tryParse(String response) {
        try {
            AnswerAnalysis analysis = parser.parse(response, AnswerAnalysis.class);
            if (analysis != null
                    && analysis.correctness() >= 0 && analysis.correctness() <= 100
                    && analysis.depth() >= 0 && analysis.depth() <= 100) {
                return analysis;
            }
        } catch (Exception ignored) {
            // 解析失败则进入重试 / 降级分支
        }
        return null;
    }
}
