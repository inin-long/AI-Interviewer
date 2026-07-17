package com.inin.aiinterviewer.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inin.aiinterviewer.agent.model.EvaluationPayload;
import com.inin.aiinterviewer.agent.support.StructuredAiResponseParser;
import com.inin.aiinterviewer.application.dto.InterviewMessageDto;
import com.inin.aiinterviewer.application.dto.InterviewCompletionStateDto;
import com.inin.aiinterviewer.application.dto.InterviewReportDto;
import com.inin.aiinterviewer.application.dto.InterviewSessionDto;
import com.inin.aiinterviewer.application.exception.ApplicationException;
import com.inin.aiinterviewer.application.exception.AIException;
import com.inin.aiinterviewer.application.exception.BusinessException;
import com.inin.aiinterviewer.application.exception.ErrorCode;
import com.inin.aiinterviewer.application.exception.SystemException;
import com.inin.aiinterviewer.domain.enums.InterviewStatus;
import com.inin.aiinterviewer.domain.enums.EvidenceSignal;
import com.inin.aiinterviewer.domain.model.EvaluationEvidence;
import com.inin.aiinterviewer.domain.model.EvidenceLedger;
import com.inin.aiinterviewer.domain.model.ClaimLedger;
import com.inin.aiinterviewer.domain.model.InterviewPlanSettings;
import com.inin.aiinterviewer.domain.model.Message;
import com.inin.aiinterviewer.infrastructure.ai.ChatService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.LinkedHashMap;
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
        ClaimLedger claimLedger = consistencyIssueService.ledger(userId, sessionId);
        Map<Long, Integer> questionNumbers = sessionService.messageQuestionNumbers(userId, sessionId);
        String summary = compactSummary(messages);
        resultService.beginGeneration(userId, session);
        try {
            EvaluationNarrativePayload narrative = parser.parse(
                    chatService.chat(evaluationPrompt(
                            session.jobTitle(), evidenceLedger, claimLedger)),
                    EvaluationNarrativePayload.class);
            validate(narrative);
            EvaluationPayload payload = scoreAggregator.aggregate(
                    evidenceLedger, claimLedger, narrative.summary());
            String markdown = markdown(
                    session, previous, payload, summary, messages, evidenceLedger,
                    claimLedger, questionNumbers);
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
            EvidenceLedger evidenceLedger,
            ClaimLedger claimLedger
    ) {
        return """
                你是技术面试证据摘要助手，不负责评分。分数将由程序根据逐轮证据确定。
                只能概括下面的证据账本和一致性结果，不得补充对话中未形成证据的能力结论，
                不得把 INSUFFICIENT（证据不足）描述成 NEGATIVE（能力较弱）。
                必须只返回 JSON，不要 Markdown，不要返回任何分数字段：
                {"summary":"基于证据的综合评价；明确区分已观察结论与证据不足"}

                目标岗位：%s
                能力证据汇总：%s
                逐条证据：%s
                一致性结果：%s
                """.formatted(jobTitle, json(evidenceLedger.summaries()),
                json(evidenceLedger.evidence()), json(claimLedger.issues()));
    }

    private void validate(EvaluationNarrativePayload payload) {
        if (payload == null || payload.summary() == null || payload.summary().isBlank()) {
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
            InterviewSessionDto session,
            com.inin.aiinterviewer.agent.state.InterviewState state,
            EvaluationPayload value,
            String contextSummary,
            List<InterviewMessageDto> messages,
            EvidenceLedger evidenceLedger,
            ClaimLedger claimLedger,
            Map<Long, Integer> questionNumbers
    ) {
        InterviewPlanSettings settings = InterviewPlanSettings.fromRules(session.planSnapshot().rules());
        String trace = !value.overallScored()
                ? "当前没有形成可用评分证据，所有能力结论均应视为证据不足。"
                : "综合判断关联证据：" + evidenceLedger.evidence().stream().limit(8)
                        .map(evidence -> "`" + evidence.id() + "`（Q"
                                + questionNumbers.getOrDefault(evidence.messageId(), 0) + "）")
                        .collect(java.util.stream.Collectors.joining("、"));
        return """
                # 技术面试报告

                ## 1. 面试基本信息

                - 面试：%s
                - 目标岗位：%s
                - 模式：%s
                - Persona：%s（仅影响提问语气，不影响评分）
                - 难度：%s
                - 已完成问题：%d

                ## 2. 综合结论

                **综合得分：%s**

                **综合评价：**

                %s

                %s

                ## 3. 能力评分与置信度

                %s

                ### 证据与置信度摘要

                %s

                ## 4. 关键能力证据

                %s

                ## 5. 核心主张可信度

                %s

                ## 6. 逻辑链完整度

                %s

                ## 7. 压力场景表现

                %s

                ## 8. 决策与取舍风格

                %s

                ## 9. 协作与观点修正能力

                %s

                ## 10. 前后不一致及澄清结果

                %s

                ## 11. 优势

                %s

                ## 12. 风险点

                %s

                ## 13. 改进建议

                %s

                ## 14. 学习计划

                %s

                ## 15. 关键问答证据

                %s

                ### 知识库参考依据

                %s

                ### 问答摘要

                %s
                """.formatted(
                inline(session.title(), 128), inline(session.jobTitle(), 128), settings.mode(),
                settings.persona(), session.planSnapshot().difficulty(),
                messages.stream().filter(message -> message.role() == Message.Role.ASSISTANT).count(),
                overallScoreText(value), inline(value.summary(), 1_200), trace,
                dimensionScoreMarkdown(value, evidenceLedger, questionNumbers),
                confidenceMarkdown(evidenceLedger), evidenceMarkdown(evidenceLedger, questionNumbers),
                claimMarkdown(claimLedger, questionNumbers), logicMarkdown(state.logicChainResult()),
                pressureScenarioMarkdown(state), decisionMarkdown(claimLedger, state.logicChainResult()),
                collaborationMarkdown(evidenceLedger, questionNumbers), consistencyMarkdown(claimLedger),
                strengthsMarkdown(evidenceLedger, questionNumbers), risksMarkdown(evidenceLedger, questionNumbers),
                improvementMarkdown(evidenceLedger, state.logicChainResult()),
                learningMarkdown(evidenceLedger, state.logicChainResult(), session.knowledgeSnapshot()),
                keyQaMarkdown(messages, evidenceLedger, questionNumbers), citationMarkdown(messages),
                inline(contextSummary, 2_400));
    }

    private String overallScoreText(EvaluationPayload value) {
        return value.overallScored()
                ? value.overallScore() + " / 100（置信度 " + confidenceLevel(value.overallConfidence())
                        + " " + format(value.overallConfidence()) + "）"
                : "证据不足（不作能力结论）";
    }

    private String dimensionScoreMarkdown(
            EvaluationPayload value,
            EvidenceLedger ledger,
            Map<Long, Integer> questionNumbers
    ) {
        LinkedHashMap<String, String> labels = new LinkedHashMap<>();
        labels.put(EvidenceScoreAggregator.TECHNICAL, "技术基础");
        labels.put(EvidenceScoreAggregator.PROBLEM_SOLVING, "问题解决");
        labels.put(EvidenceScoreAggregator.PROJECT, "项目经验");
        labels.put(EvidenceScoreAggregator.SYSTEM_DESIGN, "系统设计");
        labels.put(EvidenceScoreAggregator.COMMUNICATION, "沟通表达");
        labels.put(EvidenceScoreAggregator.COMPREHENSIVE, "综合能力");
        Map<String, Integer> scores = Map.of(
                EvidenceScoreAggregator.TECHNICAL, value.technicalScore(),
                EvidenceScoreAggregator.PROBLEM_SOLVING, value.problemSolvingScore(),
                EvidenceScoreAggregator.PROJECT, value.projectScore(),
                EvidenceScoreAggregator.SYSTEM_DESIGN, value.systemDesignScore(),
                EvidenceScoreAggregator.COMMUNICATION, value.communicationScore(),
                EvidenceScoreAggregator.COMPREHENSIVE, value.comprehensiveScore());
        StringBuilder markdown = new StringBuilder(
                "| 维度 | 得分 | 置信度 | 判断来源 |\n| --- | ---: | --- | --- |\n");
        labels.forEach((key, label) -> {
            EvaluationPayload.EvidenceTrace trace = value.scoreEvidence().get(key);
            boolean legacy = trace == null;
            boolean scored = legacy || trace.scored();
            markdown.append("| ").append(label).append(" | ")
                    .append(scored ? scores.get(key) : "证据不足")
                    .append(" | ")
                    .append(legacy ? "历史报告未记录" : confidenceLevel(trace.confidence())
                            + " " + format(trace.confidence()))
                    .append(" | ")
                    .append(legacy ? "历史报告未记录"
                            : scoreSources(trace, ledger, questionNumbers))
                    .append(" |\n");
        });
        return markdown.toString().stripTrailing();
    }

    private String scoreSources(
            EvaluationPayload.EvidenceTrace trace,
            EvidenceLedger ledger,
            Map<Long, Integer> questionNumbers
    ) {
        if (trace.evidenceIds().isEmpty()) return "无——不作能力结论";
        Map<String, EvaluationEvidence> byId = ledger.evidence().stream()
                .collect(java.util.stream.Collectors.toMap(
                        EvaluationEvidence::id, value -> value, (left, right) -> left));
        return trace.evidenceIds().stream().limit(5).map(id -> {
            EvaluationEvidence evidence = byId.get(id);
            if (evidence == null) return "`" + id + "`";
            String claims = evidence.relatedClaimIds().isEmpty() ? ""
                    : " / 主张 " + evidence.relatedClaimIds().stream()
                            .map(claimId -> "`" + claimId + "`")
                            .collect(java.util.stream.Collectors.joining("、"));
            return "`" + id + "` / Q" + questionNumbers.getOrDefault(evidence.messageId(), 0)
                    + " / 消息 `" + evidence.messageId() + "`" + claims;
        }).collect(java.util.stream.Collectors.joining("<br>"));
    }

    private String confidenceLevel(double value) {
        if (value >= 0.7) return "高";
        if (value >= 0.45) return "中";
        return "低";
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record EvaluationNarrativePayload(String summary) {
    }

    String evidenceMarkdown(EvidenceLedger ledger) {
        return evidenceMarkdown(ledger, Map.of());
    }

    private String evidenceMarkdown(EvidenceLedger ledger, Map<Long, Integer> questionNumbers) {
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
                    .append(" · Q").append(questionNumbers.getOrDefault(evidence.messageId(), 0))
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

    private String confidenceMarkdown(EvidenceLedger ledger) {
        if (ledger.summaries().isEmpty()) return "暂无能力证据，评分置信度不足。";
        StringBuilder markdown = new StringBuilder("| 能力 | 证据数 | 评分置信度 |\n| --- | ---: | ---: |\n");
        ledger.summaries().values().stream()
                .sorted(java.util.Comparator.comparing(summary -> summary.competencyCode()))
                .forEach(summary -> markdown.append("| ").append(inline(summary.competencyCode(), 64))
                        .append(" | ").append(summary.evidenceCount()).append(" | ")
                        .append(format(summary.confidence())).append(" |\n"));
        return markdown.toString().stripTrailing();
    }

    private String claimMarkdown(ClaimLedger ledger, Map<Long, Integer> questionNumbers) {
        if (ledger.claims().isEmpty()) return "本次回答未提取到可独立验证的核心主张。";
        StringBuilder markdown = new StringBuilder();
        ledger.claims().stream()
                .sorted(java.util.Comparator.comparingDouble(
                        com.inin.aiinterviewer.domain.model.InterviewClaim::importance).reversed())
                .limit(20)
                .forEach(claim -> markdown.append("- `").append(claim.id()).append("` · Q")
                        .append(questionNumbers.getOrDefault(claim.sourceMessageId(), 0)).append(" · **")
                        .append(claim.type()).append(" / ").append(claim.status()).append("** · 可信度 ")
                        .append(format(claim.credibility())).append(" · ")
                        .append(inline(claim.content(), 260))
                        .append(claim.missingEvidence().isEmpty() ? ""
                                : "；仍缺：" + inline(String.join("、", claim.missingEvidence()), 240))
                        .append("\n"));
        return markdown.toString().stripTrailing();
    }

    private String logicMarkdown(com.inin.aiinterviewer.agent.model.LogicChainResult logic) {
        if (logic == null || logic.skipped()) return "本次没有形成可用的逻辑链分析。";
        if (logic.degraded()) return "逻辑链分析节点已安全降级，本节不作推测性判断。";
        StringBuilder markdown = new StringBuilder();
        appendField(markdown, "问题判断", logic.problemDiagnosis());
        appendField(markdown, "备选方案", String.join("、", logic.alternatives()));
        appendField(markdown, "决策", logic.decision());
        appendField(markdown, "依据与机制", logic.reasoning());
        appendField(markdown, "执行动作", String.join("、", logic.actions()));
        appendField(markdown, "结果", logic.outcome());
        appendField(markdown, "验证", logic.validation());
        appendField(markdown, "反思", logic.reflection());
        if (!logic.gaps().isEmpty()) {
            markdown.append("\n**仍存在的逻辑缺口：**\n\n");
            logic.gaps().forEach(gap -> markdown.append("- **").append(gap.type())
                    .append("** · 严重度 ").append(format(gap.severity())).append(" · ")
                    .append(inline(gap.description(), 260))
                    .append(gap.relatedClaimIds().isEmpty() ? ""
                            : " · 关联主张 " + gap.relatedClaimIds().stream()
                                    .map(id -> "`" + id + "`")
                                    .collect(java.util.stream.Collectors.joining("、")))
                    .append("\n"));
        }
        return markdown.isEmpty() ? "当前逻辑链字段不足，暂不下结论。" : markdown.toString().stripTrailing();
    }

    private String pressureScenarioMarkdown(com.inin.aiinterviewer.agent.state.InterviewState state) {
        var pressure = state.pressureState();
        StringBuilder markdown = new StringBuilder("- 最终压力等级：**")
                .append(pressure.level()).append("**\n- 连续施压轮数：")
                .append(pressure.consecutivePressureTurns()).append("\n- 安全调整：")
                .append(pressure.safetyAdjusted() ? "是" : "否").append("\n- 控制依据：")
                .append(inline(pressure.reason(), 300));
        var scenario = state.activeScenario();
        if (scenario == null) {
            markdown.append("\n\n本次未进入持续情境场景，不据此评价压力场景能力。");
            return markdown.toString();
        }
        markdown.append("\n\n**场景：** ").append(inline(scenario.objective(), 240))
                .append("（").append(scenario.status()).append("，")
                .append(scenario.currentRound()).append(" / ").append(scenario.maxRounds()).append(" 轮）\n\n");
        for (var event : scenario.events()) {
            markdown.append("- 第 ").append(event.round()).append(" 轮 · ")
                    .append(event.type()).append(" · ").append(inline(event.description(), 240))
                    .append(" · 变量变化：`").append(inline(String.valueOf(event.changes()), 200))
                    .append("`\n");
        }
        return markdown.toString().stripTrailing();
    }

    private String decisionMarkdown(
            ClaimLedger ledger,
            com.inin.aiinterviewer.agent.model.LogicChainResult logic
    ) {
        List<com.inin.aiinterviewer.domain.model.InterviewClaim> decisions = ledger.claims().stream()
                .filter(claim -> claim.type() == com.inin.aiinterviewer.domain.enums.ClaimType.DECISION)
                .toList();
        if (decisions.isEmpty() && (logic == null || logic.decision().isBlank())) {
            return "缺少可追溯的决策或取舍证据，本节不作风格推断。";
        }
        StringBuilder markdown = new StringBuilder();
        decisions.stream().limit(8).forEach(claim -> markdown.append("- `").append(claim.id())
                .append("` · ").append(inline(claim.content(), 280)).append("\n"));
        if (logic != null && !logic.decision().isBlank()) {
            markdown.append("\n- 最近一轮决策链：").append(inline(logic.decision(), 260));
            if (!logic.reasoning().isBlank()) {
                markdown.append("；依据：").append(inline(logic.reasoning(), 260));
            }
        }
        return markdown.toString().stripTrailing();
    }

    private String collaborationMarkdown(EvidenceLedger ledger, Map<Long, Integer> questionNumbers) {
        List<EvaluationEvidence> evidence = ledger.evidence().stream()
                .filter(item -> item.competencyCode().equals(
                        CollaborationEvidenceCollector.COMPETENCY_CODE)).toList();
        if (evidence.isEmpty()) return "未观察到足以判断协作或观点修正方式的明确行为证据。";
        return evidence.stream().map(item -> "- `" + item.id() + "` · Q"
                        + questionNumbers.getOrDefault(item.messageId(), 0) + " · "
                        + signalLabel(item.signal()) + " · " + inline(item.reason(), 300))
                .collect(java.util.stream.Collectors.joining("\n"));
    }

    private String consistencyMarkdown(ClaimLedger ledger) {
        if (ledger.issues().isEmpty()) return "本次未形成需要报告的跨轮一致性问题。";
        return ledger.issues().stream().map(issue -> "- `" + issue.id() + "` · **"
                        + issue.type() + " / " + issue.status() + "** · "
                        + inline(issue.description(), 260)
                        + (issue.resolution().isBlank() ? "；尚待澄清"
                        : "；澄清结果：" + inline(issue.resolution(), 260))
                        + " · 关联主张 " + issue.relatedClaimIds().stream()
                                .map(id -> "`" + id + "`")
                                .collect(java.util.stream.Collectors.joining("、")))
                .collect(java.util.stream.Collectors.joining("\n"));
    }

    private String strengthsMarkdown(EvidenceLedger ledger, Map<Long, Integer> questionNumbers) {
        List<EvaluationEvidence> strengths = ledger.evidence().stream()
                .filter(item -> item.signal() == EvidenceSignal.POSITIVE)
                .filter(item -> item.confidence() >= 0.55)
                .filter(item -> !item.relatedClaimIds().isEmpty())
                .sorted(java.util.Comparator.comparingDouble(EvaluationEvidence::strength).reversed())
                .limit(8).toList();
        if (strengths.isEmpty()) return "当前没有达到报告阈值的正向证据，不推测优势。";
        return strengths.stream().map(item -> "- " + inline(item.reason(), 280)
                        + "（" + judgmentSource(item, questionNumbers) + "）")
                .collect(java.util.stream.Collectors.joining("\n"));
    }

    private String risksMarkdown(EvidenceLedger ledger, Map<Long, Integer> questionNumbers) {
        List<EvaluationEvidence> risks = ledger.evidence().stream()
                .filter(item -> item.signal() == EvidenceSignal.NEGATIVE
                        || item.signal() == EvidenceSignal.INSUFFICIENT)
                .filter(item -> !item.relatedClaimIds().isEmpty())
                .limit(10).toList();
        if (risks.isEmpty()) return "当前没有达到报告阈值的负向证据；未覆盖能力仍需在后续面试中验证。";
        return risks.stream().map(item -> "- " + (item.signal() == EvidenceSignal.INSUFFICIENT
                        ? "**证据不足（不等同能力不足）：** " : "**待改进证据：** ")
                        + inline(item.reason(), 280) + "（"
                        + judgmentSource(item, questionNumbers) + "）")
                .collect(java.util.stream.Collectors.joining("\n"));
    }

    private String judgmentSource(
            EvaluationEvidence evidence,
            Map<Long, Integer> questionNumbers
    ) {
        String claims = evidence.relatedClaimIds().stream()
                .map(id -> "`" + id + "`")
                .collect(java.util.stream.Collectors.joining("、"));
        return "证据 `" + evidence.id() + "`，Q"
                + questionNumbers.getOrDefault(evidence.messageId(), 0)
                + "，消息 `" + evidence.messageId() + "`，主张 " + claims;
    }

    private String improvementMarkdown(
            EvidenceLedger ledger,
            com.inin.aiinterviewer.agent.model.LogicChainResult logic
    ) {
        List<String> items = new java.util.ArrayList<>();
        ledger.evidence().stream()
                .filter(item -> item.signal() == EvidenceSignal.NEGATIVE
                        || item.signal() == EvidenceSignal.INSUFFICIENT)
                .limit(5).forEach(item -> items.add("围绕 " + item.competencyCode()
                        + " 补充可验证案例（来源证据 `" + item.id() + "`）"));
        if (logic != null) logic.gaps().stream().limit(5).forEach(gap -> items.add(
                "练习补全 " + gap.type() + "：" + inline(gap.description(), 180)));
        return items.isEmpty() ? "继续积累带基线、个人行动和验证结果的岗位案例。"
                : items.stream().map(item -> "- " + item).collect(java.util.stream.Collectors.joining("\n"));
    }

    private String learningMarkdown(
            EvidenceLedger ledger,
            com.inin.aiinterviewer.agent.model.LogicChainResult logic,
            List<com.inin.aiinterviewer.application.dto.KnowledgeDocumentSnapshotDto> knowledge
    ) {
        java.util.LinkedHashSet<String> topics = new java.util.LinkedHashSet<>();
        ledger.evidence().stream()
                .filter(item -> item.signal() == EvidenceSignal.NEGATIVE
                        || item.signal() == EvidenceSignal.INSUFFICIENT)
                .map(EvaluationEvidence::competencyCode).forEach(topics::add);
        if (logic != null) logic.gaps().stream().map(gap -> gap.type().name()).forEach(topics::add);
        if (topics.isEmpty()) topics.add("岗位核心能力的可验证案例复盘");
        String topicList = topics.stream().limit(8).map(topic -> "- 学习主题：**" + topic + "**")
                .collect(java.util.stream.Collectors.joining("\n"));
        String documents = knowledge == null || knowledge.isEmpty()
                ? "- 尚无关联知识文档，可在知识库补充对应材料。"
                : knowledge.stream().limit(8)
                        .map(document -> "- 关联知识：**" + inline(document.name(), 120) + "**")
                        .collect(java.util.stream.Collectors.joining("\n"));
        return topicList + "\n" + documents
                + "\n- 建议使用报告页的“创建专项训练方案”，以教练模式完成针对性复试。";
    }

    private String keyQaMarkdown(
            List<InterviewMessageDto> messages,
            EvidenceLedger ledger,
            Map<Long, Integer> questionNumbers
    ) {
        Map<Integer, List<EvaluationEvidence>> evidenceByQuestion = ledger.evidence().stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        item -> questionNumbers.getOrDefault(item.messageId(), 0),
                        java.util.LinkedHashMap::new, java.util.stream.Collectors.toList()));
        StringBuilder markdown = new StringBuilder();
        int questionNumber = 0;
        String currentQuestion = "";
        for (InterviewMessageDto message : messages) {
            if (message.role() == Message.Role.ASSISTANT) {
                questionNumber++;
                currentQuestion = message.content();
            } else if (message.role() == Message.Role.USER && questionNumber > 0) {
                markdown.append("### Q").append(questionNumber).append("\n\n**问题：** ")
                        .append(inline(currentQuestion, 420)).append("\n\n**回答：** ")
                        .append(inline(message.content(), 700)).append("\n\n**评分证据：** ");
                List<EvaluationEvidence> evidence = evidenceByQuestion.getOrDefault(questionNumber, List.of());
                markdown.append(evidence.isEmpty() ? "暂无" : evidence.stream()
                        .map(item -> "`" + item.id() + "`")
                        .collect(java.util.stream.Collectors.joining("、"))).append("\n\n");
            }
        }
        return markdown.isEmpty() ? "暂无完整问答。" : markdown.toString().stripTrailing();
    }

    private void appendField(StringBuilder markdown, String label, String value) {
        if (value != null && !value.isBlank()) {
            markdown.append("- **").append(label).append("：** ")
                    .append(inline(value, 500)).append("\n");
        }
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
