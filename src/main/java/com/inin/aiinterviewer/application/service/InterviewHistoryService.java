package com.inin.aiinterviewer.application.service;

import com.inin.aiinterviewer.application.dto.InterviewHistoryDetailDto;
import com.inin.aiinterviewer.application.dto.InterviewHistoryItemDto;
import com.inin.aiinterviewer.application.dto.InterviewPlanDto;
import com.inin.aiinterviewer.application.dto.InterviewReportDto;
import com.inin.aiinterviewer.application.dto.InterviewReportStateDto;
import com.inin.aiinterviewer.application.dto.InterviewSessionDto;
import com.inin.aiinterviewer.application.dto.ResumeDto;
import com.inin.aiinterviewer.domain.enums.InterviewStatus;
import com.inin.aiinterviewer.domain.enums.ReportStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class InterviewHistoryService {
    private final InterviewSessionService sessionService;
    private final InterviewResultService resultService;
    private final ResumeService resumeService;

    public InterviewHistoryService(
            InterviewSessionService sessionService,
            InterviewResultService resultService,
            ResumeService resumeService
    ) {
        this.sessionService = sessionService;
        this.resultService = resultService;
        this.resumeService = resumeService;
    }

    @Transactional(readOnly = true)
    public List<InterviewHistoryItemDto> list(long userId, String keyword, InterviewStatus status) {
        String normalized = keyword == null ? "" : keyword.strip().toLowerCase(Locale.ROOT);
        List<InterviewSessionDto> sessions = sessionService.list(userId);
        java.util.Map<Long, String> resumeNames = resumeService.list(userId).stream()
                .filter(resume -> resume.id() != null)
                .collect(java.util.stream.Collectors.toMap(ResumeDto::id, ResumeDto::originalName, (a, b) -> a));
        return sessions.stream()
                .filter(session -> status == null || session.status() == status)
                .filter(session -> normalized.isBlank()
                        || contains(session.title(), normalized)
                        || contains(session.jobTitle(), normalized))
                .map(session -> toItem(userId, session, resumeNames))
                .toList();
    }

    @Transactional(readOnly = true)
    public InterviewHistoryDetailDto detail(long userId, long sessionId) {
        return new InterviewHistoryDetailDto(
                sessionService.require(userId, sessionId),
                sessionService.messages(userId, sessionId),
                resultService.find(userId, sessionId));
    }

    private InterviewHistoryItemDto toItem(long userId, InterviewSessionDto session, java.util.Map<Long, String> resumeNames) {
        Optional<InterviewReportDto> report = resultService.find(userId, session.id());
        InterviewReportStateDto reportState = resultService.state(userId, session.id());
        String resumeName = session.resumeId() == null ? null : resumeNames.getOrDefault(session.resumeId(), null);
        String planIconPath = planIconPath(session);
        List<String> tags = buildTags(session);
        List<String> sessionStages = sessionStages(session);
        String summary = report.map(InterviewReportDto::summary).orElse(null);
        String reportStatusText = reportStatusText(reportState.status());
        return new InterviewHistoryItemDto(
                session.id(), session.title(), session.jobTitle(), session.status(), session.stage(),
                session.startedTime(), session.completedTime(), session.updateTime(),
                sessionService.messages(userId, session.id()).size(), report.isPresent(),
                report.map(InterviewReportDto::overallScore).orElse(null),
                planIconPath, resumeName, tags, sessionStages, summary, reportStatusText);
    }

    private String planIconPath(InterviewSessionDto session) {
        InterviewPlanDto plan = session.planSnapshot();
        if (plan == null) return null;
        Object value = plan.rules() == null ? null : plan.rules().get(InterviewPlanAssetService.ICON_PATH_RULE);
        if (value == null) return null;
        String text = String.valueOf(value);
        return text.isBlank() ? null : text;
    }

    private List<String> buildTags(InterviewSessionDto session) {
        List<String> tags = new ArrayList<>();
        InterviewPlanDto plan = session.planSnapshot();
        if (plan != null) {
            for (String category : plan.knowledgeCategories()) {
                if (tags.size() >= 4) break;
                if (category != null && !category.isBlank()) tags.add(category);
            }
        }
        return tags;
    }

    private List<String> sessionStages(InterviewSessionDto session) {
        InterviewPlanDto plan = session.planSnapshot();
        if (plan == null) return List.of();
        return plan.stages() == null ? List.of() : plan.stages();
    }

    private String reportStatusText(ReportStatus status) {
        if (status == null) return "未生成";
        return switch (status) {
            case NOT_STARTED -> "未生成";
            case GENERATING -> "生成中";
            case FAILED -> "生成失败";
            case COMPLETED -> "已生成";
        };
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }

    @Transactional
    public void delete(long userId, long sessionId) {
        sessionService.delete(userId, sessionId);
    }
}
