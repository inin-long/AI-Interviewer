package com.inin.aiinterviewer.agent.node;

import com.inin.aiinterviewer.agent.model.ProbePlan;
import com.inin.aiinterviewer.agent.model.ScenarioDirectionResult;
import com.inin.aiinterviewer.agent.prompt.AgentPrompts;
import com.inin.aiinterviewer.agent.state.InterviewGraphState;
import com.inin.aiinterviewer.agent.support.StructuredAiResponseParser;
import com.inin.aiinterviewer.domain.enums.PressureLevel;
import com.inin.aiinterviewer.domain.enums.ProbeStrategy;
import com.inin.aiinterviewer.infrastructure.ai.ChatService;
import org.bsc.langgraph4j.action.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class ScenarioDirectorNode implements NodeAction<InterviewGraphState> {

    private static final Logger log = LoggerFactory.getLogger(ScenarioDirectorNode.class);
    private static final List<String> UNSAFE_TERMS = List.of(
            "蠢", "愚蠢", "撒谎", "骗子", "无能", "垃圾", "闭嘴",
            "stupid", "idiot", "liar", "incompetent");

    private final ChatService chatService;
    private final StructuredAiResponseParser parser;

    public ScenarioDirectorNode(ChatService chatService, StructuredAiResponseParser parser) {
        this.chatService = chatService;
        this.parser = parser;
    }

    @Override
    public Map<String, Object> apply(InterviewGraphState state) {
        if (state.activeScenario() == null) {
            return output(state, ScenarioDirectionResult.skipped("no_active_scenario"));
        }
        if (!state.activeScenario().introduced()) {
            return output(state, ScenarioDirectionResult.kickoff(kickoffQuestion(state)));
        }
        if (state.answer().isBlank()) {
            return output(state, ScenarioDirectionResult.skipped("no_candidate_decision"));
        }
        if (state.data().containsKey(InterviewGraphState.SCENARIO_DIRECTION_RESULT)) {
            return output(state, state.scenarioDirectionResult());
        }
        String response = "";
        RuntimeException firstFailure;
        try {
            response = chatService.chat(AgentPrompts.scenarioDirection(state));
            return output(state, validate(state, parser.parse(response, ScenarioDirectionResult.class)));
        } catch (RuntimeException exception) {
            firstFailure = exception;
        }
        try {
            String repaired = chatService.chat(AgentPrompts.repairScenarioDirection(state, response));
            return output(state, validate(state, parser.parse(repaired, ScenarioDirectionResult.class)));
        } catch (RuntimeException repairFailure) {
            log.warn("Scenario direction degraded after one repair attempt: first={}, repair={}",
                    firstFailure.getClass().getSimpleName(), repairFailure.getClass().getSimpleName());
            return output(state, ScenarioDirectionResult.degraded("scenario_direction_failed"));
        }
    }

    private ScenarioDirectionResult validate(
            InterviewGraphState state,
            ScenarioDirectionResult result
    ) {
        if (result == null || result.skipped() || result.degraded() || result.eventType() == null
                || result.decisionAction().isBlank() || result.decisionAction().length() > 1_000
                || result.decisionRationale().isBlank() || result.decisionRationale().length() > 2_000
                || result.eventDescription().isBlank() || result.eventDescription().length() > 2_000
                || result.changes().isEmpty() || result.changes().size() > 12
                || !state.activeScenario().variables().keySet().containsAll(result.changes().keySet())
                || result.nextQuestion().isBlank() || result.nextQuestion().length() > 1_000
                || containsUnsafeLanguage(result)) {
            throw new IllegalArgumentException("Invalid scenario direction result");
        }
        return new ScenarioDirectionResult(
                result.decisionAction(), result.decisionRationale(), result.eventType(),
                result.eventDescription(), result.changes(), result.nextQuestion(),
                result.completeAfterEvent(), false, false, false, "");
    }

    private boolean containsUnsafeLanguage(ScenarioDirectionResult result) {
        String content = (result.eventDescription() + " " + result.nextQuestion())
                .toLowerCase(Locale.ROOT);
        return UNSAFE_TERMS.stream().anyMatch(content::contains);
    }

    private Map<String, Object> output(
            InterviewGraphState state,
            ScenarioDirectionResult result
    ) {
        if (!result.requiresScenarioPrompt()) {
            return Map.of(InterviewGraphState.SCENARIO_DIRECTION_RESULT, result);
        }
        ProbeStrategy strategy = result.kickoff()
                ? ProbeStrategy.INTRODUCE_CONSTRAINT
                : switch (result.eventType()) {
                    case RESOURCE_SHOCK, DEPENDENCY_FAILURE, TRAFFIC_SPIKE -> ProbeStrategy.INTRODUCE_FAILURE;
                    case CONSTRAINT_CHANGE, REQUIREMENT_CHANGE, STAKEHOLDER_ESCALATION ->
                            ProbeStrategy.INTRODUCE_CONSTRAINT;
                    case RECOVERY_SIGNAL -> ProbeStrategy.CHALLENGE_ASSUMPTION;
                };
        ProbePlan previous = state.probePlan();
        ProbePlan scenarioPlan = new ProbePlan(
                previous.targetClaimId(), previous.targetLogicGap(),
                previous.targetConsistencyIssueId(), previous.targetDeferredProbeId(),
                result.nextQuestion(), strategy, PressureLevel.STANDARD,
                result.kickoff() ? "按方案场景比例进入结构化情境沙盘"
                        : "场景事件由候选人本轮决策触发：" + result.eventDescription(),
                state.activeScenario().evaluatedCompetencies(), true,
                previous.targetCompetencyCode());
        return Map.of(
                InterviewGraphState.SCENARIO_DIRECTION_RESULT, result,
                InterviewGraphState.PROBE_PLAN, scenarioPlan);
    }

    private String kickoffQuestion(InterviewGraphState state) {
        var scenario = state.activeScenario();
        String facts = String.join("；", scenario.knownFacts());
        String constraints = scenario.constraints().stream()
                .filter(constraint -> constraint.active())
                .map(constraint -> constraint.description())
                .reduce((left, right) -> left + "；" + right)
                .orElse("无额外约束");
        return "%s 你将作为%s，目标是%s。已知事实：%s。当前约束：%s。请说明你首先会采取什么行动，以及判断依据。"
                .formatted(scenario.background(), scenario.candidateRole(), scenario.objective(),
                        facts, constraints);
    }
}
