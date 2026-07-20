package com.inin.aiinterviewer.ui.controller;

import com.inin.aiinterviewer.application.dto.InterviewHistoryItemDto;
import com.inin.aiinterviewer.application.service.InterviewHistoryService;
import com.inin.aiinterviewer.domain.enums.InterviewStatus;
import com.inin.aiinterviewer.ui.navigation.ContentNavigator;
import com.inin.aiinterviewer.ui.navigation.Route;
import com.inin.aiinterviewer.ui.navigation.JavaFxViewManager;
import com.inin.aiinterviewer.ui.state.UserSessionState;
import com.inin.aiinterviewer.application.exception.GlobalExceptionHandler;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
@Scope("prototype")
public class InterviewHistoryController {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final InterviewHistoryService historyService;
    private final UserSessionState sessionState;
    private final ContentNavigator contentNavigator;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusBox;
    @FXML private TableView<InterviewHistoryItemDto> historyTable;
    @FXML private TableColumn<InterviewHistoryItemDto, String> titleColumn;
    @FXML private TableColumn<InterviewHistoryItemDto, String> jobColumn;
    @FXML private TableColumn<InterviewHistoryItemDto, String> statusColumn;
    @FXML private TableColumn<InterviewHistoryItemDto, String> durationColumn;
    @FXML private TableColumn<InterviewHistoryItemDto, String> scoreColumn;
    @FXML private TableColumn<InterviewHistoryItemDto, String> updatedColumn;
    @FXML private Label summaryLabel;
    @FXML private Button detailButton;
    @FXML private Button continueButton;
    @FXML private Button reportButton;
    @FXML private Button deleteButton;

    private final JavaFxViewManager viewManager;
    private final GlobalExceptionHandler exceptionHandler;

    public InterviewHistoryController(
            InterviewHistoryService historyService,
            UserSessionState sessionState,
            ContentNavigator contentNavigator,
            JavaFxViewManager viewManager,
            GlobalExceptionHandler exceptionHandler
    ) {
        this.historyService = historyService;
        this.sessionState = sessionState;
        this.contentNavigator = contentNavigator;
        this.viewManager = viewManager;
        this.exceptionHandler = exceptionHandler;
    }

    @FXML
    private void initialize() {
        statusBox.getItems().setAll("全部状态", "进行中", "已暂停", "已完成", "异常中止");
        statusBox.getSelectionModel().selectFirst();
        titleColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().title()));
        jobColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().jobTitle()));
        statusColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(statusText(cell.getValue().status())));
        durationColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(durationText(cell.getValue())));
        scoreColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(
                cell.getValue().score() == null ? "—" : cell.getValue().score() + " 分"));
        updatedColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(timeText(cell.getValue().updateTime())));
        detailButton.disableProperty().bind(historyTable.getSelectionModel().selectedItemProperty().isNull());
        deleteButton.disableProperty().bind(historyTable.getSelectionModel().selectedItemProperty().isNull());
        historyTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, selected) -> {
            boolean active = selected != null && (selected.status() == InterviewStatus.RUNNING
                    || selected.status() == InterviewStatus.PAUSED || selected.status() == InterviewStatus.CREATED);
            continueButton.setDisable(!active);
            reportButton.setDisable(selected == null || !selected.reportAvailable());
        });
        historyTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && selected() != null) openDetail();
        });
        searchField.setOnAction(event -> refresh());
        refresh();
    }

    @FXML
    private void refresh() {
        var records = historyService.list(userId(), searchField.getText(), selectedStatus());
        historyTable.getItems().setAll(records);
        long completed = records.stream().filter(item -> item.status() == InterviewStatus.COMPLETED).count();
        summaryLabel.setText("共 " + records.size() + " 条记录 · 已完成 " + completed + " 条");
    }

    @FXML
    private void clearFilters() {
        searchField.clear();
        statusBox.getSelectionModel().selectFirst();
        refresh();
    }

    @FXML
    private void createInterview() {
        contentNavigator.showRoute(Route.PLAN);
    }

    @FXML
    private void openDetail() {
        InterviewHistoryItemDto selected = selected();
        if (selected != null) {
            contentNavigator.showSubPage(
                    "/fxml/interview-history-detail-view.fxml", "面试记录", selected.sessionId());
        }
    }

    @FXML
    private void continueInterview() {
        InterviewHistoryItemDto selected = selected();
        if (selected != null) {
            contentNavigator.showSubPage(
                    "/fxml/interview-workspace-view.fxml", "模拟面试", selected.sessionId());
        }
    }

    @FXML
    private void openReport() {
        InterviewHistoryItemDto selected = selected();
        if (selected != null && selected.reportAvailable()) {
            contentNavigator.showSubPage(
                    "/fxml/report-detail-view.fxml", "面试报告", selected.sessionId());
        }
    }

    private InterviewHistoryItemDto selected() {
        return historyTable.getSelectionModel().getSelectedItem();
    }

    private long userId() { return sessionState.requireCurrentUser().id(); }

    private InterviewStatus selectedStatus() {
        switch (statusBox.getSelectionModel().getSelectedIndex()) {
            case 1: return InterviewStatus.RUNNING;
            case 2: return InterviewStatus.PAUSED;
            case 3: return InterviewStatus.COMPLETED;
            case 4: return InterviewStatus.FAILED;
            default: return null;
        }
    }

    private String statusText(InterviewStatus status) {
        if (status == null) return "—";
        switch (status) {
            case CREATED: return "待开始";
            case RUNNING: return "进行中";
            case PAUSED: return "已暂停";
            case COMPLETED: return "已完成";
            case FAILED: return "异常中止";
            default: return "未知";
        }
    }

    private String durationText(InterviewHistoryItemDto item) {
        if (item.startedTime() == null) return "—";
        LocalDateTime end = item.completedTime() == null ? item.updateTime() : item.completedTime();
        long minutes = Math.max(0, Duration.between(item.startedTime(), end).toMinutes());
        return minutes + " 分钟";
    }

    private String timeText(LocalDateTime time) {
        return time == null ? "—" : TIME_FORMAT.format(time);
    }

    @FXML
    private void deleteRecord() {
        InterviewHistoryItemDto selected = selected();
        if (selected == null) return;
        Alert confirmation = new Alert(
                Alert.AlertType.CONFIRMATION,
                "确定要删除这条面试记录吗？此操作不可恢复。",
                ButtonType.CANCEL, ButtonType.OK);
        confirmation.setHeaderText("删除面试记录");
        if (confirmation.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }
        try {
            historyService.delete(userId(), selected.sessionId());
            refresh();
            viewManager.showInfo("删除成功", "面试记录已删除。");
        } catch (RuntimeException exception) {
            viewManager.showError(exceptionHandler.toUserMessage(exception));
        }
    }
}
