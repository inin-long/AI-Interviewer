package com.inin.aiinterviewer.ui.controller;

import com.inin.aiinterviewer.application.dto.SaveSkillArticleCommand;
import com.inin.aiinterviewer.application.dto.SkillArticleDto;
import com.inin.aiinterviewer.application.service.SkillsLibraryService;
import com.inin.aiinterviewer.ui.component.AppDialog;
import com.inin.aiinterviewer.ui.component.AppDialogs;
import com.inin.aiinterviewer.ui.component.AppSelect;
import com.inin.aiinterviewer.ui.navigation.ContentNavigator;
import com.inin.aiinterviewer.ui.navigation.ContextAwareController;
import com.inin.aiinterviewer.ui.state.UserSessionState;
import jakarta.annotation.Resource;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Scope("prototype")
public class SkillArticleEditorController implements ContextAwareController<SkillArticleDto> {

    @Resource
    private ContentNavigator navigator;

    @Resource
    private UserSessionState userSessionState;

    @Resource
    private SkillsLibraryService skillsLibraryService;

    @FXML
    private TextField titleField;

    @FXML
    private AppSelect<String> categoryCombo;

    @FXML
    private TextArea summaryField;

    @FXML
    private TextArea contentArea;

    private Long editingId = null;

    public void initializeContext(SkillArticleDto data) {
        categoryCombo.getItems().addAll("STAR法则", "行为面试", "礼仪指南",
                "沟通技巧", "薪资谈判", "群面技巧", "压力面试", "其他");
        if (data != null) {
            editingId = data.id();
            titleField.setText(data.title());
            categoryCombo.setValue(data.category());
            summaryField.setText(data.summary());
            contentArea.setText(data.contentMarkdown());
        } else {
            categoryCombo.setValue("其他");
        }
    }

    @FXML
    private void handleSave() {
        String title = titleField.getText().strip();
        String category = categoryCombo.getValue();
        if (title.isBlank()) {
            showWarning("请输入标题");
            return;
        }
        if (category == null || category.isBlank()) {
            showWarning("请选择分类");
            return;
        }

        Long userId = userSessionState.requireCurrentUser().id();
        SaveSkillArticleCommand cmd = new SaveSkillArticleCommand(
                category, title,
                summaryField.getText().strip(),
                contentArea.getText(),
                List.of()
        );
        if (editingId != null) {
            skillsLibraryService.updateArticle(userId, editingId, cmd);
        } else {
            skillsLibraryService.createArticle(userId, cmd);
        }
        navigator.back();
    }

    @FXML
    private void handleBack() {
        navigator.back();
    }

    private void showWarning(String message) {
        AppDialogs.showMessage(
                categoryCombo.getScene() == null ? null : categoryCombo.getScene().getWindow(),
                "请检查输入",
                "需要补充信息",
                message,
                AppDialog.Tone.WARNING);
    }
}
