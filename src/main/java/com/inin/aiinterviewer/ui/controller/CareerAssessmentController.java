package com.inin.aiinterviewer.ui.controller;

import com.inin.aiinterviewer.application.dto.AssessmentQuestionDto;
import com.inin.aiinterviewer.application.dto.AssessmentResultDto;
import com.inin.aiinterviewer.application.dto.AssessmentTemplateDto;
import com.inin.aiinterviewer.application.exception.GlobalExceptionHandler;
import com.inin.aiinterviewer.application.service.CareerAssessmentService;
import com.inin.aiinterviewer.ui.navigation.ContentNavigator;
import com.inin.aiinterviewer.ui.navigation.JavaFxViewManager;
import com.inin.aiinterviewer.ui.state.UserSessionState;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@Scope("prototype")
public class CareerAssessmentController {

    private final CareerAssessmentService assessmentService;
    private final UserSessionState sessionState;
    private final ContentNavigator contentNavigator;
    private final JavaFxViewManager viewManager;
    private final GlobalExceptionHandler exceptionHandler;

    @FXML private ListView<AssessmentTemplateDto> templateListView;
    @FXML private VBox questionHost;
    @FXML private Button submitButton;
    @FXML private Label hintLabel;

    private final Map<Long, ToggleGroup> groups = new LinkedHashMap<>();
    private List<AssessmentQuestionDto> currentQuestions = new ArrayList<>();
    private String currentCode;

    public CareerAssessmentController(
            CareerAssessmentService assessmentService,
            UserSessionState sessionState,
            ContentNavigator contentNavigator,
            JavaFxViewManager viewManager,
            GlobalExceptionHandler exceptionHandler
    ) {
        this.assessmentService = assessmentService;
        this.sessionState = sessionState;
        this.contentNavigator = contentNavigator;
        this.viewManager = viewManager;
        this.exceptionHandler = exceptionHandler;
    }

    @FXML
    private void initialize() {
        templateListView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(AssessmentTemplateDto item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                setText(item.title());
                setWrapText(true);
            }
        });
        templateListView.getSelectionModel().selectedItemProperty().addListener((obs, old, value) -> {
            if (value != null) loadQuestions(value);
        });
        submitButton.setDisable(true);
        templateListView.getItems().setAll(assessmentService.listTemplates());
    }

    @FXML
    private void openHistory() {
        contentNavigator.showSubPage("/fxml/career-history-view.fxml", "我的测评记录", null);
    }

    @FXML
    private void submit() {
        if (currentQuestions.isEmpty()) {
            return;
        }
        List<Integer> indices = new ArrayList<>();
        for (AssessmentQuestionDto question : currentQuestions) {
            ToggleGroup group = groups.get(question.id());
            if (group == null || group.getSelectedToggle() == null) {
                indices.add(-1);
            } else {
                indices.add(group.getToggles().indexOf(group.getSelectedToggle()));
            }
        }
        if (indices.contains(-1)) {
            hintLabel.setText("还有题目未作答，请完成全部题目后再提交。");
            return;
        }
        try {
            AssessmentResultDto result = assessmentService.submit(
                    currentCode, sessionState.requireCurrentUser().id(), indices);
            contentNavigator.showSubPage("/fxml/career-report-view.fxml", "测评报告", result.id());
        } catch (RuntimeException exception) {
            viewManager.showError(exceptionHandler.toUserMessage(exception));
        }
    }

    private void loadQuestions(AssessmentTemplateDto template) {
        currentCode = template.code();
        currentQuestions = assessmentService.getQuestions(template.code());
        groups.clear();
        questionHost.getChildren().clear();
        hintLabel.setText(template.description());
        int index = 1;
        for (AssessmentQuestionDto question : currentQuestions) {
            ToggleGroup group = new ToggleGroup();
            groups.put(question.id(), group);
            VBox block = new VBox(8);
            block.getStyleClass().add("content-card");
            block.setPadding(new javafx.geometry.Insets(16, 18, 16, 18));
            Label prompt = new Label(index + ". " + question.content());
            prompt.getStyleClass().add("field-label");
            prompt.setWrapText(true);
            block.getChildren().add(prompt);
            for (int i = 0; i < question.options().size(); i++) {
                RadioButton option = new RadioButton(question.options().get(i).label());
                option.setToggleGroup(group);
                option.setWrapText(true);
                block.getChildren().add(option);
            }
            questionHost.getChildren().add(block);
            index++;
        }
        submitButton.setDisable(false);
    }
}
