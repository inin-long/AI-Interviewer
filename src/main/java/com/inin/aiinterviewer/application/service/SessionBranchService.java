package com.inin.aiinterviewer.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inin.aiinterviewer.agent.graph.InterviewGraph;
import com.inin.aiinterviewer.agent.model.ClaimExtractionResult;
import com.inin.aiinterviewer.agent.model.EvidenceCollectionResult;
import com.inin.aiinterviewer.agent.model.InterviewTurnInput;
import com.inin.aiinterviewer.agent.model.LogicChainResult;
import com.inin.aiinterviewer.agent.state.InterviewState;
import com.inin.aiinterviewer.agent.state.StateSerializer;
import com.inin.aiinterviewer.application.dto.InterviewSessionDto;
import com.inin.aiinterviewer.application.dto.SessionBranchDto;
import com.inin.aiinterviewer.application.exception.ApplicationException;
import com.inin.aiinterviewer.application.exception.BusinessException;
import com.inin.aiinterviewer.application.exception.ErrorCode;
import com.inin.aiinterviewer.application.exception.SystemException;
import com.inin.aiinterviewer.domain.entity.AgentCheckpointEntity;
import com.inin.aiinterviewer.domain.entity.InterviewMessageEntity;
import com.inin.aiinterviewer.domain.entity.SessionBranchEntity;
import com.inin.aiinterviewer.domain.enums.EvidenceSignal;
import com.inin.aiinterviewer.domain.enums.SessionBranchStatus;
import com.inin.aiinterviewer.domain.model.BranchComparison;
import com.inin.aiinterviewer.domain.model.EvaluationEvidence;
import com.inin.aiinterviewer.domain.model.Message;
import com.inin.aiinterviewer.infrastructure.database.mapper.AgentCheckpointMapper;
import com.inin.aiinterviewer.infrastructure.database.mapper.InterviewMessageMapper;
import com.inin.aiinterviewer.infrastructure.database.mapper.SessionBranchMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class SessionBranchService {

    private final SessionBranchMapper branchMapper;
    private final AgentCheckpointMapper checkpointMapper;
    private final InterviewMessageMapper messageMapper;
    private final InterviewSessionService sessionService;
    private final EvidenceLedgerService evidenceLedgerService;
    private final InterviewGraph interviewGraph;
    private final CollaborationEvidenceCollector collaborationEvidenceCollector;
    private final StateSerializer stateSerializer;
    private final ObjectMapper objectMapper;

    public SessionBranchService(
            SessionBranchMapper branchMapper,
            AgentCheckpointMapper checkpointMapper,
            InterviewMessageMapper messageMapper,
            InterviewSessionService sessionService,
            EvidenceLedgerService evidenceLedgerService,
            InterviewGraph interviewGraph,
            CollaborationEvidenceCollector collaborationEvidenceCollector,
            StateSerializer stateSerializer,
            ObjectMapper objectMapper
    ) {
        this.branchMapper = branchMapper;
        this.checkpointMapper = checkpointMapper;
        this.messageMapper = messageMapper;
        this.sessionService = sessionService;
        this.evidenceLedgerService = evidenceLedgerService;
        this.interviewGraph = interviewGraph;
        this.collaborationEvidenceCollector = collaborationEvidenceCollector;
        this.stateSerializer = stateSerializer;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public SessionBranchDto create(
            long userId,
            long sourceSessionId,
            int questionNumber,
            String parentBranchId
    ) {
        InterviewSessionDto session = sessionService.require(userId, sourceSessionId);
        if (questionNumber < 1) throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        if (parentBranchId != null && !parentBranchId.isBlank()) {
            SessionBranchEntity parent = requireEntity(userId, parentBranchId);
            if (!parent.getSourceSessionId().equals(sourceSessionId)) {
                throw new BusinessException(ErrorCode.INVALID_STATE);
            }
        }
        SourceTurn turn = sourceTurn(userId, sourceSessionId, questionNumber);
        AgentCheckpointEntity checkpoint = sourceCheckpoint(
                userId, sourceSessionId, questionNumber, turn.question());

        SessionBranchEntity entity = new SessionBranchEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setUserId(userId);
        entity.setSourceSessionId(sourceSessionId);
        entity.setSourceCheckpointId(checkpoint.getId());
        entity.setParentBranchId(parentBranchId == null || parentBranchId.isBlank()
                ? null : parentBranchId.strip());
        entity.setSourceQuestionNumber(questionNumber);
        entity.setTitle(safeTitle(session.title() + " · Q" + questionNumber + " 分支复盘"));
        entity.setStatus(SessionBranchStatus.DRAFT);
        entity.setSourceStateJson(checkpoint.getStateJson());
        entity.setOriginalQuestion(turn.question().getContent());
        entity.setOriginalAnswer(turn.answer().getContent());
        branchMapper.insert(entity);
        return require(userId, entity.getId());
    }

    @Transactional(readOnly = true)
    public SessionBranchDto require(long userId, String branchId) {
        return toDto(requireEntity(userId, branchId));
    }

    @Transactional(readOnly = true)
    public List<SessionBranchDto> list(long userId, long sourceSessionId) {
        sessionService.require(userId, sourceSessionId);
        return branchMapper.findAll(userId, sourceSessionId).stream().map(this::toDto).toList();
    }

    public SessionBranchDto submitAnswer(long userId, String branchId, String newAnswer) {
        String normalized = newAnswer == null ? "" : newAnswer.strip();
        if (normalized.isBlank() || normalized.length() > 20_000) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        if (branchMapper.startComparison(branchId, userId, normalized) != 1) {
            throw new BusinessException(ErrorCode.INVALID_STATE);
        }
        try {
            SessionBranchEntity branch = requireEntity(userId, branchId);
            BranchAnalysis analysis = analyze(userId, branch, normalized);
            BranchComparison comparison = compare(branch, analysis);
            String markdown = comparisonMarkdown(branch, normalized, comparison);
            if (branchMapper.complete(
                    branchId, userId, write(comparison), markdown) != 1) {
                throw new BusinessException(ErrorCode.INVALID_STATE);
            }
            return require(userId, branchId);
        } catch (RuntimeException exception) {
            branchMapper.fail(branchId, userId, failureMessage(exception));
            throw exception;
        }
    }

    private BranchAnalysis analyze(long userId, SessionBranchEntity branch, String newAnswer) {
        InterviewSessionDto session = sessionService.require(userId, branch.getSourceSessionId());
        InterviewState source = stateSerializer.deserialize(branch.getSourceStateJson());
        if (source.userId() != userId || source.sessionId() != branch.getSourceSessionId()) {
            throw new BusinessException(ErrorCode.INVALID_STATE);
        }
        InterviewTurnInput originalInput = input(session, source, branch.getOriginalAnswer());
        ClaimExtractionResult originalClaims = interviewGraph.extractClaims(originalInput);
        LogicChainResult originalLogic = interviewGraph.evaluateLogic(
                originalInput.withClaimExtraction(originalClaims));

        InterviewTurnInput newInput = input(session, source, newAnswer);
        ClaimExtractionResult newClaims = interviewGraph.extractClaims(newInput);
        newInput = newInput.withClaimExtraction(newClaims);
        LogicChainResult newLogic = interviewGraph.evaluateLogic(newInput);
        newInput = newInput.withLogicChainResult(newLogic);
        EvidenceCollectionResult newEvidence = collaborationEvidenceCollector.enrich(
                newAnswer, interviewGraph.collectEvidence(newInput));

        SourceTurn sourceTurn = sourceTurn(
                userId, branch.getSourceSessionId(), branch.getSourceQuestionNumber());
        List<EvaluationEvidence> originalEvidence = evidenceLedgerService
                .ledger(userId, branch.getSourceSessionId()).evidence().stream()
                .filter(evidence -> evidence.messageId() == sourceTurn.answer().getId()).toList();
        return new BranchAnalysis(
                originalClaims, originalLogic, originalEvidence,
                newClaims, newLogic, newEvidence, sourceTurn.followUp());
    }

    private InterviewTurnInput input(
            InterviewSessionDto session,
            InterviewState source,
            String answer
    ) {
        List<Message> messages = new ArrayList<>(source.messages());
        messages.add(new Message(Message.Role.USER, answer, LocalDateTime.now()));
        String profile = session.profileSnapshot() == null ? "" : session.profileSnapshot().toString();
        String domainPack = sessionService.domainPackSnapshot(source.userId(), source.sessionId())
                .map(snapshot -> Map.of(
                        "id", snapshot.id(), "version", snapshot.version(),
                        "displayName", snapshot.content().displayName(),
                        "competencies", snapshot.content().competencies(),
                        "rubrics", snapshot.content().rubrics()).toString())
                .orElse("");
        return new InterviewTurnInput(
                source.stage(), source.currentQuestion(), answer, session.planSnapshot(), messages,
                source.summary(), "", profile, domainPack, source.claimLedger().toString(),
                source.evidenceLedger().summaries().toString(), null, List.of(),
                source.pressureState(), source.activeScenario(), null, null, null, null);
    }

    private BranchComparison compare(SessionBranchEntity branch, BranchAnalysis analysis) {
        double originalLogic = logicCompleteness(analysis.originalLogic());
        double newLogic = logicCompleteness(analysis.newLogic());
        int originalScore = evidenceScore(analysis.originalEvidence());
        int newScore = evidenceScoreCandidates(analysis.newEvidence().evidence());
        Set<String> originalGaps = gapTypes(analysis.originalLogic());
        Set<String> newGaps = gapTypes(analysis.newLogic());
        List<String> resolved = originalGaps.stream().filter(type -> !newGaps.contains(type)).sorted().toList();
        String branchFollowUp = branchFollowUp(analysis.newLogic(), analysis.newClaims());
        boolean revised = viewpointRevised(
                branch.getOriginalAnswer(), branch.getNewAnswer(),
                analysis.originalClaims(), analysis.newClaims());
        String summary = "逻辑链完整度从 %s 提升到 %s，证据数量从 %d 变为 %d，证据质量分从 %d 变为 %d；%s。"
                .formatted(percent(originalLogic), percent(newLogic), analysis.originalEvidence().size(),
                        analysis.newEvidence().evidence().size(), originalScore, newScore,
                        resolved.isEmpty() ? "原始缺口仍需继续验证"
                                : "已解决缺口 " + String.join("、", resolved));
        return new BranchComparison(
                originalLogic, newLogic, analysis.originalEvidence().size(),
                analysis.newEvidence().evidence().size(), originalScore, newScore,
                analysis.originalFollowUp(), branchFollowUp, revised,
                resolved, newGaps.stream().sorted().toList(), summary);
    }

    private double logicCompleteness(LogicChainResult logic) {
        if (logic == null || logic.skipped() || logic.degraded()) return 0;
        int present = 0;
        if (!logic.problemDiagnosis().isBlank()) present++;
        if (!logic.alternatives().isEmpty()) present++;
        if (!logic.decision().isBlank()) present++;
        if (!logic.reasoning().isBlank()) present++;
        if (!logic.actions().isEmpty()) present++;
        if (!logic.outcome().isBlank()) present++;
        if (!logic.validation().isBlank()) present++;
        if (!logic.reflection().isBlank()) present++;
        double base = present / 8.0;
        double penalty = logic.gaps().stream().mapToDouble(gap -> gap.severity()).average().orElse(0) * 0.4;
        return Math.max(0, Math.min(1, base - penalty));
    }

    private int evidenceScore(List<EvaluationEvidence> evidence) {
        if (evidence.isEmpty()) return 0;
        double total = evidence.stream().mapToDouble(item -> signed(
                item.signal(), item.strength(), item.confidence())).average().orElse(0);
        return score(total);
    }

    private int evidenceScoreCandidates(List<EvidenceCollectionResult.EvidenceCandidate> evidence) {
        if (evidence.isEmpty()) return 0;
        double total = evidence.stream().mapToDouble(item -> signed(
                item.signal(), item.strength(), item.confidence())).average().orElse(0);
        return score(total);
    }

    private double signed(EvidenceSignal signal, double strength, double confidence) {
        double direction = signal == EvidenceSignal.POSITIVE ? 1
                : signal == EvidenceSignal.NEGATIVE ? -1 : 0;
        return direction * strength * confidence;
    }

    private int score(double signedEvidence) {
        return (int) Math.round(Math.max(0, Math.min(100, 50 + signedEvidence * 50)));
    }

    private Set<String> gapTypes(LogicChainResult logic) {
        if (logic == null) return Set.of();
        return logic.gaps().stream().map(gap -> gap.type().name())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private String branchFollowUp(LogicChainResult logic, ClaimExtractionResult claims) {
        if (logic != null && !logic.gaps().isEmpty()) {
            return "你在新回答中仍需补充“%s”，请说明具体事实和验证方式。"
                    .formatted(logic.gaps().getFirst().description());
        }
        if (claims != null) {
            var claim = claims.claims().stream()
                    .filter(value -> !value.missingEvidence().isEmpty()).findFirst();
            if (claim.isPresent()) {
                return "针对“%s”，请补充%s。".formatted(
                        claim.get().content(), String.join("、", claim.get().missingEvidence()));
            }
        }
        return "这次回答已经补全主要逻辑，请进一步说明你会如何在真实环境中验证结果。";
    }

    private boolean viewpointRevised(
            String originalAnswer,
            String newAnswer,
            ClaimExtractionResult originalClaims,
            ClaimExtractionResult newClaims
    ) {
        String normalizedOriginal = normalize(originalAnswer);
        String normalizedNew = normalize(newAnswer);
        if (normalizedOriginal.equals(normalizedNew)) return false;
        boolean explicit = List.of("修正", "重新考虑", "调整", "改为", "补充", "收回")
                .stream().anyMatch(newAnswer::contains);
        Set<String> before = originalClaims.claims().stream().map(claim -> normalize(claim.content()))
                .collect(java.util.stream.Collectors.toSet());
        Set<String> after = newClaims.claims().stream().map(claim -> normalize(claim.content()))
                .collect(java.util.stream.Collectors.toSet());
        return explicit || !before.equals(after);
    }

    private SourceTurn sourceTurn(long userId, long sessionId, int questionNumber) {
        sessionService.require(userId, sessionId);
        List<InterviewMessageEntity> messages = messageMapper.findAll(userId, sessionId);
        int current = 0;
        for (int index = 0; index < messages.size(); index++) {
            InterviewMessageEntity message = messages.get(index);
            if (message.getRole() != Message.Role.ASSISTANT) continue;
            current++;
            if (current != questionNumber) continue;
            InterviewMessageEntity answer = null;
            String followUp = "";
            for (int next = index + 1; next < messages.size(); next++) {
                InterviewMessageEntity candidate = messages.get(next);
                if (candidate.getRole() == Message.Role.USER && answer == null) {
                    answer = candidate;
                } else if (candidate.getRole() == Message.Role.ASSISTANT) {
                    followUp = candidate.getContent();
                    break;
                }
            }
            if (answer == null) throw new BusinessException(ErrorCode.INVALID_STATE);
            return new SourceTurn(message, answer, followUp);
        }
        throw new BusinessException(ErrorCode.VALIDATION_FAILED);
    }

    private AgentCheckpointEntity sourceCheckpoint(
            long userId,
            long sessionId,
            int questionNumber,
            InterviewMessageEntity question
    ) {
        return checkpointMapper.findLatestFirst(userId, sessionId).stream()
                .filter(checkpoint -> checkpoint.getNodeName().equals("agent_turn_completed")
                        || checkpoint.getNodeName().equals("question_stream_interrupted"))
                .filter(checkpoint -> checkpointMatches(
                        checkpoint, userId, sessionId, questionNumber, question.getContent()))
                .min(Comparator.comparingLong(AgentCheckpointEntity::getId))
                .orElseThrow(() -> new BusinessException(ErrorCode.CHECKPOINT_NOT_FOUND));
    }

    private boolean checkpointMatches(
            AgentCheckpointEntity checkpoint,
            long userId,
            long sessionId,
            int questionNumber,
            String question
    ) {
        try {
            InterviewState state = stateSerializer.deserialize(checkpoint.getStateJson());
            long assistantCount = state.messages().stream()
                    .filter(message -> message.role() == Message.Role.ASSISTANT).count();
            return state.userId() == userId && state.sessionId() == sessionId
                    && assistantCount == questionNumber
                    && state.currentQuestion().equals(question)
                    && !state.messages().isEmpty()
                    && state.messages().getLast().role() == Message.Role.ASSISTANT;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private String comparisonMarkdown(
            SessionBranchEntity branch,
            String newAnswer,
            BranchComparison comparison
    ) {
        return """
                # %s

                ## 原始问题

                %s

                ## 两次回答

                ### 原始回答

                %s

                ### 分支回答

                %s

                ## 局部对比

                | 项目 | 原始回答 | 分支回答 |
                | --- | ---: | ---: |
                | 逻辑链完整度 | %s | %s |
                | 证据数量 | %d | %d |
                | 证据质量分 | %d | %d |

                - 观点发生实质修正：%s
                - 已解决缺口：%s
                - 仍待补充：%s

                ## 追问变化

                - 原始追问：%s
                - 分支建议追问：%s

                ## 对比结论

                %s
                """.formatted(escape(branch.getTitle()), escape(branch.getOriginalQuestion()),
                escape(branch.getOriginalAnswer()), escape(newAnswer),
                percent(comparison.originalLogicCompleteness()),
                percent(comparison.newLogicCompleteness()),
                comparison.originalEvidenceCount(), comparison.newEvidenceCount(),
                comparison.originalEvidenceScore(), comparison.newEvidenceScore(),
                comparison.viewpointRevised() ? "是" : "否",
                emptyLabel(comparison.resolvedGapTypes()), emptyLabel(comparison.remainingGapTypes()),
                escape(comparison.originalFollowUp().isBlank() ? "原流程未生成后续追问" : comparison.originalFollowUp()),
                escape(comparison.branchFollowUp()), escape(comparison.summary()));
    }

    private SessionBranchEntity requireEntity(long userId, String branchId) {
        if (branchId == null || branchId.isBlank()) throw new BusinessException(ErrorCode.BRANCH_NOT_FOUND);
        return branchMapper.findById(branchId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BRANCH_NOT_FOUND));
    }

    private SessionBranchDto toDto(SessionBranchEntity entity) {
        BranchComparison comparison = entity.getStatus() == SessionBranchStatus.COMPLETED
                ? readComparison(entity.getComparisonJson()) : null;
        return new SessionBranchDto(
                entity.getId(), entity.getSourceSessionId(), entity.getSourceCheckpointId(),
                entity.getParentBranchId(), entity.getSourceQuestionNumber(), entity.getTitle(),
                entity.getStatus(), entity.getOriginalQuestion(), entity.getOriginalAnswer(),
                entity.getNewAnswer(), comparison, entity.getComparisonMarkdown(),
                entity.getErrorMessage(), entity.getCreateTime(), entity.getUpdateTime());
    }

    private BranchComparison readComparison(String json) {
        try {
            return objectMapper.readValue(json, BranchComparison.class);
        } catch (JsonProcessingException exception) {
            throw new SystemException(ErrorCode.DATA_ACCESS_FAILED, exception);
        }
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new SystemException(ErrorCode.SYSTEM_ERROR, exception);
        }
    }

    private String failureMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof ApplicationException application) {
                return truncate(application.getErrorCode().userMessage(), 500);
            }
            current = current.getCause();
        }
        return "分支比较失败，请重试";
    }

    private String safeTitle(String value) {
        return truncate(value == null ? "关键问题分支复盘" : value.strip(), 128);
    }

    private String truncate(String value, int length) {
        return value.length() <= length ? value : value.substring(0, length);
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("\\s+", "").strip();
    }

    private String percent(double value) {
        return String.format(Locale.ROOT, "%.0f%%", value * 100);
    }

    private String emptyLabel(List<String> values) {
        return values.isEmpty() ? "无" : values.stream().map(this::escape)
                .collect(java.util.stream.Collectors.joining("、"));
    }

    private String escape(String value) {
        return (value == null ? "" : value.replaceAll("\\s+", " ").strip())
                .replace("\\", "\\\\").replace("*", "\\*").replace("_", "\\_")
                .replace("[", "\\[").replace("]", "\\]").replace("#", "\\#").replace("|", "\\|");
    }

    private record SourceTurn(
            InterviewMessageEntity question,
            InterviewMessageEntity answer,
            String followUp
    ) {
    }

    private record BranchAnalysis(
            ClaimExtractionResult originalClaims,
            LogicChainResult originalLogic,
            List<EvaluationEvidence> originalEvidence,
            ClaimExtractionResult newClaims,
            LogicChainResult newLogic,
            EvidenceCollectionResult newEvidence,
            String originalFollowUp
    ) {
    }
}
