package com.inin.aiinterviewer.application.service;

import com.inin.aiinterviewer.agent.model.EvaluationPayload;
import com.inin.aiinterviewer.application.dto.InterviewPlanDto;
import com.inin.aiinterviewer.application.dto.InterviewReportDto;
import com.inin.aiinterviewer.application.dto.InterviewSessionDto;
import com.inin.aiinterviewer.domain.enums.InterviewDifficulty;
import com.inin.aiinterviewer.domain.enums.InterviewStage;
import com.inin.aiinterviewer.domain.enums.InterviewStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TrainingProgressServiceTest {

    @Test
    void comparesOnlyCompletedTrainingSessionsOwnedByTheSourceInterview() {
        InterviewSessionService sessions = mock(InterviewSessionService.class);
        InterviewResultService results = mock(InterviewResultService.class);
        TrainingProgressService service = new TrainingProgressService(sessions, results);
        long userId = 7;
        long sourceId = 11;
        var sourceSession = session(sourceId, InterviewStatus.COMPLETED, Map.of());
        var completedTraining = session(12, InterviewStatus.COMPLETED,
                Map.of(TrainingRecommendationService.SOURCE_SESSION_RULE, sourceId));
        var runningTraining = session(13, InterviewStatus.RUNNING,
                Map.of(TrainingRecommendationService.SOURCE_SESSION_RULE, sourceId));
        var unrelated = session(14, InterviewStatus.COMPLETED,
                Map.of(TrainingRecommendationService.SOURCE_SESSION_RULE, 99));
        when(sessions.require(userId, sourceId)).thenReturn(sourceSession);
        when(sessions.list(userId)).thenReturn(List.of(
                completedTraining, runningTraining, unrelated, sourceSession));

        var sourceReport = report(sourceId, 70, 68, true);
        var retestReport = report(12, 82, 80, true);
        when(results.find(userId, sourceId)).thenReturn(Optional.of(sourceReport));
        when(results.find(userId, 12)).thenReturn(Optional.of(retestReport));

        assertThat(service.find(userId, sourceId)).singleElement().satisfies(change -> {
            assertThat(change.sessionId()).isEqualTo(12);
            assertThat(change.sourceScore()).isEqualTo(70);
            assertThat(change.retestScore()).isEqualTo(82);
            assertThat(change.overallDelta()).isEqualTo(12);
            assertThat(change.dimensions()).anySatisfy(dimension -> {
                assertThat(dimension.key()).isEqualTo(EvidenceScoreAggregator.TECHNICAL);
                assertThat(dimension.delta()).isEqualTo(12);
            });
        });
    }

    @Test
    void doesNotManufactureScoreChangesForInsufficientEvidence() {
        InterviewSessionService sessions = mock(InterviewSessionService.class);
        InterviewResultService results = mock(InterviewResultService.class);
        TrainingProgressService service = new TrainingProgressService(sessions, results);
        var sourceSession = session(21, InterviewStatus.COMPLETED, Map.of());
        var training = session(22, InterviewStatus.COMPLETED,
                Map.of(TrainingRecommendationService.SOURCE_SESSION_RULE, "21"));
        when(sessions.require(3, 21)).thenReturn(sourceSession);
        when(sessions.list(3)).thenReturn(List.of(training));
        when(results.find(3, 21)).thenReturn(Optional.of(report(21, 50, 50, false)));
        when(results.find(3, 22)).thenReturn(Optional.of(report(22, 78, 76, true)));

        assertThat(service.find(3, 21)).singleElement().satisfies(change -> {
            assertThat(change.sourceScored()).isFalse();
            assertThat(change.overallDelta()).isNull();
            assertThat(change.dimensions()).isEmpty();
        });
    }

    private InterviewSessionDto session(long id, InterviewStatus status, Map<String, Object> rules) {
        LocalDateTime now = LocalDateTime.of(2026, 7, 17, 10, 0).plusMinutes(id);
        InterviewPlanDto plan = new InterviewPlanDto(
                id, "专项训练", "Java 工程师", "", InterviewDifficulty.MEDIUM,
                30, 5, null, rules, List.of("INTRODUCTION", "SUMMARY"),
                false, now, now);
        return new InterviewSessionDto(
                id, id, null, null, "专项训练 " + id, "Java 工程师", plan,
                null, List.of(), InterviewStage.COMPLETED, status, "v2", now,
                status == InterviewStatus.COMPLETED ? now : null, now, now);
    }

    private InterviewReportDto report(long interviewId, int overall, int technical, boolean scored) {
        EvaluationPayload.EvidenceTrace trace = new EvaluationPayload.EvidenceTrace(
                scored, scored ? 0.8 : 0, scored ? List.of("e-" + interviewId) : List.of(),
                scored ? List.of(interviewId) : List.of(),
                scored ? List.of("c-" + interviewId) : List.of(), "证据汇总");
        Map<String, EvaluationPayload.EvidenceTrace> traces = Map.of(
                EvidenceScoreAggregator.OVERALL, trace,
                EvidenceScoreAggregator.TECHNICAL, trace);
        return new InterviewReportDto(
                interviewId, interviewId, "报告", overall,
                Map.of(EvidenceScoreAggregator.TECHNICAL, technical), "", "",
                Map.of(), List.of(), traces, scored ? 0.8 : 0, scored);
    }
}
