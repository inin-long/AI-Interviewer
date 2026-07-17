package com.inin.aiinterviewer.application.service;

import com.inin.aiinterviewer.application.dto.CoachingFeedbackDto;
import com.inin.aiinterviewer.domain.enums.EvidenceSignal;
import com.inin.aiinterviewer.domain.enums.InterviewMode;
import com.inin.aiinterviewer.domain.model.InterviewPlanSettings;
import com.inin.aiinterviewer.domain.model.Message;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Service
public class CoachingFeedbackService {

    private static final List<String> REFERENCE_STRUCTURE = List.of(
            "先交代场景、目标和关键约束",
            "明确个人职责、判断依据与取舍标准",
            "按顺序说明关键行动和实现细节",
            "给出结果、数据来源和验证方式",
            "补充风险、替代方案与事后反思");

    private final InterviewSessionService sessionService;
    private final EvidenceLedgerService evidenceLedgerService;

    public CoachingFeedbackService(
            InterviewSessionService sessionService,
            EvidenceLedgerService evidenceLedgerService
    ) {
        this.sessionService = sessionService;
        this.evidenceLedgerService = evidenceLedgerService;
    }

    @Transactional(readOnly = true)
    public CoachingFeedbackDto feedback(long userId, long sessionId) {
        var session = sessionService.require(userId, sessionId);
        if (InterviewPlanSettings.fromRules(session.planSnapshot().rules()).mode()
                != InterviewMode.COACHING) {
            return CoachingFeedbackDto.unavailable();
        }
        var state = sessionService.loadLatestState(userId, sessionId).orElse(null);
        if (state == null || state.latestAnswer() == null || state.latestAnswer().isBlank()) {
            return CoachingFeedbackDto.unavailable();
        }
        var messages = sessionService.messages(userId, sessionId);
        var latestUserMessage = messages.reversed().stream()
                .filter(message -> message.role() == Message.Role.USER)
                .findFirst().orElse(null);
        if (latestUserMessage == null) return CoachingFeedbackDto.unavailable();

        var evidence = evidenceLedgerService.ledger(userId, sessionId).evidence().stream()
                .filter(item -> item.messageId() == latestUserMessage.id()).toList();
        LinkedHashSet<String> covered = new LinkedHashSet<>();
        LinkedHashSet<String> missing = new LinkedHashSet<>();
        evidence.forEach(item -> {
            String content = item.competencyCode() + "：" + item.reason();
            if (item.signal() == EvidenceSignal.POSITIVE) covered.add(content);
            else missing.add(content);
        });
        if (state.analysis() != null) missing.addAll(state.analysis().missingPoints());

        List<String> gaps = state.logicChainResult().gaps().stream()
                .map(gap -> gap.description().isBlank() ? gap.type().name() : gap.description())
                .distinct().toList();
        missing.addAll(gaps);
        int answeredQuestions = (int) messages.stream()
                .filter(message -> message.role() == Message.Role.USER).count();
        String hint = hint(new ArrayList<>(missing), state.probePlan().expectedEvidence());
        return new CoachingFeedbackDto(
                true, answeredQuestions,
                covered.isEmpty() ? List.of("本轮尚未形成可确认的正向能力证据")
                        : List.copyOf(covered),
                missing.isEmpty() ? List.of("未发现必须补充的关键内容") : List.copyOf(missing),
                gaps.isEmpty() ? List.of("本轮未识别出明显逻辑缺口") : gaps,
                REFERENCE_STRUCTURE, hint, answeredQuestions > 0);
    }

    private String hint(List<String> missing, List<String> expectedEvidence) {
        if (!missing.isEmpty()) {
            return "优先补充“" + missing.getFirst() + "”，并说明它如何影响你的判断或结果。";
        }
        if (expectedEvidence != null && !expectedEvidence.isEmpty()) {
            return "可以从“" + String.join("、", expectedEvidence) + "”中选择一项，用具体事实展开。";
        }
        return "尝试用‘背景—职责—行动—取舍—结果—验证’顺序重新组织回答。";
    }
}
