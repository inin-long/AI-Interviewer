package com.inin.aiinterviewer.application.service;

import com.inin.aiinterviewer.application.dto.InterviewPlanDto;
import com.inin.aiinterviewer.domain.enums.InterviewStage;
import com.inin.aiinterviewer.domain.enums.SimulationType;
import com.inin.aiinterviewer.domain.model.DomainPack;
import com.inin.aiinterviewer.domain.model.DomainPackSnapshot;
import com.inin.aiinterviewer.domain.model.InterviewPlanSettings;
import com.inin.aiinterviewer.domain.model.ScenarioConstraint;
import com.inin.aiinterviewer.domain.model.ScenarioDefinition;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ScenarioSchedulingService {

    public Optional<ScenarioDefinition> select(
            long sessionId,
            InterviewPlanDto plan,
            DomainPackSnapshot domainPack,
            InterviewStage stage,
            long askedQuestions
    ) {
        if (plan == null || domainPack == null || domainPack.content() == null
                || domainPack.content().scenarios().isEmpty()
                || stage == InterviewStage.SUMMARY || stage == InterviewStage.COMPLETED) {
            return Optional.empty();
        }
        InterviewPlanSettings settings;
        try {
            settings = InterviewPlanSettings.fromRules(plan.rules());
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
        int ratio = settings.scenarioRatio();
        int totalQuestions = plan.questionCount();
        if (ratio == 0 || askedQuestions < 0 || askedQuestions >= totalQuestions) {
            return Optional.empty();
        }

        int targetScenarioQuestions = Math.max(
                2, (int) Math.ceil(totalQuestions * ratio / 100.0));
        targetScenarioQuestions = Math.min(totalQuestions, targetScenarioQuestions);
        int firstScenarioQuestion = totalQuestions - targetScenarioQuestions + 1;
        long nextQuestionNumber = askedQuestions + 1;
        if (nextQuestionNumber < firstScenarioQuestion
                || totalQuestions - askedQuestions < 2) {
            return Optional.empty();
        }

        List<DomainPack.ScenarioTemplate> templates = domainPack.content().scenarios();
        int index = Math.floorMod(Long.hashCode(sessionId), templates.size());
        DomainPack.ScenarioTemplate template = templates.get(index);
        int availableEventRounds = totalQuestions - (int) askedQuestions - 1;
        int maxRounds = Math.min(template.maxRounds(), Math.max(1, availableEventRounds));
        return Optional.of(toDefinition(template, maxRounds));
    }

    private ScenarioDefinition toDefinition(
            DomainPack.ScenarioTemplate template,
            int maxRounds
    ) {
        LinkedHashMap<String, Object> hiddenInformation = new LinkedHashMap<>();
        hiddenInformation.putAll(template.hiddenInformation());
        hiddenInformation.put("templateId", template.id());
        hiddenInformation.put("injectableEvents", template.events());

        List<ScenarioConstraint> constraints = new ArrayList<>();
        for (int index = 0; index < template.constraints().size(); index++) {
            constraints.add(new ScenarioConstraint(
                    "constraint-" + (index + 1), template.constraints().get(index), true, true));
        }
        return new ScenarioDefinition(
                SimulationType.valueOf(template.type()), template.objective(), template.background(),
                template.candidateRole(), template.knownFacts(), template.assumptions(),
                Map.copyOf(hiddenInformation), template.variables(), constraints,
                template.competencies(), template.endConditions(), maxRounds);
    }
}
