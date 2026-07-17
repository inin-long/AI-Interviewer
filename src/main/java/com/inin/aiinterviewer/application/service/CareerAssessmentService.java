package com.inin.aiinterviewer.application.service;

import com.inin.aiinterviewer.application.dto.AssessmentOption;
import com.inin.aiinterviewer.application.dto.AssessmentQuestionDto;
import com.inin.aiinterviewer.application.dto.AssessmentResultDto;
import com.inin.aiinterviewer.application.dto.AssessmentTemplateDto;
import com.inin.aiinterviewer.application.exception.BusinessException;
import com.inin.aiinterviewer.application.exception.ErrorCode;
import com.inin.aiinterviewer.application.util.JsonUtils;
import com.inin.aiinterviewer.domain.entity.AssessmentAnswerEntity;
import com.inin.aiinterviewer.domain.entity.AssessmentQuestionEntity;
import com.inin.aiinterviewer.domain.entity.AssessmentResultEntity;
import com.inin.aiinterviewer.domain.entity.AssessmentTemplateEntity;
import com.inin.aiinterviewer.infrastructure.database.mapper.AssessmentAnswerMapper;
import com.inin.aiinterviewer.infrastructure.database.mapper.AssessmentQuestionMapper;
import com.inin.aiinterviewer.infrastructure.database.mapper.AssessmentResultMapper;
import com.inin.aiinterviewer.infrastructure.database.mapper.AssessmentTemplateMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class CareerAssessmentService {

    private static final List<String> HOLLAND_ORDER = List.of("R", "I", "A", "S", "E", "C");
    private static final Map<String, String> HOLLAND_NAME = Map.of(
            "R", "现实型", "I", "研究型", "A", "艺术型",
            "S", "社会型", "E", "企业型", "C", "常规型");
    private static final Map<String, String> HOLLAND_DESC = Map.of(
            "R", "喜欢动手操作、使用工具与机械，偏好具体可感知的任务。",
            "I", "喜欢观察、分析、探索规律，享受思考与解决抽象问题。",
            "A", "喜欢创造、表达与想象，追求美感与独特性。",
            "S", "喜欢帮助、教导、支持他人，重视人际与协作。",
            "E", "喜欢领导、说服、达成目标，乐于影响与组织。",
            "C", "喜欢秩序、精确、按规则行事，重视稳定与细节。");
    private static final Map<String, String> MBTI_DESC = Map.of(
            "E", "精力来自人际互动与外界刺激，乐于表达与协作。",
            "I", "精力来自独处与深度思考，偏好专注与内省。",
            "S", "关注具体事实、经验与细节，重视落地与可靠。",
            "N", "关注未来可能、整体含义与联想，重视想象与方向。",
            "T", "以逻辑、因果与客观标准做决定，追求公正一致。",
            "F", "以价值、感受与他人处境做决定，追求和谐体谅。",
            "J", "喜欢计划、确定与闭环，倾向提前安排。",
            "P", "喜欢灵活、开放与适应，倾向保留选项。");

    private final AssessmentTemplateMapper templateMapper;
    private final AssessmentQuestionMapper questionMapper;
    private final AssessmentResultMapper resultMapper;
    private final AssessmentAnswerMapper answerMapper;

    public CareerAssessmentService(
            AssessmentTemplateMapper templateMapper,
            AssessmentQuestionMapper questionMapper,
            AssessmentResultMapper resultMapper,
            AssessmentAnswerMapper answerMapper
    ) {
        this.templateMapper = templateMapper;
        this.questionMapper = questionMapper;
        this.resultMapper = resultMapper;
        this.answerMapper = answerMapper;
    }

    @Transactional(readOnly = true)
    public List<AssessmentTemplateDto> listTemplates() {
        return templateMapper.findAll().stream()
                .map(entity -> new AssessmentTemplateDto(entity.getId(), entity.getCode(),
                        entity.getTitle(), entity.getDescription()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AssessmentQuestionDto> getQuestions(String templateCode) {
        AssessmentTemplateEntity template = requireTemplate(templateCode);
        return questionMapper.findByTemplateId(template.getId()).stream()
                .map(this::toQuestionDto)
                .toList();
    }

    @Transactional
    public AssessmentResultDto submit(String templateCode, long userId, List<Integer> optionIndices) {
        AssessmentTemplateEntity template = requireTemplate(templateCode);
        List<AssessmentQuestionEntity> questions = questionMapper.findByTemplateId(template.getId());
        if (optionIndices == null || optionIndices.size() != questions.size()) {
            throw new BusinessException(ErrorCode.ASSESSMENT_INCOMPLETE);
        }

        Map<String, Integer> scores = new LinkedHashMap<>();
        for (int i = 0; i < questions.size(); i++) {
            AssessmentQuestionEntity question = questions.get(i);
            List<AssessmentOption> options = JsonUtils.readList(question.getOptionsJson(), AssessmentOption.class);
            int index = optionIndices.get(i);
            if (index < 0 || index >= options.size()) {
                throw new BusinessException(ErrorCode.ASSESSMENT_INCOMPLETE);
            }
            scores.merge(options.get(index).score(), 1, Integer::sum);
        }

        String resultCode = template.getCode().equals("HOLLAND")
                ? hollandCode(scores)
                : mbtiCode(scores);

        String report = buildReport(template.getCode(), scores, resultCode);

        AssessmentResultEntity result = new AssessmentResultEntity();
        result.setUserId(userId);
        result.setTemplateCode(template.getCode());
        result.setResultCode(resultCode);
        result.setScoresJson(JsonUtils.toJson(scores));
        result.setReportMarkdown(report);
        resultMapper.insert(result);

        for (int i = 0; i < questions.size(); i++) {
            AssessmentAnswerEntity answer = new AssessmentAnswerEntity();
            answer.setResultId(result.getId());
            answer.setQuestionId(questions.get(i).getId());
            answer.setOptionIndex(optionIndices.get(i));
            answerMapper.insert(answer);
        }

        return toResultDto(result);
    }

    @Transactional(readOnly = true)
    public List<AssessmentResultDto> listResults(long userId) {
        return resultMapper.findAllByUserId(userId).stream()
                .map(this::toResultDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public AssessmentResultDto getResult(long userId, long resultId) {
        return resultMapper.findByIdAndUserId(resultId, userId)
                .map(this::toResultDto)
                .orElseThrow(() -> new BusinessException(ErrorCode.ASSESSMENT_NOT_FOUND));
    }

    @Transactional
    public void deleteResult(long userId, long resultId) {
        if (resultMapper.logicalDelete(resultId, userId) != 1) {
            throw new BusinessException(ErrorCode.ASSESSMENT_NOT_FOUND);
        }
    }

    private String hollandCode(Map<String, Integer> scores) {
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(scores.entrySet());
        boolean anyPositive = entries.stream().anyMatch(e -> e.getValue() > 0);
        entries.sort(Comparator
                .comparingInt((Map.Entry<String, Integer> e) -> -e.getValue())
                .thenComparingInt(e -> HOLLAND_ORDER.indexOf(e.getKey())));
        StringBuilder code = new StringBuilder();
        for (Map.Entry<String, Integer> entry : entries) {
            if (code.length() >= 3) break;
            if (anyPositive && entry.getValue() <= 0) continue;
            code.append(entry.getKey());
        }
        while (code.length() < 3) {
            for (String letter : HOLLAND_ORDER) {
                if (code.indexOf(letter) < 0) {
                    code.append(letter);
                    break;
                }
            }
        }
        return code.toString();
    }

    private String mbtiCode(Map<String, Integer> scores) {
        String eOrI = scores.getOrDefault("E", 0) >= scores.getOrDefault("I", 0) ? "E" : "I";
        String sOrN = scores.getOrDefault("N", 0) >= scores.getOrDefault("S", 0) ? "N" : "S";
        String tOrF = scores.getOrDefault("T", 0) >= scores.getOrDefault("F", 0) ? "T" : "F";
        String jOrP = scores.getOrDefault("J", 0) >= scores.getOrDefault("P", 0) ? "J" : "P";
        return eOrI + sOrN + tOrF + jOrP;
    }

    private String buildReport(String templateCode, Map<String, Integer> scores, String resultCode) {
        if (templateCode.equals("HOLLAND")) {
            return buildHollandReport(scores, resultCode);
        }
        return buildMbtiReport(scores, resultCode);
    }

    private String buildHollandReport(Map<String, Integer> scores, String resultCode) {
        StringBuilder sb = new StringBuilder();
        sb.append("# 霍兰德职业兴趣报告\n\n");
        sb.append("**你的兴趣代码：").append(resultCode).append("**\n\n");
        sb.append("| 类型 | 得分 |\n| --- | --- |\n");
        for (String letter : HOLLAND_ORDER) {
            sb.append("| ").append(letter).append(" ").append(HOLLAND_NAME.get(letter)).append(" | ")
                    .append(scores.getOrDefault(letter, 0)).append(" |\n");
        }
        sb.append("\n## 类型解读\n");
        for (String letter : HOLLAND_ORDER) {
            boolean top = resultCode.indexOf(letter) >= 0;
            sb.append("### ").append(letter).append(" ").append(HOLLAND_NAME.get(letter));
            if (top) sb.append("（高分类型）");
            sb.append("\n").append(HOLLAND_DESC.get(letter)).append("\n");
        }
        sb.append("\n## 职业方向建议\n");
        sb.append("- 优先结合你的高分类型：");
        for (int i = 0; i < resultCode.length(); i++) {
            String letter = String.valueOf(resultCode.charAt(i));
            sb.append(HOLLAND_NAME.get(letter)).append("（").append(letter).append("）");
            if (i < resultCode.length() - 1) sb.append("、");
        }
        sb.append("。\n");
        sb.append("- 在「岗位库与面试题库」中按对应方向筛选面试题，做针对性练习。\n");
        sb.append("\n## 下一步\n");
        sb.append("- 用「AI 职业规划」生成从当前岗位到目标岗位的技能学习路线。\n");
        sb.append("- 在「面试技巧库」中复习 STAR 法则与行为题表达。\n");
        return sb.toString();
    }

    private String buildMbtiReport(Map<String, Integer> scores, String resultCode) {
        StringBuilder sb = new StringBuilder();
        sb.append("# MBTI 性格类型报告\n\n");
        sb.append("**你的类型：").append(resultCode).append("**\n\n");
        sb.append("| 维度 | 你的倾向 | 分数 |\n| --- | --- | --- |\n");
        sb.append("| 精力导向 | 外向 E / 内向 I | E ").append(scores.getOrDefault("E", 0))
                .append(" / I ").append(scores.getOrDefault("I", 0)).append(" |\n");
        sb.append("| 信息获取 | 实感 S / 直觉 N | S ").append(scores.getOrDefault("S", 0))
                .append(" / N ").append(scores.getOrDefault("N", 0)).append(" |\n");
        sb.append("| 决策方式 | 思考 T / 情感 F | T ").append(scores.getOrDefault("T", 0))
                .append(" / F ").append(scores.getOrDefault("F", 0)).append(" |\n");
        sb.append("| 生活态度 | 判断 J / 感知 P | J ").append(scores.getOrDefault("J", 0))
                .append(" / P ").append(scores.getOrDefault("P", 0)).append(" |\n\n");
        sb.append("## 类型解读\n");
        for (char letter : resultCode.toCharArray()) {
            String key = String.valueOf(letter);
            sb.append("- **").append(key).append("**：").append(MBTI_DESC.get(key)).append("\n");
        }
        sb.append("\n## 适合的工作方式\n");
        sb.append("- 发挥你的性格优势安排学习与协作节奏，避免长期处在相反极性的压力下。\n");
        sb.append("- 面试中可主动说明自己偏好的沟通与决策方式，让面试官看到自我认知。\n");
        sb.append("\n## 下一步\n");
        sb.append("- 用「AI 职业规划」把性格倾向落到具体的岗位与技能路线。\n");
        sb.append("- 在「面试技巧库」中练习把经历讲成有结构的故事。\n");
        return sb.toString();
    }

    private AssessmentTemplateEntity requireTemplate(String templateCode) {
        return templateMapper.findByCode(templateCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.ASSESSMENT_NOT_FOUND));
    }

    private AssessmentQuestionDto toQuestionDto(AssessmentQuestionEntity entity) {
        List<AssessmentOption> options = JsonUtils.readList(entity.getOptionsJson(), AssessmentOption.class);
        return new AssessmentQuestionDto(entity.getId(), entity.getTemplateId(), entity.getDimension(),
                entity.getContent(), options, entity.getSortOrder());
    }

    private AssessmentResultDto toResultDto(AssessmentResultEntity entity) {
        Map<String, Integer> scores = JsonUtils.readScoreMap(entity.getScoresJson());
        return new AssessmentResultDto(entity.getId(), entity.getTemplateCode(), entity.getResultCode(),
                scores, entity.getReportMarkdown(), entity.getCreateTime());
    }
}
