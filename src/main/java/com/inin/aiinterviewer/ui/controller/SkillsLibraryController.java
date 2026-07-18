package com.inin.aiinterviewer.ui.controller;

import com.inin.aiinterviewer.application.dto.SkillArticleDto;
import com.inin.aiinterviewer.application.service.SkillsLibraryService;
import com.inin.aiinterviewer.ui.navigation.ContentNavigator;
import com.inin.aiinterviewer.ui.navigation.ContextAwareController;
import com.inin.aiinterviewer.ui.navigation.Route;
import com.inin.aiinterviewer.ui.state.UserSessionState;
import jakarta.annotation.Resource;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Scope("prototype")
public class SkillsLibraryController implements ContextAwareController<Object> {

    @Resource
    private ContentNavigator navigator;

    @Resource
    private UserSessionState userSessionState;

    @Resource
    private SkillsLibraryService skillsLibraryService;

    @FXML
    private ComboBox<String> categoryFilter;

    @FXML
    private ListView<SkillArticleDto> articleList;

    private final ObservableList<SkillArticleDto> items = FXCollections.observableArrayList();

    /**
     * 筛选下拉选项 → 数据库英文值（一一对应，无重复映射）
     * 只有 4 个真实分类 + "全部分类"
     */
    private static final Map<String, String> FILTER_MAP = Map.of(
            "STAR 法则", "STAR",
            "行为面试", "BEHAVIOR",
            "礼仪指南", "ETIQUETTE",
            "通用技巧", "GENERAL"
    );

    /** 数据库英文值 → 中文显示名（用于列表项徽标） */
    private static final Map<String, String> CATEGORY_LABEL_MAP = Map.of(
            "STAR", "STAR 法则",
            "BEHAVIOR", "行为面试",
            "ETIQUETTE", "礼仪指南",
            "GENERAL", "通用技巧"
    );

    /** 数据库英文值 → 徽标样式类 */
    private static final Map<String, String> CATEGORY_BADGE_MAP = Map.of(
            "STAR", "badge-info",
            "BEHAVIOR", "badge-tech",
            "ETIQUETTE", "badge-purple",
            "GENERAL", "badge-neutral"
    );

    public void initializeContext(Object context) {
        // no-op for root page
    }

    @FXML
    public void initialize() {
        // 筛选项与数据库分类 1:1 对应——不会出现"不同筛选项显示相同内容"
        categoryFilter.getItems().addAll(
                "全部分类", "STAR 法则", "行为面试", "礼仪指南", "通用技巧");
        categoryFilter.setValue("全部分类");

        articleList.setItems(items);
        articleList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(SkillArticleDto item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                // 标题
                Label title = new Label(item.title());
                title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #171a21;");
                title.setWrapText(true);

                // 分类徽标
                String catKey = item.category();
                String catLabel = CATEGORY_LABEL_MAP.getOrDefault(catKey, catKey);
                String catBadge = CATEGORY_BADGE_MAP.getOrDefault(catKey, "badge-neutral");
                Label category = new Label("  " + catLabel + "  ");
                category.setStyle("-fx-font-size: 11px; -fx-font-weight: 700;");
                category.getStyleClass().addAll("badge", catBadge);

                // 摘要（200 字符）
                String summaryRaw = item.summary() != null ? item.summary() : "";
                String summaryText = summaryRaw.length() > 200
                        ? summaryRaw.substring(0, 200) + "…" : summaryRaw;
                Label summary = new Label(summaryText);
                summary.setWrapText(true);
                summary.setStyle("-fx-font-size: 13px; -fx-text-fill: #4a5568;");

                // 正文预览（Markdown → 纯文本，去掉所有语法符号）
                String contentPreview = "";
                if (item.contentMarkdown() != null && !item.contentMarkdown().isBlank()) {
                    String raw = stripMarkdown(item.contentMarkdown());
                    // 去掉首行（通常与 title 重复）
                    raw = raw.trim();
                    int firstNl = raw.indexOf('\n');
                    if (firstNl > 0) raw = raw.substring(firstNl).trim();
                    contentPreview = raw.length() > 180 ? raw.substring(0, 180) + "…" : raw;
                }
                if (!contentPreview.isBlank()) {
                    Label preview = new Label(contentPreview);
                    preview.setWrapText(true);
                    preview.setStyle("-fx-font-size: 12px; -fx-text-fill: #718096; -fx-padding: 4 0 0 0;");

                    // 阅读提示
                    Label hint = new Label("📖 点击查看完整内容");
                    hint.setStyle("-fx-font-size: 11px; -fx-text-fill: #7382e8; "
                            + "-fx-font-weight: 600; -fx-padding: 6 0 2 0;");

                    VBox box = new VBox(6, title, category, summary, preview, hint);
                    box.setStyle("-fx-background-color: transparent; -fx-padding: 8 4 8 4;");
                    box.setFillWidth(true);
                    setGraphic(box);
                } else {
                    VBox box = new VBox(6, title, category, summary);
                    box.setStyle("-fx-background-color: transparent; -fx-padding: 8 4 8 4;");
                    box.setFillWidth(true);
                    setGraphic(box);
                }
            }
        });

        // 单击即打开详情
        articleList.setOnMouseClicked(e -> {
            if (e.getClickCount() >= 1) {
                SkillArticleDto selected = articleList.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    navigator.showSubRoute(Route.SKILL_ARTICLE_DETAIL, selected);
                }
            }
        });

        categoryFilter.setOnAction(e -> loadData());

        loadData();
    }

    /**
     * 将 Markdown 原文转为纯文本预览。
     * 去掉标题标记、加粗/斜体、引用、表格语法、代码块、链接等，
     * 只保留可阅读的文本内容。
     */
    private static String stripMarkdown(String md) {
        String text = md;
        // 代码块 ```...``` → 整块替换为占位符
        text = text.replaceAll("```[\\s\\S]*?```", "[代码示例]");
        // 行内代码 `xxx` → xxx
        text = text.replaceAll("`([^`]*)`", "$1");
        // 标题 ### ## # → 去掉井号和前后空格
        text = text.replaceAll("^#{1,6}\\s+", "");
        text = text.replaceAll("(?m)\n#{1,6}\\s+", "\n");
        // 加粗 **text** → text
        text = text.replaceAll("\\*\\*([^*]+)\\*\\*", "$1");
        // 斜体 *text* 或 _text_ → text（注意：在加粗之后处理）
        text = text.replaceAll("\\*([^*]+)\\*", "$1");
        text = text.replaceAll("_([^_]+)_", "$1");
        // 引用 > text → text
        text = text.replaceAll("(?m)^>\\s?", "");
        // 无序列表 - [ ] → 去掉标记
        text = text.replaceAll("(?m)^[\\-\\*+]\\s+", "");
        // 有序列表 1. → 去掉数字编号
        text = text.replaceAll("(?m)^\\d+\\.\\s+", "");
        // 分隔线 --- *** ___ → 空行
        text = text.replaceAll("(?m)^[-*_]{3,}\\s*$", "");
        // 链接 [text](url) → text
        text = text.replaceAll("\\[([^\\]]+)\\]\\([^)]+\\)", "$1");
        // 图片 ![alt](url) → [图片]
        text = text.replaceAll("!\\[[^\\]]*\\]\\([^)]+\\)", "[图片]");
        // 表格 | ... | 行 → 去掉管道符号，合并空格
        text = text.replaceAll("\\|", " ");
        // 分隔线 ---|=== 等
        text = text.replaceAll("(?m)^\\s*[\\-:=]{3,}\\s*$", "");
        // HTML 标签 <br> <p> 等 → 去掉
        text = text.replaceAll("<[^>]+>", "");
        // 合并多余空行（超过2个连续换行压缩成2个）
        text = text.replaceAll("\n{3,}", "\n\n");
        return text.trim();
    }

    private void loadData() {
        try {
            String filter = categoryFilter.getValue();
            Long userId = userSessionState.requireCurrentUser().id();
            List<SkillArticleDto> articles;
            if (filter == null || "全部分类".equals(filter)) {
                articles = skillsLibraryService.listArticles(userId);
            } else {
                String dbCategory = FILTER_MAP.getOrDefault(filter, filter);
                articles = skillsLibraryService.listByCategory(userId, dbCategory);
            }
            items.setAll(articles);
        } catch (RuntimeException ex) {
            Throwable root = ex;
            while (root.getCause() != null && root.getCause() != root) root = root.getCause();
            Alert alert = new Alert(Alert.AlertType.WARNING,
                    "加载面试技巧失败: " + (root.getMessage() != null ? root.getMessage() : ex.getMessage()));
            alert.setHeaderText("加载失败");
            alert.showAndWait();
        }
    }

    @FXML
    private void handleAdd() {
        navigator.showSubRoute(Route.SKILL_ARTICLE_EDITOR, null);
    }
}
