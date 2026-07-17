package com.inin.aiinterviewer.agent.node;

import com.inin.aiinterviewer.agent.model.ProbePlan;
import com.inin.aiinterviewer.agent.model.QuestionQualityContext;
import com.inin.aiinterviewer.agent.model.QuestionQualityIssue;
import com.inin.aiinterviewer.application.dto.InterviewPlanDto;
import com.inin.aiinterviewer.domain.enums.InterviewDifficulty;
import com.inin.aiinterviewer.domain.enums.InterviewStage;
import com.inin.aiinterviewer.domain.enums.PressureLevel;
import com.inin.aiinterviewer.domain.enums.ProbeStrategy;
import com.inin.aiinterviewer.domain.model.Message;
import com.inin.aiinterviewer.domain.model.PressureState;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class QuestionQualityGateNodeTest {

    private final QuestionQualityGateNode gate = new QuestionQualityGateNode();

    @Test
    void approvesOneEvidenceSeekingQuestionForTheCurrentTarget() {
        assertThat(gate.review(context(InterviewDifficulty.SENIOR),
                "你会如何用基线、对照组和监控数据证明 Redis 带来的性能收益？"))
                .satisfies(result -> {
                    assertThat(result.approved()).isTrue();
                    assertThat(result.issues()).isEmpty();
                });
    }

    @Test
    void rejectsDuplicatesMultiPartLeaksFabricationAndMeaninglessPressure() {
        QuestionQualityContext context = context(InterviewDifficulty.SENIOR);

        assertThat(gate.review(context, "你如何验证 Redis 的收益？你还做了哪些优化？").issues())
                .contains(QuestionQualityIssue.TOO_MANY_QUESTIONS);
        assertThat(gate.review(context, "正确答案是使用对照实验，你是否同意？").issues())
                .contains(QuestionQualityIssue.REFERENCE_ANSWER_LEAK);
        assertThat(gate.review(context, "你曾在火星支付项目工作，请说明当时如何验证收益？").issues())
                .contains(QuestionQualityIssue.FABRICATED_CANDIDATE_CONTEXT);
        assertThat(gate.review(context, "这都不会，你根本不懂性能优化，请重新说明。 ").issues())
                .contains(QuestionQualityIssue.MEANINGLESS_PRESSURE);
        assertThat(gate.review(context, "请说明 Redis 收益如何验证。 ").issues())
                .contains(QuestionQualityIssue.DUPLICATE_QUESTION);
    }

    @Test
    void rejectsStageTargetDifficultyAndCompetencyMismatches() {
        assertThat(gate.review(context(InterviewDifficulty.SENIOR), "请做一下自我介绍。 ").issues())
                .contains(QuestionQualityIssue.STAGE_MISMATCH, QuestionQualityIssue.TARGET_MISMATCH);
        assertThat(gate.review(context(InterviewDifficulty.EXPERT), "什么是缓存？").issues())
                .contains(QuestionQualityIssue.DIFFICULTY_MISMATCH);
        assertThat(gate.review(context(InterviewDifficulty.SENIOR), "请说明你的婚姻情况。 ").issues())
                .contains(QuestionQualityIssue.COMPETENCY_MISMATCH,
                        QuestionQualityIssue.UNDECLARED_BACKGROUND);
        assertThat(gate.review(context(InterviewDifficulty.SENIOR), "Redis 可以提升性能。 ").issues())
                .contains(QuestionQualityIssue.NOT_EVIDENCE_SEEKING);
    }

    @Test
    void fallsBackToTheTrustedProbeObjectiveWithoutInternalIdentifiers() {
        String fallback = gate.fallback(context(InterviewDifficulty.SENIOR));

        assertThat(fallback)
                .contains("验证 Redis 收益归因", "基线", "对照组")
                .doesNotContain("claim-42", "VERIFY_DATA_SOURCE");
    }

    private QuestionQualityContext context(InterviewDifficulty difficulty) {
        InterviewPlanDto plan = new InterviewPlanDto(
                1L, "Java 面试", "Java 后端工程师", "负责高并发服务",
                difficulty, 45, 8, null, Map.of(), List.of("TECHNICAL_DEEP_DIVE"),
                false, LocalDateTime.now(), LocalDateTime.now());
        ProbePlan probe = new ProbePlan(
                "claim-42", "验证 Redis 收益归因", ProbeStrategy.VERIFY_DATA_SOURCE,
                PressureLevel.STANDARD, "缺少数据来源", List.of("基线", "对照组", "监控数据"), false);
        return new QuestionQualityContext(
                InterviewStage.TECHNICAL_DEEP_DIVE, plan, probe, PressureState.initial(), null,
                List.of(new Message(Message.Role.ASSISTANT,
                        "请说明 Redis 收益如何验证。", LocalDateTime.now())),
                "候选人参与过订单系统", "缓存与性能指标");
    }
}
