package com.inin.aiinterviewer.ui.controller;

import com.inin.aiinterviewer.agent.model.EvaluationPayload;
import com.inin.aiinterviewer.application.dto.InterviewReportDto;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class InterviewReportControllerTest {

    @Test
    void refreshesPersistedCitationSectionWhenOpeningAnExistingReport() {
        String persisted = """
                # 面试报告

                ## 综合评价

                表现稳定。

                ## 参考依据

                - **旧文档** · 片段 6

                  > # 原始 Markdown
                """;

        String refreshed = InterviewReportController.refreshCitationSection(
                persisted,
                "- **知识库** · 片段 1\n\n  > 已清洗的可读内容");

        assertThat(refreshed)
                .contains("## 综合评价", "## 参考依据", "片段 1", "已清洗的可读内容")
                .doesNotContain("片段 6", "# 原始 Markdown");
    }

    @Test
    void alignsMarkdownScoreTableWithEvidenceBackedSidebarValues() {
        EvaluationPayload.EvidenceTrace scored = new EvaluationPayload.EvidenceTrace(
                true, 0.8, List.of("E1"), List.of(1L), List.of("C1"), "证据充分");
        EvaluationPayload.EvidenceTrace insufficient = new EvaluationPayload.EvidenceTrace(
                false, 0.2, List.of(), List.of(), List.of(), "证据不足");
        InterviewReportDto report = new InterviewReportDto(
                1, 2, "Java 后端面试", 70,
                Map.of(
                        "technical", 75,
                        "problemSolving", 65,
                        "project", 70,
                        "communication", 60,
                        "comprehensive", 68),
                "summary", "",
                Map.of(), List.of(),
                Map.of(
                        "technical", scored,
                        "problemSolving", insufficient,
                        "project", insufficient,
                        "communication", scored,
                        "comprehensive", scored),
                0.7, true);

        String aligned = InterviewReportController.synchronizeScoreTable("""
                | 维度 | 得分 |
                | --- | --- |
                | 技术基础 | 75.0 |
                | 问题解决 | 65.0 |
                | 项目经验 | 70.0 |
                | 沟通表达 | 60.0 |
                | 综合能力 | 68.0 |
                """, report);

        assertThat(aligned)
                .contains("| 技术基础 | 75.0 |")
                .contains("| 问题解决 | 证据不足 |")
                .contains("| 项目经验 | 证据不足 |")
                .contains("| 沟通表达 | 60.0 |")
                .contains("| 综合能力 | 68.0 |")
                .doesNotContain("| 问题解决 | 65.0 |", "| 项目经验 | 70.0 |");
    }
}
