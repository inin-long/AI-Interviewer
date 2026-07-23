package com.inin.aiinterviewer.ui.component;

import javafx.concurrent.Worker;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebView;

public class MarkdownView extends StackPane {
    private final WebView webView = new WebView();
    private final MarkdownDocumentRenderer renderer = new MarkdownDocumentRenderer();
    private String markdown = "";
    private boolean compact = false;

    public MarkdownView() {
        getStyleClass().add("markdown-view");
        webView.setContextMenuEnabled(true);
        webView.getEngine().setJavaScriptEnabled(true);
        // Let WebView shrink to content height instead of default 600px
        webView.setMinHeight(0);
        webView.setPrefHeight(1);
        webView.getEngine().getLoadWorker().stateProperty().addListener((observable, oldState, state) -> {
            if (state == Worker.State.SUCCEEDED) {
                webView.getEngine().executeScript("document.body.setAttribute('tabindex', '0')");
                syncHeightToContent();
            }
        });
        // Forward wheel events to the nearest parent ScrollPane so the whole page scrolls
        webView.addEventFilter(ScrollEvent.SCROLL, event -> {
            ScrollPane scrollPane = findParentScrollPane(this);
            if (scrollPane == null) return;
            double contentHeight = scrollPane.getContent().getBoundsInLocal().getHeight();
            double viewportHeight = scrollPane.getViewportBounds().getHeight();
            double maxScroll = Math.max(1.0, contentHeight - viewportHeight);
            double delta = -event.getDeltaY() / maxScroll;
            double newValue = Math.max(0.0, Math.min(1.0, scrollPane.getVvalue() + delta));
            scrollPane.setVvalue(newValue);
            event.consume();
        });
        getChildren().add(webView);
    }

    public void setCompact(boolean compact) {
        this.compact = compact;
    }

    private static ScrollPane findParentScrollPane(Node node) {
        Node parent = node.getParent();
        while (parent != null) {
            if (parent instanceof ScrollPane sp) {
                return sp;
            }
            parent = parent.getParent();
        }
        return null;
    }

    public void setMarkdown(String markdown) {
        this.markdown = markdown == null ? "" : markdown;
        String html = compact ? renderer.renderCompact(this.markdown) : renderer.render(this.markdown);
        webView.getEngine().loadContent(html, "text/html");
    }

    private void syncHeightToContent() {
        try {
            Object result = webView.getEngine().executeScript(
                    "Math.max(document.body.scrollHeight, document.documentElement.scrollHeight)");
            if (result instanceof Number number) {
                double contentHeight = number.doubleValue() + 4.0;
                webView.setPrefHeight(contentHeight);
                setPrefHeight(contentHeight);
                setMaxHeight(contentHeight);
            }
        } catch (Exception ignored) {
            // Fallback: keep default behavior
        }
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
