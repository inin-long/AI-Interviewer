package com.inin.aiinterviewer.agent.state;

import com.inin.aiinterviewer.agent.model.ProbePlan;
import com.inin.aiinterviewer.domain.model.ClaimLedger;
import com.inin.aiinterviewer.domain.model.EvaluationEvidence;
import com.inin.aiinterviewer.domain.model.EvidenceLedger;
import com.inin.aiinterviewer.domain.enums.EvidenceSignal;
import com.inin.aiinterviewer.domain.enums.ConsistencyIssueStatus;
import com.inin.aiinterviewer.domain.enums.ConsistencyIssueType;
import com.inin.aiinterviewer.domain.model.ConsistencyIssue;
import com.inin.aiinterviewer.domain.model.DeferredProbe;
import com.inin.aiinterviewer.domain.enums.ProbeStrategy;
import com.inin.aiinterviewer.domain.enums.PressureLevel;
import com.inin.aiinterviewer.domain.enums.ScenarioStatus;
import com.inin.aiinterviewer.domain.enums.SimulationType;
import com.inin.aiinterviewer.domain.model.PressureState;
import com.inin.aiinterviewer.domain.model.ScenarioState;
import com.inin.aiinterviewer.domain.enums.InterviewStage;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class JacksonStateSerializerTest {

    @Test
    void roundTripsCurrentStateVersion() {
        JacksonStateSerializer serializer = new JacksonStateSerializer(
                JsonMapper.builder().findAndAddModules().build());
        InterviewState state = new InterviewState(
                InterviewState.CURRENT_VERSION,
                12,
                34,
                InterviewStage.INTRODUCTION,
                List.of(),
                "请简单介绍一下自己。",
                null,
                null,
                null,
                null,
                Map.of("durationMinutes", 45),
                ""
        );

        InterviewState restored = serializer.deserialize(serializer.serialize(state));

        assertThat(restored).isEqualTo(state);
    }

    @Test
    void upgradesVersionOneCheckpointWithAnEmptyClaimLedger() {
        JacksonStateSerializer serializer = new JacksonStateSerializer(
                JsonMapper.builder().findAndAddModules().build());
        String legacy = """
                {"stateVersion":"1.0","sessionId":12,"userId":34,"stage":"INTRODUCTION",
                "messages":[],"currentQuestion":"请介绍自己","latestAnswer":"回答",
                "analysis":null,"evaluation":null,"profile":null,"rules":{},"summary":""}
                """;

        InterviewState restored = serializer.deserialize(legacy);

        assertThat(restored.stateVersion()).isEqualTo(InterviewState.CURRENT_VERSION);
        assertThat(restored.claimLedger().claims()).isEmpty();
    }

    @Test
    void upgradesVersionTwoCheckpointBeforeProbePlanningWasAdded() {
        JacksonStateSerializer serializer = new JacksonStateSerializer(
                JsonMapper.builder().findAndAddModules().build());
        String versionTwo = """
                {"stateVersion":"2.0","sessionId":12,"userId":34,"stage":"INTRODUCTION",
                "messages":[],"currentQuestion":"请介绍自己","latestAnswer":"回答",
                "analysis":null,"evaluation":null,"profile":null,"rules":{},"summary":"",
                "claimLedger":{"claims":[]}}
                """;

        InterviewState restored = serializer.deserialize(versionTwo);

        assertThat(restored.stateVersion()).isEqualTo(InterviewState.CURRENT_VERSION);
        assertThat(restored.claimLedger().claims()).isEmpty();
        assertThat(restored.probePlan()).isNull();
    }

    @Test
    void upgradesVersionTwoPointOneCheckpointAndPreservesProbePlan() {
        JacksonStateSerializer serializer = new JacksonStateSerializer(
                JsonMapper.builder().findAndAddModules().build());
        InterviewState previous = new InterviewState(
                "2.1", 12, 34, InterviewStage.RESUME_REVIEW, List.of(), "问题", "回答",
                null, null, null, Map.of(), "", ClaimLedger.empty(),
                ProbePlan.stageOpening("验证项目经验"));

        InterviewState restored = serializer.deserialize(serializer.serialize(previous));

        assertThat(restored.stateVersion()).isEqualTo(InterviewState.CURRENT_VERSION);
        assertThat(restored.probePlan().objective()).isEqualTo("验证项目经验");
        assertThat(restored.logicChainResult().skipped()).isTrue();
    }

    @Test
    void upgradesVersionTwoPointTwoAndRoundTripsEvidenceLedger() {
        JacksonStateSerializer serializer = new JacksonStateSerializer(
                JsonMapper.builder().findAndAddModules().build());
        InterviewState previous = new InterviewState(
                "2.2", 12, 34, InterviewStage.SYSTEM_DESIGN, List.of(), "问题", "回答",
                null, null, null, Map.of(), "", ClaimLedger.empty(),
                new EvidenceLedger(List.of(new EvaluationEvidence(
                        "evidence-1", 12, 99, "SYSTEM_DESIGN", EvidenceSignal.POSITIVE,
                        0.8, 0.7, "能够说明关键设计决策", List.of("claim-1"),
                        LocalDateTime.of(2026, 1, 2, 3, 4)))),
                com.inin.aiinterviewer.agent.model.LogicChainResult.skippedResult(),
                ProbePlan.stageOpening("验证系统设计"));

        InterviewState restored = serializer.deserialize(serializer.serialize(previous));

        assertThat(restored.stateVersion()).isEqualTo(InterviewState.CURRENT_VERSION);
        assertThat(restored.evidenceLedger().evidence()).singleElement()
                .satisfies(evidence -> {
                    assertThat(evidence.id()).isEqualTo("evidence-1");
                    assertThat(evidence.signal()).isEqualTo(EvidenceSignal.POSITIVE);
                    assertThat(evidence.relatedClaimIds()).containsExactly("claim-1");
                });
    }

    @Test
    void upgradesVersionTwoPointThreeAndPreservesConsistencyIssues() {
        JacksonStateSerializer serializer = new JacksonStateSerializer(
                JsonMapper.builder().findAndAddModules().build());
        ConsistencyIssue issue = new ConsistencyIssue(
                "issue-1", 12, ConsistencyIssueType.OWNERSHIP_CONFLICT,
                ConsistencyIssueStatus.CLARIFIED, "职责范围需要澄清",
                List.of("claim-1", "claim-2"), 88L, "请说明职责边界", "",
                LocalDateTime.of(2026, 1, 2, 3, 4), LocalDateTime.of(2026, 1, 2, 3, 5));
        InterviewState previous = new InterviewState(
                "2.3", 12, 34, InterviewStage.PROJECT_EXPERIENCE, List.of(), "问题", "回答",
                null, null, null, Map.of(), "", new ClaimLedger(List.of(), List.of(issue)),
                EvidenceLedger.empty(), com.inin.aiinterviewer.agent.model.LogicChainResult.skippedResult(),
                ProbePlan.stageOpening("澄清职责"));

        InterviewState restored = serializer.deserialize(serializer.serialize(previous));

        assertThat(restored.stateVersion()).isEqualTo(InterviewState.CURRENT_VERSION);
        assertThat(restored.claimLedger().issues()).singleElement().satisfies(value -> {
            assertThat(value.id()).isEqualTo("issue-1");
            assertThat(value.status()).isEqualTo(ConsistencyIssueStatus.CLARIFIED);
            assertThat(value.relatedClaimIds()).containsExactly("claim-1", "claim-2");
        });
    }

    @Test
    void upgradesVersionTwoPointFourWithAnEmptyDeferredProbeList() {
        JacksonStateSerializer serializer = new JacksonStateSerializer(
                JsonMapper.builder().findAndAddModules().build());
        InterviewState previous = new InterviewState(
                "2.4", 12, 34, InterviewStage.PROJECT_EXPERIENCE, List.of(), "问题", "回答",
                null, null, null, Map.of(), "", ClaimLedger.empty(), EvidenceLedger.empty(),
                com.inin.aiinterviewer.agent.model.LogicChainResult.skippedResult(),
                ProbePlan.stageOpening("验证项目经验"));

        InterviewState restored = serializer.deserialize(serializer.serialize(previous));

        assertThat(restored.stateVersion()).isEqualTo(InterviewState.CURRENT_VERSION);
        assertThat(restored.deferredProbes()).isEmpty();
    }

    @Test
    void roundTripsDeferredProbesInCurrentVersion() {
        JacksonStateSerializer serializer = new JacksonStateSerializer(
                JsonMapper.builder().findAndAddModules().build());
        DeferredProbe deferred = new DeferredProbe(
                "probe-1", 12, "claim-1", InterviewStage.SYSTEM_DESIGN,
                ProbeStrategy.ASK_TRADE_OFF, "延迟验证取舍", false,
                LocalDateTime.of(2026, 1, 2, 3, 4), LocalDateTime.of(2026, 1, 2, 3, 5));
        InterviewState current = new InterviewState(
                InterviewState.CURRENT_VERSION, 12, 34, InterviewStage.PROJECT_EXPERIENCE,
                List.of(), "问题", "回答", null, null, null, Map.of(), "",
                ClaimLedger.empty(), EvidenceLedger.empty(),
                com.inin.aiinterviewer.agent.model.LogicChainResult.skippedResult(),
                ProbePlan.stageOpening("验证项目经验"), List.of(deferred),
                new PressureState(PressureLevel.CHALLENGING, 1, "claim:claim-1", 1,
                        false, false, false, "挑战关键假设"));

        InterviewState restored = serializer.deserialize(serializer.serialize(current));

        assertThat(restored).isEqualTo(current);
    }

    @Test
    void upgradesVersionTwoPointFiveWithInitialPressureState() {
        JacksonStateSerializer serializer = new JacksonStateSerializer(
                JsonMapper.builder().findAndAddModules().build());
        InterviewState previous = new InterviewState(
                "2.5", 12, 34, InterviewStage.SYSTEM_DESIGN, List.of(), "问题", "回答",
                null, null, null, Map.of(), "", ClaimLedger.empty(), EvidenceLedger.empty(),
                com.inin.aiinterviewer.agent.model.LogicChainResult.skippedResult(),
                ProbePlan.stageOpening("验证系统设计"), List.of());

        InterviewState restored = serializer.deserialize(serializer.serialize(previous));

        assertThat(restored.stateVersion()).isEqualTo(InterviewState.CURRENT_VERSION);
        assertThat(restored.pressureState()).isEqualTo(PressureState.initial());
    }

    @Test
    void upgradesVersionTwoPointSixWithoutAnActiveScenario() {
        JacksonStateSerializer serializer = new JacksonStateSerializer(
                JsonMapper.builder().findAndAddModules().build());
        InterviewState previous = new InterviewState(
                "2.6", 12, 34, InterviewStage.SYSTEM_DESIGN, List.of(), "问题", "回答",
                null, null, null, Map.of(), "", ClaimLedger.empty(), EvidenceLedger.empty(),
                com.inin.aiinterviewer.agent.model.LogicChainResult.skippedResult(),
                ProbePlan.stageOpening("验证系统设计"), List.of(), PressureState.initial());

        InterviewState restored = serializer.deserialize(serializer.serialize(previous));

        assertThat(restored.stateVersion()).isEqualTo(InterviewState.CURRENT_VERSION);
        assertThat(restored.activeScenario()).isNull();
    }

    @Test
    void roundTripsActiveScenarioInCurrentVersion() {
        JacksonStateSerializer serializer = new JacksonStateSerializer(
                JsonMapper.builder().findAndAddModules().build());
        LocalDateTime now = LocalDateTime.of(2026, 7, 17, 0, 0);
        ScenarioState scenario = new ScenarioState(
                "scenario-1", 12, SimulationType.INCIDENT_RESPONSE,
                "验证故障处置", "查询服务延迟增加", "当班技术负责人",
                List.of("流量翻倍"), List.of("允许扩容"), Map.of("rootCause", "primaryLag"),
                Map.of("databaseCpu", 68), Map.of("databaseCpu", 86),
                List.of(), List.of(), List.of(), List.of("故障处置"), List.of("恢复稳定"),
                3, 1, ScenarioStatus.ACTIVE, "", now, now);
        InterviewState current = new InterviewState(
                InterviewState.CURRENT_VERSION, 12, 34, InterviewStage.SYSTEM_DESIGN,
                List.of(), "问题", "回答", null, null, null, Map.of(), "",
                ClaimLedger.empty(), EvidenceLedger.empty(),
                com.inin.aiinterviewer.agent.model.LogicChainResult.skippedResult(),
                ProbePlan.stageOpening("验证故障处置"), List.of(), PressureState.initial(), scenario);

        InterviewState restored = serializer.deserialize(serializer.serialize(current));

        assertThat(restored).isEqualTo(current);
        assertThat(restored.activeScenario().hiddenInformation())
                .containsEntry("rootCause", "primaryLag");
    }
}
