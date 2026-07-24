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
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;
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
    @FXML private ScrollPane questionScrollPane;
    @FXML private VBox questionHost;
    @FXML private Button submitButton;
    @FXML private Label hintLabel;
    @FXML private Label progressLabel;
    @FXML private VBox emptyState;
    @FXML private VBox hintBar;

    private final Map<Long, ToggleGroup> groups = new LinkedHashMap<>();
    private final Map<String, Double> scrollPositions = new LinkedHashMap<>();
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
            private final VBox root = new VBox(4);
            private final HBox titleRow = new HBox(8);
            private final FontIcon icon = new FontIcon();
            private final Label titleLabel = new Label();
            private final Label descLabel = new Label();

            {
                root.getStyleClass().add("assessment-template-cell");
                titleRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                titleRow.getChildren().addAll(icon, titleLabel);
                root.getChildren().addAll(titleRow, descLabel);
                icon.setIconSize(20);
                icon.getStyleClass().add("assessment-template-icon");
                titleLabel.getStyleClass().add("assessment-template-title");
                descLabel.getStyleClass().add("assessment-template-desc");
                descLabel.setWrapText(true);
                descLabel.setMaxWidth(Region.USE_PREF_SIZE);
                descLabel.prefWidthProperty().bind(list.widthProperty().subtract(52));
                root.setFillWidth(true);
                root.setMaxWidth(Region.USE_PREF_SIZE);
                root.prefWidthProperty().bind(list.widthProperty().subtract(8));
            }

            @Override
            protected void updateItem(AssessmentTemplateDto item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                // Choose icon based on code
                String code = item.code();
                if (code != null && code.toLowerCase().contains("mbti")) {
                    icon.setIconLiteral("mdi2b-brain");
                } else if (code != null && (code.toLowerCase().contains("holland") || code.toLowerCase().contains("riasec"))) {
                    icon.setIconLiteral("mdi2c-compass-outline");
                } else {
                    icon.setIconLiteral("mdi2c-clipboard-text-outline");
                }
                titleLabel.setText(item.title());
                descLabel.setText(item.description() != null ? item.description() : "");
                setGraphic(root);
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
            showHint("还有题目未作答，请完成全部题目后再提交。");
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
        if (currentCode != null) {
            scrollPositions.put(currentCode, questionScrollPane.getVvalue());
        }
        currentCode = template.code();
        currentQuestions = assessmentService.getQuestions(template.code());
        groups.clear();
        questionHost.getChildren().clear();

        // Hide empty state
        emptyState.setVisible(false);
        emptyState.setManaged(false);

        // Show hint bar with template description
        if (template.description() != null && !template.description().isBlank()) {
            hintLabel.setText(template.description());
            hintBar.setVisible(true);
            hintBar.setManaged(true);
        } else {
            hintBar.setVisible(false);
            hintBar.setManaged(false);
        }

        int total = currentQuestions.size();
        progressLabel.setText("共 " + total + " 题");

        int index = 1;
        for (AssessmentQuestionDto question : currentQuestions) {
            ToggleGroup group = new ToggleGroup();
            groups.put(question.id(), group);

            // Question card with left accent border
            VBox block = new VBox(8);
            block.getStyleClass().addAll("content-card", "question-card");
            block.setPadding(new javafx.geometry.Insets(12, 16, 10, 16));

            // Question number + prompt row
            HBox headerRow = new HBox(10);
            headerRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

            // Number badge
            VBox numBadge = new VBox();
            numBadge.setAlignment(javafx.geometry.Pos.CENTER);
            numBadge.setPrefSize(24, 24);
            numBadge.setMinSize(24, 24);
            numBadge.setMaxSize(24, 24);
            numBadge.getStyleClass().add("question-number-badge");
            Label numLabel = new Label(String.valueOf(index));
            numLabel.getStyleClass().add("question-number-text");
            numBadge.getChildren().add(numLabel);

            Label prompt = new Label(question.content());
            prompt.getStyleClass().add("question-prompt-text");
            prompt.setWrapText(true);
            HBox.setHgrow(prompt, Priority.ALWAYS);

            headerRow.getChildren().addAll(numBadge, prompt);
            block.getChildren().add(headerRow);

            // Options
            VBox optionsBox = new VBox(4);
            optionsBox.getStyleClass().add("question-options-box");
            for (int i = 0; i < question.options().size(); i++) {
                HBox optionRow = new HBox(10);
                optionRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                optionRow.setMaxWidth(Double.MAX_VALUE);
                optionRow.setPickOnBounds(true);
                optionRow.getStyleClass().add("question-option-row");

                RadioButton option = new RadioButton(question.options().get(i).label());
                option.setToggleGroup(group);
                option.setWrapText(true);
                option.setMaxWidth(Double.MAX_VALUE);
                option.getStyleClass().add("question-option-radio");
                HBox.setHgrow(option, Priority.ALWAYS);

                option.selectedProperty().addListener((obs, oldVal, selected) -> {
                    if (Boolean.TRUE.equals(selected)) {
                        if (!optionRow.getStyleClass().contains("selected")) {
                            optionRow.getStyleClass().add("selected");
                        }
                    } else {
                        optionRow.getStyleClass().remove("selected");
                    }
                });
                optionRow.setOnMouseClicked(event -> {
                    if (!option.isDisabled()) {
                        option.setSelected(true);
                        event.consume();
                    }
                });

                optionRow.getChildren().add(option);
                optionsBox.getChildren().add(optionRow);
            }
            block.getChildren().add(optionsBox);
            questionHost.getChildren().add(block);
            index++;
        }
        submitButton.setDisable(false);
        double scrollPosition = scrollPositions.getOrDefault(currentCode, 0.0);
        questionScrollPane.setVvalue(scrollPosition);
        javafx.application.Platform.runLater(() -> {
            if (template.code().equals(currentCode)) {
                questionScrollPane.setVvalue(scrollPosition);
            }
        });
    }

    private void showHint(String message) {
        hintLabel.setText(message);
        hintBar.setVisible(true);
        hintBar.setManaged(true);
    }
}
