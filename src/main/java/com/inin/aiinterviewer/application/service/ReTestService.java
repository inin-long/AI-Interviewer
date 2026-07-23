package com.inin.aiinterviewer.application.service;

import com.inin.aiinterviewer.application.dto.InterviewPlanDto;
import com.inin.aiinterviewer.application.dto.InterviewReportDto;
import com.inin.aiinterviewer.application.dto.InterviewSessionDto;
import com.inin.aiinterviewer.application.dto.SaveInterviewPlanCommand;
import com.inin.aiinterviewer.application.exception.BusinessException;
import com.inin.aiinterviewer.application.exception.ErrorCode;
import com.inin.aiinterviewer.domain.enums.InterviewDifficulty;
import com.inin.aiinterviewer.domain.enums.InterviewMode;
import com.inin.aiinterviewer.domain.enums.InterviewerPersona;
import com.inin.aiinterviewer.domain.enums.PressureLevel;
import com.inin.aiinterviewer.domain.enums.VerificationStrictness;
import com.inin.aiinterviewer.domain.model.InterviewPlanSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 复试服务：基于初试成绩判定资格，创建难度更高的复试计划。
 * <p>
 * 资格条件：初试模式为 FORMAL_SIMULATION，报告已完成且总分达到 70 分。
 * 复试计划会提升难度一级（JUNIOR→MEDIUM→SENIOR→EXPERT，EXPERT 保持），
 * 并在 rules 中记录源面试 ID，用于后续前后评分对比。
 */
@Service
public class ReTestService {

    private static final Logger log = LoggerFactory.getLogger(ReTestService.class);

    /** 初试总分达到此阈值才有资格进入复试 */
    public static final int RE_TEST_THRESHOLD = 70;

    /** rules 中记录源面试 ID 的 key */
    public static final String RE_TEST_SOURCE_SESSION_RULE = "reTestSourceSessionId";

    private final InterviewSessionService sessionService;
    private final InterviewResultService resultService;
    private final InterviewPlanService planService;

    public ReTestService(
            InterviewSessionService sessionService,
            InterviewResultService resultService,
            InterviewPlanService planService
    ) {
        this.sessionService = sessionService;
        this.resultService = resultService;
        this.planService = planService;
    }

    /**
     * 判断指定面试是否具备复试资格。
     *
     * @return 如果具备资格返回包含初试报告的 Optional，否则返回空
     */
    public Optional<InterviewReportDto> checkEligibility(long userId, long sessionId) {
        InterviewSessionDto session = sessionService.require(userId, sessionId);
        if (!isFormalSimulation(session)) {
            log.debug("Session {} is not FORMAL_SIMULATION, re-test not applicable", sessionId);
            return Optional.empty();
        }
        Optional<InterviewReportDto> report = resultService.find(userId, sessionId);
        if (report.isEmpty()) {
            log.debug("Session {} has no completed report, re-test not available", sessionId);
            return Optional.empty();
        }
        InterviewReportDto dto = report.get();
        if (!dto.overallScored() || dto.overallScore() < RE_TEST_THRESHOLD) {
            log.debug("Session {} overall score {} below threshold {}, re-test not eligible",
                    sessionId, dto.overallScore(), RE_TEST_THRESHOLD);
            return Optional.empty();
        }
        return report;
    }

    /**
     * 基于初试创建复试计划。难度提升一级，模式设为 RE_TEST。
     */
    public InterviewPlanDto createReTestPlan(long userId, long sourceSessionId) {
        InterviewReportDto sourceReport = checkEligibility(userId, sourceSessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_STATE));
        InterviewSessionDto sourceSession = sessionService.require(userId, sourceSessionId);
        InterviewPlanDto sourcePlan = sourceSession.planSnapshot();

        InterviewDifficulty upgraded = upgradeDifficulty(
                sourcePlan == null ? null : sourcePlan.difficulty());

        LinkedHashMap<String, Object> rules = new LinkedHashMap<>();
        if (sourcePlan != null && sourcePlan.rules() != null) {
            rules.putAll(sourcePlan.rules());
        }
        rules.put(RE_TEST_SOURCE_SESSION_RULE, sourceSessionId);
        rules.put(InterviewPlanSettings.MODE_KEY, InterviewMode.RE_TEST.name());
        // 复试使用更高压力和更严格验证
        rules.put(InterviewPlanSettings.PERSONA_KEY, InterviewerPersona.TECH_LEAD.name());
        rules.put(InterviewPlanSettings.PRESSURE_KEY, PressureLevel.HIGH_PRESSURE.name());
        rules.put(InterviewPlanSettings.STRICTNESS_KEY, VerificationStrictness.STRICT.name());

        InterviewPlanSettings settings = new InterviewPlanSettings(
                InterviewMode.RE_TEST,
                InterviewerPersona.TECH_LEAD,
                PressureLevel.HIGH_PRESSURE,
                VerificationStrictness.STRICT,
                0);
        Map<String, Object> mergedRules = settings.mergeInto(rules);

        String jobTitle = sourcePlan == null ? sourceSession.jobTitle() : sourcePlan.jobTitle();
        String jobDescription = sourcePlan == null ? "" : sourcePlan.jobDescription();
        String name = safeName(sourceSession.title()) + " · 复试";

        // 复试题量与初试相同，时长可适当延长
        int questionCount = sourcePlan == null ? 8 : sourcePlan.questionCount();
        int duration = sourcePlan == null ? 45 : sourcePlan.durationMinutes();
        Long resumeId = sourcePlan == null ? sourceSession.resumeId() : sourcePlan.resumeId();
        Long profileId = sourcePlan == null ? sourceSession.profileId() : sourcePlan.profileId();
        String domainPackId = sourcePlan == null ? null : sourcePlan.domainPackId();
        List<String> stages = sourcePlan == null
                ? List.of("INTRODUCTION", "TECHNICAL_DEEP_DIVE", "SYSTEM_DESIGN",
                        "PROJECT_EXPERIENCE", "SUMMARY")
                : sourcePlan.stages();

        return planService.create(userId, new SaveInterviewPlanCommand(
                name, jobTitle, jobDescription, upgraded, duration, questionCount,
                resumeId, profileId, List.of(), mergedRules, stages, domainPackId,
                sourcePlan == null ? List.of() : sourcePlan.knowledgeCategories()));
    }

    private boolean isFormalSimulation(InterviewSessionDto session) {
        if (session.planSnapshot() == null || session.planSnapshot().rules() == null) {
            return true; // 没有 rules 默认当作正式模拟
        }
        Object mode = session.planSnapshot().rules().get(InterviewPlanSettings.MODE_KEY);
        if (mode == null) return true;
        return InterviewMode.FORMAL_SIMULATION.name().equalsIgnoreCase(String.valueOf(mode));
    }

    private InterviewDifficulty upgradeDifficulty(InterviewDifficulty current) {
        if (current == null) return InterviewDifficulty.SENIOR;
        return switch (current) {
            case JUNIOR -> InterviewDifficulty.MEDIUM;
            case MEDIUM -> InterviewDifficulty.SENIOR;
            case SENIOR, EXPERT -> InterviewDifficulty.EXPERT;
        };
    }

    private String safeName(String value) {
        String normalized = value == null ? "面试" : value.strip();
        return normalized.isBlank() ? "面试" : normalized;
    }
}
