package com.inin.aiinterviewer.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inin.aiinterviewer.agent.model.EvidenceCollectionResult;
import com.inin.aiinterviewer.application.exception.BusinessException;
import com.inin.aiinterviewer.application.exception.ErrorCode;
import com.inin.aiinterviewer.application.exception.SystemException;
import com.inin.aiinterviewer.domain.entity.EvaluationEvidenceEntity;
import com.inin.aiinterviewer.domain.model.EvaluationEvidence;
import com.inin.aiinterviewer.domain.model.EvidenceLedger;
import com.inin.aiinterviewer.domain.model.Message;
import com.inin.aiinterviewer.infrastructure.database.mapper.EvaluationEvidenceMapper;
import com.inin.aiinterviewer.infrastructure.database.mapper.InterviewClaimMapper;
import com.inin.aiinterviewer.infrastructure.database.mapper.InterviewMessageMapper;
import com.inin.aiinterviewer.infrastructure.database.mapper.InterviewSessionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class EvidenceLedgerService {

    private final EvaluationEvidenceMapper evidenceMapper;
    private final InterviewClaimMapper claimMapper;
    private final InterviewMessageMapper messageMapper;
    private final InterviewSessionMapper sessionMapper;
    private final ObjectMapper objectMapper;

    public EvidenceLedgerService(
            EvaluationEvidenceMapper evidenceMapper,
            InterviewClaimMapper claimMapper,
            InterviewMessageMapper messageMapper,
            InterviewSessionMapper sessionMapper,
            ObjectMapper objectMapper
    ) {
        this.evidenceMapper = evidenceMapper;
        this.claimMapper = claimMapper;
        this.messageMapper = messageMapper;
        this.sessionMapper = sessionMapper;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public EvidenceLedger recordLatestAnswer(
            long userId,
            long sessionId,
            EvidenceCollectionResult result
    ) {
        requireSession(userId, sessionId);
        long messageId = messageMapper.findLatestUserMessage(userId, sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_STATE)).getId();
        return record(userId, sessionId, messageId, result);
    }

    @Transactional
    public EvidenceLedger record(
            long userId,
            long sessionId,
            long messageId,
            EvidenceCollectionResult result
    ) {
        requireSession(userId, sessionId);
        messageMapper.findById(messageId, userId, sessionId)
                .filter(message -> message.getRole() == Message.Role.USER)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_STATE));
        if (result == null || result.degraded()) return ledger(userId, sessionId);
        if (result.evidence().size() > 12) throw new BusinessException(ErrorCode.VALIDATION_FAILED);

        var claims = claimMapper.findAll(userId, sessionId);
        Set<String> allowedClaimIds = claims.stream()
                .map(entity -> entity.getId()).collect(java.util.stream.Collectors.toSet());
        List<String> sourceClaimIds = claims.stream()
                .filter(entity -> entity.getSourceMessageId() == messageId)
                .map(entity -> entity.getId()).toList();
        LinkedHashMap<String, EvidenceCollectionResult.EvidenceCandidate> unique = new LinkedHashMap<>();
        for (EvidenceCollectionResult.EvidenceCandidate candidate : result.evidence()) {
            validate(candidate);
            unique.putIfAbsent(candidate.competencyCode() + "\u0000" + candidate.reason(), candidate);
        }

        evidenceMapper.deleteByMessage(userId, sessionId, messageId);
        for (EvidenceCollectionResult.EvidenceCandidate candidate : unique.values()) {
            evidenceMapper.insert(entity(
                    userId, sessionId, messageId, candidate, allowedClaimIds, sourceClaimIds));
        }
        return ledger(userId, sessionId);
    }

    @Transactional(readOnly = true)
    public EvidenceLedger ledger(long userId, long sessionId) {
        requireSession(userId, sessionId);
        return new EvidenceLedger(evidenceMapper.findAll(userId, sessionId).stream()
                .map(this::toModel).toList());
    }

    @Transactional(readOnly = true)
    public String compactSummary(long userId, long sessionId) {
        List<EvaluationEvidence> values = ledger(userId, sessionId).evidence();
        if (values.size() > 40) values = values.subList(values.size() - 40, values.size());
        return write(values);
    }

    @Transactional
    public void deleteBySession(long userId, long sessionId) {
        requireSession(userId, sessionId);
        evidenceMapper.deleteBySession(userId, sessionId);
    }

    private EvaluationEvidenceEntity entity(
            long userId,
            long sessionId,
            long messageId,
            EvidenceCollectionResult.EvidenceCandidate candidate,
            Set<String> allowedClaimIds,
            List<String> sourceClaimIds
    ) {
        LinkedHashSet<String> claimIds = new LinkedHashSet<>();
        for (String claimId : candidate.relatedClaimIds()) {
            if (claimId != null && allowedClaimIds.contains(claimId)) claimIds.add(claimId);
        }
        // A structured evidence item must remain traceable even when the provider omits
        // relatedClaimIds. The claims extracted from the same answer are the only safe fallback.
        if (claimIds.isEmpty()) claimIds.addAll(sourceClaimIds);
        EvaluationEvidenceEntity entity = new EvaluationEvidenceEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setUserId(userId);
        entity.setSessionId(sessionId);
        entity.setMessageId(messageId);
        entity.setCompetencyCode(candidate.competencyCode());
        entity.setSignal(candidate.signal());
        entity.setStrength(candidate.strength());
        entity.setConfidence(candidate.confidence());
        entity.setReason(candidate.reason());
        entity.setRelatedClaimIdsJson(write(List.copyOf(claimIds)));
        return entity;
    }

    private void validate(EvidenceCollectionResult.EvidenceCandidate candidate) {
        if (candidate == null || candidate.signal() == null
                || !candidate.competencyCode().matches("[A-Z][A-Z0-9_]{1,63}")
                || !Double.isFinite(candidate.strength()) || candidate.strength() < 0
                || candidate.strength() > 1 || !Double.isFinite(candidate.confidence())
                || candidate.confidence() < 0 || candidate.confidence() > 1
                || candidate.reason().isBlank() || candidate.reason().length() > 1_000
                || candidate.relatedClaimIds().size() > 12) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
    }

    private EvaluationEvidence toModel(EvaluationEvidenceEntity entity) {
        return new EvaluationEvidence(
                entity.getId(), entity.getSessionId(), entity.getMessageId(), entity.getCompetencyCode(),
                entity.getSignal(), entity.getStrength(), entity.getConfidence(), entity.getReason(),
                readList(entity.getRelatedClaimIdsJson()), entity.getCreateTime());
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
