package com.inin.aiinterviewer.ui.controller;

import com.inin.aiinterviewer.application.dto.SkillArticleDto;
import com.inin.aiinterviewer.application.service.SkillsLibraryService;
import com.inin.aiinterviewer.ui.component.AppDialog;
import com.inin.aiinterviewer.ui.component.AppDialogs;
import com.inin.aiinterviewer.ui.navigation.ContentNavigator;
import com.inin.aiinterviewer.ui.navigation.ContextAwareController;
import com.inin.aiinterviewer.ui.navigation.Route;
import com.inin.aiinterviewer.ui.state.UserSessionState;
import jakarta.annotation.Resource;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
@Scope("prototype")
public class SkillsLibraryController implements ContextAwareController<Object> {

    @Resource
    private ContentNavigator navigator;

    @Resource
    private UserSessionState userSessionState;

    @Resource
    private SkillsLibraryService skillsLibraryService;

    @FXML private ToggleButton allFilterButton;
    @FXML private ToggleButton starFilterButton;
    @FXML private ToggleButton behaviorFilterButton;
    @FXML private ToggleButton etiquetteFilterButton;
    @FXML private ToggleButton generalFilterButton;

    @FXML private TextField searchField;
    @FXML private Button addButton;

    @FXML private ListView<SkillArticleDto> articleList;

    private final ObservableList<SkillArticleDto> items = FXCollections.observableArrayList();
    private final List<SkillArticleDto> allItems = new ArrayList<>();

    /** 分类英文值 → 中文显示名 */
    private static final Map<String, String> CATEGORY_LABEL_MAP = Map.of(
            "STAR", "STAR 法则",
            "BEHAVIOR", "行为面试",
            "ETIQUETTE", "礼仪指南",
            "GENERAL", "通用技巧"
    );

    /** 分类英文值 → 图标 */
    private static final Map<String, String> CATEGORY_ICON_MAP = Map.of(
            "STAR", "mdi2s-star-outline",
            "BEHAVIOR", "mdi2a-account-group-outline",
            "ETIQUETTE", "mdi2h-handshake-outline",
            "GENERAL", "mdi2l-lightbulb-on-outline"
    );

    /** 分类英文值 → 徽标样式类 */
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
        articleList.setItems(items);
        articleList.setCellFactory(list -> new SkillArticleCardCell());

        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());

        articleList.setOnMouseClicked(e -> {
            if (e.getClickCount() >= 1) {
                SkillArticleDto selected = articleList.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    navigator.showSubRoute(Route.SKILL_ARTICLE_DETAIL, selected);
                }
            }
        });

        loadData();
    }

    @FXML
    private void applyFilters() {
        if (articleList == null) return;
        if (!allFilterButton.isSelected() && !starFilterButton.isSelected()
                && !behaviorFilterButton.isSelected() && !etiquetteFilterButton.isSelected()
                && !generalFilterButton.isSelected()) {
            allFilterButton.setSelected(true);
        }

        String keyword = searchField == null || searchField.getText() == null
                ? "" : searchField.getText().strip().toLowerCase(Locale.ROOT);

        List<SkillArticleDto> filtered = allItems.stream()
                .filter(this::matchesSelectedCategory)
                .filter(item -> matchesKeyword(item, keyword))
                .sorted(Comparator.comparing(SkillArticleDto::createTime, Comparator.reverseOrder()))
                .toList();

        items.setAll(filtered);
    }

    @FXML
    private void refresh() {
        loadData();
    }

    @FXML
    private void handleImport() {
        AppDialogs.showMessage(
                articleList.getScene() == null ? null : articleList.getScene().getWindow(),
                "批量导入",
                "批量导入",
                "批量导入功能开发中，请使用「新增文章」逐篇添加。",
                AppDialog.Tone.INFORMATION);
    }

    @FXML
    private void handleAdd() {
        navigator.showSubRoute(Route.SKILL_ARTICLE_EDITOR, null);
    }

    private boolean matchesSelectedCategory(SkillArticleDto item) {
        String category = item.category();
        if (allFilterButton.isSelected()) return true;
        if (starFilterButton.isSelected()) return "STAR".equalsIgnoreCase(category);
        if (behaviorFilterButton.isSelected()) return "BEHAVIOR".equalsIgnoreCase(category);
        if (etiquetteFilterButton.isSelected()) return "ETIQUETTE".equalsIgnoreCase(category);
        if (generalFilterButton.isSelected()) return "GENERAL".equalsIgnoreCase(category);
        return true;
    }

    private boolean matchesKeyword(SkillArticleDto item, String keyword) {
        if (keyword.isEmpty()) return true;
        String title = item.title() == null ? "" : item.title().toLowerCase(Locale.ROOT);
        String summary = item.summary() == null ? "" : item.summary().toLowerCase(Locale.ROOT);
        String category = item.category() == null ? "" : item.category().toLowerCase(Locale.ROOT);
        String content = item.contentMarkdown() == null ? "" : item.contentMarkdown().toLowerCase(Locale.ROOT);
        boolean tagMatch = item.tags() != null && item.tags().stream()
                .anyMatch(tag -> tag.toLowerCase(Locale.ROOT).contains(keyword));
        return title.contains(keyword)
                || summary.contains(keyword)
                || category.contains(keyword)
                || content.contains(keyword)
                || tagMatch;
    }

    private void loadData() {
        try {
            long userId = userSessionState.requireCurrentUser().id();
            List<SkillArticleDto> articles = skillsLibraryService.listArticles(userId);
            allItems.clear();
            allItems.addAll(articles);
            applyFilters();
        } catch (RuntimeException ex) {
            Throwable root = ex;
            while (root.getCause() != null && root.getCause() != root) root = root.getCause();
            AppDialogs.showMessage(
                    articleList.getScene() == null ? null : articleList.getScene().getWindow(),
                    "加载失败",
                    "面试技巧加载失败",
                    root.getMessage() != null ? root.getMessage() : ex.getMessage(),
                    AppDialog.Tone.WARNING);
        }
    }

    /**
     * 将 Markdown 原文转为纯文本预览。
     */
    private static String stripMarkdown(String md) {
        String text = md;
        text = text.replaceAll("```[\\s\\S]*?```", "[代码示例]");
        text = text.replaceAll("`([^`]*)`", "$1");
        text = text.replaceAll("^#{1,6}\\s+", "");
        text = text.replaceAll("(?m)\n#{1,6}\\s+", "\n");
        text = text.replaceAll("\\*\\*([^*]+)\\*\\*", "$1");
        text = text.replaceAll("\\*([^*]+)\\*", "$1");
        text = text.replaceAll("_([^_]+)_", "$1");
        text = text.replaceAll("(?m)^>\\s?", "");
        text = text.replaceAll("(?m)^[\\-\\*+]\\s+", "");
        text = text.replaceAll("(?m)^\\d+\\.\\s+", "");
        text = text.replaceAll("(?m)^[-*_]{3,}\\s*$", "");
        text = text.replaceAll("\\[([^\\]]+)\\]\\([^)]+\\)", "$1");
        text = text.replaceAll("!\\[[^\\]]*\\]\\([^)]+\\)", "[图片]");
        text = text.replaceAll("\\|", " ");
        text = text.replaceAll("(?m)^\\s*[\\-:=]{3,}\\s*$", "");
        text = text.replaceAll("<[^>]+>", "");
        text = text.replaceAll("\n{3,}", "\n\n");
        return text.trim();
    }

    private final class SkillArticleCardCell extends ListCell<SkillArticleDto> {
        @Override
        protected void updateItem(SkillArticleDto item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                return;
            }
            setText(null);
            setGraphic(buildCard(item));
        }

        private HBox buildCard(SkillArticleDto item) {
            // Category badge icon box
            String categoryKey = item.category() == null ? "GENERAL" : item.category().toUpperCase(Locale.ROOT);
            String catLabel = CATEGORY_LABEL_MAP.getOrDefault(categoryKey, categoryKey);
            String catIconLiteral = CATEGORY_ICON_MAP.getOrDefault(categoryKey, "mdi2b-book-open-outline");
            String catBadgeClass = CATEGORY_BADGE_MAP.getOrDefault(categoryKey, "badge-neutral");

            StackPane iconBox = new StackPane(new FontIcon(catIconLiteral));
            iconBox.getStyleClass().addAll("skills-card-icon-box", categoryKey.toLowerCase(Locale.ROOT));

            // Title
            Label title = new Label(item.title());
            title.getStyleClass().add("skills-card-title");
            title.setWrapText(true);

            // Category label badge
            Label categoryLabel = new Label(catLabel);
            categoryLabel.getStyleClass().addAll("badge", catBadgeClass, "skills-card-category");

            // Summary
            String summaryText = item.summary() != null && !item.summary().isBlank()
                    ? item.summary()
                    : stripMarkdown(item.contentMarkdown());
            if (summaryText.length() > 160) {
                summaryText = summaryText.substring(0, 160) + "…";
            }
            Label summary = new Label(summaryText);
            summary.getStyleClass().add("skills-card-summary");
            summary.setWrapText(true);

            // Tags
            FlowPane tagsFlow = new FlowPane(6, 4);
            tagsFlow.getStyleClass().add("skills-card-tags");
            if (item.tags() != null) {
                for (String tag : item.tags()) {
                    Label tagLabel = new Label(tag);
                    tagLabel.getStyleClass().add("skills-card-tag");
                    tagsFlow.getChildren().add(tagLabel);
                }
            }

            // Meta info: date
            String dateStr = item.createTime() != null
                    ? item.createTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    : "";
            Label dateLabel = new Label(dateStr);
            dateLabel.getStyleClass().add("skills-card-date");

            HBox metaRow = new HBox(12, categoryLabel, dateLabel);
            metaRow.setAlignment(Pos.CENTER_LEFT);

            VBox content = new VBox(5, title, metaRow, summary, tagsFlow);
            content.setFillWidth(true);
            HBox.setHgrow(content, Priority.ALWAYS);

            // Action buttons
            Button viewButton = new Button("查看", new FontIcon("mdi2e-eye-outline"));
            viewButton.getStyleClass().addAll("skills-card-action", "skills-card-action-primary");
            viewButton.setOnAction(event -> navigator.showSubRoute(Route.SKILL_ARTICLE_DETAIL, item));

            Button editButton = new Button("编辑", new FontIcon("mdi2p-pencil-outline"));
            editButton.getStyleClass().addAll("skills-card-action", "skills-card-action-secondary");
            editButton.setOnAction(event -> navigator.showSubRoute(Route.SKILL_ARTICLE_EDITOR, item));

            VBox actions = new VBox(6, viewButton, editButton);
            actions.setAlignment(Pos.CENTER);

            HBox card = new HBox(14, iconBox, content, actions);
            card.setAlignment(Pos.CENTER_LEFT);
            card.setPadding(new Insets(12, 14, 12, 14));
            card.getStyleClass().add("skills-card");
            card.setOnMouseClicked(event -> {
                if (event.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                    getListView().getSelectionModel().select(item);
                    navigator.showSubRoute(Route.SKILL_ARTICLE_DETAIL, item);
                }
            });
            viewButton.setOnMouseClicked(event -> event.consume());
            editButton.setOnMouseClicked(event -> event.consume());
            return card;
        }
    }
}
