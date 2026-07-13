package com.inin.aiinterviewer.ui.controller;

import com.inin.aiinterviewer.application.dto.BackgroundTaskDto;
import com.inin.aiinterviewer.application.service.BackgroundTaskService;
import com.inin.aiinterviewer.domain.enums.BackgroundTaskStatus;
import com.inin.aiinterviewer.domain.enums.BackgroundTaskType;
import com.inin.aiinterviewer.ui.navigation.ContentNavigator;
import com.inin.aiinterviewer.ui.state.UserSessionState;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
@Scope("prototype")
public class BackgroundTaskController {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final BackgroundTaskService taskService;
    private final UserSessionState sessionState;
    private final ContentNavigator contentNavigator;

    @FXML private TableView<BackgroundTaskDto> taskTable;
    @FXML private TableColumn<BackgroundTaskDto, String> typeColumn;
    @FXML private TableColumn<BackgroundTaskDto, String> statusColumn;
    @FXML private TableColumn<BackgroundTaskDto, String> attemptColumn;
    @FXML private TableColumn<BackgroundTaskDto, LocalDateTime> createTimeColumn;
    @FXML private TableColumn<BackgroundTaskDto, LocalDateTime> updateTimeColumn;
    @FXML private Label summaryLabel;
    @FXML private Button detailButton;

    public BackgroundTaskController(
            BackgroundTaskService taskService,
            UserSessionState sessionState,
            ContentNavigator contentNavigator
    ) {
        this.taskService = taskService;
        this.sessionState = sessionState;
        this.contentNavigator = contentNavigator;
    }

    @FXML
    private void initialize() {
        typeColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(typeText(cell.getValue().type())));
        statusColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(statusText(cell.getValue().status())));
        attemptColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().attemptCount() + " 次"));
        configureTimeColumn(createTimeColumn, BackgroundTaskDto::createTime);
        configureTimeColumn(updateTimeColumn, BackgroundTaskDto::updateTime);
        detailButton.disableProperty().bind(taskTable.getSelectionModel().selectedItemProperty().isNull());
        taskTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && taskTable.getSelectionModel().getSelectedItem() != null) {
                viewSelected();
            }
        });
        refresh();
    }

    @FXML
    private void refresh() {
        var tasks = taskService.listDtos(sessionState.requireCurrentUser().id());
        taskTable.getItems().setAll(tasks);
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
    private void back() {
        contentNavigator.back();
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
