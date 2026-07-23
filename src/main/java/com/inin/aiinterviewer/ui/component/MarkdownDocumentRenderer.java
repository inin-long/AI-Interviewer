package com.inin.aiinterviewer.ui.component;

import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.Image;
import org.commonmark.node.Node;
import org.commonmark.node.Text;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import java.util.List;

public final class MarkdownDocumentRenderer {
    private static final List<Extension> EXTENSIONS = List.of(TablesExtension.create());
    private static final Parser PARSER = Parser.builder().extensions(EXTENSIONS).build();
    private static final HtmlRenderer RENDERER = HtmlRenderer.builder()
            .extensions(EXTENSIONS)
            .escapeHtml(false)
            .sanitizeUrls(true)
            .percentEncodeUrls(true)
            .build();

    public String render(String markdown) {
        return render(markdown, false);
    }

    public String renderCompact(String markdown) {
        return render(markdown, true);
    }

    private String render(String markdown, boolean compact) {
        String source = markdown == null || markdown.isBlank()
                ? "# 暂无报告内容\n\n报告尚未生成或内容为空。"
                : markdown;
        Node document = PARSER.parse(source);
        removeRemoteImages(document);
        String body = RENDERER.render(document);
        String bodyPadding = compact ? "6px 0 0" : "34px 42px 56px";
        String articleMargin = compact ? "0" : "0 auto";
        String h1Margin = compact ? "0 0 6px" : "0 0 28px";
        String h2Margin = compact ? "10px 0 4px" : "34px 0 14px";
        String h3Margin = compact ? "8px 0 3px" : "26px 0 10px";
        String pMargin = compact ? "2px 0 0" : "10px 0";
        String ulPadding = compact ? "0 0 0 18px" : "0 0 0 24px";
        String ulMargin = compact ? "3px 0 0" : "10px 0";
        String liMargin = compact ? "1px 0 0" : "4px 0";
        String tableMargin = compact ? "6px 0 0" : "18px 0 26px";
        String blockquoteMargin = compact ? "6px 0 0" : "18px 0";
        String preMargin = compact ? "6px 0 0" : "18px 0";
        String prePadding = compact ? "10px" : "16px";
        String hrMargin = compact ? "6px 0 0" : "28px 0";
        String template = """
                <!doctype html>
                <html lang="zh-CN">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <style>
                    :root { color-scheme: light; }
                    * { box-sizing: border-box; }
                    html { scroll-behavior: smooth; background: #ffffff; }
                    body {
                      margin: 0; padding: %s; color: #20242d;
                      background: #ffffff;
                      font-family: "Segoe UI", "Microsoft YaHei UI", sans-serif;
                      font-size: 15px; line-height: 1.78;
                    }
                    article { max-width: 880px; margin: %s; }
                    h1 { margin: %s; font-size: 30px; line-height: 1.3; color: #171a21; }
                    h2 { margin: %s; padding-bottom: 9px; border-bottom: 1px solid #e4e7ec;
                         font-size: 21px; color: #20242d; }
                    h3 { margin: %s; font-size: 17px; }
                    p { margin: %s; }
                    strong { color: #3446b5; }
                    table { width: 100%%; border-collapse: separate; border-spacing: 0; margin: %s; }
                    th, td { padding: 11px 14px; border: 1px solid #e1e5eb; text-align: left; vertical-align: top; }
                    th { background: #f6f7fb; font-weight: 700; color: #343b49; }
                    tr:nth-child(even) td { background: #fafbfc; }
                    td:first-child, th:first-child { position: sticky; left: 0; z-index: 1; min-width: 72px; background: inherit; }
                    td { word-break: break-word; overflow-wrap: anywhere; }
                    td code { word-break: break-all; }
                    blockquote { margin: %s; padding: 10px 18px; border-left: 4px solid #7382e8;
                                 background: #f3f4ff; color: #4f5766; }
                    code { padding: 2px 6px; border-radius: 4px; background: #f1f3f6;
                           font-family: "Cascadia Mono", Consolas, monospace; }
                    pre { overflow-x: auto; padding: %s; border: 1px solid #e1e5eb; border-radius: 8px;
                          background: #f7f8fa; }
                    pre code { padding: 0; background: transparent; }
                    ul, ol { padding-left: %s; margin: %s; }
                    li { margin: %s; }
                    a { color: #4f63d9; text-decoration: none; }
                    hr { border: 0; border-top: 1px solid #e4e7ec; margin: %s; }
                    ::selection { background: #dfe3ff; }
                  </style>
                </head>
                <body><article>{{REPORT_BODY}}</article>
                <script>
                  document.addEventListener('click', function(event) {
                    var link = event.target.closest('a');
                    if (link) event.preventDefault();
                  });
                </script>
                </body></html>
                """;
        return template.formatted(bodyPadding, articleMargin, h1Margin, h2Margin, h3Margin, pMargin,
                        tableMargin, blockquoteMargin, prePadding, preMargin, ulPadding, ulMargin, liMargin, hrMargin)
                .replace("{{REPORT_BODY}}", body);
    }

    private void removeRemoteImages(Node document) {
        document.accept(new AbstractVisitor() {
            @Override
            public void visit(Image image) {
                String label = image.getTitle();
                image.insertBefore(new Text(label == null || label.isBlank() ? "[图片已隐藏]" : label));
                image.unlink();
            }
        });
    }
}
