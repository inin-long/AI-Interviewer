package com.inin.aiinterviewer.application.service;

import com.inin.aiinterviewer.application.dto.InterviewHistoryDetailDto;
import com.inin.aiinterviewer.application.dto.InterviewHistoryItemDto;
import com.inin.aiinterviewer.application.exception.BusinessException;
import com.inin.aiinterviewer.application.exception.ErrorCode;
import com.inin.aiinterviewer.domain.entity.InterviewSessionEntity;
import com.inin.aiinterviewer.domain.enums.InterviewStatus;
import com.inin.aiinterviewer.infrastructure.database.mapper.InterviewSessionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class InterviewHistoryService {
    private static final Logger log = LoggerFactory.getLogger(InterviewHistoryService.class);
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 300;

    private final InterviewSessionService sessionService;
    private final InterviewResultService resultService;
    private final InterviewSessionMapper sessionMapper;

    public InterviewHistoryService(
            InterviewSessionService sessionService,
            InterviewResultService resultService,
            InterviewSessionMapper sessionMapper
    ) {
        this.sessionService = sessionService;
        this.resultService = resultService;
        this.sessionMapper = sessionMapper;
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

    /**
     * 删除面试记录，带重试机制应对 SQLite 并发锁冲突。
     * 使用轻量级查询获取状态（避免 JSON 反序列化带来的潜在异常）。
     * 如果面试正在进行中，先尝试暂停以释放后台任务。
     * 注意：本方法刻意不加 @Transactional，使每次重试都是独立事务，
     * 避免事务跨越 quietSleep 时长期持有连接/锁反而更易触发 database is locked。
     */
    public void delete(long userId, long sessionId) {
        log.info("[DELETE] 入口: userId={}, sessionId={}", userId, sessionId);

        // 轻量级查询状态：只读 session 表，不反序列化 JSON 快照
        InterviewStatus status = queryStatusOnly(userId, sessionId);
        log.info("[DELETE] 查询状态结果: {}", status);

        // 如果正在运行，先尝试暂停以释放后台任务
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

        // 带重试的删除操作
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
        // 直接抛出原始异常，保留 ApplicationException 类型（BusinessException/SystemException）
        // 不再用 RuntimeException 包装，否则 GlobalExceptionHandler.toUserMessage 会兜底成"系统发生未知错误"
        if (lastError instanceof RuntimeException rt) {
            throw rt;
        }
        throw new RuntimeException("删除面试记录失败(已重试" + MAX_RETRIES + "次)", lastError);
    }

    /**
     * 轻量级状态查询：仅查 interview_session 表的状态字段，
     * 避免 require() 触发 plan/profile/knowledge 的 JSON 反序列化。
     */
    private InterviewStatus queryStatusOnly(long userId, long sessionId) {
        try {
            InterviewSessionEntity entity = sessionMapper.findByIdAndUserId(sessionId, userId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.INTERVIEW_NOT_FOUND));
            return entity.getStatus();
        } catch (BusinessException e) {
            throw e;  // 会话不存在，直接向上抛
        } catch (Exception e) {
            // 状态查询本身失败（极少见），返回 null 让后续删除逻辑自行处理
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
