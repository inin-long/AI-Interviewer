package com.inin.aiinterviewer.application.service;

import com.inin.aiinterviewer.application.dto.InterviewHistoryDetailDto;
import com.inin.aiinterviewer.application.dto.InterviewHistoryItemDto;
import com.inin.aiinterviewer.domain.enums.InterviewStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class InterviewHistoryService {
    private final InterviewSessionService sessionService;
    private final InterviewResultService resultService;

    public InterviewHistoryService(
            InterviewSessionService sessionService,
            InterviewResultService resultService
    ) {
        this.sessionService = sessionService;
        this.resultService = resultService;
    }

    @Transactional(readOnly = true)
    public List<InterviewHistoryItemDto> list(long userId, String keyword, InterviewStatus status) {
        String normalized = keyword == null ? "" : keyword.strip().toLowerCase(Locale.ROOT);
        return sessionService.list(userId).stream()
                .filter(session -> status == null || session.status() == status)
                .filter(session -> normalized.isBlank()
                        || contains(session.title(), normalized)
                        || contains(session.jobTitle(), normalized))
                .map(session -> {
                    var report = resultService.find(userId, session.id());
                    return new InterviewHistoryItemDto(
                            session.id(), session.title(), session.jobTitle(), session.status(), session.stage(),
                            session.startedTime(), session.completedTime(), session.updateTime(),
                            sessionService.messages(userId, session.id()).size(), report.isPresent(),
                            report.map(value -> value.overallScore()).orElse(null));
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public InterviewHistoryDetailDto detail(long userId, long sessionId) {
        return new InterviewHistoryDetailDto(
                sessionService.require(userId, sessionId),
                sessionService.messages(userId, sessionId),
                resultService.find(userId, sessionId));
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }

    @Transactional
    public void delete(long userId, long sessionId) {
        sessionService.delete(userId, sessionId);
    }
}
