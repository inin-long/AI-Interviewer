package com.inin.aiinterviewer.agent.node;

import com.inin.aiinterviewer.agent.model.QuestionQualityContext;
import com.inin.aiinterviewer.agent.model.QuestionQualityIssue;
import com.inin.aiinterviewer.agent.model.QuestionQualityResult;
import com.inin.aiinterviewer.domain.enums.InterviewDifficulty;
import com.inin.aiinterviewer.domain.enums.InterviewStage;
import com.inin.aiinterviewer.domain.model.Message;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class QuestionQualityGateNode {

    private static final List<String> ANSWER_LEAKS = List.of(
            "正确答案是", "参考答案是", "标准答案", "你应该使用", "显然应该", "答案其实是");
    private static final List<String> AGGRESSIVE_PRESSURE = List.of(
            "你根本不懂", "这都不会", "能力太差", "撒谎", "不诚实", "愚蠢", "闭嘴", "废物");
    private static final List<String> OFF_TOPIC = List.of(
            "婚姻", "生育", "星座", "籍贯", "宗教", "家庭收入", "父母职业", "政治立场");
    private static final List<String> GENERIC_QUESTIONS = List.of(
            "请介绍一下你自己", "请做一下自我介绍", "你有什么优点", "你有什么缺点",
            "为什么选择我们公司", "你为什么离职");
    private static final List<String> EVIDENCE_WORDS = List.of(
            "请", "如何", "什么", "为何", "为什么", "怎样", "哪些", "是否", "能否", "说明", "介绍");
    private static final Pattern FABRICATED_EMPLOYMENT = Pattern.compile(
            "你(?:曾在|在)([^，。！？?]{2,32})(?:工作|任职|担任|负责)");

    public QuestionQualityResult review(QuestionQualityContext context, String question) {
        String normalized = normalizeText(question);
        Set<QuestionQualityIssue> issues = new LinkedHashSet<>();
        if (normalized.length() < 6 || normalized.length() > 600) {
            issues.add(QuestionQualityIssue.EMPTY_OR_UNCLEAR);
        }
        if (questionCount(question) > 1) {
            issues.add(QuestionQualityIssue.TOO_MANY_QUESTIONS);
        }
        if (containsAny(normalized, ANSWER_LEAKS)) {
            issues.add(QuestionQualityIssue.REFERENCE_ANSWER_LEAK);
        }
        if (containsAny(normalized, AGGRESSIVE_PRESSURE)) {
            issues.add(QuestionQualityIssue.MEANINGLESS_PRESSURE);
        }
        if (containsAny(normalized, OFF_TOPIC)) {
            issues.add(QuestionQualityIssue.COMPETENCY_MISMATCH);
            issues.add(QuestionQualityIssue.UNDECLARED_BACKGROUND);
        }
        if (!containsAny(normalized, EVIDENCE_WORDS)) {
            issues.add(QuestionQualityIssue.NOT_EVIDENCE_SEEKING);
        }
        if (isDuplicate(context, normalized)) {
            issues.add(QuestionQualityIssue.DUPLICATE_QUESTION);
        }
        if (hasSpecificTarget(context) && containsAny(normalized, GENERIC_QUESTIONS)) {
            issues.add(QuestionQualityIssue.TARGET_MISMATCH);
        }
        if (isStageMismatch(context.stage(), normalized)) {
            issues.add(QuestionQualityIssue.STAGE_MISMATCH);
        }
        if (isDifficultyMismatch(context, normalized)) {
            issues.add(QuestionQualityIssue.DIFFICULTY_MISMATCH);
        }
        if (fabricatesCandidateContext(context, question)) {
            issues.add(QuestionQualityIssue.FABRICATED_CANDIDATE_CONTEXT);
        }
        return issues.isEmpty()
                ? QuestionQualityResult.pass()
                : QuestionQualityResult.rejected(List.copyOf(issues));
    }

    public String fallback(QuestionQualityContext context) {
        if (context.probePlan() != null && !context.probePlan().objective().isBlank()) {
            String objective = sanitizeObjective(context.probePlan().objective());
            if (objective.endsWith("？") || objective.endsWith("?")) return objective;
            String evidence = context.probePlan().expectedEvidence().isEmpty()
                    ? "具体判断、个人行动和可验证依据"
                    : String.join("、", context.probePlan().expectedEvidence().stream().limit(3).toList());
            return "请围绕“%s”，说明你的%s。".formatted(objective, evidence);
        }
        String role = context.plan() == null || context.plan().jobTitle() == null
                ? "目标岗位" : context.plan().jobTitle();
        return switch (context.stage()) {
            case INTRODUCTION -> "请用两到三分钟介绍与你申请的%s最相关的一段经历，并说明你承担的职责。".formatted(role);
            case RESUME_REVIEW -> "请选择简历中与%s最相关的一段经历，说明背景、个人行动和可验证结果。".formatted(role);
            case PROJECT_EXPERIENCE -> "请介绍一个最能体现你岗位能力的项目，并说明关键决策、个人贡献和结果依据。";
            case TECHNICAL_DEEP_DIVE -> "请选取一个关键技术决策，说明约束、备选方案、取舍依据和验证方式。";
            case SYSTEM_DESIGN -> "请说明你会如何划分系统边界，并给出容量、故障恢复和演进方面的取舍依据。";
            case CODING -> "请说明你会如何拆解当前编程问题，并分析核心实现的复杂度和边界条件。";
            case BEHAVIORAL -> "请介绍一次需要跨团队推进的问题，说明分歧、你的行动和最终结果。";
            case SUMMARY, COMPLETED -> "请总结本次讨论中最能代表你岗位能力的一项决策及其可验证依据。";
        };
    }

    private boolean hasSpecificTarget(QuestionQualityContext context) {
        if (context.probePlan() == null) return false;
        return context.probePlan().targetsClaim()
                || context.probePlan().targetsLogicGap()
                || context.probePlan().targetsConsistencyIssue()
                || context.probePlan().targetsDeferredProbe()
                || context.probePlan().targetsCompetency()
                || context.probePlan().shouldInjectScenario();
    }

    private boolean isDuplicate(QuestionQualityContext context, String question) {
        for (Message message : context.messages()) {
            if (message.role() != Message.Role.ASSISTANT) continue;
            String previous = normalizeText(message.content());
            if (previous.equals(question) || dice(previous, question) >= 0.92) return true;
        }
        return false;
    }

    private boolean isStageMismatch(InterviewStage stage, String question) {
        if (stage == InterviewStage.INTRODUCTION) return false;
        return question.contains("自我介绍") || question.contains("介绍一下你自己");
    }

    private boolean isDifficultyMismatch(QuestionQualityContext context, String question) {
        if (context.plan() == null
                || (context.plan().difficulty() != InterviewDifficulty.SENIOR
                && context.plan().difficulty() != InterviewDifficulty.EXPERT)) return false;
        if (context.stage() != InterviewStage.TECHNICAL_DEEP_DIVE
                && context.stage() != InterviewStage.SYSTEM_DESIGN) return false;
        return question.length() < 32 && (question.contains("什么是") || question.contains("定义是什么"));
    }

    private boolean fabricatesCandidateContext(QuestionQualityContext context, String question) {
        Matcher matcher = FABRICATED_EMPLOYMENT.matcher(question);
        if (!matcher.find()) return false;
        String assertedContext = normalizeText(matcher.group(1));
        if (assertedContext.isBlank()) return false;
        StringBuilder declared = new StringBuilder(context.candidateProfileContext())
                .append(' ').append(context.domainPackContext());
        if (context.plan() != null) {
            declared.append(' ').append(context.plan().jobTitle())
                    .append(' ').append(context.plan().jobDescription());
        }
        context.messages().forEach(message -> declared.append(' ').append(message.content()));
        return !normalizeText(declared.toString()).contains(assertedContext);
    }

    private int questionCount(String value) {
        if (value == null) return 0;
        return (int) value.chars().filter(character -> character == '?' || character == '？').count();
    }

    private boolean containsAny(String value, List<String> candidates) {
        return candidates.stream().anyMatch(value::contains);
    }

    private String sanitizeObjective(String objective) {
        String sanitized = objective.replaceAll("[\\r\\n]+", " ").strip();
        if (sanitized.length() > 160) sanitized = sanitized.substring(0, 160).strip();
        return sanitized.replace("“", "").replace("”", "");
    }

    private String normalizeText(String value) {
        if (value == null) return "";
        return value.toLowerCase(Locale.ROOT).replaceAll("[\\s，。！？；：、,.!?;:\\-_'\"“”‘’（）()]+", "");
    }

    private double dice(String left, String right) {
        if (left.length() < 2 || right.length() < 2) return left.equals(right) ? 1 : 0;
        List<String> leftPairs = pairs(left);
        List<String> rightPairs = new ArrayList<>(pairs(right));
        int matches = 0;
        for (String pair : leftPairs) {
            if (rightPairs.remove(pair)) matches++;
        }
        return 2.0 * matches / (leftPairs.size() + rightPairs.size());
    }

    private List<String> pairs(String value) {
        List<String> pairs = new ArrayList<>();
        for (int i = 0; i < value.length() - 1; i++) pairs.add(value.substring(i, i + 2));
        return pairs;
    }
}
