package com.inin.aiinterviewer.agent.node;

import com.inin.aiinterviewer.agent.model.ProbePlan;
import com.inin.aiinterviewer.agent.state.InterviewGraphState;
import com.inin.aiinterviewer.domain.enums.InterviewStage;
import com.inin.aiinterviewer.domain.enums.PressureLevel;
import com.inin.aiinterviewer.domain.model.InterviewPlanSettings;
import com.inin.aiinterviewer.domain.model.InterviewStrategy;
import com.inin.aiinterviewer.domain.model.Message;
import com.inin.aiinterviewer.domain.model.PressureState;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class DecisionValidatorNode implements NodeAction<InterviewGraphState> {

    @Override
    public Map<String, Object> apply(InterviewGraphState state) {
        long asked = state.messages().stream()
                .filter(message -> message.role() == Message.Role.ASSISTANT).count();
        InterviewStage stage = allowedStage(state, asked);
        ProbePlan probe = validProbe(state);
        PressureLevel configuredMaximum = InterviewPlanSettings
                .fromRules(state.plan().rules()).pressureLevel();
        PressureLevel pressure = lower(state.pressureState().level(), configuredMaximum);
        boolean consecutiveLimitReached = state.pressureState().consecutivePressureTurns() > 2
                && pressure.ordinal() > PressureLevel.STANDARD.ordinal();
        if (consecutiveLimitReached) pressure = PressureLevel.STANDARD;
        PressureState pressureState = state.pressureState();
        if (pressure != pressureState.level()) {
            probe = probe.withPressureLevel(pressure);
            pressureState = new PressureState(
                    pressure,
                    pressure.ordinal() > PressureLevel.STANDARD.ordinal()
                            ? pressureState.consecutivePressureTurns() : 0,
                    pressureState.lastTargetKey(), pressureState.repeatedTargetTurns(),
                    pressureState.sufficientEvidence(), true, pressureState.safetyAdjusted(),
                    consecutiveLimitReached
                            ? "连续高强度追问超过程序上限，决策校验已降压"
                            : "追问压力超过方案上限，决策校验已降压");
        }
        int remaining = Math.max(0, state.plan().questionCount() - (int) asked);
        boolean scenarioTurn = state.activeScenario() != null
                && state.scenarioDirectionResult().requiresScenarioPrompt();
        InterviewStrategy strategy = new InterviewStrategy(
                stage, probe.strategy(), probe.targetClaimId(), probe.targetCompetencyCode(),
                probe.objective(), pressure, remaining, scenarioTurn,
                probe.reason().isBlank() ? "决策已通过程序规则校验" : probe.reason());

        LinkedHashMap<String, Object> output = new LinkedHashMap<>();
        output.put(InterviewGraphState.STAGE, stage);
        output.put(InterviewGraphState.PROBE_PLAN, probe);
        output.put(InterviewGraphState.PRESSURE_STATE, pressureState);
        output.put(InterviewGraphState.STRATEGY, strategy);
        return Map.copyOf(output);
    }

    private InterviewStage allowedStage(InterviewGraphState state, long askedQuestions) {
        if (state.stage() == InterviewStage.COMPLETED) {
            if (askedQuestions >= state.plan().questionCount()) return InterviewStage.COMPLETED;
            return state.plan().stages().reversed().stream()
                    .map(this::stage)
                    .filter(value -> value != null && value != InterviewStage.COMPLETED)
                    .findFirst().orElse(InterviewStage.SUMMARY);
        }
        if (state.plan().stages().isEmpty()
                || state.plan().stages().contains(state.stage().name())) return state.stage();
        return state.plan().stages().stream()
                .map(this::stage)
                .filter(value -> value != null && value != InterviewStage.COMPLETED)
                .findFirst().orElse(InterviewStage.INTRODUCTION);
    }

    private InterviewStage stage(String value) {
        try {
            return InterviewStage.valueOf(value);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private ProbePlan validProbe(InterviewGraphState state) {
        ProbePlan probe = state.probePlan();
        boolean targeted = probe.targetsClaim() || probe.targetsLogicGap()
                || probe.targetsConsistencyIssue() || probe.targetsDeferredProbe()
                || probe.targetsCompetency() || probe.shouldInjectScenario();
        if (!probe.objective().isBlank() && targeted) return probe;

        var competency = state.coverage().competencies().entrySet().stream()
                .filter(entry -> entry.getValue().needsVerification())
                .max(Comparator.comparingDouble(entry -> entry.getValue().priority()));
        if (competency.isPresent()) {
            String code = competency.get().getKey();
            return ProbePlan.stageOpening(
                    "验证岗位能力 " + code + " 的具体经历、个人行动和结果依据", code);
        }
        return ProbePlan.stageOpening(
                "验证当前阶段的岗位核心能力", "STAGE_" + state.stage().name());
    }

    private PressureLevel lower(PressureLevel requested, PressureLevel maximum) {
        return requested.ordinal() <= maximum.ordinal() ? requested : maximum;
    }
}
