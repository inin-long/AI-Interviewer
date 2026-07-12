package com.inin.aiinterviewer.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inin.aiinterviewer.agent.model.EvaluationPayload;
import com.inin.aiinterviewer.agent.support.StructuredAiResponseParser;
import com.inin.aiinterviewer.application.dto.InterviewMessageDto;
import com.inin.aiinterviewer.application.dto.InterviewReportDto;
import com.inin.aiinterviewer.application.exception.AIException;
import com.inin.aiinterviewer.application.exception.BusinessException;
import com.inin.aiinterviewer.application.exception.ErrorCode;
import com.inin.aiinterviewer.application.exception.SystemException;
import com.inin.aiinterviewer.domain.enums.InterviewStatus;
import com.inin.aiinterviewer.infrastructure.ai.ChatService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InterviewCompletionService {

    private final InterviewSessionService sessionService;
    private final InterviewResultService resultService;
    private final ChatService chatService;
    private final StructuredAiResponseParser parser;
    private final ObjectMapper objectMapper;

    public InterviewCompletionService(
            InterviewSessionService sessionService,
            InterviewResultService resultService,
            ChatService chatService,
            StructuredAiResponseParser parser,
            ObjectMapper objectMapper
    ) {
        this.sessionService = sessionService;
        this.resultService = resultService;
        this.chatService = chatService;
        this.parser = parser;
        this.objectMapper = objectMapper;
    }

    public InterviewReportDto complete(long userId, long sessionId) {
        var session = sessionService.require(userId, sessionId);
        if (session.status() != InterviewStatus.RUNNING) {
            throw new BusinessException(ErrorCode.INVALID_STATE);
        }
        List<InterviewMessageDto> messages = sessionService.messages(userId, sessionId);
        var previous = sessionService.loadLatestState(userId, sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHECKPOINT_NOT_FOUND));
        String summary = compactSummary(messages);
        EvaluationPayload payload = parser.parse(
                chatService.chat(evaluationPrompt(session.jobTitle(), messages, summary)),
                EvaluationPayload.class);
        validate(payload);
        String markdown = markdown(session.title(), payload, summary);
        return resultService.complete(userId, session, messages, previous, payload, summary, markdown);
    }

    private String evaluationPrompt(String jobTitle, List<InterviewMessageDto> messages, String summary) {
        return """
                你是技术面试评分器。只依据给定问答评分，不推测未出现的能力。
                必须只返回 JSON，不要 Markdown：
                {"overallScore":0到100整数,"technicalScore":0到100整数,
                "problemSolvingScore":0到100整数,"projectScore":0到100整数,
                "systemDesignScore":0到100整数,"communicationScore":0到100整数,
                "comprehensiveScore":0到100整数,"summary":"综合评价"}

                目标岗位：%s
                对话摘要：%s
                完整问答：%s
                """.formatted(jobTitle, summary, json(messages));
    }

    private void validate(EvaluationPayload payload) {
        int[] scores = {payload.overallScore(), payload.technicalScore(), payload.problemSolvingScore(),
                payload.projectScore(), payload.systemDesignScore(), payload.communicationScore(),
                payload.comprehensiveScore()};
        for (int score : scores) {
            if (score < 0 || score > 100) {
                throw new AIException(ErrorCode.AI_RESPONSE_INVALID,
                        new IllegalArgumentException("Evaluation score outside 0..100"));
            }
        }
        if (payload.summary() == null || payload.summary().isBlank()) {
            throw new AIException(ErrorCode.AI_RESPONSE_INVALID,
                    new IllegalArgumentException("Evaluation summary is blank"));
        }
    }

    private String compactSummary(List<InterviewMessageDto> messages) {
        StringBuilder summary = new StringBuilder();
        for (InterviewMessageDto message : messages) {
            String role = message.role().name().equals("USER") ? "候选人" : "面试官";
            String content = message.content().replaceAll("\\s+", " ").strip();
            if (content.length() > 180) content = content.substring(0, 180) + "…";
            if (summary.length() + content.length() > 2400) break;
            summary.append(role).append("：").append(content).append("\n");
        }
        return summary.toString().strip();
    }

    private String markdown(String title, EvaluationPayload value, String contextSummary) {
        return """
                # %s · 面试报告

                **综合得分：%d / 100**

                | 维度 | 得分 |
                | --- | ---: |
                | 技术基础 | %d |
                | 问题解决 | %d |
                | 项目经验 | %d |
                | 系统设计 | %d |
                | 沟通表达 | %d |
                | 综合能力 | %d |

                ## 综合评价

                %s

                ## 问答摘要

                %s
                """.formatted(title, value.overallScore(), value.technicalScore(),
                value.problemSolvingScore(), value.projectScore(), value.systemDesignScore(),
                value.communicationScore(), value.comprehensiveScore(), value.summary(), contextSummary);
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new SystemException(ErrorCode.SYSTEM_ERROR, exception);
        }
    }
}
