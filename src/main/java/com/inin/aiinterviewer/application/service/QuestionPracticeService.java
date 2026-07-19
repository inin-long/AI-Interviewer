package com.inin.aiinterviewer.application.service;

import com.inin.aiinterviewer.application.dto.InterviewQuestionDto;
import com.inin.aiinterviewer.application.exception.BusinessException;
import com.inin.aiinterviewer.application.exception.ErrorCode;
import com.inin.aiinterviewer.agent.support.StructuredAiResponseParser;
import com.inin.aiinterviewer.config.properties.LlmProperties;
import com.inin.aiinterviewer.domain.model.AnswerScore;
import com.inin.aiinterviewer.infrastructure.ai.ChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 题库练习答题的即时 AI 评分服务。
 *
 * 用户在练习页写下自己的答案后提交，本服务调用大模型对回答进行打分与点评，
 * 结果即时返回、不落库（避免额外表结构与 Flyway 迁移风险）。
 */
@Service
public class QuestionPracticeService {

    private static final Logger log = LoggerFactory.getLogger(QuestionPracticeService.class);

    private final QuestionBankService questionBankService;
    private final ChatService chatService;
    private final StructuredAiResponseParser responseParser;
    private final LlmProperties llmProperties;

    public QuestionPracticeService(
            QuestionBankService questionBankService,
            ChatService chatService,
            StructuredAiResponseParser responseParser,
            LlmProperties llmProperties
    ) {
        this.questionBankService = questionBankService;
        this.chatService = chatService;
        this.responseParser = responseParser;
        this.llmProperties = llmProperties;
    }

    /**
     * 根据题目 id 与用户回答，流式调用 AI 打分点评。
     *
     * <p>相比阻塞式 {@code chat()}，本方法在模型刚输出分数（score/correctness/depth）
     * 时就先通过 {@link ScoreEvent.Scores} 回传，让界面无需等待整段点评生成即可显示评分卡，
     * 显著降低“等待感”。完整结果由 {@link ScoreEvent.Done} 在解析完成后回传。
     *
     * @param userId     当前用户
     * @param questionId 题目 id
     * @param userAnswer 用户作答内容
     * @return 评分事件流（Scores → Done / Error）
     */
    public Flux<ScoreEvent> streamScore(long userId, long questionId, String userAnswer) {
        if (userAnswer == null || userAnswer.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        if (!llmProperties.isConfigured()) {
            throw new BusinessException(ErrorCode.AI_NOT_CONFIGURED);
        }

        InterviewQuestionDto question = questionBankService.listQuestions(userId).stream()
                .filter(item -> item.id() != null && item.id().equals(questionId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));

        String prompt = buildPrompt(question, userAnswer.strip());

        return Flux.create(sink -> {
            StringBuilder buffer = new StringBuilder();
            AtomicBoolean scoreEmitted = new AtomicBoolean(false);
            AtomicReference<PartialScore> lastScores = new AtomicReference<>();
            Disposable upstream = chatService.stream(prompt).subscribe(
                    chunk -> {
                        buffer.append(chunk);
                        if (!scoreEmitted.get()) {
                            PartialScore partial = extractScores(buffer.toString());
                            if (partial != null) {
                                scoreEmitted.set(true);
                                lastScores.set(partial);
                                sink.next(new ScoreEvent.Scores(
                                        partial.score(), partial.correctness(), partial.depth()));
                            }
                        }
                    },
                    sink::error,
                    () -> {
                        AnswerScore full = safeParse(buffer.toString());
                        if (full != null) {
                            sink.next(new ScoreEvent.Done(full));
                        } else if (lastScores.get() != null) {
                            // 分数已拿到但整段 JSON 无法解析：用已得分数兜底
                            PartialScore p = lastScores.get();
                            sink.next(new ScoreEvent.Done(new AnswerScore(
                                    p.score(), p.correctness(), p.depth(),
                                    java.util.List.of(), java.util.List.of(), "", "")));
                        } else {
                            sink.next(new ScoreEvent.Error("AI 返回内容无法解析为评分结果"));
                        }
                        sink.complete();
                    });
            sink.onDispose(upstream::dispose);
        });
    }

    /**
     * 从（可能不完整的）模型输出中提前抽取分数段。
     * 只扫描 {@code "strengths"} 之前的区域，避免被点评正文里的数字干扰。
     */
    private PartialScore extractScores(String text) {
        int cut = text.indexOf("\"strengths\"");
        String region = cut >= 0 ? text.substring(0, cut) : text;
        Integer score = findInt(region, "\"score\"");
        Integer correctness = findInt(region, "\"correctness\"");
        Integer depth = findInt(region, "\"depth\"");
        if (score == null || correctness == null || depth == null) {
            return null;
        }
        return new PartialScore(clamp(score), clamp(correctness), clamp(depth));
    }

    private Integer findInt(String text, String key) {
        int i = text.indexOf(key);
        if (i < 0) return null;
        int colon = text.indexOf(':', i + key.length());
        if (colon < 0) return null;
        int j = colon + 1;
        while (j < text.length() && Character.isWhitespace(text.charAt(j))) j++;
        int k = j;
        while (k < text.length() && Character.isDigit(text.charAt(k))) k++;
        if (k == j) return null;
        try {
            return Integer.parseInt(text.substring(j, k));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private AnswerScore safeParse(String text) {
        try {
            return normalize(responseParser.parse(text, AnswerScore.class));
        } catch (RuntimeException e) {
            return null;
        }
    }

    private record PartialScore(int score, int correctness, int depth) {}

    private String buildPrompt(InterviewQuestionDto question, String userAnswer) {
        String job = question.jobTitle() == null || question.jobTitle().isBlank()
                ? "通用" : question.jobTitle();
        String difficulty = question.difficulty() == null ? "未标注" : switch (question.difficulty()) {
            case JUNIOR -> "初级";
            case MEDIUM -> "中级";
            case SENIOR -> "高级";
            case EXPERT -> "专家";
        };
        String reference = question.referenceAnswer() == null || question.referenceAnswer().isBlank()
                ? "（无参考答案，请依据专业常识评判）" : question.referenceAnswer();

        return """
                你是一位资深面试官，请对候选人针对以下面试题的回答进行专业评分与点评。
                
                【岗位方向】%s
                【题目难度】%s
                【题目标题】%s
                【题目内容】
                %s
                
                【参考答案 / 评分要点】
                %s
                
                【候选人回答】
                %s
                
                评分要求：
                1. score / correctness / depth 均为 0-100 整数，要客观、有区分度。
                2. strengths 写 3-4 条具体优点，每条指出答得好的具体点（不要泛泛而谈）。
                3. weaknesses 写 2-3 条具体不足或遗漏点，同样要具体。
                4. suggestion 写一句可执行的改进建议。
                5. feedback 写一段通顺的总评（150-300 字），像面试官给候选人的书面反馈一样自然，
                   涵盖整体表现评价 + 核心亮点 + 最需要补强的方向。
                6. 字符限制（非常重要）：所有字段只允许使用「中文、英文、阿拉伯数字、常规中英文标点
                   （，。！？：；""''（）《》、. , ! ? : ; ' " ( ) 等）」。
                   绝对不要使用任何 emoji、特殊符号或装饰性字符（如 ★ ◆ ● ▲ ✓ ⚠ ✅ 等），
                   也不要用 Markdown 代码块包裹。
                
                严格只输出如下 JSON，不要包含任何多余文字、解释或 Markdown 代码块标记：
                {
                  "score": 综合得分（0-100 整数）,
                  "correctness": 答案正确性/契合度得分（0-100 整数）,
                  "depth": 思考深度/完整度得分（0-100 整数）,
                  "strengths": ["具体优点1", "具体优点2", "具体优点3"],
                  "weaknesses": ["具体不足1", "具体不足2"],
                  "suggestion": "一句可执行的改进建议",
                  "feedback": "一段通顺的自然语言总评，150-300字"
                }
                """.formatted(job, difficulty, question.title(), question.content(), reference, userAnswer);
    }

    private AnswerScore normalize(AnswerScore raw) {
        return new AnswerScore(
                clamp(raw.score()),
                clamp(raw.correctness()),
                clamp(raw.depth()),
                raw.strengths().stream().map(this::sanitize).toList(),
                raw.weaknesses().stream().map(this::sanitize).toList(),
                sanitize(raw.suggestion()),
                sanitize(raw.feedback())
        );
    }

    /**
     * 清洗模型输出里的 emoji / 装饰性符号（★ ◆ ● ▲ ✓ ⚠ 等）与零宽变体选择符，
     * 保留中文、英文、数字和常规中英文标点。WebView 渲染缺失字形时会显示成方块乱码，
     * 故在返回前端前统一剥离这些字符。
     */
    private static final java.util.regex.Pattern GARBAGE = java.util.regex.Pattern.compile(
            "[\u2190-\u21FF\u2300-\u23FF\u2460-\u24FF\u25A0-\u25FF\u2600-\u27BF\u2B00-\u2BFF\uFE00-\uFE0F]"
                    + "|[\uD800-\uDBFF][\uDC00-\uDFFF]");

    private String sanitize(String text) {
        if (text == null) return null;
        return GARBAGE.matcher(text).replaceAll("").strip();
    }

    private int clamp(int value) {
        if (value < 0) return 0;
        if (value > 100) return 100;
        return value;
    }
}
