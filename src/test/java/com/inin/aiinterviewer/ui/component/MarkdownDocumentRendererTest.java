package com.inin.aiinterviewer.ui.component;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MarkdownDocumentRendererTest {
    private final MarkdownDocumentRenderer renderer = new MarkdownDocumentRenderer();

    @Test
    void rendersHeadingsTablesAndEscapesExecutableContent() {
        String html = renderer.render("""
                # 面试报告

                | 维度 | 得分 |
                | --- | ---: |
                | 技术 | 88 |

                <script>alert('unsafe')</script>

                [危险链接](javascript:alert('unsafe'))

                ![远程图片](https://example.com/tracker.png "外部图片")
                """);

        assertThat(html).contains("<h1>面试报告</h1>", "<table>", ">88</td>");
        assertThat(html).contains("&lt;script&gt;alert('unsafe')&lt;/script&gt;");
        assertThat(html).doesNotContain("<script>alert", "javascript:alert");
        assertThat(html).contains("外部图片").doesNotContain("<img", "tracker.png");
    }

    @Test
    void providesReadableEmptyState() {
        assertThat(renderer.render(" ")).contains("暂无报告内容", "报告尚未生成或内容为空");
    }
}
