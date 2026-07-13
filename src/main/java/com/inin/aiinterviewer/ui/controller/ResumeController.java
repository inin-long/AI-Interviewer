package com.inin.aiinterviewer.ui.controller;

import com.inin.aiinterviewer.application.dto.ResumeDto;
import com.inin.aiinterviewer.application.exception.GlobalExceptionHandler;
import com.inin.aiinterviewer.application.service.ResumeService;
import com.inin.aiinterviewer.application.service.ResumeTaskService;
import com.inin.aiinterviewer.application.service.BackgroundTaskService;
import com.inin.aiinterviewer.domain.enums.BackgroundTaskStatus;
import com.inin.aiinterviewer.domain.enums.ResumeStatus;
import com.inin.aiinterviewer.ui.navigation.JavaFxViewManager;
import com.inin.aiinterviewer.ui.navigation.ContentNavigator;
import com.inin.aiinterviewer.ui.state.UserSessionState;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.concurrent.Task;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.FileChooser;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
@Scope("prototype")
public class ResumeController {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ResumeService resumeService;
    private final ResumeTaskService resumeTaskService;
    private final BackgroundTaskService backgroundTaskService;
    private final UserSessionState sessionState;
    private final JavaFxViewManager viewManager;
    private final ContentNavigator contentNavigator;
    private final GlobalExceptionHandler exceptionHandler;

    @FXML private TableView<ResumeDto> resumeTable;
    @FXML private TableColumn<ResumeDto, String> nameColumn;
    @FXML private TableColumn<ResumeDto, String> typeColumn;
    @FXML private TableColumn<ResumeDto, String> sizeColumn;
    @FXML private TableColumn<ResumeDto, String> statusColumn;
    @FXML private TableColumn<ResumeDto, LocalDateTime> updateTimeColumn;
    @FXML private Label summaryLabel;
    @FXML private Label taskLabel;
    @FXML private Button uploadButton;
    @FXML private Button deleteButton;
    @FXML private Button viewButton;
    private Timeline statusWatcher;

    public ResumeController(
            ResumeService resumeService,
            ResumeTaskService resumeTaskService,
            BackgroundTaskService backgroundTaskService,
            UserSessionState sessionState,
            ContentNavigator contentNavigator,
            JavaFxViewManager viewManager,
            GlobalExceptionHandler exceptionHandler
    ) {
        this.resumeService = resumeService;
        this.resumeTaskService = resumeTaskService;
        this.backgroundTaskService = backgroundTaskService;
        this.sessionState = sessionState;
        this.contentNavigator = contentNavigator;
        this.viewManager = viewManager;
        this.exceptionHandler = exceptionHandler;
    }

    @FXML
    private void initialize() {
        nameColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().originalName()));
        typeColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().fileType().toUpperCase()));
        sizeColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(formatSize(cell.getValue().fileSize())));
        statusColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(formatStatus(cell.getValue().status())));
        updateTimeColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().updateTime()));
        updateTimeColumn.setCellFactory(column -> new javafx.scene.control.TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : TIME_FORMAT.format(value));
            }
        });
        deleteButton.disableProperty().bind(resumeTable.getSelectionModel().selectedItemProperty().isNull());
        viewButton.disableProperty().bind(resumeTable.getSelectionModel().selectedItemProperty().isNull());
        resumeTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && resumeTable.getSelectionModel().getSelectedItem() != null) {
                viewSelected();
            }
        });
        refresh();
    }

    @FXML
    private void uploadResume() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("选择简历");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "简历文件 (*.pdf, *.docx, *.md, *.txt)", "*.pdf", "*.docx", "*.md", "*.txt"));
        File selected = chooser.showOpenDialog(uploadButton.getScene().getWindow());
        if (selected == null) {
            return;
        }

        long userId = sessionState.requireCurrentUser().id();
        Task<ResumeTaskService.QueuedResume> uploadTask = new Task<>() {
            @Override
            protected ResumeTaskService.QueuedResume call() {
                return resumeTaskService.uploadAndEnqueue(userId, selected.toPath());
            }
        };
        uploadButton.setDisable(true);
        taskLabel.setText("正在保存 “" + selected.getName() + "” 并创建后台任务…");
        uploadTask.setOnSucceeded(event -> {
            uploadButton.setDisable(false);
            var queued = uploadTask.getValue();
            taskLabel.setText("已加入后台解析队列：" + queued.resume().originalName());
            refresh();
            watchStatus(queued.resume().id(), queued.taskId(), queued.resume().originalName());
        });
        uploadTask.setOnFailed(event -> {
            uploadButton.setDisable(false);
            taskLabel.setText("上传失败");
            viewManager.showError(exceptionHandler.toUserMessage(uploadTask.getException()));
        });
        Thread worker = new Thread(uploadTask, "resume-upload");
        worker.setDaemon(true);
        worker.start();
    }

    private void watchStatus(long resumeId, long taskId, String name) {
        if (statusWatcher != null) statusWatcher.stop();
        statusWatcher = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            var resumes = resumeService.list(sessionState.requireCurrentUser().id());
            resumeTable.getItems().setAll(resumes);
            long completed = resumes.stream().filter(item -> item.status() == ResumeStatus.COMPLETED).count();
            summaryLabel.setText("共 " + resumes.size() + " 份 · 已解析 " + completed + " 份");
            var backgroundTask = backgroundTaskService.requireDto(
                    sessionState.requireCurrentUser().id(), taskId);
            if (backgroundTask.status() == BackgroundTaskStatus.SUCCESS) {
                taskLabel.setText("解析完成：" + name);
                statusWatcher.stop();
            } else if (backgroundTask.status() == BackgroundTaskStatus.FAILED) {
                taskLabel.setText("解析失败：" + name + "；错误已保留，可重新上传或稍后重试。");
                statusWatcher.stop();
            } else if (backgroundTask.status() == BackgroundTaskStatus.PENDING
                    && backgroundTask.attemptCount() > 0) {
                taskLabel.setText("解析失败后等待第 " + (backgroundTask.attemptCount() + 1) + " 次重试：" + name);
            }
        }));
        statusWatcher.setCycleCount(60);
        statusWatcher.play();
    }

    @FXML
    private void deleteSelected() {
        ResumeDto selected = resumeTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                "删除简历 “" + selected.originalName() + "”？本地文件也会被删除。",
                ButtonType.CANCEL, ButtonType.OK);
        confirmation.setHeaderText("确认删除简历");
        if (confirmation.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }
        try {
            resumeService.delete(sessionState.requireCurrentUser().id(), selected.id());
            taskLabel.setText("简历已删除");
            refresh();
        } catch (RuntimeException exception) {
            viewManager.showError(exceptionHandler.toUserMessage(exception));
        }
    }

    @FXML
    private void viewSelected() {
        ResumeDto selected = resumeTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            contentNavigator.showSubPage(
                    "/fxml/resume-detail-view.fxml",
                    "候选人画像",
                    selected.id());
        }
    }

    @FXML
    private void refresh() {
        var resumes = resumeService.list(sessionState.requireCurrentUser().id());
        resumeTable.getItems().setAll(resumes);
        long completed = resumes.stream().filter(item -> item.status() == ResumeStatus.COMPLETED).count();
        summaryLabel.setText("共 " + resumes.size() + " 份 · 已解析 " + completed + " 份");
    }

    private String formatStatus(ResumeStatus status) {
        return switch (status) {
            case UPLOADED -> "已上传";
            case PARSING -> "解析中";
            case COMPLETED -> "已解析";
            case FAILED -> "解析失败";
        };
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }
}
