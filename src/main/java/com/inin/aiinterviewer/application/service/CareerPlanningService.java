package com.inin.aiinterviewer.application.service;

import com.inin.aiinterviewer.application.dto.CareerPlanDto;
import com.inin.aiinterviewer.application.dto.GeneratePlanCommand;
import com.inin.aiinterviewer.application.dto.OptimizeResumeCommand;
import com.inin.aiinterviewer.application.dto.ResumeOptimizationDto;
import com.inin.aiinterviewer.application.exception.AIException;
import com.inin.aiinterviewer.application.exception.BusinessException;
import com.inin.aiinterviewer.application.exception.ErrorCode;
import com.inin.aiinterviewer.application.util.JsonUtils;
import com.inin.aiinterviewer.domain.entity.CareerPlanEntity;
import com.inin.aiinterviewer.domain.entity.ResumeOptimizationEntity;
import com.inin.aiinterviewer.infrastructure.ai.ChatService;
import com.inin.aiinterviewer.infrastructure.database.mapper.CareerPlanMapper;
import com.inin.aiinterviewer.infrastructure.database.mapper.ResumeOptimizationMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;

@Service
public class CareerPlanningService {

    private static final Logger log = LoggerFactory.getLogger(CareerPlanningService.class);
    private static final Duration AI_TIMEOUT = Duration.ofSeconds(305);

    private final ChatService chatService;
    private final CareerPlanMapper careerPlanMapper;
    private final ResumeOptimizationMapper resumeOptimizationMapper;
    private final ExecutorService aiExecutor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "career-ai-timeout");
        t.setDaemon(true);
        return t;
    });

    public CareerPlanningService(
            ChatService chatService,
            CareerPlanMapper careerPlanMapper,
            ResumeOptimizationMapper resumeOptimizationMapper
    ) {
        this.chatService = chatService;
        this.careerPlanMapper = careerPlanMapper;
        this.resumeOptimizationMapper = resumeOptimizationMapper;
    }

    @Transactional
    public CareerPlanDto generatePlan(long userId, GeneratePlanCommand command) {
        if (command.targetRole() == null || command.targetRole().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        String prompt = buildPlanPrompt(command);
        String markdown = callAiWithTimeout(prompt, "职业规划");
        // 降级：AI 不可用时使用本地模板
        if (markdown == null || markdown.isBlank()) {
            log.info("[CareerPlan] AI 不可用，使用本地模板: userId={}", userId);
            markdown = localPlanDraft(command);
        }
        CareerPlanEntity entity = new CareerPlanEntity();
        entity.setUserId(userId);
        entity.setCurrentRole(command.currentRole() == null ? null : command.currentRole().strip());
        entity.setTargetRole(command.targetRole().strip());
        entity.setIndustry(command.industry() == null ? null : command.industry().strip());
        entity.setExperienceYears(command.experienceYears() == null ? null : command.experienceYears().strip());
        entity.setPlanMarkdown(markdown);
        careerPlanMapper.insert(entity);
        return toPlanDto(requirePlan(entity.getId(), userId));
    }

    @Transactional(readOnly = true)
    public List<CareerPlanDto> listPlans(long userId) {
        return careerPlanMapper.findAllByUserId(userId).stream()
                .map(this::toPlanDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public CareerPlanDto getPlan(long userId, long planId) {
        return careerPlanMapper.findByIdAndUserId(planId, userId)
                .map(this::toPlanDto)
                .orElseThrow(() -> new BusinessException(ErrorCode.CAREER_PLAN_NOT_FOUND));
    }

    @Transactional
    public void deletePlan(long userId, long planId) {
        if (careerPlanMapper.logicalDelete(planId, userId) != 1) {
            throw new BusinessException(ErrorCode.CAREER_PLAN_NOT_FOUND);
        }
    }

    @Transactional
    public ResumeOptimizationDto optimizeResume(long userId, OptimizeResumeCommand command) {
        if (command.originalText() == null || command.originalText().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        String original = command.originalText().strip();
        String prompt = buildResumePrompt(original);
        String response = callAiWithTimeout(prompt, "简历优化");
        List<String> highlights;
        String optimized;
        if (response != null && !response.isBlank()) {
            optimized = response;
            highlights = List.of("已由 AI 重写：动词开头、量化成果、成就导向。");
        } else {
            log.info("[CareerPlan] 简历优化 AI 不可用，使用本地建议: userId={}", userId);
            optimized = localResumeDraft(original);
            highlights = List.of(
                    "未配置 AI：以下为通用建议，配置后可得自动改写。",
                    "用强动词开头（负责→主导／搭建／推动）。",
                    "为每条经历补充可量化的成果。",
                    "把职责描述改写为成就描述。");
        }
        ResumeOptimizationEntity entity = new ResumeOptimizationEntity();
        entity.setUserId(userId);
        entity.setOriginalText(original);
        entity.setOptimizedText(optimized);
        entity.setHighlightsJson(JsonUtils.toJson(highlights));
        resumeOptimizationMapper.insert(entity);
        return toOptimizationDto(requireOptimization(entity.getId(), userId));
    }

    @Transactional(readOnly = true)
    public List<ResumeOptimizationDto> listOptimizations(long userId) {
        return resumeOptimizationMapper.findAllByUserId(userId).stream()
                .map(this::toOptimizationDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ResumeOptimizationDto getOptimization(long userId, long optimizationId) {
        return resumeOptimizationMapper.findByIdAndUserId(optimizationId, userId)
                .map(this::toOptimizationDto)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESUME_OPT_NOT_FOUND));
    }

    @Transactional
    public void deleteOptimization(long userId, long optimizationId) {
        if (resumeOptimizationMapper.logicalDelete(optimizationId, userId) != 1) {
            throw new BusinessException(ErrorCode.RESUME_OPT_NOT_FOUND);
        }
    }

    private String buildPlanPrompt(GeneratePlanCommand command) {
        return """
                你是一名资深职业规划顾问。请基于以下信息，为中文用户生成一份结构清晰、可执行的职业规划（使用 Markdown，二级标题分节）：

                # 职业规划：从「%s」走向「%s」
                ## 1. 目标定位
                ## 2. 能力差距分析
                ## 3. 技能学习路线（分 0-3 个月、3-6 个月、6-12 个月，每阶段给出可量化目标与推荐资源类型）
                ## 4. 职业发展路径图（阶段化里程碑）
                ## 5. 风险与应对建议

                输入信息：
                - 当前岗位：%s
                - 目标岗位：%s
                - 所在行业：%s
                - 工作年限：%s
                """.formatted(
                orDash(command.currentRole()), orDash(command.targetRole()),
                orDash(command.currentRole()), orDash(command.targetRole()),
                orDash(command.industry()), orDash(command.experienceYears()));
    }

    private String buildResumePrompt(String original) {
        return """
                你是简历优化专家。请把下面的简历原文改写为更专业、量化、结果导向的中文表述，必须保留事实、不编造。
                输出格式（Markdown）：
                ## 优化后的简历
                （完整改写文本）
                ## 改写亮点
                - （3-5 条，说明关键改写点）

                原文：
                %s
                """.formatted(original);
    }

    /**
     * 带超时的 AI 调用。超时或异常时返回 null（调用方负责降级）。
     */
    private String callAiWithTimeout(String prompt, String featureName) {
        try {
            Future<String> future = aiExecutor.submit(() -> chatService.chat(prompt));
            String result = future.get(AI_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
            log.info("[CareerPlan] {} AI 调用成功，响应长度: {}", featureName, result.length());
            return result;
        } catch (TimeoutException e) {
            log.warn("[CareerPlan] {} AI 调用超时 ({}s)，将使用本地降级", featureName, AI_TIMEOUT.toSeconds());
            return null;
        } catch (AIException e) {
            log.warn("[CareerPlan] {} AI 调用异常: {} - {}", featureName, e.getErrorCode(), e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("[CareerPlan] {} AI 调用未知错误: {}", featureName, e.getMessage(), e);
            return null;
        }
    }

    private String localPlanDraft(GeneratePlanCommand command) {
        return """
                # 职业规划（本地草稿）

                > 当前未配置 AI 服务，以下为基于输入的本地模板，仅供参考；配置 AI 后可生成更精细的路径图与学习路线。

                ## 目标定位
                从「%s」走向「%s」，行业方向：%s，当前工作年限：%s。

                ## 能力差距分析
                请对照目标岗位的招聘要求，梳理硬技能（技术/工具）与软技能（沟通/管理）的差距。

                ## 技能学习路线
                - 0-3 个月：补齐目标岗位要求的基础能力，完成 1-2 个可展示的小项目。
                - 3-6 个月：在真实业务中承担目标岗位相关职责，沉淀量化成果。
                - 6-12 个月：形成可迁移的方法论，准备内部转岗或外部机会。

                ## 职业发展路径图
                当前岗位 → 相关项目/横向拓展 → 目标岗位初级 → 目标岗位资深。

                ## 风险与应对
                - 市场变化：保持学习能力与多选方向。
                - 进度滞后：把大目标拆成周度可验证的小目标。
                """.formatted(orDash(command.currentRole()), orDash(command.targetRole()),
                orDash(command.industry()), orDash(command.experienceYears()));
    }

    private String localResumeDraft(String original) {
        return "## 优化后的简历（本地草稿）\n\n"
                + "> 未配置 AI 服务，以下为原文。配置后可获得动词开头、量化成果的自动改写。\n\n"
                + original
                + "\n\n## 改写亮点\n- 用强动词开头，突出主动贡献。\n"
                + "- 为每条经历补充可量化成果。\n- 将职责描述改写为成就描述。";
    }

    private String orDash(String value) {
        return value == null || value.isBlank() ? "（未填写）" : value.strip();
    }

    private CareerPlanEntity requirePlan(long planId, long userId) {
        return careerPlanMapper.findByIdAndUserId(planId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CAREER_PLAN_NOT_FOUND));
    }

    private ResumeOptimizationEntity requireOptimization(long optimizationId, long userId) {
        return resumeOptimizationMapper.findByIdAndUserId(optimizationId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESUME_OPT_NOT_FOUND));
    }

    private CareerPlanDto toPlanDto(CareerPlanEntity entity) {
        return new CareerPlanDto(entity.getId(), entity.getCurrentRole(), entity.getTargetRole(),
                entity.getIndustry(), entity.getExperienceYears(), entity.getPlanMarkdown(),
                entity.getCreateTime());
    }

    private ResumeOptimizationDto toOptimizationDto(ResumeOptimizationEntity entity) {
        return new ResumeOptimizationDto(entity.getId(), entity.getOriginalText(),
                entity.getOptimizedText(), JsonUtils.readStringList(entity.getHighlightsJson()),
                entity.getCreateTime());
    }
}
