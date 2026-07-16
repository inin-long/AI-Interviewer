package com.inin.aiinterviewer.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inin.aiinterviewer.agent.model.ConsistencyCheckResult;
import com.inin.aiinterviewer.agent.model.ConsistencyContext;
import com.inin.aiinterviewer.application.exception.BusinessException;
import com.inin.aiinterviewer.application.exception.ErrorCode;
import com.inin.aiinterviewer.application.exception.SystemException;
import com.inin.aiinterviewer.domain.entity.ConsistencyIssueEntity;
import com.inin.aiinterviewer.domain.entity.InterviewClaimEntity;
import com.inin.aiinterviewer.domain.enums.ClaimType;
import com.inin.aiinterviewer.domain.enums.ConsistencyIssueStatus;
import com.inin.aiinterviewer.domain.model.ClaimLedger;
import com.inin.aiinterviewer.domain.model.ConsistencyIssue;
import com.inin.aiinterviewer.domain.model.InterviewClaim;
import com.inin.aiinterviewer.domain.model.Message;
import com.inin.aiinterviewer.infrastructure.database.mapper.ConsistencyIssueMapper;
import com.inin.aiinterviewer.infrastructure.database.mapper.InterviewClaimMapper;
import com.inin.aiinterviewer.infrastructure.database.mapper.InterviewMessageMapper;
import com.inin.aiinterviewer.infrastructure.database.mapper.InterviewSessionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class ConsistencyIssueService {

    private static final Set<ClaimType> CONFLICT_PRONE_TYPES = EnumSet.of(
            ClaimType.FACT, ClaimType.OWNERSHIP, ClaimType.METRIC,
            ClaimType.DECISION, ClaimType.OPINION, ClaimType.RESULT);
    private static final List<String> PERSONALITY_JUDGMENTS = List.of(
            "撒谎", "说谎", "不诚实", "人格", "欺骗", "liar", "dishonest");

    private final ConsistencyIssueMapper issueMapper;
    private final InterviewClaimMapper claimMapper;
    private final InterviewMessageMapper messageMapper;
    private final InterviewSessionMapper sessionMapper;
    private final ObjectMapper objectMapper;

    public ConsistencyIssueService(
            ConsistencyIssueMapper issueMapper,
            InterviewClaimMapper claimMapper,
            InterviewMessageMapper messageMapper,
            InterviewSessionMapper sessionMapper,
            ObjectMapper objectMapper
    ) {
        this.issueMapper = issueMapper;
        this.claimMapper = claimMapper;
        this.messageMapper = messageMapper;
        this.sessionMapper = sessionMapper;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public ConsistencyContext prepareContext(long userId, long sessionId) {
        requireSession(userId, sessionId);
        var latestAnswer = messageMapper.findLatestUserMessage(userId, sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_STATE));
        List<InterviewClaim> allClaims = claims(userId, sessionId);
        List<InterviewClaim> current = allClaims.stream()
                .filter(claim -> claim.sourceMessageId() == latestAnswer.getId()).toList();
        List<InterviewClaim> historical = allClaims.stream()
                .filter(claim -> claim.sourceMessageId() != latestAnswer.getId()).toList();
        List<ConsistencyIssue> openIssues = issues(userId, sessionId).stream()
                .filter(ConsistencyIssue::open).toList();
        long answerCount = messageMapper.findAll(userId, sessionId).stream()
                .filter(message -> message.getRole() == Message.Role.USER).count();
        boolean awaitingResolution = openIssues.stream()
                .anyMatch(issue -> issue.status() == ConsistencyIssueStatus.CLARIFIED);
        boolean topicHit = current.stream().anyMatch(candidate -> CONFLICT_PRONE_TYPES.contains(candidate.type())
                && historical.stream().anyMatch(previous -> previous.type() == candidate.type()));
        boolean periodic = answerCount >= 3 && answerCount % 3 == 0;
        boolean runRequested = !current.isEmpty() && !historical.isEmpty()
                && (awaitingResolution || topicHit || periodic);
        String reason = awaitingResolution ? "clarification_answered"
                : topicHit ? "related_claim_topic"
                : periodic ? "periodic_check" : "not_due";
        return new ConsistencyContext(
                runRequested, reason, current, recentRelevant(historical, current), openIssues);
    }

    @Transactional
    public AppliedConsistency apply(
            long userId,
            long sessionId,
            ConsistencyCheckResult result
    ) {
        requireSession(userId, sessionId);
        if (result == null || result.skipped() || result.degraded()) {
            return new AppliedConsistency(
                    result == null ? ConsistencyCheckResult.degraded("missing_result") : result,
                    ledger(userId, sessionId));
        }
        if (result.issues().size() > 8 || result.resolutions().size() > 8) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        List<InterviewClaim> claims = claims(userId, sessionId);
        Set<String> allowedClaimIds = claims.stream().map(InterviewClaim::id)
                .collect(java.util.stream.Collectors.toSet());
        long latestMessageId = messageMapper.findLatestUserMessage(userId, sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_STATE)).getId();
        Set<String> currentClaimIds = claims.stream()
                .filter(claim -> claim.sourceMessageId() == latestMessageId)
                .map(InterviewClaim::id).collect(java.util.stream.Collectors.toSet());

        for (var resolution : result.resolutions()) {
            applyResolution(userId, sessionId, resolution);
        }
        for (var candidate : result.issues()) {
            insertPotential(userId, sessionId, candidate, allowedClaimIds, currentClaimIds);
        }

        List<ConsistencyIssue> persisted = issues(userId, sessionId);
        List<ConsistencyCheckResult.IssueCandidate> normalizedIssues = new ArrayList<>();
        for (var candidate : result.issues()) {
            if (candidate == null || candidate.type() == null) continue;
            List<String> ids = normalizedClaimIds(candidate.relatedClaimIds(), allowedClaimIds);
            persisted.stream()
                    .filter(issue -> issue.status() == ConsistencyIssueStatus.POTENTIAL)
                    .filter(issue -> issue.type() == candidate.type())
                    .filter(issue -> issue.relatedClaimIds().equals(ids))
                    .findFirst()
                    .ifPresent(issue -> normalizedIssues.add(new ConsistencyCheckResult.IssueCandidate(
                            issue.id(), issue.type(), issue.description(), issue.relatedClaimIds(),
                            issue.clarificationQuestion(), candidate.confidence())));
        }
        List<ConsistencyCheckResult.ResolutionCandidate> appliedResolutions = result.resolutions().stream()
                .filter(candidate -> candidate != null && candidate.confidence() >= 0.55)
                .filter(candidate -> candidate.status() == ConsistencyIssueStatus.RESOLVED
                        || candidate.status() == ConsistencyIssueStatus.CONFIRMED_CONFLICT)
                .toList();
        return new AppliedConsistency(
                new ConsistencyCheckResult(normalizedIssues, appliedResolutions),
                new ClaimLedger(claims, persisted));
    }

    @Transactional
    public ClaimLedger markClarificationAsked(long userId, long sessionId, String issueId) {
        requireSession(userId, sessionId);
        if (issueId == null || issueId.isBlank()) return ledger(userId, sessionId);
        long messageId = messageMapper.findLatestAssistantMessage(userId, sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_STATE)).getId();
        if (issueMapper.markClarified(issueId, userId, sessionId, messageId) != 1) {
            boolean alreadyMarked = issueMapper.findById(issueId, userId, sessionId)
                    .filter(issue -> issue.getStatus() == ConsistencyIssueStatus.CLARIFIED)
                    .filter(issue -> java.util.Objects.equals(issue.getClarificationMessageId(), messageId))
                    .isPresent();
            if (!alreadyMarked) throw new BusinessException(ErrorCode.INVALID_STATE);
        }
        return ledger(userId, sessionId);
    }

    @Transactional(readOnly = true)
    public ClaimLedger ledger(long userId, long sessionId) {
        requireSession(userId, sessionId);
        return new ClaimLedger(claims(userId, sessionId), issues(userId, sessionId));
    }

    @Transactional(readOnly = true)
    public String compactSummary(long userId, long sessionId) {
        ConsistencyContext context = prepareContext(userId, sessionId);
        return write(context);
    }

    private void insertPotential(
            long userId,
            long sessionId,
            ConsistencyCheckResult.IssueCandidate candidate,
            Set<String> allowedClaimIds,
            Set<String> currentClaimIds
    ) {
        validateIssue(candidate);
        if (candidate.confidence() < 0.55) return;
        List<String> claimIds = normalizedClaimIds(candidate.relatedClaimIds(), allowedClaimIds);
        if (claimIds.size() < 2 || claimIds.stream().noneMatch(currentClaimIds::contains)
                || claimIds.stream().allMatch(currentClaimIds::contains)) {
            return;
        }
        ConsistencyIssueEntity entity = new ConsistencyIssueEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setUserId(userId);
        entity.setSessionId(sessionId);
        entity.setIssueType(candidate.type());
        entity.setStatus(ConsistencyIssueStatus.POTENTIAL);
        entity.setDescription(candidate.description());
        entity.setRelatedClaimIdsJson(write(claimIds));
        entity.setClarificationQuestion(candidate.clarificationQuestion());
        entity.setResolution("");
        issueMapper.insertOrRefresh(entity);
    }

    private void applyResolution(
            long userId,
            long sessionId,
            ConsistencyCheckResult.ResolutionCandidate candidate
    ) {
        if (candidate == null || candidate.issueId().isBlank() || candidate.status() == null
                || (candidate.status() != ConsistencyIssueStatus.RESOLVED
                && candidate.status() != ConsistencyIssueStatus.CONFIRMED_CONFLICT)
                || candidate.confidence() < 0.55 || candidate.confidence() > 1
                || candidate.resolution().isBlank() || candidate.resolution().length() > 2_000
                || containsPersonalityJudgment(candidate.resolution())) {
            return;
        }
        issueMapper.findById(candidate.issueId(), userId, sessionId)
                .filter(issue -> issue.getStatus() == ConsistencyIssueStatus.CLARIFIED)
                .ifPresent(issue -> issueMapper.resolve(
                        candidate.issueId(), userId, sessionId, candidate.status(), candidate.resolution()));
    }

    private void validateIssue(ConsistencyCheckResult.IssueCandidate candidate) {
        if (candidate == null || candidate.type() == null
                || candidate.description().isBlank() || candidate.description().length() > 2_000
                || candidate.clarificationQuestion().isBlank()
                || candidate.clarificationQuestion().length() > 1_000
                || !Double.isFinite(candidate.confidence()) || candidate.confidence() < 0
                || candidate.confidence() > 1 || candidate.relatedClaimIds().size() > 12
                || containsPersonalityJudgment(candidate.description())
                || containsPersonalityJudgment(candidate.clarificationQuestion())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
    }

    private boolean containsPersonalityJudgment(String value) {
        String normalized = value == null ? "" : value.toLowerCase(java.util.Locale.ROOT);
        return PERSONALITY_JUDGMENTS.stream().anyMatch(normalized::contains);
    }

    private List<InterviewClaim> recentRelevant(
            List<InterviewClaim> historical,
            List<InterviewClaim> current
    ) {
        Set<ClaimType> currentTypes = current.stream().map(InterviewClaim::type)
                .collect(java.util.stream.Collectors.toSet());
        return historical.stream()
                .sorted(Comparator
                        .comparingInt((InterviewClaim claim) -> currentTypes.contains(claim.type()) ? 0 : 1)
                        .thenComparing(Comparator.comparingDouble(InterviewClaim::importance).reversed()))
                .limit(20).toList();
    }

    private List<String> normalizedClaimIds(List<String> values, Set<String> allowed) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        if (values != null) {
            values.stream().filter(allowed::contains).sorted().forEach(result::add);
        }
        return List.copyOf(result);
    }

    private List<InterviewClaim> claims(long userId, long sessionId) {
        return claimMapper.findAll(userId, sessionId).stream().map(this::toClaim).toList();
    }

    private List<ConsistencyIssue> issues(long userId, long sessionId) {
        return issueMapper.findAll(userId, sessionId).stream().map(this::toIssue).toList();
    }

    private InterviewClaim toClaim(InterviewClaimEntity entity) {
        return new InterviewClaim(
                entity.getId(), entity.getSessionId(), entity.getSourceMessageId(), entity.getClaimType(),
                entity.getContent(), entity.getImportance(), entity.getCredibility(), entity.getStatus(),
                readList(entity.getMissingEvidenceJson()), readList(entity.getSupportingEvidenceIdsJson()),
                readList(entity.getConflictingEvidenceIdsJson()), entity.getCreateTime(), entity.getUpdateTime());
    }

    private ConsistencyIssue toIssue(ConsistencyIssueEntity entity) {
        return new ConsistencyIssue(
                entity.getId(), entity.getSessionId(), entity.getIssueType(), entity.getStatus(),
                entity.getDescription(), readList(entity.getRelatedClaimIdsJson()),
                entity.getClarificationMessageId(), entity.getClarificationQuestion(), entity.getResolution(),
                entity.getCreateTime(), entity.getUpdateTime());
    }

    private void requireSession(long userId, long sessionId) {
        if (sessionMapper.findByIdAndUserId(sessionId, userId).isEmpty()) {
            throw new BusinessException(ErrorCode.INTERVIEW_NOT_FOUND);
        }
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new SystemException(ErrorCode.SYSTEM_ERROR, exception);
        }
    }

    private List<String> readList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            List<String> values = objectMapper.readValue(json, new TypeReference<>() { });
            return values == null ? List.of() : List.copyOf(values);
        } catch (JsonProcessingException exception) {
            throw new SystemException(ErrorCode.DATA_ACCESS_FAILED, exception);
        }
    }

    public record AppliedConsistency(ConsistencyCheckResult result, ClaimLedger ledger) { }
}
