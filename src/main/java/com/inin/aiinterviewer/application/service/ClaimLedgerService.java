package com.inin.aiinterviewer.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inin.aiinterviewer.agent.model.ClaimExtractionResult;
import com.inin.aiinterviewer.application.exception.BusinessException;
import com.inin.aiinterviewer.application.exception.ErrorCode;
import com.inin.aiinterviewer.application.exception.SystemException;
import com.inin.aiinterviewer.domain.entity.InterviewClaimEntity;
import com.inin.aiinterviewer.domain.enums.ClaimStatus;
import com.inin.aiinterviewer.domain.model.ClaimLedger;
import com.inin.aiinterviewer.domain.model.InterviewClaim;
import com.inin.aiinterviewer.infrastructure.database.mapper.InterviewClaimMapper;
import com.inin.aiinterviewer.infrastructure.database.mapper.InterviewMessageMapper;
import com.inin.aiinterviewer.infrastructure.database.mapper.InterviewSessionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ClaimLedgerService {
    private final InterviewClaimMapper claimMapper;
    private final InterviewMessageMapper messageMapper;
    private final InterviewSessionMapper sessionMapper;
    private final ObjectMapper objectMapper;

    public ClaimLedgerService(
            InterviewClaimMapper claimMapper,
            InterviewMessageMapper messageMapper,
            InterviewSessionMapper sessionMapper,
            ObjectMapper objectMapper
    ) {
        this.claimMapper = claimMapper;
        this.messageMapper = messageMapper;
        this.sessionMapper = sessionMapper;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ClaimLedger recordLatestAnswer(long userId, long sessionId, ClaimExtractionResult extraction) {
        requireSession(userId, sessionId);
        long sourceMessageId = messageMapper.findLatestUserMessage(userId, sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_STATE)).getId();
        return record(userId, sessionId, sourceMessageId, extraction);
    }

    @Transactional
    public ClaimLedger record(
            long userId, long sessionId, long sourceMessageId, ClaimExtractionResult extraction
    ) {
        requireSession(userId, sessionId);
        messageMapper.findById(sourceMessageId, userId, sessionId)
                .filter(message -> message.getRole() == com.inin.aiinterviewer.domain.model.Message.Role.USER)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_STATE));
        if (extraction == null || extraction.degraded()) return ledger(userId, sessionId);
        if (extraction.claims().size() > 12) throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        for (ClaimExtractionResult.ClaimCandidate candidate : extraction.claims()) {
            validate(candidate);
        }
        claimMapper.deleteBySourceMessage(userId, sessionId, sourceMessageId);
        for (ClaimExtractionResult.ClaimCandidate candidate : extraction.claims()) {
            claimMapper.insert(entity(userId, sessionId, sourceMessageId, candidate));
        }
        return ledger(userId, sessionId);
    }

    @Transactional(readOnly = true)
    public ClaimLedger ledger(long userId, long sessionId) {
        requireSession(userId, sessionId);
        return new ClaimLedger(claimMapper.findAll(userId, sessionId).stream().map(this::toModel).toList());
    }

    @Transactional(readOnly = true)
    public String compactSummary(long userId, long sessionId) {
        List<InterviewClaim> pending = ledger(userId, sessionId).pendingVerification();
        if (pending.size() > 12) pending = pending.subList(0, 12);
        return write(pending);
    }

    @Transactional
    public void deleteBySession(long userId, long sessionId) {
        requireSession(userId, sessionId);
        claimMapper.deleteBySession(userId, sessionId);
    }

    private InterviewClaimEntity entity(
            long userId,
            long sessionId,
            long sourceMessageId,
            ClaimExtractionResult.ClaimCandidate candidate
    ) {
        InterviewClaimEntity entity = new InterviewClaimEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setUserId(userId);
        entity.setSessionId(sessionId);
        entity.setSourceMessageId(sourceMessageId);
        entity.setClaimType(candidate.type());
        entity.setContent(candidate.content().strip());
        entity.setImportance(candidate.importance());
        entity.setCredibility(candidate.credibility());
        entity.setStatus(ClaimStatus.UNVERIFIED);
        entity.setMissingEvidenceJson(write(candidate.missingEvidence()));
        entity.setSupportingEvidenceIdsJson("[]");
        entity.setConflictingEvidenceIdsJson("[]");
        return entity;
    }

    private void validate(ClaimExtractionResult.ClaimCandidate candidate) {
        if (candidate == null || candidate.type() == null || candidate.content() == null
                || candidate.content().isBlank() || candidate.content().length() > 1_000
                || !Double.isFinite(candidate.importance()) || candidate.importance() < 0
                || candidate.importance() > 1 || !Double.isFinite(candidate.credibility())
                || candidate.credibility() < 0 || candidate.credibility() > 1
                || candidate.missingEvidence().size() > 10) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
    }

    private InterviewClaim toModel(InterviewClaimEntity entity) {
        return new InterviewClaim(
                entity.getId(), entity.getSessionId(), entity.getSourceMessageId(), entity.getClaimType(),
                entity.getContent(), entity.getImportance(), entity.getCredibility(), entity.getStatus(),
                readList(entity.getMissingEvidenceJson()), readList(entity.getSupportingEvidenceIdsJson()),
                readList(entity.getConflictingEvidenceIdsJson()), entity.getCreateTime(), entity.getUpdateTime());
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
}
