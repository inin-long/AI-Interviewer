package com.inin.aiinterviewer.application.service;

import com.inin.aiinterviewer.application.dto.InterviewPlanDto;
import com.inin.aiinterviewer.application.exception.BusinessException;
import com.inin.aiinterviewer.application.exception.ErrorCode;
import com.inin.aiinterviewer.domain.entity.DeferredProbeEntity;
import com.inin.aiinterviewer.domain.enums.ClaimType;
import com.inin.aiinterviewer.domain.enums.InterviewStage;
import com.inin.aiinterviewer.domain.enums.ProbeStrategy;
import com.inin.aiinterviewer.domain.model.DeferredProbe;
import com.inin.aiinterviewer.domain.model.InterviewClaim;
import com.inin.aiinterviewer.infrastructure.database.mapper.DeferredProbeMapper;
import com.inin.aiinterviewer.infrastructure.database.mapper.InterviewMessageMapper;
import com.inin.aiinterviewer.infrastructure.database.mapper.InterviewSessionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class DeferredProbeService {

    private final DeferredProbeMapper probeMapper;
    private final InterviewSessionMapper sessionMapper;
    private final InterviewMessageMapper messageMapper;
    private final ClaimLedgerService claimLedgerService;

    public DeferredProbeService(
            DeferredProbeMapper probeMapper,
            InterviewSessionMapper sessionMapper,
            InterviewMessageMapper messageMapper,
            ClaimLedgerService claimLedgerService
    ) {
        this.probeMapper = probeMapper;
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
        this.claimLedgerService = claimLedgerService;
    }

    @Transactional
    public List<DeferredProbe> scheduleLatestAnswer(
            long userId,
            long sessionId,
            InterviewPlanDto plan,
            InterviewStage currentStage,
            boolean consistencyCheckDegraded
    ) {
        requireSession(userId, sessionId);
        long latestMessageId = messageMapper.findLatestUserMessage(userId, sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_STATE)).getId();
        List<InterviewClaim> currentClaims = claimLedgerService.ledger(userId, sessionId).claims().stream()
                .filter(claim -> claim.sourceMessageId() == latestMessageId).toList();
        List<InterviewStage> stages = configuredStages(plan.stages());
        for (InterviewClaim claim : currentClaims) {
            ScheduleTarget target = target(claim);
            if (target == null || claim.importance() < 0.75 || claim.missingEvidence().isEmpty()
                    || !isFutureStage(stages, currentStage, target.stage())) {
                continue;
            }
            schedule(userId, sessionId, claim, target.stage(), target.strategy(),
                    "将主张“%s”延迟到 %s 阶段验证：%s".formatted(
                            abbreviate(claim.content()), target.stage(),
                            String.join("、", claim.missingEvidence())));
        }
        if (consistencyCheckDegraded && !currentClaims.isEmpty()) {
            InterviewStage retryStage = nextSubstantiveStage(stages, currentStage);
            if (retryStage != null) {
                InterviewClaim target = currentClaims.stream()
                        .max(Comparator.comparingDouble(InterviewClaim::importance)).orElseThrow();
                schedule(userId, sessionId, target, retryStage, ProbeStrategy.CROSS_CHECK_HISTORY,
                        "一致性检查暂时不可用，延迟到后续阶段交叉验证该主张");
            }
        }
        return all(userId, sessionId);
    }

    @Transactional
    public List<DeferredProbe> markCompleted(long userId, long sessionId, String probeId) {
        requireSession(userId, sessionId);
        if (probeId == null || probeId.isBlank()) return all(userId, sessionId);
        if (probeMapper.markCompleted(probeId, userId, sessionId) != 1) {
            boolean alreadyCompleted = probeMapper.findById(probeId, userId, sessionId)
                    .filter(entity -> Boolean.TRUE.equals(entity.getCompleted())).isPresent();
            if (!alreadyCompleted) throw new BusinessException(ErrorCode.INVALID_STATE);
        }
        return all(userId, sessionId);
    }

    @Transactional(readOnly = true)
    public List<DeferredProbe> all(long userId, long sessionId) {
        requireSession(userId, sessionId);
        return probeMapper.findAll(userId, sessionId).stream().map(this::toModel).toList();
    }

    @Transactional(readOnly = true)
    public List<DeferredProbe> pending(long userId, long sessionId) {
        return all(userId, sessionId).stream().filter(probe -> !probe.completed()).toList();
    }

    private void schedule(
            long userId,
            long sessionId,
            InterviewClaim claim,
            InterviewStage preferredStage,
            ProbeStrategy strategy,
            String reason
    ) {
        if (claim == null || claim.id().isBlank() || preferredStage == null || strategy == null
                || reason == null || reason.isBlank() || reason.length() > 1_000) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        DeferredProbeEntity entity = new DeferredProbeEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setUserId(userId);
        entity.setSessionId(sessionId);
        entity.setTargetClaimId(claim.id());
        entity.setPreferredStage(preferredStage);
        entity.setStrategy(strategy);
        entity.setReason(reason.strip());
        entity.setCompleted(false);
        probeMapper.insertIfAbsent(entity);
    }

    private ScheduleTarget target(InterviewClaim claim) {
        return switch (claim.type()) {
            case OWNERSHIP -> new ScheduleTarget(
                    InterviewStage.PROJECT_EXPERIENCE, ProbeStrategy.VERIFY_PERSONAL_OWNERSHIP);
            case METRIC, RESULT -> new ScheduleTarget(
                    InterviewStage.TECHNICAL_DEEP_DIVE, ProbeStrategy.REQUEST_METRIC_BREAKDOWN);
            case FAILURE -> new ScheduleTarget(
                    InterviewStage.SYSTEM_DESIGN, ProbeStrategy.INTRODUCE_FAILURE);
            case DECISION -> new ScheduleTarget(
                    InterviewStage.SYSTEM_DESIGN, ProbeStrategy.ASK_TRADE_OFF);
            case CONSTRAINT -> new ScheduleTarget(
                    InterviewStage.SYSTEM_DESIGN, ProbeStrategy.INTRODUCE_CONSTRAINT);
            case CAUSALITY -> new ScheduleTarget(
                    InterviewStage.TECHNICAL_DEEP_DIVE, ProbeStrategy.TRACE_CAUSAL_CHAIN);
            case OPINION -> new ScheduleTarget(
                    InterviewStage.BEHAVIORAL, ProbeStrategy.CHALLENGE_ASSUMPTION);
            case FACT -> null;
        };
    }

    private boolean isFutureStage(
            List<InterviewStage> stages,
            InterviewStage current,
            InterviewStage preferred
    ) {
        int currentIndex = stages.indexOf(current);
        int preferredIndex = stages.indexOf(preferred);
        return preferredIndex >= 0 && (currentIndex < 0 || preferredIndex > currentIndex);
    }

    private InterviewStage nextSubstantiveStage(List<InterviewStage> stages, InterviewStage current) {
        int start = Math.max(-1, stages.indexOf(current));
        for (int index = start + 1; index < stages.size(); index++) {
            InterviewStage stage = stages.get(index);
            if (stage != InterviewStage.SUMMARY && stage != InterviewStage.COMPLETED) return stage;
        }
        return null;
    }

    private List<InterviewStage> configuredStages(List<String> values) {
        if (values == null) return List.of();
        List<InterviewStage> stages = new ArrayList<>();
        for (String value : values) {
            try {
                InterviewStage stage = InterviewStage.valueOf(value);
                if (!stages.contains(stage)) stages.add(stage);
            } catch (IllegalArgumentException ignored) {
                // Ignore old stage values in persisted plans.
            }
        }
        return List.copyOf(stages);
    }

    private DeferredProbe toModel(DeferredProbeEntity entity) {
        return new DeferredProbe(
                entity.getId(), entity.getSessionId(), entity.getTargetClaimId(),
                entity.getPreferredStage(), entity.getStrategy(), entity.getReason(),
                Boolean.TRUE.equals(entity.getCompleted()), entity.getCreateTime(), entity.getUpdateTime());
    }

    private void requireSession(long userId, long sessionId) {
        if (sessionMapper.findByIdAndUserId(sessionId, userId).isEmpty()) {
            throw new BusinessException(ErrorCode.INTERVIEW_NOT_FOUND);
        }
    }

    private String abbreviate(String content) {
        String compact = content == null ? "" : content.replaceAll("\\s+", " ").strip();
        return compact.length() <= 80 ? compact : compact.substring(0, 80) + "…";
    }

    private record ScheduleTarget(InterviewStage stage, ProbeStrategy strategy) { }
}
