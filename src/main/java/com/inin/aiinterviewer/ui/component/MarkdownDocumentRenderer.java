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
            .escapeHtml(true)
            .sanitizeUrls(true)
            .percentEncodeUrls(true)
            .build();

    public String render(String markdown) {
        String source = markdown == null || markdown.isBlank()
                ? "# 暂无报告内容\n\n报告尚未生成或内容为空。"
                : markdown;
        Node document = PARSER.parse(source);
        removeRemoteImages(document);
        String body = RENDERER.render(document);
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
                      margin: 0; padding: 34px 42px 56px; color: #20242d;
                      background: #ffffff;
                      font-family: "Segoe UI", "Microsoft YaHei UI", sans-serif;
                      font-size: 15px; line-height: 1.78;
                    }
                    article { max-width: 880px; margin: 0 auto; }
                    h1 { margin: 0 0 28px; font-size: 30px; line-height: 1.3; color: #171a21; }
                    h2 { margin: 34px 0 14px; padding-bottom: 9px; border-bottom: 1px solid #e4e7ec;
                         font-size: 21px; color: #20242d; }
                    h3 { margin: 26px 0 10px; font-size: 17px; }
                    p { margin: 10px 0; }
                    strong { color: #3446b5; }
                    table { width: 100%; border-collapse: separate; border-spacing: 0; margin: 18px 0 26px; }
                    th, td { padding: 11px 14px; border: 1px solid #e1e5eb; text-align: left; vertical-align: top; }
                    th { background: #f6f7fb; font-weight: 700; color: #343b49; }
                    tr:nth-child(even) td { background: #fafbfc; }
                    td:first-child, th:first-child { position: sticky; left: 0; z-index: 1; min-width: 72px; background: inherit; }
                    td { word-break: break-word; overflow-wrap: anywhere; }
                    td code { word-break: break-all; }
                    blockquote { margin: 18px 0; padding: 10px 18px; border-left: 4px solid #7382e8;
                                 background: #f3f4ff; color: #4f5766; }
                    code { padding: 2px 6px; border-radius: 4px; background: #f1f3f6;
                           font-family: "Cascadia Mono", Consolas, monospace; }
                    pre { overflow-x: auto; padding: 16px; border: 1px solid #e1e5eb; border-radius: 8px;
                          background: #f7f8fa; }
                    pre code { padding: 0; background: transparent; }
                    ul, ol { padding-left: 24px; }
                    a { color: #4f63d9; text-decoration: none; }
                    hr { border: 0; border-top: 1px solid #e4e7ec; margin: 28px 0; }
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
        return template.replace("{{REPORT_BODY}}", body);
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
