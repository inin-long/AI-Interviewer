package com.inin.aiinterviewer.ui.controller;

import com.inin.aiinterviewer.application.dto.SkillArticleDto;
import com.inin.aiinterviewer.application.service.SkillsLibraryService;
import com.inin.aiinterviewer.ui.component.AppDialog;
import com.inin.aiinterviewer.ui.component.AppDialogs;
import com.inin.aiinterviewer.ui.component.AppSelect;
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
import java.util.stream.Collectors;

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
    private AppSelect<String> categoryFilter;

    @FXML
    private ListView<SkillArticleDto> articleList;

    private final ObservableList<SkillArticleDto> items = FXCollections.observableArrayList();

    public void initializeContext(Object context) {
        // no-op for root page
    }

    @FXML
    public void initialize() {
        categoryFilter.getItems().addAll("全部分类", "STAR法则", "行为面试", "礼仪指南",
                "沟通技巧", "薪资谈判", "群面技巧", "压力面试", "其他");
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
                Label title = new Label(item.title());
                title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
                Label category = new Label(item.category());
                category.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");
                Label summary = new Label(item.summary() != null && item.summary().length() > 80
                        ? item.summary().substring(0, 80) + "..." : item.summary());
                summary.setWrapText(true);
                summary.setStyle("-fx-font-size: 13px;");
                VBox box = new VBox(4, title, category, summary);
                setGraphic(box);
            }
        });

        articleList.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                SkillArticleDto selected = articleList.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    navigator.showSubRoute(Route.SKILL_ARTICLE_DETAIL, selected);
                }
            }
        });

        categoryFilter.setOnAction(e -> loadData());

        loadData();
    }

    private void loadData() {
        try {
            String filter = categoryFilter.getValue();
            Long userId = userSessionState.requireCurrentUser().id();
            List<SkillArticleDto> articles;
            if (filter == null || "全部分类".equals(filter)) {
                articles = skillsLibraryService.listArticles(userId);
            } else {
                articles = skillsLibraryService.listByCategory(userId, filter);
            }
            items.setAll(articles);
        } catch (RuntimeException ex) {
            Throwable root = ex;
            while (root.getCause() != null && root.getCause() != root) root = root.getCause();
            AppDialogs.showMessage(
                    categoryFilter.getScene() == null ? null : categoryFilter.getScene().getWindow(),
                    "加载失败",
                    "面试技巧加载失败",
                    root.getMessage() != null ? root.getMessage() : ex.getMessage(),
                    AppDialog.Tone.WARNING);
        }
    }

    @FXML
    private void handleAdd() {
        navigator.showSubRoute(Route.SKILL_ARTICLE_EDITOR, null);
    }
}
