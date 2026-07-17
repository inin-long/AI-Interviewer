package com.inin.aiinterviewer.application.service;

import com.inin.aiinterviewer.agent.model.EvaluationPayload;
import com.inin.aiinterviewer.application.dto.InterviewReportDto;
import com.inin.aiinterviewer.application.dto.InterviewSessionDto;
import com.inin.aiinterviewer.application.dto.TrainingScoreChangeDto;
import com.inin.aiinterviewer.domain.enums.InterviewStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class TrainingProgressService {

    private static final Map<String, String> DIMENSIONS;

    static {
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        values.put(EvidenceScoreAggregator.TECHNICAL, "技术能力");
        values.put(EvidenceScoreAggregator.PROBLEM_SOLVING, "问题解决");
        values.put(EvidenceScoreAggregator.PROJECT, "项目经验");
        values.put(EvidenceScoreAggregator.SYSTEM_DESIGN, "系统设计");
        values.put(EvidenceScoreAggregator.COMMUNICATION, "沟通能力");
        values.put(EvidenceScoreAggregator.COMPREHENSIVE, "综合评价");
        DIMENSIONS = java.util.Collections.unmodifiableMap(values);
    }

    private final InterviewSessionService sessionService;
    private final InterviewResultService resultService;

    public TrainingProgressService(
            InterviewSessionService sessionService,
            InterviewResultService resultService
    ) {
        this.sessionService = sessionService;
        this.resultService = resultService;
    }

    public List<TrainingScoreChangeDto> find(long userId, long sourceSessionId) {
        sessionService.require(userId, sourceSessionId);
        var source = resultService.find(userId, sourceSessionId);
        if (source.isEmpty()) return List.of();

        return sessionService.list(userId).stream()
                .filter(session -> session.id() != sourceSessionId)
                .filter(session -> session.status() == InterviewStatus.COMPLETED)
                .filter(session -> sourceSessionId(session) == sourceSessionId)
                .sorted(java.util.Comparator.comparing(
                        InterviewSessionDto::completedTime,
                        java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())))
                .flatMap(session -> resultService.find(userId, session.id()).stream()
                        .map(report -> change(source.get(), session, report)))
                .toList();
    }

    private TrainingScoreChangeDto change(
            InterviewReportDto source,
            InterviewSessionDto session,
            InterviewReportDto retest
    ) {
        boolean sourceScored = source.overallScored();
        boolean retestScored = retest.overallScored();
        Integer overallDelta = sourceScored && retestScored
                ? retest.overallScore() - source.overallScore() : null;
        List<TrainingScoreChangeDto.DimensionChange> dimensions = new ArrayList<>();
        DIMENSIONS.forEach((key, label) -> {
            if (scored(source, key) && scored(retest, key)) {
                int before = source.dimensions().getOrDefault(key, 0);
                int after = retest.dimensions().getOrDefault(key, 0);
                dimensions.add(new TrainingScoreChangeDto.DimensionChange(
                        key, label, before, after, after - before));
            }
        });
        return new TrainingScoreChangeDto(
                session.id(), session.title(), source.overallScore(), sourceScored,
                retest.overallScore(), retestScored, overallDelta, dimensions,
                session.completedTime() == null ? LocalDateTime.MIN : session.completedTime());
    }

    private boolean scored(InterviewReportDto report, String key) {
        if (report.scoreEvidence().isEmpty()) return true;
        EvaluationPayload.EvidenceTrace trace = report.scoreEvidence().get(key);
        return trace != null && trace.scored();
    }

    private long sourceSessionId(InterviewSessionDto session) {
        if (session.planSnapshot() == null || session.planSnapshot().rules() == null) return -1;
        Object value = session.planSnapshot().rules()
                .get(TrainingRecommendationService.SOURCE_SESSION_RULE);
        if (value instanceof Number number) return number.longValue();
        try {
            return value == null ? -1 : Long.parseLong(value.toString());
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }
}
