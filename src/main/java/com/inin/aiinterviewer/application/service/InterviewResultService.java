package com.inin.aiinterviewer.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inin.aiinterviewer.agent.model.EvaluationPayload;
import com.inin.aiinterviewer.agent.state.InterviewState;
import com.inin.aiinterviewer.agent.state.StateSerializer;
import com.inin.aiinterviewer.application.dto.InterviewMessageDto;
import com.inin.aiinterviewer.application.dto.InterviewReportDto;
import com.inin.aiinterviewer.application.dto.InterviewReportStateDto;
import com.inin.aiinterviewer.application.dto.InterviewSessionDto;
import com.inin.aiinterviewer.application.dto.EvaluationEvidenceDto;
import com.inin.aiinterviewer.application.exception.BusinessException;
import com.inin.aiinterviewer.application.exception.ErrorCode;
import com.inin.aiinterviewer.application.exception.SystemException;
import com.inin.aiinterviewer.domain.entity.AgentCheckpointEntity;
import com.inin.aiinterviewer.domain.entity.EvaluationEntity;
import com.inin.aiinterviewer.domain.entity.InterviewReportEntity;
import com.inin.aiinterviewer.domain.enums.InterviewStage;
import com.inin.aiinterviewer.domain.enums.ReportStatus;
import com.inin.aiinterviewer.domain.model.ClaimLedger;
import com.inin.aiinterviewer.domain.model.EvaluationResult;
import com.inin.aiinterviewer.domain.model.Message;
import com.inin.aiinterviewer.infrastructure.database.mapper.AgentCheckpointMapper;
import com.inin.aiinterviewer.infrastructure.database.mapper.InterviewResultMapper;
import com.inin.aiinterviewer.infrastructure.database.mapper.InterviewMessageMapper;
import com.inin.aiinterviewer.infrastructure.database.mapper.InterviewSessionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class InterviewResultService {

    private final InterviewResultMapper resultMapper;
    private final InterviewSessionMapper sessionMapper;
    private final AgentCheckpointMapper checkpointMapper;
    private final InterviewMessageMapper messageMapper;
    private final StateSerializer stateSerializer;
    private final ObjectMapper objectMapper;
    private final EvidenceLedgerService evidenceLedgerService;
    private final EvidenceScoreAggregator scoreAggregator;

    public InterviewResultService(
            InterviewResultMapper resultMapper,
            InterviewSessionMapper sessionMapper,
            AgentCheckpointMapper checkpointMapper,
            InterviewMessageMapper messageMapper,
            StateSerializer stateSerializer,
            ObjectMapper objectMapper,
            EvidenceLedgerService evidenceLedgerService,
            EvidenceScoreAggregator scoreAggregator
    ) {
        this.resultMapper = resultMapper;
        this.sessionMapper = sessionMapper;
        this.checkpointMapper = checkpointMapper;
        this.messageMapper = messageMapper;
        this.stateSerializer = stateSerializer;
        this.objectMapper = objectMapper;
        this.evidenceLedgerService = evidenceLedgerService;
        this.scoreAggregator = scoreAggregator;
    }

    @Transactional
    public InterviewReportStateDto beginGeneration(long userId, InterviewSessionDto session) {
        Optional<InterviewReportEntity> existing = resultMapper.findReport(userId, session.id());
        if (existing.isEmpty()) {
            InterviewReportEntity report = new InterviewReportEntity();
            report.setUserId(userId);
            report.setInterviewId(session.id());
            report.setTitle(session.title() + " · 面试报告");
            report.setStatus(ReportStatus.GENERATING);
            resultMapper.insertReport(report);
            return new InterviewReportStateDto(ReportStatus.GENERATING, "");
        }
        InterviewReportEntity report = existing.get();
        if (report.getStatus() == ReportStatus.COMPLETED
                || resultMapper.restartReport(report.getId(), userId) != 1) {
            throw new BusinessException(ErrorCode.INVALID_STATE);
        }
        return new InterviewReportStateDto(ReportStatus.GENERATING, "");
    }

    @Transactional
    public void failGeneration(long userId, long interviewId, String errorMessage) {
        InterviewReportEntity report = resultMapper.findReport(userId, interviewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_STATE));
        String safeMessage = errorMessage == null || errorMessage.isBlank()
                ? "报告生成失败，请重试" : errorMessage.strip();
        if (safeMessage.length() > 500) safeMessage = safeMessage.substring(0, 500);
        if (resultMapper.failReport(report.getId(), userId, safeMessage) != 1) {
            throw new BusinessException(ErrorCode.INVALID_STATE);
        }
    }

    @Transactional(readOnly = true)
    public InterviewReportStateDto state(long userId, long interviewId) {
        return resultMapper.findReport(userId, interviewId)
                .map(report -> new InterviewReportStateDto(report.getStatus(), report.getErrorMessage()))
                .orElseGet(() -> new InterviewReportStateDto(ReportStatus.NOT_STARTED, ""));
    }

    @Transactional
    public InterviewReportDto complete(
            long userId,
            InterviewSessionDto session,
            List<InterviewMessageDto> messages,
            InterviewState previous,
            EvaluationPayload payload,
            String summary,
            String markdown
    ) {
        InterviewReportEntity report = resultMapper.findReport(userId, session.id())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_STATE));
        if (report.getStatus() != ReportStatus.GENERATING
                || resultMapper.findEvaluation(userId, session.id()).isPresent()) {
            throw new BusinessException(ErrorCode.INVALID_STATE);
        }

        EvaluationEntity evaluation = toEntity(userId, session.id(), payload);
        resultMapper.insertEvaluation(evaluation);

        report.setEvaluationId(evaluation.getId());
        report.setContentMarkdown(markdown);
        report.setScore(payload.overallScore());
        report.setStatus(ReportStatus.COMPLETED);
        if (resultMapper.completeReport(
                report.getId(), userId, evaluation.getId(), markdown, payload.overallScore()) != 1) {
            throw new BusinessException(ErrorCode.INVALID_STATE);
        }

        if (sessionMapper.complete(session.id(), userId) != 1) {
            throw new BusinessException(ErrorCode.INVALID_STATE);
        }

        EvaluationResult result = new EvaluationResult(
                payload.overallScore(), dimensions(payload), payload.summary());
        InterviewState completed = new InterviewState(
                previous.stateVersion(), session.id(), userId, InterviewStage.COMPLETED,
                messages.stream().map(this::toMessage).toList(), previous.currentQuestion(),
                previous.latestAnswer(), previous.analysis(), result, previous.profile(),
                previous.rules(), summary, previous.claimLedger(), previous.evidenceLedger(),
                previous.logicChainResult(), previous.probePlan(), previous.deferredProbes(),
                previous.pressureState(), previous.activeScenario(), previous.coverage(),
                previous.strategy());
        AgentCheckpointEntity checkpoint = new AgentCheckpointEntity();
        checkpoint.setUserId(userId);
        checkpoint.setSessionId(session.id());
        checkpoint.setNodeName("session_completed");
        checkpoint.setStateJson(stateSerializer.serialize(completed));
        checkpoint.setStateVersion(completed.stateVersion());
        checkpointMapper.insert(checkpoint);

        return toDto(report, payload);
    }

    @Transactional(readOnly = true)
    public Optional<InterviewReportDto> find(long userId, long interviewId) {
        return resultMapper.findReport(userId, interviewId).flatMap(report ->
                report.getStatus() != ReportStatus.COMPLETED ? Optional.empty() :
                        resultMapper.findEvaluation(userId, interviewId).map(evaluation ->
                                toDto(report, readPayload(evaluation.getContentJson()))));
    }

    private EvaluationEntity toEntity(long userId, long interviewId, EvaluationPayload payload) {
        EvaluationEntity entity = new EvaluationEntity();
        entity.setUserId(userId);
        entity.setInterviewId(interviewId);
        entity.setOverallScore(payload.overallScore());
        entity.setTechnicalScore(payload.technicalScore());
        entity.setProblemSolvingScore(payload.problemSolvingScore());
        entity.setProjectScore(payload.projectScore());
        entity.setCommunicationScore(payload.communicationScore());
        entity.setComprehensiveScore(payload.comprehensiveScore());
        entity.setContentJson(writeJson(payload));
        return entity;
    }

    private InterviewReportDto toDto(InterviewReportEntity report, EvaluationPayload payload) {
        var ledger = evidenceLedgerService.ledger(report.getUserId(), report.getInterviewId());
        Map<Long, Integer> questionNumbers = messageQuestionNumbers(
                report.getUserId(), report.getInterviewId());
        // 评分器只产出分数与总结，通常不返回 scoreEvidence；用证据账本聚合补齐，
        // 以保证报告页的评分证据面板、置信度与“点击分数跳转到证据”可用。AI 分数/总结仍保留。
        EvaluationPayload aggregated = scoreAggregator.aggregate(
                ledger, ClaimLedger.empty(), payload.summary());
        EvaluationPayload effective = payload.scoreEvidence().isEmpty()
                ? new EvaluationPayload(
                        payload.overallScore(), payload.technicalScore(), payload.problemSolvingScore(),
                        payload.projectScore(), payload.communicationScore(),
                        payload.comprehensiveScore(), payload.summary(),
                        aggregated.scoreEvidence(), aggregated.overallConfidence(), aggregated.overallScored())
                : payload;
        Map<String, Double> confidence = effective.scoreEvidence().isEmpty()
                ? ledger.summaries().entrySet().stream()
                        .collect(java.util.stream.Collectors.toMap(
                                Map.Entry::getKey, entry -> entry.getValue().confidence(), (a, b) -> a))
                : effective.scoreEvidence().entrySet().stream()
                        .filter(entry -> !entry.getKey().equals(EvidenceScoreAggregator.OVERALL))
                        .collect(java.util.stream.Collectors.toMap(
                                Map.Entry::getKey, entry -> entry.getValue().confidence(), (a, b) -> a));
        return new InterviewReportDto(report.getId(), report.getInterviewId(), report.getTitle(),
                effective.overallScore(), dimensions(effective), effective.summary(), report.getContentMarkdown(),
                confidence, ledger.evidence().stream()
                        .map(evidence -> EvaluationEvidenceDto.from(
                                evidence, questionNumbers.getOrDefault(evidence.messageId(), 0)))
                        .toList(), effective.scoreEvidence(), effective.overallConfidence(),
                effective.overallScored());
    }

    private Map<Long, Integer> messageQuestionNumbers(long userId, long sessionId) {
        Map<Long, Integer> result = new java.util.LinkedHashMap<>();
        int questionNumber = 0;
        for (var message : sessionMessageEntities(userId, sessionId)) {
            if (message.getRole() == Message.Role.ASSISTANT) questionNumber++;
            if (questionNumber > 0) result.put(message.getId(), questionNumber);
        }
        return Map.copyOf(result);
    }

    private List<com.inin.aiinterviewer.domain.entity.InterviewMessageEntity> sessionMessageEntities(
            long userId,
            long sessionId
    ) {
        return messageMapper.findAll(userId, sessionId);
    }

    private Map<String, Integer> dimensions(EvaluationPayload payload) {
        return Map.of(
                "technical", payload.technicalScore(),
                "problemSolving", payload.problemSolvingScore(),
                "project", payload.projectScore(),
                "communication", payload.communicationScore(),
                "comprehensive", payload.comprehensiveScore());
    }

    private Message toMessage(InterviewMessageDto dto) {
        return new Message(dto.role(), dto.content(), dto.createTime());
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new SystemException(ErrorCode.SYSTEM_ERROR, exception);
        }
    }

    private EvaluationPayload readPayload(String json) {
        try {
            EvaluationPayload payload = objectMapper.readValue(json, EvaluationPayload.class);
            if (!objectMapper.readTree(json).has("scoreEvidence")) {
                return new EvaluationPayload(
                        payload.overallScore(), payload.technicalScore(),
                        payload.problemSolvingScore(), payload.projectScore(),
                        payload.communicationScore(),
                        payload.comprehensiveScore(), payload.summary());
            }
            return payload;
        } catch (JsonProcessingException exception) {
            throw new SystemException(ErrorCode.SYSTEM_ERROR, exception);
        }
    }
}
