package com.inin.aiinterviewer.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inin.aiinterviewer.agent.model.EvaluationPayload;
import com.inin.aiinterviewer.agent.support.StructuredAiResponseParser;
import com.inin.aiinterviewer.application.dto.InterviewCompletionStateDto;
import com.inin.aiinterviewer.application.dto.InterviewMessageDto;
import com.inin.aiinterviewer.application.dto.InterviewReportDto;
import com.inin.aiinterviewer.application.exception.AIException;
import com.inin.aiinterviewer.application.exception.ApplicationException;
import com.inin.aiinterviewer.application.exception.BusinessException;
import com.inin.aiinterviewer.application.exception.ErrorCode;
import com.inin.aiinterviewer.application.exception.SystemException;
import com.inin.aiinterviewer.domain.enums.InterviewStatus;
import com.inin.aiinterviewer.domain.model.Message;
import com.inin.aiinterviewer.infrastructure.ai.ChatService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InterviewCompletionService {

    private final InterviewSessionService sessionService;
    private final InterviewResultService resultService;
    private final ChatService chatService;
    private final StructuredAiResponseParser parser;
    private final ObjectMapper objectMapper;
    // 以下三个依赖仅保留以保证 Spring 构造器注入兼容，本类已改用 develop 的简洁报告流程，不再使用
    private final EvidenceLedgerService evidenceLedgerService;
    private final ConsistencyIssueService consistencyIssueService;
    private final EvidenceScoreAggregator scoreAggregator;
    private final Set<String> generationsInProgress = ConcurrentHashMap.newKeySet();

    public InterviewCompletionService(
            InterviewSessionService sessionService,
            InterviewResultService resultService,
            ChatService chatService,
            StructuredAiResponseParser parser,
            ObjectMapper objectMapper,
            EvidenceLedgerService evidenceLedgerService,
            ConsistencyIssueService consistencyIssueService,
            EvidenceScoreAggregator scoreAggregator
    ) {
        this.sessionService = sessionService;
        this.resultService = resultService;
        this.chatService = chatService;
        this.parser = parser;
        this.objectMapper = objectMapper;
        this.evidenceLedgerService = evidenceLedgerService;
        this.consistencyIssueService = consistencyIssueService;
        this.scoreAggregator = scoreAggregator;
    }

    public InterviewReportDto complete(long userId, long sessionId) {
        String generationKey = userId + ":" + sessionId;
        if (!generationsInProgress.add(generationKey)) {
            throw new BusinessException(ErrorCode.INVALID_STATE);
        }
        try {
            return completeInternal(userId, sessionId);
        } finally {
            generationsInProgress.remove(generationKey);
        }
    }

    public InterviewCompletionStateDto state(long userId, long sessionId) {
        var session = sessionService.require(userId, sessionId);
        List<InterviewMessageDto> messages = sessionService.messages(userId, sessionId);
        var reportState = resultService.state(userId, sessionId);
        return new InterviewCompletionStateDto(
                finalAnswerSaved(session.planSnapshot().questionCount(), messages, session.status()),
                reportState.status(), reportState.failureMessage());
    }

    private InterviewReportDto completeInternal(long userId, long sessionId) {
        var session = sessionService.require(userId, sessionId);
        if (session.status() != InterviewStatus.RUNNING
                && session.status() != InterviewStatus.PAUSED
                && session.status() != InterviewStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.INVALID_STATE);
        }
        List<InterviewMessageDto> messages = sessionService.messages(userId, sessionId);
        if (!finalAnswerSaved(session.planSnapshot().questionCount(), messages, session.status())) {
            throw new BusinessException(ErrorCode.INVALID_STATE);
        }
        var previous = sessionService.loadLatestState(userId, sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHECKPOINT_NOT_FOUND));
        String summary = compactSummary(messages);
        resultService.beginGeneration(userId, session);
        try {
            EvaluationPayload payload = parser.parse(
                    chatService.chat(evaluationPrompt(session.jobTitle(), messages, summary)),
                    EvaluationPayload.class);
            validate(payload);
            String markdown = markdown(session.title(), payload, summary, messages);
            return resultService.complete(userId, session, messages, previous, payload, summary, markdown);
        } catch (RuntimeException exception) {
            try {
                resultService.failGeneration(userId, sessionId, failureMessage(exception));
            } catch (RuntimeException persistenceFailure) {
                exception.addSuppressed(persistenceFailure);
            }
            throw exception;
        }
    }

    private boolean finalAnswerSaved(int questionLimit, List<InterviewMessageDto> messages, InterviewStatus status) {
        if (messages.isEmpty()) return false;
        boolean answeredAll = messages.stream()
                .filter(message -> message.role() == Message.Role.ASSISTANT)
                .count() >= questionLimit
                && messages.getLast().role() == Message.Role.USER;
        boolean endedEarly = status == InterviewStatus.COMPLETED
                && messages.stream().anyMatch(message -> message.role() == Message.Role.USER);
        return answeredAll || endedEarly;
    }

    private String failureMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof ApplicationException applicationException) {
                return applicationException.getErrorCode().userMessage();
            }
            current = current.getCause();
        }
        return "报告生成失败，请重试";
    }

    private String evaluationPrompt(String jobTitle, List<InterviewMessageDto> messages, String summary) {
        return """
                你是专业面试评分器。只依据给定的真实问答内容评分，不推测未出现的能力，不编造未提及的经历。
                必须只返回 JSON，不要 Markdown。返回字段如下：
                {"overallScore":0到100整数,"technicalScore":0到100整数,
                "problemSolvingScore":0到100整数,"projectScore":0到100整数,
                "communicationScore":0到100整数,
                "comprehensiveScore":0到100整数,"summary":"综合评价：具体说明候选人表现、优势和改进点"}

                目标岗位：%s
                对话摘要：%s
                完整问答：%s
                """.formatted(jobTitle, summary, json(messages.stream()
                        .map(message -> Map.of(
                                "role", message.role().name(),
                                "content", message.content()))
                        .toList()));
    }

    private void validate(EvaluationPayload payload) {
        int[] scores = {payload.overallScore(), payload.technicalScore(), payload.problemSolvingScore(),
                payload.projectScore(), payload.communicationScore(),
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

    private String markdown(
            String title,
            EvaluationPayload value,
            String contextSummary,
            List<InterviewMessageDto> messages
    ) {
        return """
                # %s · 面试报告

                **综合得分：%s / 100**

                | 维度 | 得分 |
                | --- | ---: |
                | 技术基础 | %s |
                | 问题解决 | %s |
                | 项目经验 | %s |
                | 沟通表达 | %s |
                | 综合能力 | %s |

                ## 综合评价

                %s

                ## 问答摘要

                %s

                ## 参考依据

                %s
                """.formatted(title, scoreMarkdown(value.overallScore()),
                scoreMarkdown(value.technicalScore()), scoreMarkdown(value.problemSolvingScore()),
                scoreMarkdown(value.projectScore()),
                scoreMarkdown(value.communicationScore()), scoreMarkdown(value.comprehensiveScore()),
                value.summary(), contextSummary, citationMarkdown(messages));
    }

    private String scoreMarkdown(int score) {
        return "<span style=\"color:%s;font-weight:700;\">%.1f</span>".formatted(
                scoreColor(score), (double) score);
    }

    private String scoreColor(int score) {
        if (score >= 80) return "#27845B";
        if (score >= 60) return "#2F80C4";
        if (score >= 40) return "#B5780E";
        return "#C53B46";
    }

    String citationMarkdown(List<InterviewMessageDto> messages) {
        StringBuilder markdown = new StringBuilder();
        int questionNumber = 0;
        for (InterviewMessageDto message : messages) {
            if (message.role() != Message.Role.ASSISTANT) continue;
            questionNumber++;
            if (message.citations().isEmpty()) continue;
            markdown.append("### 第 ").append(questionNumber).append(" 题\n\n")
                    .append("**问题：** ").append(inline(message.content(), 160)).append("\n\n");
            message.citations().forEach(citation -> markdown
                    .append("- **").append(inline(citation.documentName(), 100)).append("** · 片段 ")
                    .append(citation.chunkIndex() + 1).append("\n\n")
                    .append("  > ").append(quote(citation.excerpt())).append("\n\n"));
        }
        return markdown.isEmpty()
                ? "本次面试未使用知识库片段作为提问依据。"
                : markdown.toString().stripTrailing();
    }

    private String inline(String value, int maxLength) {
        String normalized = value == null ? "" : value.replaceAll("\\s+", " ").strip();
        if (normalized.length() > maxLength) normalized = normalized.substring(0, maxLength) + "…";
        return normalized.replace("&", "&amp;")
                .replace("<", "&lt;").replace(">", "&gt;")
                .replace("\\", "\\\\")
                .replace("*", "\\*").replace("_", "\\_")
                .replace("[", "\\[").replace("]", "\\]")
                .replace("#", "\\#").replace("|", "\\|");
    }

    private String quote(String value) {
        return inline(value, 320).replace(">", "\\>");
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new SystemException(ErrorCode.SYSTEM_ERROR, exception);
        }
    }
}
