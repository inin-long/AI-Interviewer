package com.inin.aiinterviewer.application.service;

import com.inin.aiinterviewer.agent.model.EvidenceCollectionResult;
import com.inin.aiinterviewer.agent.model.LogicChainResult;
import com.inin.aiinterviewer.agent.state.InterviewState;
import com.inin.aiinterviewer.agent.state.StateSerializer;
import com.inin.aiinterviewer.application.dto.SaveInterviewPlanCommand;
import com.inin.aiinterviewer.application.exception.BusinessException;
import com.inin.aiinterviewer.domain.entity.AgentCheckpointEntity;
import com.inin.aiinterviewer.domain.enums.EvidenceSignal;
import com.inin.aiinterviewer.domain.enums.InterviewDifficulty;
import com.inin.aiinterviewer.domain.enums.InterviewMode;
import com.inin.aiinterviewer.domain.enums.LogicGapType;
import com.inin.aiinterviewer.domain.enums.PressureLevel;
import com.inin.aiinterviewer.domain.model.InterviewPlanSettings;
import com.inin.aiinterviewer.domain.model.LogicGap;
import com.inin.aiinterviewer.infrastructure.ai.EmbeddingService;
import com.inin.aiinterviewer.infrastructure.database.mapper.AgentCheckpointMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TrainingRecommendationServiceIntegrationTest.FakeEmbeddingConfiguration.class)
class TrainingRecommendationServiceIntegrationTest {

    @TempDir
    static Path applicationHome;

    @DynamicPropertySource
    static void applicationProperties(DynamicPropertyRegistry registry) {
        registry.add("ai.interviewer.home", () -> applicationHome.toString());
        registry.add("task.enabled", () -> false);
        registry.add("rag.chunk-size", () -> 120);
        registry.add("rag.overlap", () -> 20);
    }

    @Autowired private UserService userService;
    @Autowired private KnowledgeDocumentService knowledgeService;
    @Autowired private InterviewPlanService planService;
    @Autowired private InterviewSessionService sessionService;
    @Autowired private EvidenceLedgerService evidenceLedgerService;
    @Autowired private TrainingRecommendationService trainingService;
    @Autowired private AgentCheckpointMapper checkpointMapper;
    @Autowired private StateSerializer stateSerializer;

    @Test
    void turnsWeakEvidenceLogicGapsAndKnowledgeIntoACoachingPlan() throws Exception {
        var owner = userService.register("training-owner", "Training Owner", "safe-password");
        var other = userService.register("training-other", "Training Other", "safe-password");
        Path notes = applicationHome.resolve("system-design-baseline.md");
        Files.writeString(notes, ("系统设计评审需要记录容量基线、备选方案、关键取舍和监控验证。\n").repeat(10));
        var document = knowledgeService.uploadAndIndex(owner.id(), notes, "系统设计");

        var sourcePlan = planService.create(owner.id(), new SaveInterviewPlanCommand(
                "高级 Java 面试", "Java 后端工程师", "负责高并发核心服务",
                InterviewDifficulty.SENIOR, 45, 8, null, null, List.of(document.id()),
                Map.of("focus", "系统设计"),
                List.of("INTRODUCTION", "SYSTEM_DESIGN", "SUMMARY"), "java-backend-1.0.0"));
        var session = sessionService.create(owner.id(), sourcePlan.id());
        sessionService.saveAssistantOutput(
                owner.id(), session.id(), "你如何验证容量方案？", null, false);
        sessionService.appendUserAnswer(
                owner.id(), session.id(), "我会直接扩容，应该能够解决问题。");

        evidenceLedgerService.recordLatestAnswer(owner.id(), session.id(),
                new EvidenceCollectionResult(List.of(
                        new EvidenceCollectionResult.EvidenceCandidate(
                                "SYSTEM_DESIGN", EvidenceSignal.NEGATIVE, 0.85, 0.9,
                                "回答没有给出容量基线和验证指标", List.of()),
                        new EvidenceCollectionResult.EvidenceCandidate(
                                "SYSTEM_DESIGN", EvidenceSignal.INSUFFICIENT, 0.7, 0.8,
                                "尚未比较备选方案与成本取舍", List.of()))));
        saveLogicGap(owner.id(), session.id());

        var recommendation = trainingService.recommend(owner.id(), session.id());
        assertThat(recommendation.topics()).singleElement().satisfies(topic -> {
            assertThat(topic.competencyCode()).isEqualTo("SYSTEM_DESIGN");
            assertThat(topic.title()).isEqualTo("系统设计与取舍");
            assertThat(topic.priority()).isEqualTo(3);
            assertThat(topic.sourceEvidenceIds()).hasSize(2);
            assertThat(topic.sourceQuestionNumbers()).containsExactly(1);
            assertThat(topic.rationale()).contains("容量基线", "备选方案");
        });
        assertThat(recommendation.exercises()).singleElement().satisfies(exercise -> {
            assertThat(exercise.logicGapType()).isEqualTo("MISSING_BASELINE");
            assertThat(exercise.title()).isEqualTo("补齐对照基线");
            assertThat(exercise.instruction()).contains("背景", "个人行动", "验证方法");
        });
        assertThat(recommendation.knowledgeResources()).singleElement().satisfies(resource -> {
            assertThat(resource.documentId()).isEqualTo(document.id());
            assertThat(resource.name()).contains("system-design-baseline");
        });

        var trainingPlan = trainingService.createTrainingPlan(owner.id(), session.id());
        var settings = InterviewPlanSettings.fromRules(trainingPlan.rules());
        assertThat(settings.mode()).isEqualTo(InterviewMode.COACHING);
        assertThat(settings.pressureLevel()).isEqualTo(PressureLevel.RELAXED);
        assertThat(settings.scenarioRatio()).isZero();
        assertThat(trainingPlan.name()).contains("专项训练");
        assertThat(trainingPlan.knowledgeDocumentIds()).containsExactly(document.id());
        assertThat(trainingPlan.stages())
                .contains("INTRODUCTION", "SYSTEM_DESIGN", "PROJECT_EXPERIENCE", "SUMMARY");
        assertThat(String.valueOf(trainingPlan.rules().get(
                TrainingRecommendationService.SOURCE_SESSION_RULE)))
                .isEqualTo(Long.toString(session.id()));
        assertThat(trainingPlan.rules())
                .containsEntry("coachingHintsEnabled", true)
                .containsEntry("coachingReanswerEnabled", true)
                .containsEntry("coachingReferenceStructureEnabled", true);
        assertThat(InterviewPlanSettings.fromRules(
                planService.require(sourcePlan.id(), owner.id()).rules()).mode())
                .isEqualTo(InterviewMode.FORMAL_SIMULATION);

        assertThatThrownBy(() -> trainingService.recommend(other.id(), session.id()))
                .isInstanceOf(BusinessException.class);
    }

    private void saveLogicGap(long userId, long sessionId) {
        InterviewState previous = sessionService.loadLatestState(userId, sessionId).orElseThrow();
        LogicChainResult logic = new LogicChainResult(
                List.of(), "需要容量规划", List.of(), "直接扩容", "", List.of("扩容"),
                "", "", "", List.of(new LogicGap(
                        LogicGapType.MISSING_BASELINE, "缺少容量与性能基线", 0.9, List.of())),
                false, false, "");
        InterviewState withGap = new InterviewState(
                previous.stateVersion(), previous.sessionId(), previous.userId(), previous.stage(),
                previous.messages(), previous.currentQuestion(), previous.latestAnswer(), previous.analysis(),
                previous.evaluation(), previous.profile(), previous.rules(), previous.summary(),
                previous.claimLedger(), previous.evidenceLedger(), logic, previous.probePlan(),
                previous.deferredProbes(), previous.pressureState(), previous.activeScenario());
        AgentCheckpointEntity checkpoint = new AgentCheckpointEntity();
        checkpoint.setUserId(userId);
        checkpoint.setSessionId(sessionId);
        checkpoint.setNodeName("training_gap_test");
        checkpoint.setStateJson(stateSerializer.serialize(withGap));
        checkpoint.setStateVersion(withGap.stateVersion());
        checkpointMapper.insert(checkpoint);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FakeEmbeddingConfiguration {
        @Bean
        @Primary
        EmbeddingService trainingEmbeddingService() {
            return text -> new float[]{1f, 0.5f, 0.25f};
        }
    }
}
