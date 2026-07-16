package com.inin.aiinterviewer.application.service;

import com.inin.aiinterviewer.agent.state.StateSerializer;
import com.inin.aiinterviewer.application.dto.InterviewPlanDto;
import com.inin.aiinterviewer.application.dto.InterviewSessionDto;
import com.inin.aiinterviewer.application.dto.KnowledgeDocumentDto;
import com.inin.aiinterviewer.application.dto.SaveInterviewPlanCommand;
import com.inin.aiinterviewer.application.dto.TrainingRecommendationDto;
import com.inin.aiinterviewer.domain.enums.EvidenceSignal;
import com.inin.aiinterviewer.domain.enums.InterviewMode;
import com.inin.aiinterviewer.domain.enums.InterviewerPersona;
import com.inin.aiinterviewer.domain.enums.LogicGapType;
import com.inin.aiinterviewer.domain.enums.PressureLevel;
import com.inin.aiinterviewer.domain.enums.VerificationStrictness;
import com.inin.aiinterviewer.domain.model.EvaluationEvidence;
import com.inin.aiinterviewer.domain.model.InterviewPlanSettings;
import com.inin.aiinterviewer.domain.model.LogicGap;
import com.inin.aiinterviewer.infrastructure.database.mapper.AgentCheckpointMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TrainingRecommendationService {

    public static final String SOURCE_SESSION_RULE = "trainingSourceSessionId";
    public static final String SOURCE_EVIDENCE_RULE = "trainingSourceEvidenceIds";
    public static final String SOURCE_GAP_RULE = "trainingLogicGapTypes";

    private static final int MAX_TOPICS = 8;
    private static final int MAX_EXERCISES = 8;
    private static final int MAX_KNOWLEDGE = 5;

    private final InterviewSessionService sessionService;
    private final InterviewPlanService planService;
    private final EvidenceLedgerService evidenceLedgerService;
    private final KnowledgeDocumentService knowledgeService;
    private final AgentCheckpointMapper checkpointMapper;
    private final StateSerializer stateSerializer;

    public TrainingRecommendationService(
            InterviewSessionService sessionService,
            InterviewPlanService planService,
            EvidenceLedgerService evidenceLedgerService,
            KnowledgeDocumentService knowledgeService,
            AgentCheckpointMapper checkpointMapper,
            StateSerializer stateSerializer
    ) {
        this.sessionService = sessionService;
        this.planService = planService;
        this.evidenceLedgerService = evidenceLedgerService;
        this.knowledgeService = knowledgeService;
        this.checkpointMapper = checkpointMapper;
        this.stateSerializer = stateSerializer;
    }

    public TrainingRecommendationDto recommend(long userId, long sourceSessionId) {
        InterviewSessionDto session = sessionService.require(userId, sourceSessionId);
        List<EvaluationEvidence> evidence = evidenceLedgerService
                .ledger(userId, sourceSessionId).evidence();
        List<TrainingRecommendationDto.TrainingTopic> topics = topics(
                evidence, sessionService.messageQuestionNumbers(userId, sourceSessionId));
        List<TrainingRecommendationDto.TrainingExercise> exercises = exercises(
                userId, sourceSessionId);
        List<TrainingRecommendationDto.KnowledgeResource> knowledge = knowledge(
                userId, session, topics, exercises);
        return new TrainingRecommendationDto(sourceSessionId, topics, exercises, knowledge);
    }

    public InterviewPlanDto createTrainingPlan(long userId, long sourceSessionId) {
        InterviewSessionDto session = sessionService.require(userId, sourceSessionId);
        TrainingRecommendationDto recommendation = recommend(userId, sourceSessionId);
        InterviewPlanDto source = session.planSnapshot();

        LinkedHashMap<String, Object> rules = new LinkedHashMap<>();
        if (source != null && source.rules() != null) rules.putAll(source.rules());
        rules.put(SOURCE_SESSION_RULE, sourceSessionId);
        rules.put(SOURCE_EVIDENCE_RULE, recommendation.topics().stream()
                .flatMap(topic -> topic.sourceEvidenceIds().stream()).distinct().toList());
        rules.put(SOURCE_GAP_RULE, recommendation.exercises().stream()
                .map(TrainingRecommendationDto.TrainingExercise::logicGapType).distinct().toList());
        rules.put("trainingTopics", recommendation.topics().stream()
                .map(TrainingRecommendationDto.TrainingTopic::title).toList());
        rules.put("coachingHintsEnabled", true);
        rules.put("coachingReanswerEnabled", true);
        rules.put("coachingReferenceStructureEnabled", true);
        rules.put("focus", focus(recommendation));
        Map<String, Object> coachingRules = new InterviewPlanSettings(
                InterviewMode.COACHING, InterviewerPersona.TECH_LEAD,
                PressureLevel.RELAXED, VerificationStrictness.STANDARD, 0)
                .mergeInto(rules);

        int questionCount = Math.max(4, Math.min(12,
                recommendation.topics().size() * 2 + recommendation.exercises().size()));
        int duration = Math.max(20, Math.min(60, questionCount * 5));
        List<Long> knowledgeIds = recommendation.knowledgeResources().stream()
                .map(TrainingRecommendationDto.KnowledgeResource::documentId).toList();
        String jobTitle = source == null ? session.jobTitle() : source.jobTitle();
        String jobDescription = source == null ? "" : source.jobDescription();
        String trainingDescription = (jobDescription == null ? "" : jobDescription.strip())
                + "\n\n专项训练目标：" + focus(recommendation);
        String domainPackId = source == null
                ? session.domainPack() == null ? null : session.domainPack().id()
                : source.domainPackId();

        return planService.create(userId, new SaveInterviewPlanCommand(
                safeName(session.title() + " · 专项训练"), jobTitle, trainingDescription,
                source == null ? null : source.difficulty(), duration, questionCount,
                source == null ? session.resumeId() : source.resumeId(),
                source == null ? session.profileId() : source.profileId(),
                knowledgeIds, coachingRules, stages(recommendation),
                domainPackId));
    }

    private List<TrainingRecommendationDto.TrainingTopic> topics(
            List<EvaluationEvidence> evidence,
            Map<Long, Integer> questionNumbers
    ) {
        Map<String, List<EvaluationEvidence>> grouped = evidence.stream()
                .filter(item -> item.signal() == EvidenceSignal.NEGATIVE
                        || item.signal() == EvidenceSignal.INSUFFICIENT)
                .collect(Collectors.groupingBy(
                        EvaluationEvidence::competencyCode, LinkedHashMap::new, Collectors.toList()));
        List<TrainingRecommendationDto.TrainingTopic> result = grouped.entrySet().stream()
                .map(entry -> topic(entry.getKey(), entry.getValue(), questionNumbers))
                .sorted(Comparator.comparingInt(TrainingRecommendationDto.TrainingTopic::priority).reversed()
                        .thenComparing(TrainingRecommendationDto.TrainingTopic::title))
                .limit(MAX_TOPICS).toList();
        if (!result.isEmpty()) return result;

        return evidence.stream().min(Comparator.comparingDouble(EvaluationEvidence::confidence))
                .map(item -> List.of(new TrainingRecommendationDto.TrainingTopic(
                        item.competencyCode(), competencyTitle(item.competencyCode()),
                        "当前没有明确负向证据，建议通过复试继续确认低置信度判断：" + truncate(item.reason(), 180),
                        1, List.of(item.id()), question(item, questionNumbers))))
                .orElseGet(() -> List.of(new TrainingRecommendationDto.TrainingTopic(
                        "STRUCTURED_EVIDENCE", "结构化证据表达",
                        "当前证据账本不足，建议围绕个人行动、量化结果和验证方式补充可追溯证据。",
                        1, List.of(), List.of())));
    }

    private TrainingRecommendationDto.TrainingTopic topic(
            String code,
            List<EvaluationEvidence> values,
            Map<Long, Integer> questionNumbers
    ) {
        int priority = values.stream().anyMatch(item -> item.signal() == EvidenceSignal.NEGATIVE)
                ? 3 : 2;
        String rationale = values.stream().sorted(Comparator
                        .comparingDouble((EvaluationEvidence item) -> item.strength() * item.confidence())
                        .reversed())
                .map(EvaluationEvidence::reason).filter(value -> value != null && !value.isBlank())
                .distinct().limit(3).collect(Collectors.joining("；"));
        List<Integer> questions = values.stream().map(EvaluationEvidence::messageId)
                .map(questionNumbers::get).filter(java.util.Objects::nonNull)
                .distinct().sorted().toList();
        return new TrainingRecommendationDto.TrainingTopic(
                code, competencyTitle(code), truncate(rationale, 480), priority,
                values.stream().map(EvaluationEvidence::id).distinct().toList(), questions);
    }

    private List<Integer> question(EvaluationEvidence item, Map<Long, Integer> questionNumbers) {
        Integer question = questionNumbers.get(item.messageId());
        return question == null ? List.of() : List.of(question);
    }

    private List<TrainingRecommendationDto.TrainingExercise> exercises(
            long userId,
            long sessionId
    ) {
        LinkedHashMap<String, LogicGap> unique = new LinkedHashMap<>();
        checkpointMapper.findLatestFirst(userId, sessionId).forEach(checkpoint -> {
            try {
                var logic = stateSerializer.deserialize(checkpoint.getStateJson()).logicChainResult();
                if (logic == null || logic.skipped() || logic.degraded()) return;
                logic.gaps().stream().sorted(Comparator.comparingDouble(LogicGap::severity).reversed())
                        .forEach(gap -> unique.putIfAbsent(
                                gap.type().name() + "\u0000" + gap.description(), gap));
            } catch (RuntimeException ignored) {
                // An invalid historical checkpoint is skipped just like normal recovery.
            }
        });
        return unique.values().stream().sorted(Comparator.comparingDouble(LogicGap::severity).reversed())
                .limit(MAX_EXERCISES).map(gap -> new TrainingRecommendationDto.TrainingExercise(
                        gap.type().name(), gapTitle(gap.type()), exerciseInstruction(gap),
                        gap.severity(), gap.relatedClaimIds())).toList();
    }

    private List<TrainingRecommendationDto.KnowledgeResource> knowledge(
            long userId,
            InterviewSessionDto session,
            List<TrainingRecommendationDto.TrainingTopic> topics,
            List<TrainingRecommendationDto.TrainingExercise> exercises
    ) {
        List<KnowledgeDocumentDto> ready = knowledgeService.listReady(userId);
        Map<Long, KnowledgeDocumentDto> byId = ready.stream()
                .collect(Collectors.toMap(KnowledgeDocumentDto::id, value -> value));
        LinkedHashMap<Long, TrainingRecommendationDto.KnowledgeResource> result = new LinkedHashMap<>();
        session.knowledgeSnapshot().forEach(snapshot -> {
            KnowledgeDocumentDto document = byId.get(snapshot.id());
            if (document != null && result.size() < MAX_KNOWLEDGE) {
                result.put(document.id(), resource(document,
                        "本次面试已冻结关联，可直接用于复习薄弱主题"));
            }
        });
        if (result.size() >= MAX_KNOWLEDGE) return List.copyOf(result.values());

        Set<String> keywords = knowledgeKeywords(topics, exercises);
        ready.stream().filter(document -> !result.containsKey(document.id()))
                .map(document -> Map.entry(document, relevance(document, keywords)))
                .filter(entry -> entry.getValue() > 0)
                .sorted(Map.Entry.<KnowledgeDocumentDto, Integer>comparingByValue().reversed()
                        .thenComparing(entry -> entry.getKey().name()))
                .limit(MAX_KNOWLEDGE - result.size())
                .forEach(entry -> result.put(entry.getKey().id(), resource(
                        entry.getKey(), "文档名称或分类与专项训练主题匹配")));
        return List.copyOf(result.values());
    }

    private TrainingRecommendationDto.KnowledgeResource resource(
            KnowledgeDocumentDto document,
            String reason
    ) {
        return new TrainingRecommendationDto.KnowledgeResource(
                document.id(), document.name(), document.category(), reason);
    }

    private Set<String> knowledgeKeywords(
            List<TrainingRecommendationDto.TrainingTopic> topics,
            List<TrainingRecommendationDto.TrainingExercise> exercises
    ) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        topics.forEach(topic -> {
            values.add(topic.competencyCode().toLowerCase(Locale.ROOT));
            values.add(topic.title().toLowerCase(Locale.ROOT));
            for (String token : topic.competencyCode().toLowerCase(Locale.ROOT).split("_")) {
                if (token.length() >= 3) values.add(token);
            }
        });
        exercises.forEach(exercise -> values.add(exercise.title().toLowerCase(Locale.ROOT)));
        return values;
    }

    private int relevance(KnowledgeDocumentDto document, Set<String> keywords) {
        String text = (document.name() + " " + document.originalName() + " " + document.category())
                .toLowerCase(Locale.ROOT);
        return (int) keywords.stream().filter(value -> !value.isBlank() && text.contains(value)).count();
    }

    private List<String> stages(TrainingRecommendationDto recommendation) {
        LinkedHashSet<String> stages = new LinkedHashSet<>();
        stages.add("INTRODUCTION");
        for (var topic : recommendation.topics()) {
            String code = topic.competencyCode();
            if (code.contains("SYSTEM") || code.contains("ARCHITECT")) stages.add("SYSTEM_DESIGN");
            else if (code.contains("COMMUNICATION") || code.contains("COLLABORATION")
                    || code.contains("LEADERSHIP")) stages.add("BEHAVIORAL");
            else stages.add("TECHNICAL_DEEP_DIVE");
        }
        if (!recommendation.exercises().isEmpty()) stages.add("PROJECT_EXPERIENCE");
        stages.add("SUMMARY");
        return List.copyOf(stages);
    }

    private String focus(TrainingRecommendationDto recommendation) {
        List<String> values = new ArrayList<>();
        recommendation.topics().stream().map(TrainingRecommendationDto.TrainingTopic::title)
                .limit(5).forEach(values::add);
        recommendation.exercises().stream().map(TrainingRecommendationDto.TrainingExercise::title)
                .filter(value -> !values.contains(value)).limit(3).forEach(values::add);
        return values.isEmpty() ? "结构化表达与证据验证" : String.join("、", values);
    }

    private String competencyTitle(String code) {
        return switch (code == null ? "" : code) {
            case "TECHNICAL_FOUNDATION", "TECHNICAL" -> "技术基础";
            case "PROBLEM_SOLVING" -> "问题分析与验证";
            case "PROJECT_EXPERIENCE", "OWNERSHIP" -> "项目贡献与职责边界";
            case "SYSTEM_DESIGN", "ARCHITECTURE" -> "系统设计与取舍";
            case "COMMUNICATION" -> "沟通表达";
            case "COLLABORATION" -> "协作与观点修正";
            case "DECISION_MAKING" -> "决策与风险权衡";
            default -> code == null || code.isBlank() ? "专项能力" : code.replace('_', ' ');
        };
    }

    private String gapTitle(LogicGapType type) {
        return switch (type) {
            case MISSING_BASELINE -> "补齐对照基线";
            case MISSING_MECHANISM -> "解释作用机制";
            case MISSING_EXECUTION_PATH -> "展开执行路径";
            case MISSING_ALTERNATIVES -> "比较备选方案";
            case MISSING_TRADE_OFF -> "说明关键取舍";
            case MISSING_VALIDATION -> "设计结果验证";
            case MISSING_PERSONAL_CONTRIBUTION -> "澄清个人贡献";
            case MISSING_FAILURE_HANDLING -> "补充失败预案";
            case CAUSALITY_JUMP -> "校验因果链";
            case RESULT_WITHOUT_EVIDENCE -> "为结果补充证据";
        };
    }

    private String exerciseInstruction(LogicGap gap) {
        return "围绕“" + truncate(gap.description(), 160)
                + "”重新组织一个回答，至少写出背景、可比较基线、个人行动、关键取舍、量化结果和验证方法。";
    }

    private String safeName(String value) {
        String normalized = value == null ? "专项训练" : value.strip();
        return truncate(normalized.isBlank() ? "专项训练" : normalized, 128);
    }

    private String truncate(String value, int length) {
        if (value == null) return "";
        return value.length() <= length ? value : value.substring(0, length);
    }
}
