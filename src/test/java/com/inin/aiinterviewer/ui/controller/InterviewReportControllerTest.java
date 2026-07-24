package com.inin.aiinterviewer.ui.controller;

import org.junit.jupiter.api.Test;

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
}
