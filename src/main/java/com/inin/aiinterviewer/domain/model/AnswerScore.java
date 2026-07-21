package com.inin.aiinterviewer.domain.model;

import java.io.Serializable;
import java.util.List;

/**
 * 题库练习答题的 AI 评分结果（即时评分，不落库）。
 *
 * @param score       综合得分（0~100）
 * @param correctness 答案正确性 / 契合度得分（0~100）
 * @param depth       思考深度 / 完整度得分（0~100）
 * @param strengths   回答中的优点
 * @param weaknesses  回答中的不足 / 遗漏点
 * @param suggestion  改进建议（一段话）
 * @param feedback    整体点评（Markdown 文本）
 */
public record AnswerScore(
        int score,
        int correctness,
        int depth,
        List<String> strengths,
        List<String> weaknesses,
        String suggestion,
        String feedback
) implements Serializable {
    public AnswerScore {
        strengths = strengths == null ? List.of() : List.copyOf(strengths);
        weaknesses = weaknesses == null ? List.of() : List.copyOf(weaknesses);
    }
}
