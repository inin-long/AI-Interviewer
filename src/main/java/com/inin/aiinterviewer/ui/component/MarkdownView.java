package com.inin.aiinterviewer.ui.component;

import javafx.concurrent.Worker;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebView;

public class MarkdownView extends StackPane {
    private final WebView webView = new WebView();
    private final MarkdownDocumentRenderer renderer = new MarkdownDocumentRenderer();
    private String markdown = "";

    public MarkdownView() {
        getStyleClass().add("markdown-view");
        webView.setContextMenuEnabled(true);
        webView.getEngine().setJavaScriptEnabled(true);
        webView.getEngine().getLoadWorker().stateProperty().addListener((observable, oldState, state) -> {
            if (state == Worker.State.SUCCEEDED) {
                webView.getEngine().executeScript("document.body.setAttribute('tabindex', '0')");
            }
        });
        getChildren().add(webView);
    }

    public void setMarkdown(String markdown) {
        this.markdown = markdown == null ? "" : markdown;
        webView.getEngine().loadContent(renderer.render(this.markdown), "text/html");
    }

    public String getMarkdown() {
        return markdown;
    }

    public void scrollToHeading(String heading) {
        scrollToText(heading);
    }

    public void scrollToText(String text) {
        if (text == null || text.isBlank()) return;
        if (webView.getEngine().getLoadWorker().getState() != Worker.State.SUCCEEDED) return;
        String escaped = text.replace("\\", "\\\\").replace("'", "\\'")
                .replace("\r", " ").replace("\n", " ");
        webView.getEngine().executeScript("""
                (function() {
                  var expected = '%s';
                  var nodes = document.querySelectorAll('h1,h2,h3,p,strong');
                  for (var i = 0; i < nodes.length; i++) {
                    if (nodes[i].textContent.indexOf(expected) >= 0) {
                      nodes[i].scrollIntoView({behavior: 'smooth', block: 'start'});
                      return true;
                    }
                  }
                  return false;
                })();
                """.formatted(escaped));
    }
}
