package com.inin.aiinterviewer.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inin.aiinterviewer.agent.model.EvaluationPayload;
import com.inin.aiinterviewer.agent.support.StructuredAiResponseParser;
import com.inin.aiinterviewer.application.dto.InterviewMessageDto;
import com.inin.aiinterviewer.application.dto.InterviewCompletionStateDto;
import com.inin.aiinterviewer.application.dto.InterviewReportDto;
import com.inin.aiinterviewer.application.exception.ApplicationException;
import com.inin.aiinterviewer.application.exception.AIException;
import com.inin.aiinterviewer.application.exception.BusinessException;
import com.inin.aiinterviewer.application.exception.ErrorCode;
import com.inin.aiinterviewer.application.exception.SystemException;
import com.inin.aiinterviewer.domain.enums.InterviewStatus;
import com.inin.aiinterviewer.domain.enums.EvidenceSignal;
import com.inin.aiinterviewer.domain.model.EvaluationEvidence;
import com.inin.aiinterviewer.domain.model.EvidenceLedger;
import com.inin.aiinterviewer.domain.model.Message;
import com.inin.aiinterviewer.infrastructure.ai.ChatService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InterviewCompletionService {

    private final InterviewSessionService sessionService;
    private final InterviewResultService resultService;
    private final ChatService chatService;
    private final StructuredAiResponseParser parser;
    private final ObjectMapper objectMapper;
    private final EvidenceLedgerService evidenceLedgerService;
    private final Set<String> generationsInProgress = ConcurrentHashMap.newKeySet();

    public InterviewCompletionService(
            InterviewSessionService sessionService,
            InterviewResultService resultService,
            ChatService chatService,
            StructuredAiResponseParser parser,
            ObjectMapper objectMapper,
            EvidenceLedgerService evidenceLedgerService
    ) {
        this.sessionService = sessionService;
        this.resultService = resultService;
        this.chatService = chatService;
        this.parser = parser;
        this.objectMapper = objectMapper;
        this.evidenceLedgerService = evidenceLedgerService;
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
                finalAnswerSaved(session.planSnapshot().questionCount(), messages),
                reportState.status(), reportState.failureMessage());
    }

    private InterviewReportDto completeInternal(long userId, long sessionId) {
        var session = sessionService.require(userId, sessionId);
        if (session.status() != InterviewStatus.RUNNING && session.status() != InterviewStatus.PAUSED) {
            throw new BusinessException(ErrorCode.INVALID_STATE);
        }
        List<InterviewMessageDto> messages = sessionService.messages(userId, sessionId);
        if (!finalAnswerSaved(session.planSnapshot().questionCount(), messages)) {
            throw new BusinessException(ErrorCode.INVALID_STATE);
        }
        var previous = sessionService.loadLatestState(userId, sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHECKPOINT_NOT_FOUND));
        EvidenceLedger evidenceLedger = evidenceLedgerService.ledger(userId, sessionId);
        String summary = compactSummary(messages);
        resultService.beginGeneration(userId, session);
        try {
            EvaluationPayload payload = parser.parse(
                    chatService.chat(evaluationPrompt(
                            session.jobTitle(), messages, summary, evidenceLedger)),
                    EvaluationPayload.class);
            validate(payload);
            String markdown = markdown(
                    session.title(), payload, summary, messages, evidenceLedger);
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

    private boolean finalAnswerSaved(int questionLimit, List<InterviewMessageDto> messages) {
        long questions = messages.stream()
                .filter(message -> message.role() == Message.Role.ASSISTANT)
                .count();
        return questions >= questionLimit && !messages.isEmpty()
                && messages.getLast().role() == Message.Role.USER;
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

    private String evaluationPrompt(
            String jobTitle,
            List<InterviewMessageDto> messages,
            String summary,
            EvidenceLedger evidenceLedger
    ) {
        return """
                你是技术面试评分器。评分必须以证据账本为主要依据，并用完整问答校验上下文。
                不得把 INSUFFICIENT（证据不足）当作 NEGATIVE（负面能力证据）。
                低置信度能力只能给出保守结论；没有证据的能力不得推测。
                strength 表示信号强度，confidence 表示证据可靠程度，两者不得混用。
                必须只返回 JSON，不要 Markdown：
                {"overallScore":0到100整数,"technicalScore":0到100整数,
                "problemSolvingScore":0到100整数,"projectScore":0到100整数,
                "systemDesignScore":0到100整数,"communicationScore":0到100整数,
                "comprehensiveScore":0到100整数,"summary":"综合评价"}

                目标岗位：%s
                对话摘要：%s
                能力证据汇总：%s
                逐条证据：%s
                完整问答：%s
                """.formatted(jobTitle, summary, json(evidenceLedger.summaries()),
                json(evidenceLedger.evidence()), json(messages.stream()
                        .map(message -> java.util.Map.of(
                                "role", message.role().name(),
                                "content", message.content()))
                        .toList()));
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

    private String markdown(
            String title,
            EvaluationPayload value,
            String contextSummary,
            List<InterviewMessageDto> messages,
            EvidenceLedger evidenceLedger
    ) {
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

                ## 证据与置信度

                %s

                ## 参考依据

                %s
                """.formatted(title, value.overallScore(), value.technicalScore(),
                value.problemSolvingScore(), value.projectScore(), value.systemDesignScore(),
                value.communicationScore(), value.comprehensiveScore(), value.summary(), contextSummary,
                evidenceMarkdown(evidenceLedger), citationMarkdown(messages));
    }

    String evidenceMarkdown(EvidenceLedger ledger) {
        if (ledger.evidence().isEmpty()) {
            return "本次面试没有形成可用的能力证据，评分置信度不足。";
        }
        StringBuilder markdown = new StringBuilder("| 能力 | 证据数 | 正向强度 | 负向强度 | 置信度 |\n"
                + "| --- | ---: | ---: | ---: | ---: |\n");
        ledger.summaries().values().stream()
                .sorted(java.util.Comparator.comparing(summary -> summary.competencyCode()))
                .forEach(summary -> markdown.append("| ")
                        .append(inline(summary.competencyCode(), 64)).append(" | ")
                        .append(summary.evidenceCount()).append(" | ")
                        .append(format(summary.positiveStrength())).append(" | ")
                        .append(format(summary.negativeStrength())).append(" | ")
                        .append(format(summary.confidence())).append(" |\n"));
        markdown.append("\n### 证据明细\n\n");
        for (EvaluationEvidence evidence : ledger.evidence()) {
            markdown.append("- `").append(evidence.id()).append("` · **")
                    .append(inline(evidence.competencyCode(), 64)).append("** · ")
                    .append(signalLabel(evidence.signal()))
                    .append(" · 强度 ").append(format(evidence.strength()))
                    .append(" · 置信度 ").append(format(evidence.confidence()))
                    .append(" · 消息 `").append(evidence.messageId()).append("`\n\n")
                    .append("  ").append(inline(evidence.reason(), 320)).append("\n\n");
            if (!evidence.relatedClaimIds().isEmpty()) {
                markdown.append("  关联主张：")
                        .append(evidence.relatedClaimIds().stream()
                                .map(id -> "`" + id + "`")
                                .collect(java.util.stream.Collectors.joining("、")))
                        .append("\n\n");
            }
        }
        return markdown.toString().stripTrailing();
    }

    private String signalLabel(EvidenceSignal signal) {
        return switch (signal) {
            case POSITIVE -> "正向证据";
            case NEGATIVE -> "负向证据";
            case NEUTRAL -> "中性证据";
            case INSUFFICIENT -> "证据不足";
        };
    }

    private String format(double value) {
        return String.format(java.util.Locale.ROOT, "%.2f", value);
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
        return normalized.replace("\\", "\\\\")
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
