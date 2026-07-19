package com.inin.aiinterviewer.ui.controller;

import com.inin.aiinterviewer.application.dto.BackgroundTaskDto;
import com.inin.aiinterviewer.application.exception.GlobalExceptionHandler;
import com.inin.aiinterviewer.application.service.BackgroundTaskService;
import com.inin.aiinterviewer.domain.enums.BackgroundTaskStatus;
import com.inin.aiinterviewer.ui.component.AppDialogs;
import com.inin.aiinterviewer.ui.navigation.ContentNavigator;
import com.inin.aiinterviewer.ui.navigation.ContextAwareController;
import com.inin.aiinterviewer.ui.navigation.JavaFxViewManager;
import com.inin.aiinterviewer.ui.state.UserSessionState;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
@Scope("prototype")
public class BackgroundTaskDetailController implements ContextAwareController<Long> {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final BackgroundTaskService taskService;
    private final UserSessionState sessionState;
    private final ContentNavigator contentNavigator;
    private final JavaFxViewManager viewManager;
    private final GlobalExceptionHandler exceptionHandler;

    @FXML private Label taskIdLabel;
    @FXML private Label taskTypeLabel;
    @FXML private Label taskStatusLabel;
    @FXML private Label attemptLabel;
    @FXML private Label progressLabel;
    @FXML private Label createTimeLabel;
    @FXML private Label startedTimeLabel;
    @FXML private Label finishedTimeLabel;
    @FXML private Label availableTimeLabel;
    @FXML private TextArea errorArea;
    @FXML private Button retryButton;
    @FXML private Button deleteButton;

    private long taskId;

    public BackgroundTaskDetailController(
            BackgroundTaskService taskService,
            UserSessionState sessionState,
            ContentNavigator contentNavigator,
            JavaFxViewManager viewManager,
            GlobalExceptionHandler exceptionHandler
    ) {
        this.taskService = taskService;
        this.sessionState = sessionState;
        this.contentNavigator = contentNavigator;
        this.viewManager = viewManager;
        this.exceptionHandler = exceptionHandler;
    }

    @Override
    public void initializeContext(Long context) {
        if (context == null) throw new IllegalArgumentException("Task detail requires a task id");
        taskId = context;
        refresh();
    }

    @FXML
    private void refresh() {
        if (taskId == 0) return;
        populate(taskService.requireDto(sessionState.requireCurrentUser().id(), taskId));
    }

    @FXML
    private void retry() {
        try {
            BackgroundTaskDto task = taskService.retryFailed(sessionState.requireCurrentUser().id(), taskId);
            populate(task);
        } catch (RuntimeException exception) {
            viewManager.showError(exceptionHandler.toUserMessage(exception));
        }
    }

    @FXML
    private void delete() {
        if (!AppDialogs.confirm(
                deleteButton.getScene() == null ? null : deleteButton.getScene().getWindow(),
                "删除任务",
                "删除这个后台任务？",
                "删除后该任务将不再出现在任务中心，业务数据和已生成结果不会被删除。",
                "删除",
                true)) return;
        try {
            taskService.deleteTerminal(sessionState.requireCurrentUser().id(), taskId);
            contentNavigator.back();
        } catch (RuntimeException exception) {
            viewManager.showError(exceptionHandler.toUserMessage(exception));
        }
    }

    @FXML
    private void back() {
        contentNavigator.back();
    }

    private void populate(BackgroundTaskDto task) {
        taskIdLabel.setText("#" + task.id());
        taskTypeLabel.setText(BackgroundTaskController.typeText(task.type()));
        taskStatusLabel.setText(BackgroundTaskController.statusText(task.status()));
        attemptLabel.setText(task.attemptCount() + " 次");
        progressLabel.setText(task.progress() + "%");
        createTimeLabel.setText(time(task.createTime()));
        startedTimeLabel.setText(time(task.startedTime()));
        finishedTimeLabel.setText(time(task.finishedTime()));
        availableTimeLabel.setText(time(task.availableTime()));
        errorArea.setText(task.errorMessage() == null || task.errorMessage().isBlank()
                ? "当前没有错误信息。" : task.errorMessage());
        retryButton.setDisable(task.status() != BackgroundTaskStatus.FAILED);
        deleteButton.setDisable(task.status() == BackgroundTaskStatus.PENDING
                || task.status() == BackgroundTaskStatus.RUNNING);
    }

    private String time(LocalDateTime value) {
        return value == null ? "—" : TIME_FORMAT.format(value);
    }
}
