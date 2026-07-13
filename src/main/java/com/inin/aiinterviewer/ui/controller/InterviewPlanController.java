package com.inin.aiinterviewer.ui.controller;

import com.inin.aiinterviewer.application.dto.InterviewPlanDto;
import com.inin.aiinterviewer.application.exception.GlobalExceptionHandler;
import com.inin.aiinterviewer.application.service.InterviewPlanService;
import com.inin.aiinterviewer.application.service.InterviewSessionService;
import com.inin.aiinterviewer.domain.enums.InterviewDifficulty;
import com.inin.aiinterviewer.ui.navigation.ContentNavigator;
import com.inin.aiinterviewer.ui.navigation.JavaFxViewManager;
import com.inin.aiinterviewer.ui.state.UserSessionState;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
@Scope("prototype")
public class InterviewPlanController {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final InterviewPlanService planService;
    private final InterviewSessionService sessionService;
    private final UserSessionState sessionState;
    private final ContentNavigator contentNavigator;
    private final JavaFxViewManager viewManager;
    private final GlobalExceptionHandler exceptionHandler;

    @FXML private TableView<InterviewPlanDto> planTable;
    @FXML private TableColumn<InterviewPlanDto, String> planNameColumn;
    @FXML private TableColumn<InterviewPlanDto, String> jobColumn;
    @FXML private TableColumn<InterviewPlanDto, String> difficultyColumn;
    @FXML private TableColumn<InterviewPlanDto, Integer> durationColumn;
    @FXML private TableColumn<InterviewPlanDto, Integer> questionColumn;
    @FXML private TableColumn<InterviewPlanDto, String> profileColumn;
    @FXML private TableColumn<InterviewPlanDto, String> knowledgeColumn;
    @FXML private TableColumn<InterviewPlanDto, LocalDateTime> updatedColumn;
    @FXML private Label summaryLabel;
    @FXML private Button editButton;
    @FXML private Button duplicateButton;
    @FXML private Button deleteButton;
    @FXML private Button startButton;

    public InterviewPlanController(
            InterviewPlanService planService,
            InterviewSessionService sessionService,
            UserSessionState sessionState,
            ContentNavigator contentNavigator,
            JavaFxViewManager viewManager,
            GlobalExceptionHandler exceptionHandler
    ) {
        this.planService = planService;
        this.sessionService = sessionService;
        this.sessionState = sessionState;
        this.contentNavigator = contentNavigator;
        this.viewManager = viewManager;
        this.exceptionHandler = exceptionHandler;
    }

    @FXML
    private void initialize() {
        planNameColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().name()));
        jobColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().jobTitle()));
        difficultyColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(difficultyText(cell.getValue().difficulty())));
        durationColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().durationMinutes()));
        questionColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().questionCount()));
        profileColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(
                cell.getValue().profileId() == null ? "未关联" : "已关联确认画像"));
        knowledgeColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(
                cell.getValue().knowledgeDocumentIds().isEmpty()
                        ? "未选择" : cell.getValue().knowledgeDocumentIds().size() + " 个文档"));
        updatedColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().updateTime()));
        updatedColumn.setCellFactory(column -> new javafx.scene.control.TableCell<>() {
            @Override protected void updateItem(LocalDateTime value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : TIME_FORMAT.format(value));
            }
        });
        editButton.disableProperty().bind(planTable.getSelectionModel().selectedItemProperty().isNull());
        duplicateButton.disableProperty().bind(planTable.getSelectionModel().selectedItemProperty().isNull());
        deleteButton.disableProperty().bind(planTable.getSelectionModel().selectedItemProperty().isNull());
        startButton.disableProperty().bind(planTable.getSelectionModel().selectedItemProperty().isNull());
        planTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && planTable.getSelectionModel().getSelectedItem() != null) {
                editSelected();
            }
        });
        refresh();
    }

    @FXML
    private void createPlan() {
        contentNavigator.showSubPage("/fxml/plan-editor-view.fxml", "新建面试方案", null);
    }

    @FXML
    private void editSelected() {
        InterviewPlanDto selected = planTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            contentNavigator.showSubPage("/fxml/plan-editor-view.fxml", "编辑面试方案", selected.id());
        }
    }

    @FXML
    private void duplicateSelected() {
        InterviewPlanDto selected = planTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        try {
            planService.duplicate(sessionState.requireCurrentUser().id(), selected.id());
            refresh();
        } catch (RuntimeException exception) {
            viewManager.showError(exceptionHandler.toUserMessage(exception));
        }
    }

    @FXML
    private void startSelected() {
        InterviewPlanDto selected = planTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        try {
            var session = sessionService.startOrResume(
                    sessionState.requireCurrentUser().id(), selected.id());
            contentNavigator.showSubPage(
                    "/fxml/interview-workspace-view.fxml", "模拟面试", session.id());
        } catch (RuntimeException exception) {
            viewManager.showError(exceptionHandler.toUserMessage(exception));
        }
    }

    @FXML
    private void deleteSelected() {
        InterviewPlanDto selected = planTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                "删除面试方案 “" + selected.name() + "”？", ButtonType.CANCEL, ButtonType.OK);
        confirmation.setHeaderText("确认删除面试方案");
        if (confirmation.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
        try {
            planService.delete(sessionState.requireCurrentUser().id(), selected.id());
            refresh();
        } catch (RuntimeException exception) {
            viewManager.showError(exceptionHandler.toUserMessage(exception));
        }
    }

    @FXML
    private void refresh() {
        var plans = planService.list(sessionState.requireCurrentUser().id());
        planTable.getItems().setAll(plans);
        summaryLabel.setText("共 " + plans.size() + " 个可复用方案");
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
