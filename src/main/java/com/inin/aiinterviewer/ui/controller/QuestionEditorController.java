package com.inin.aiinterviewer.ui.controller;

import com.inin.aiinterviewer.application.dto.InterviewQuestionDto;
import com.inin.aiinterviewer.application.dto.JobPositionDto;
import com.inin.aiinterviewer.application.dto.SaveInterviewQuestionCommand;
import com.inin.aiinterviewer.application.exception.GlobalExceptionHandler;
import com.inin.aiinterviewer.application.service.QuestionBankService;
import com.inin.aiinterviewer.domain.enums.InterviewDifficulty;
import com.inin.aiinterviewer.domain.enums.QuestionCategory;
import com.inin.aiinterviewer.ui.navigation.ContextAwareController;
import com.inin.aiinterviewer.ui.navigation.ContentNavigator;
import com.inin.aiinterviewer.ui.navigation.JavaFxViewManager;
import com.inin.aiinterviewer.ui.state.UserSessionState;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@Scope("prototype")
public class QuestionEditorController implements ContextAwareController<Long> {

    private final QuestionBankService questionBankService;
    private final UserSessionState sessionState;
    private final ContentNavigator contentNavigator;
    private final JavaFxViewManager viewManager;
    private final GlobalExceptionHandler exceptionHandler;

    @FXML private Label hintLabel;
    @FXML private ComboBox<JobPositionDto> jobCombo;
    @FXML private ComboBox<QuestionCategory> categoryCombo;
    @FXML private ComboBox<InterviewDifficulty> difficultyCombo;
    @FXML private TextField titleField;
    @FXML private TextArea contentArea;
    @FXML private TextArea answerArea;
    @FXML private TextField tagsField;

    private Long questionId;

    public QuestionEditorController(
            QuestionBankService questionBankService,
            UserSessionState sessionState,
            ContentNavigator contentNavigator,
            JavaFxViewManager viewManager,
            GlobalExceptionHandler exceptionHandler
    ) {
        this.questionBankService = questionBankService;
        this.sessionState = sessionState;
        this.contentNavigator = contentNavigator;
        this.viewManager = viewManager;
        this.exceptionHandler = exceptionHandler;
    }

    @Override
    public void initializeContext(Long context) {
        long userId = sessionState.requireCurrentUser().id();
        jobCombo.getItems().setAll(questionBankService.listJobs(userId));
        jobCombo.setCellFactory(combo -> jobCell());
        jobCombo.setButtonCell(jobCell());
        categoryCombo.getItems().setAll(QuestionCategory.values());
        categoryCombo.setCellFactory(combo -> categoryCell());
        categoryCombo.setButtonCell(categoryCell());
        difficultyCombo.getItems().setAll(InterviewDifficulty.values());
        difficultyCombo.setCellFactory(combo -> difficultyCell());
        difficultyCombo.setButtonCell(difficultyCell());

        if (context == null) {
            hintLabel.setText("新建面试题 · 选择类别与标签以便检索");
            categoryCombo.setValue(QuestionCategory.TECHNICAL);
            difficultyCombo.setValue(InterviewDifficulty.MEDIUM);
            return;
        }
        questionId = context;
        hintLabel.setText("编辑面试题");
        InterviewQuestionDto question = questionBankService.listQuestions(userId).stream()
                .filter(item -> item.id().equals(questionId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("面试题不存在"));
        if (question.jobId() != null) {
            jobCombo.getItems().stream()
                    .filter(job -> job.id().equals(question.jobId()))
                    .findFirst().ifPresent(jobCombo::setValue);
        }
        categoryCombo.setValue(question.category());
        difficultyCombo.setValue(question.difficulty());
        titleField.setText(question.title());
        contentArea.setText(question.content());
        answerArea.setText(question.referenceAnswer());
        tagsField.setText(String.join("、", question.tags()));
    }

    @FXML
    private void save() {
        try {
            SaveInterviewQuestionCommand command = new SaveInterviewQuestionCommand(
                    jobCombo.getValue() == null ? null : jobCombo.getValue().id(),
                    categoryCombo.getValue(),
                    titleField.getText(),
                    contentArea.getText(),
                    answerArea.getText(),
                    difficultyCombo.getValue(),
                    parseTags(tagsField.getText()));
            long userId = sessionState.requireCurrentUser().id();
            if (questionId == null) {
                questionBankService.createQuestion(userId, command);
            } else {
                questionBankService.updateQuestion(userId, questionId, command);
            }
            back();
        } catch (RuntimeException exception) {
            viewManager.showError(exceptionHandler.toUserMessage(exception));
        }
    }

    @FXML
    private void back() {
        contentNavigator.back();
    }

    private List<String> parseTags(String value) {
        if (value == null || value.isBlank()) return List.of();
        return Arrays.stream(value.split("[,，]"))
                .map(String::strip).filter(item -> !item.isBlank()).toList();
    }

    private ListCell<JobPositionDto> jobCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(JobPositionDto item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.title());
            }
        };
    }

    private ListCell<QuestionCategory> categoryCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(QuestionCategory item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.label());
            }
        };
    }

    private ListCell<InterviewDifficulty> difficultyCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(InterviewDifficulty item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : difficultyText(item));
            }
        };
    }

    private String difficultyText(InterviewDifficulty difficulty) {
        return switch (difficulty) {
            case JUNIOR -> "初级";
            case MEDIUM -> "中级";
            case SENIOR -> "高级";
            case EXPERT -> "专家";
        };
    }
}
