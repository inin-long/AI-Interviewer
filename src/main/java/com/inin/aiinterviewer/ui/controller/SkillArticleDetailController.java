package com.inin.aiinterviewer.ui.controller;

import com.inin.aiinterviewer.application.dto.SkillArticleDto;
import com.inin.aiinterviewer.application.service.SkillsLibraryService;
import com.inin.aiinterviewer.ui.component.MarkdownView;
import com.inin.aiinterviewer.ui.navigation.ContentNavigator;
import com.inin.aiinterviewer.ui.navigation.ContextAwareController;
import com.inin.aiinterviewer.ui.navigation.Route;
import jakarta.annotation.Resource;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class SkillArticleDetailController implements ContextAwareController<SkillArticleDto> {

    @Resource
    private ContentNavigator navigator;

    @Resource
    private SkillsLibraryService skillsLibraryService;

    @FXML
    private Label titleLabel;

    @FXML
    private Label categoryLabel;

    @FXML
    private Label summaryLabel;

    @FXML
    private VBox contentArea;

    private SkillArticleDto article;

    public void initializeContext(SkillArticleDto data) {
        this.article = data;
        refresh();
    }

    private void refresh() {
        if (article == null) return;
        titleLabel.setText(article.title());
        categoryLabel.setText("[" + article.category() + "]");
        summaryLabel.setText(article.summary() != null ? article.summary() : "");
        contentArea.getChildren().clear();
        if (article.contentMarkdown() != null && !article.contentMarkdown().isBlank()) {
            MarkdownView md = new MarkdownView();
            md.setMarkdown(article.contentMarkdown());
            contentArea.getChildren().add(md);
        }
    }

    @FXML
    private void handleBack() {
        navigator.back();
    }

    @FXML
    private void handleEdit() {
        if (article != null) {
            navigator.showSubRoute(Route.SKILL_ARTICLE_EDITOR, article);
        }
    }
}
