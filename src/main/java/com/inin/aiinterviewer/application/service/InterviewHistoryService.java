package com.inin.aiinterviewer.application.service;

import com.inin.aiinterviewer.application.dto.InterviewHistoryDetailDto;
import com.inin.aiinterviewer.application.dto.InterviewHistoryItemDto;
import com.inin.aiinterviewer.application.dto.InterviewPlanDto;
import com.inin.aiinterviewer.application.dto.InterviewReportDto;
import com.inin.aiinterviewer.application.dto.InterviewReportStateDto;
import com.inin.aiinterviewer.application.dto.InterviewSessionDto;
import com.inin.aiinterviewer.application.dto.ResumeDto;
import com.inin.aiinterviewer.application.exception.BusinessException;
import com.inin.aiinterviewer.application.exception.ErrorCode;
import com.inin.aiinterviewer.domain.entity.InterviewSessionEntity;
import com.inin.aiinterviewer.domain.enums.InterviewStatus;
import com.inin.aiinterviewer.domain.enums.ReportStatus;
import com.inin.aiinterviewer.infrastructure.database.mapper.InterviewSessionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class InterviewHistoryService {
    private static final Logger log = LoggerFactory.getLogger(InterviewHistoryService.class);
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 300;

    private final InterviewSessionService sessionService;
    private final InterviewResultService resultService;
    private final ResumeService resumeService;
    private final InterviewSessionMapper sessionMapper;

    public InterviewHistoryService(
            InterviewSessionService sessionService,
            InterviewResultService resultService,
            ResumeService resumeService,
            InterviewSessionMapper sessionMapper
    ) {
        this.sessionService = sessionService;
        this.resultService = resultService;
        this.resumeService = resumeService;
        this.sessionMapper = sessionMapper;
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

    /**
     * 删除面试记录，带重试机制应对 SQLite 并发锁冲突。
     * 使用轻量级查询获取状态（避免 JSON 反序列化带来的潜在异常）。
     * 如果面试正在进行中，先尝试暂停以释放后台任务。
     * 注意：本方法刻意不加 @Transactional，使每次重试都是独立事务，
     * 避免事务跨越 quietSleep 时长期持有连接/锁反而更易触发 database is locked。
     */
    public void delete(long userId, long sessionId) {
        log.info("[DELETE] 入口: userId={}, sessionId={}", userId, sessionId);

        InterviewStatus status = queryStatusOnly(userId, sessionId);
        log.info("[DELETE] 查询状态结果: {}", status);

        if (status == InterviewStatus.RUNNING) {
            log.info("[DELETE] 面试进行中，尝试暂停: sessionId={}", sessionId);
            try {
                sessionService.pause(userId, sessionId);
                log.info("[DELETE] 已暂停面试: sessionId={}", sessionId);
            } catch (Exception pauseEx) {
                log.warn("[DELETE] 暂停失败，继续尝试删除: sessionId={}, 错误: {}", sessionId, pauseEx.getMessage());
            }
            quietSleep(500);
        }

        Exception lastError = null;
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                log.info("[DELETE] 尝试第 {}/{} 次: sessionId={}", attempt, MAX_RETRIES, sessionId);
                sessionService.delete(userId, sessionId);
                log.info("[DELETE] 成功: sessionId={}", sessionId);
                return;
            } catch (Exception ex) {
                lastError = ex;
                log.warn("[DELETE] 第 {} 次失败: sessionId={}, 异常类型={}, 消息={}",
                        attempt, sessionId, ex.getClass().getSimpleName(), ex.getMessage());
                if (attempt < MAX_RETRIES) {
                    quietSleep(RETRY_DELAY_MS * attempt);
                }
            }
        }
        if (lastError instanceof RuntimeException rt) {
            throw rt;
        }
        throw new RuntimeException("删除面试记录失败(已重试" + MAX_RETRIES + "次)", lastError);
    }

    private InterviewStatus queryStatusOnly(long userId, long sessionId) {
        try {
            InterviewSessionEntity entity = sessionMapper.findByIdAndUserId(sessionId, userId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.INTERVIEW_NOT_FOUND));
            return entity.getStatus();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("[DELETE] 状态查询异常（继续尝试删除）: sessionId={}, 错误: {}", sessionId, e.getMessage());
            return null;
        }
    }

    private void quietSleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
