package com.inin.aiinterviewer.application.service;

import com.inin.aiinterviewer.domain.model.AnswerScore;

/**
 * 题库练习评分的流式事件。
 * <ul>
 *   <li>{@link Scores}：分数段先回传（模型输出开头即可解析），用于让界面尽快展示评分卡。</li>
 *   <li>{@link Done}：完整解析成功后的最终评分结果（含详细点评）。</li>
 *   <li>{@link Error}：解析或调用失败。</li>
 * </ul>
 */
public sealed interface ScoreEvent permits ScoreEvent.Scores, ScoreEvent.Done, ScoreEvent.Error {

    /** 早期分数：模型刚输出 score/correctness/depth 即可先行回传。 */
    record Scores(int score, int correctness, int depth) implements ScoreEvent {}

    /** 完整评分结果（含 strengths / weaknesses / suggestion / feedback）。 */
    record Done(AnswerScore result) implements ScoreEvent {}

    /** 错误信息。 */
    record Error(String message) implements ScoreEvent {}
}
