package com.inin.aiinterviewer.agent.prompt;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.inin.aiinterviewer.agent.model.ProbePlan;
import com.inin.aiinterviewer.agent.state.InterviewGraphState;
import com.inin.aiinterviewer.application.dto.InterviewPlanDto;
import com.inin.aiinterviewer.domain.enums.InterviewDifficulty;
import com.inin.aiinterviewer.domain.enums.InterviewStage;
import com.inin.aiinterviewer.domain.enums.InterviewerPersona;
import com.inin.aiinterviewer.domain.model.InterviewPlanSettings;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PersonaRendererTest {

    @Test
    void changesOnlyQuestionVoiceForTheSameProbeObjective() {
        ProbePlan probe = ProbePlan.stageOpening("验证 Redis 收益归因");
        String peerPrompt = AgentPrompts.question(
                state(plan(InterviewerPersona.FUTURE_PEER), probe), JsonMapper.builder().build());
        String leadPrompt = AgentPrompts.question(
                state(plan(InterviewerPersona.TECH_LEAD), probe), JsonMapper.builder().build());

        assertThat(peerPrompt)
                .contains("验证 Redis 收益归因", "未来同事", "团队复用", "只控制表达方式");
        assertThat(leadPrompt)
                .contains("验证 Redis 收益归因", "技术负责人", "收益依据", "只控制表达方式");
        assertThat(peerPrompt).isNotEqualTo(leadPrompt);
    }

    @Test
    void personaNeverEntersEvidenceEvaluationPrompt() {
        String peer = AgentPrompts.evidenceCollection(
                state(plan(InterviewerPersona.FUTURE_PEER), ProbePlan.stageOpening("目标")));
        String commander = AgentPrompts.evidenceCollection(
                state(plan(InterviewerPersona.INCIDENT_COMMANDER), ProbePlan.stageOpening("目标")));

        assertThat(peer).isEqualTo(commander)
                .contains("Persona 只影响问题表达，不得影响证据提取");
    }

    private InterviewGraphState state(InterviewPlanDto plan, ProbePlan probe) {
        return new InterviewGraphState(Map.of(
                InterviewGraphState.STAGE, InterviewStage.PROJECT_EXPERIENCE,
                InterviewGraphState.PLAN, plan,
                InterviewGraphState.PROBE_PLAN, probe,
                InterviewGraphState.ANSWER, "我使用对照实验验证了收益。"));
    }

    private InterviewPlanDto plan(InterviewerPersona persona) {
        return new InterviewPlanDto(
                1L, "Java 面试", "Java 后端工程师", "高并发服务",
                InterviewDifficulty.SENIOR, 45, 8, null,
                Map.of(InterviewPlanSettings.PERSONA_KEY, persona.name()),
                List.of("PROJECT_EXPERIENCE"), false, LocalDateTime.now(), LocalDateTime.now());
    }
}
