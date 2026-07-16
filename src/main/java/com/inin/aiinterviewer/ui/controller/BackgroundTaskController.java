package com.inin.aiinterviewer.ui.controller;

import com.inin.aiinterviewer.application.dto.BackgroundTaskDto;
import com.inin.aiinterviewer.application.exception.GlobalExceptionHandler;
import com.inin.aiinterviewer.application.service.BackgroundTaskService;
import com.inin.aiinterviewer.domain.enums.BackgroundTaskStatus;
import com.inin.aiinterviewer.domain.enums.BackgroundTaskType;
import com.inin.aiinterviewer.ui.navigation.ContentNavigator;
import com.inin.aiinterviewer.ui.navigation.JavaFxViewManager;
import com.inin.aiinterviewer.ui.state.TaskNotificationCenter;
import com.inin.aiinterviewer.ui.state.UserSessionState;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
@Scope("prototype")
public class BackgroundTaskController {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final PseudoClass PENDING = PseudoClass.getPseudoClass("task-pending");
    private static final PseudoClass RUNNING = PseudoClass.getPseudoClass("task-running");
    private static final PseudoClass SUCCESS = PseudoClass.getPseudoClass("task-success");
    private static final PseudoClass FAILED = PseudoClass.getPseudoClass("task-failed");

    private final BackgroundTaskService taskService;
    private final UserSessionState sessionState;
    private final ContentNavigator contentNavigator;
    private final TaskNotificationCenter notificationCenter;
    private final JavaFxViewManager viewManager;
    private final GlobalExceptionHandler exceptionHandler;

    @FXML private BorderPane taskRoot;
    @FXML private TableView<BackgroundTaskDto> taskTable;
    @FXML private TableColumn<BackgroundTaskDto, String> typeColumn;
    @FXML private TableColumn<BackgroundTaskDto, String> statusColumn;
    @FXML private TableColumn<BackgroundTaskDto, String> attemptColumn;
    @FXML private TableColumn<BackgroundTaskDto, LocalDateTime> createTimeColumn;
    @FXML private TableColumn<BackgroundTaskDto, LocalDateTime> updateTimeColumn;
    @FXML private Label summaryLabel;
    @FXML private Button detailButton;
    @FXML private Button deleteButton;

    private TaskNotificationCenter.Registration notificationRegistration;

    public BackgroundTaskController(
            BackgroundTaskService taskService,
            UserSessionState sessionState,
            ContentNavigator contentNavigator,
            TaskNotificationCenter notificationCenter,
            JavaFxViewManager viewManager,
            GlobalExceptionHandler exceptionHandler
    ) {
        this.taskService = taskService;
        this.sessionState = sessionState;
        this.contentNavigator = contentNavigator;
        this.notificationCenter = notificationCenter;
        this.viewManager = viewManager;
        this.exceptionHandler = exceptionHandler;
    }

    @FXML
    private void initialize() {
        typeColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(typeText(cell.getValue().type())));
        statusColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(statusText(cell.getValue().status())));
        statusColumn.setCellFactory(ignored -> statusCell());
        attemptColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().attemptCount() + " 次"));
        configureTimeColumn(createTimeColumn, BackgroundTaskDto::createTime);
        configureTimeColumn(updateTimeColumn, BackgroundTaskDto::updateTime);
        detailButton.disableProperty().bind(taskTable.getSelectionModel().selectedItemProperty().isNull());
        taskTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, previous, current) -> updateDeleteButton(current));
        updateDeleteButton(null);
        taskTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && taskTable.getSelectionModel().getSelectedItem() != null) {
                viewSelected();
            }
        });
        taskRoot.sceneProperty().addListener((observable, previous, current) -> {
            if (current == null) unsubscribeFromNotifications();
            else subscribeToNotifications();
        });
        refresh();
    }

    @FXML
    private void refresh() {
        BackgroundTaskDto selected = taskTable.getSelectionModel().getSelectedItem();
        Long selectedId = selected == null ? null : selected.id();
        var tasks = taskService.listDtos(sessionState.requireCurrentUser().id());
        taskTable.getItems().setAll(tasks);
        if (selectedId != null) {
            tasks.stream()
                    .filter(task -> task.id() == selectedId)
                    .findFirst()
                    .ifPresent(taskTable.getSelectionModel()::select);
        }
        long unfinished = tasks.stream().filter(task -> task.status() == BackgroundTaskStatus.PENDING
                || task.status() == BackgroundTaskStatus.RUNNING).count();
        long failed = tasks.stream().filter(task -> task.status() == BackgroundTaskStatus.FAILED).count();
        summaryLabel.setText("共 " + tasks.size() + " 项 · 进行中 " + unfinished + " 项 · 失败 " + failed + " 项");
    }

    @FXML
    private void viewSelected() {
        BackgroundTaskDto selected = taskTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            contentNavigator.showSubPage("/fxml/task-detail-view.fxml", "任务详情", selected.id());
        }
    }

    @FXML
    private void deleteSelected() {
        BackgroundTaskDto selected = taskTable.getSelectionModel().getSelectedItem();
        if (selected == null || !isTerminal(selected.status())) return;
        Alert confirmation = new Alert(
                Alert.AlertType.CONFIRMATION,
                "删除后该任务将不再出现在任务中心，业务数据和已生成结果不会被删除。",
                ButtonType.CANCEL, ButtonType.OK);
        if (taskRoot.getScene() != null) confirmation.initOwner(taskRoot.getScene().getWindow());
        confirmation.setHeaderText("删除所选后台任务？");
        if (confirmation.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
        try {
            taskService.deleteTerminal(sessionState.requireCurrentUser().id(), selected.id());
            refresh();
        } catch (RuntimeException exception) {
            viewManager.showError(exceptionHandler.toUserMessage(exception));
        }
    }

    @FXML
    private void back() {
        unsubscribeFromNotifications();
        contentNavigator.back();
    }

    private TableCell<BackgroundTaskDto, String> statusCell() {
        TableCell<BackgroundTaskDto, String> cell = new TableCell<>() {
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                BackgroundTaskStatus status = empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()
                        ? null : getTableView().getItems().get(getIndex()).status();
                setText(empty ? null : value);
                pseudoClassStateChanged(PENDING, status == BackgroundTaskStatus.PENDING);
                pseudoClassStateChanged(RUNNING, status == BackgroundTaskStatus.RUNNING);
                pseudoClassStateChanged(SUCCESS, status == BackgroundTaskStatus.SUCCESS);
                pseudoClassStateChanged(FAILED, status == BackgroundTaskStatus.FAILED);
            }
        };
        cell.getStyleClass().add("task-status-cell");
        return cell;
    }

    private void subscribeToNotifications() {
        if (notificationRegistration != null) return;
        notificationRegistration = notificationCenter.subscribe(
                sessionState.requireCurrentUser().id(),
                notification -> Platform.runLater(() -> {
                    if (taskRoot.getScene() != null) refresh();
                }));
    }

    private void unsubscribeFromNotifications() {
        if (notificationRegistration == null) return;
        notificationRegistration.close();
        notificationRegistration = null;
    }

    private void updateDeleteButton(BackgroundTaskDto task) {
        deleteButton.setDisable(task == null || !isTerminal(task.status()));
    }

    private boolean isTerminal(BackgroundTaskStatus status) {
        return status == BackgroundTaskStatus.SUCCESS || status == BackgroundTaskStatus.FAILED;
    }

    private void configureTimeColumn(
            TableColumn<BackgroundTaskDto, LocalDateTime> column,
            java.util.function.Function<BackgroundTaskDto, LocalDateTime> valueProvider
    ) {
        column.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(valueProvider.apply(cell.getValue())));
        column.setCellFactory(ignored -> new javafx.scene.control.TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : TIME_FORMAT.format(value));
            }
        });
    }

    static String typeText(BackgroundTaskType type) {
        return switch (type) {
            case RESUME_PARSE -> "简历解析";
            case PROFILE_GENERATE -> "候选人画像生成";
            case DOCUMENT_PARSE -> "知识文档处理";
            case EMBEDDING_GENERATE -> "向量生成";
            case VECTOR_UPDATE -> "索引更新";
            case REPORT_GENERATE -> "报告生成";
        };
    }

    static String statusText(BackgroundTaskStatus status) {
        return switch (status) {
            case PENDING -> "等待执行";
            case RUNNING -> "执行中";
            case SUCCESS -> "已完成";
            case FAILED -> "失败";
        };
    }
}
