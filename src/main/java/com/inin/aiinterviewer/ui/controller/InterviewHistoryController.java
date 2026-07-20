package com.inin.aiinterviewer.ui.controller;

import com.inin.aiinterviewer.application.dto.InterviewHistoryItemDto;
import com.inin.aiinterviewer.application.service.InterviewHistoryService;
import com.inin.aiinterviewer.domain.enums.InterviewStatus;
import com.inin.aiinterviewer.ui.component.AppDialogs;
import com.inin.aiinterviewer.ui.component.AppSelect;
import com.inin.aiinterviewer.ui.navigation.ContentNavigator;
import com.inin.aiinterviewer.ui.navigation.JavaFxViewManager;
import com.inin.aiinterviewer.ui.navigation.Route;
import com.inin.aiinterviewer.ui.state.UserSessionState;
import com.inin.aiinterviewer.application.exception.GlobalExceptionHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

@Component
@Scope("prototype")
public class InterviewHistoryController {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String SORT_RECENT = "按最近时间";
    private static final String SORT_OLDEST = "按最早时间";
    private static final String SORT_SCORE = "按评分高低";
    private static final String SORT_DURATION = "按用时长度";

    private final InterviewHistoryService historyService;
    private final UserSessionState sessionState;
    private final ContentNavigator contentNavigator;
    private final JavaFxViewManager viewManager;
    private final GlobalExceptionHandler exceptionHandler;

    @FXML private TextField searchField;
    @FXML private AppSelect<String> statusBox;
    @FXML private AppSelect<String> sortBox;
    @FXML private AppSelect<String> pageSizeBox;
    @FXML private ListView<InterviewHistoryItemDto> historyList;
    @FXML private FlowPane quickFilterPane;
    @FXML private Label summaryLabel;
    @FXML private Label footerCountLabel;
    @FXML private Label weeklyCountLabel;
    @FXML private Label averageScoreLabel;
    @FXML private Label completionLabel;
    @FXML private Label latestLabel;
    @FXML private Label detailTitleLabel;
    @FXML private Label detailJobLabel;
    @FXML private Label detailStatusLabel;
    @FXML private Label detailStartLabel;
    @FXML private Label detailEndLabel;
    @FXML private Label detailDurationLabel;
    @FXML private Label detailResumeLabel;
    @FXML private Label detailScoreLabel;
    @FXML private Label detailReportLabel;
    @FXML private Label detailStagesLabel;
    @FXML private Label detailSummaryLabel;
    @FXML private Button reportDetailButton;
    @FXML private Button continueDialogButton;

    private final List<InterviewHistoryItemDto> allRecords = new ArrayList<>();
    private String activeQuickFilter = "";
    private final Map<String, Predicate<InterviewHistoryItemDto>> quickFilters = new LinkedHashMap<>();

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
        sortBox.getItems().setAll(SORT_RECENT, SORT_OLDEST, SORT_SCORE, SORT_DURATION);
        sortBox.getSelectionModel().selectFirst();
        pageSizeBox.getItems().setAll("10 条/页", "20 条/页", "50 条/页");
        pageSizeBox.getSelectionModel().selectFirst();

        historyList.setCellFactory(ignored -> new HistoryCardCell());
        historyList.getSelectionModel().selectedItemProperty().addListener(
                (observable, previous, current) -> renderDetail(current));
        statusBox.valueProperty().addListener((observable, oldValue, value) -> refresh());
        sortBox.valueProperty().addListener((observable, oldValue, value) -> refresh());
        searchField.setOnAction(event -> refresh());
        refresh();
    }

    @FXML
    private void createInterview() {
        contentNavigator.showRoute(Route.PLAN);
    }

    @FXML
    private void refresh() {
        try {
            long userId = userId();
            allRecords.clear();
            allRecords.addAll(historyService.list(userId, searchField.getText(), selectedStatus()));
            applySorting();
            renderQuickFilters();
            renderSummary();
            renderStatistics();
            historyList.getItems().setAll(filtered());
            summaryLabel.setText("（" + allRecords.size() + "）");
            footerCountLabel.setText("共 " + allRecords.size() + " 条");
            if (!historyList.getItems().isEmpty()) {
                InterviewHistoryItemDto current = historyList.getSelectionModel().getSelectedItem();
                if (current == null || !historyList.getItems().contains(current)) {
                    historyList.getSelectionModel().selectFirst();
                }
            } else {
                renderDetail(null);
            }
        } catch (RuntimeException exception) {
            showError(exception);
        }
    }

    @FXML
    private void openDetail() {
        InterviewHistoryItemDto selected = selected();
        if (selected == null) return;
        if (selected.status() == InterviewStatus.RUNNING || selected.status() == InterviewStatus.PAUSED
                || selected.status() == InterviewStatus.CREATED) {
            contentNavigator.showSubPage(
                    "/fxml/interview-workspace-view.fxml", "模拟面试", selected.sessionId());
        } else {
            contentNavigator.showSubPage(
                    "/fxml/interview-history-detail-view.fxml", "面试记录", selected.sessionId());
        }
    }

    @FXML
    private void continueInterview() {
        InterviewHistoryItemDto selected = selected();
        if (selected == null) return;
        contentNavigator.showSubPage(
                "/fxml/interview-workspace-view.fxml", "模拟面试", selected.sessionId());
    }

    @FXML
    private void openReport() {
        InterviewHistoryItemDto selected = selected();
        if (selected == null || !selected.reportAvailable()) return;
        contentNavigator.showSubPage(
                "/fxml/report-detail-view.fxml", "面试报告", selected.sessionId());
    }

    @FXML
    private void deleteRecord() {
        InterviewHistoryItemDto selected = selected();
        if (selected == null) return;
        if (!AppDialogs.confirm(
                historyList.getScene() == null ? null : historyList.getScene().getWindow(),
                "删除面试记录",
                "确认删除面试记录",
                "将永久删除这条面试记录，此操作无法撤销。",
                "删除",
                true)) return;
        try {
            historyService.delete(userId(), selected.sessionId());
            refresh();
            viewManager.showInfo("删除成功", "面试记录已删除。");
        } catch (RuntimeException exception) {
            showError(exception);
        }
    }

    private void renderDetail(InterviewHistoryItemDto item) {
        if (item == null) {
            detailTitleLabel.setText("—");
            detailJobLabel.setText("—");
            detailStatusLabel.setText("—");
            detailStatusLabel.getStyleClass().removeAll("history-status-active", "history-status-completed",
                    "history-status-paused", "history-status-failed");
            detailStartLabel.setText("—");
            detailEndLabel.setText("—");
            detailDurationLabel.setText("—");
            detailResumeLabel.setText("—");
            detailScoreLabel.setText("—");
            detailReportLabel.setText("—");
            detailStagesLabel.setText("—");
            detailSummaryLabel.setText("请选择一条记录以查看摘要");
            reportDetailButton.setDisable(true);
            continueDialogButton.setDisable(true);
            return;
        }
        detailTitleLabel.setText(Optional.ofNullable(item.title()).orElse("未命名面试"));
        detailJobLabel.setText(Optional.ofNullable(item.jobTitle()).orElse("—"));
        updateStatusStyle(detailStatusLabel, statusText(item.status()));
        detailStatusLabel.setText(statusText(item.status()));
        detailStartLabel.setText(timeText(item.startedTime()));
        detailEndLabel.setText(timeText(item.completedTime() == null ? item.updateTime() : item.completedTime()));
        detailDurationLabel.setText(durationText(item));
        detailResumeLabel.setText(item.resumeName() == null ? "未关联简历" : item.resumeName());
        detailScoreLabel.setText(item.score() == null ? "—" : item.score() + " 分");
        detailReportLabel.setText(Optional.ofNullable(item.reportStatusText()).orElse("未生成"));
        detailStagesLabel.setText(stagesText(item));
        detailSummaryLabel.setText(Optional.ofNullable(item.interviewSummary())
                .filter(text -> !text.isBlank())
                .orElse("本场面试尚未生成结构化摘要。"));
        reportDetailButton.setDisable(!item.reportAvailable());
        continueDialogButton.setDisable(false);
    }

    private void updateStatusStyle(Label label, String text) {
        label.getStyleClass().removeAll("history-status-active", "history-status-completed",
                "history-status-paused", "history-status-failed");
        String tone = switch (text) {
            case "进行中" -> "history-status-active";
            case "已完成" -> "history-status-completed";
            case "已暂停" -> "history-status-paused";
            case "异常中止" -> "history-status-failed";
            default -> null;
        };
        if (tone != null) label.getStyleClass().add(tone);
    }

    private void renderSummary() {
        long completed = allRecords.stream().filter(item -> item.status() == InterviewStatus.COMPLETED).count();
        long active = allRecords.stream().filter(item -> item.status() == InterviewStatus.RUNNING
                || item.status() == InterviewStatus.PAUSED).count();
        long failed = allRecords.stream().filter(item -> item.status() == InterviewStatus.FAILED).count();
        summaryLabel.setText("（" + allRecords.size() + "）");
        footerCountLabel.setText("共 " + allRecords.size() + " 条 · 已完成 " + completed
                + " · 进行中 " + active + " · 异常 " + failed);
    }

    private void renderStatistics() {
        LocalDate weekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        long weekly = allRecords.stream()
                .filter(item -> item.startedTime() != null
                        && !item.startedTime().toLocalDate().isBefore(weekStart))
                .count();
        weeklyCountLabel.setText(weekly + " 次");
        int[] scores = allRecords.stream()
                .filter(item -> item.score() != null)
                .mapToInt(InterviewHistoryItemDto::score)
                .toArray();
        averageScoreLabel.setText(scores.length == 0 ? "—"
                : Math.round(Arrays.stream(scores).average().orElse(0)) + " 分");
        long totalForCompletion = allRecords.size();
        long completedForRate = allRecords.stream()
                .filter(item -> item.status() == InterviewStatus.COMPLETED).count();
        long completionPercent = totalForCompletion == 0 ? 0
                : Math.round(completedForRate * 100.0 / totalForCompletion);
        completionLabel.setText(completionPercent + "%");
        LocalDateTime latestUpdate = allRecords.stream()
                .map(InterviewHistoryItemDto::updateTime)
                .filter(java.util.Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);
        latestLabel.setText(relativeDate(latestUpdate));
    }

    private void renderQuickFilters() {
        quickFilters.clear();
        LocalDate today = LocalDate.now();
        quickFilters.put("最近7天", this.matchesDate(today.minusDays(6), today));
        quickFilters.put("最近30天", this.matchesDate(today.minusDays(29), today));
        quickFilters.put("已完成", item -> item.status() == InterviewStatus.COMPLETED);
        quickFilters.put("已暂停", item -> item.status() == InterviewStatus.PAUSED);
        quickFilters.put("高分记录", item -> item.score() != null && item.score() >= 80);
        quickFilters.put("低分薄弱项", item -> item.score() != null && item.score() < 70);
        quickFilterPane.getChildren().clear();
        quickFilters.forEach((label, predicate) -> {
            Button chip = new Button(label);
            chip.getStyleClass().add("history-quick-chip");
            if (label.equals(activeQuickFilter)) chip.getStyleClass().add("active");
            chip.setOnAction(event -> toggleQuickFilter(label));
            quickFilterPane.getChildren().add(chip);
        });
    }

    private void toggleQuickFilter(String label) {
        if (label.equals(activeQuickFilter)) {
            activeQuickFilter = "";
        } else {
            activeQuickFilter = label;
        }
        historyList.getItems().setAll(filtered());
        renderQuickFilters();
    }

    private Predicate<InterviewHistoryItemDto> matchesDate(LocalDate from, LocalDate to) {
        return item -> {
            LocalDateTime time = item.startedTime() == null ? item.updateTime() : item.startedTime();
            if (time == null) return false;
            LocalDate date = time.toLocalDate();
            return !date.isBefore(from) && !date.isAfter(to);
        };
    }

    private void applySorting() {
        String sort = sortBox.getSelectionModel().getSelectedItem();
        Comparator<InterviewHistoryItemDto> comparator = switch (sort == null ? SORT_RECENT : sort) {
            case SORT_OLDEST -> Comparator.comparing(InterviewHistoryItemDto::updateTime,
                    Comparator.nullsLast(Comparator.naturalOrder()));
            case SORT_SCORE -> Comparator.comparing(InterviewHistoryItemDto::score,
                    Comparator.nullsLast(Comparator.reverseOrder()));
            case SORT_DURATION -> Comparator.comparing(InterviewHistoryItemDto::startedTime,
                    Comparator.nullsLast(Comparator.naturalOrder()));
            default -> Comparator.comparing(InterviewHistoryItemDto::updateTime,
                    Comparator.nullsLast(Comparator.reverseOrder()));
        };
        allRecords.sort(comparator);
    }

    private List<InterviewHistoryItemDto> filtered() {
        if (activeQuickFilter == null || activeQuickFilter.isEmpty()) return allRecords;
        Predicate<InterviewHistoryItemDto> predicate = quickFilters.get(activeQuickFilter);
        if (predicate == null) return allRecords;
        return allRecords.stream().filter(predicate).toList();
    }

    private InterviewHistoryItemDto selected() {
        return historyList.getSelectionModel().getSelectedItem();
    }

    private long userId() { return sessionState.requireCurrentUser().id(); }

    private InterviewStatus selectedStatus() {
        return switch (statusBox.getSelectionModel().getSelectedIndex()) {
            case 1 -> InterviewStatus.RUNNING;
            case 2 -> InterviewStatus.PAUSED;
            case 3 -> InterviewStatus.COMPLETED;
            case 4 -> InterviewStatus.FAILED;
            default -> null;
        };
    }

    private String statusText(InterviewStatus status) {
        return switch (status) {
            case CREATED -> "待开始";
            case RUNNING -> "进行中";
            case PAUSED -> "已暂停";
            case COMPLETED -> "已完成";
            case FAILED -> "异常中止";
        };
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

    private String stagesText(InterviewHistoryItemDto item) {
        if (item.sessionStages() == null || item.sessionStages().isEmpty()) return "默认流程";
        return item.sessionStages().stream()
                .map(this::stageText)
                .reduce((a, b) -> a + " / " + b).orElse("默认流程");
    }

    private String stageText(String stage) {
        if (stage == null) return "";
        return switch (stage) {
            case "INTRODUCTION" -> "自我介绍";
            case "RESUME_REVIEW" -> "简历回顾";
            case "PROJECT_EXPERIENCE" -> "项目经历";
            case "TECHNICAL_DEEP_DIVE" -> "技术深挖";
            case "SYSTEM_DESIGN" -> "系统设计";
            case "CODING" -> "代码题";
            case "BEHAVIORAL" -> "行为面试";
            case "SUMMARY" -> "总结";
            default -> stage;
        };
    }

    private String relativeDate(LocalDateTime time) {
        if (time == null) return "—";
        LocalDate date = time.toLocalDate();
        if (date.equals(LocalDate.now())) return "今天";
        if (date.equals(LocalDate.now().minusDays(1))) return "昨天";
        return DATE_FORMAT.format(date);
    }

    private void showError(RuntimeException exception) {
        viewManager.showError(exceptionHandler.toUserMessage(exception));
    }

    private final class HistoryCardCell extends ListCell<InterviewHistoryItemDto> {
        @Override
        protected void updateItem(InterviewHistoryItemDto item, boolean empty) {
            super.updateItem(item, empty);
            setText(null);
            setGraphic(empty || item == null ? null : card(item));
        }

        private HBox card(InterviewHistoryItemDto item) {
            ImageView cover = new ImageView(loadPlanImage(item));
            cover.setFitWidth(48);
            cover.setFitHeight(48);
            cover.setPreserveRatio(true);
            cover.setSmooth(true);
            StackPane coverBox = new StackPane(cover);
            coverBox.getStyleClass().add("history-cover-box");

            Label title = new Label(Optional.ofNullable(item.title()).orElse("未命名面试"));
            title.getStyleClass().add("history-row-title");
            title.setTextOverrun(OverrunStyle.ELLIPSIS);
            title.setMinWidth(0);
            title.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(title, Priority.ALWAYS);

            Label statusLabel = new Label(statusText(item.status()));
            statusLabel.getStyleClass().add("history-row-status");
            applyStatusTone(statusLabel, statusText(item.status()));
            HBox titleRow = new HBox(8, title, statusLabel);
            titleRow.setAlignment(Pos.CENTER_LEFT);
            titleRow.setMinWidth(0);
            titleRow.setMaxWidth(Double.MAX_VALUE);

            String job = Optional.ofNullable(item.jobTitle()).orElse("—");
            String duration = durationText(item);
            String updated = timeText(item.updateTime());
            Label meta = new Label(job + "  ·  " + duration + "  ·  " + updated);
            meta.getStyleClass().add("history-row-meta");
            meta.setTextOverrun(OverrunStyle.ELLIPSIS);
            meta.setMinWidth(0);
            meta.setMaxWidth(Double.MAX_VALUE);

            Label resume = new Label("使用简历：" + (item.resumeName() == null ? "未关联" : item.resumeName()));
            resume.getStyleClass().add("history-row-resume");
            resume.setTextOverrun(OverrunStyle.ELLIPSIS);
            resume.setMinWidth(0);
            resume.setMaxWidth(Double.MAX_VALUE);

            String scoreText = item.score() == null ? "评分：—" : "评分：" + item.score() + " 分";
            Label score = new Label(scoreText);
            score.getStyleClass().add("history-row-score");
            if (item.score() != null) score.getStyleClass().add("scored");

            Label tagsPrefix = new Label("标签：");
            tagsPrefix.getStyleClass().add("history-row-tags-prefix");

            FlowPane tags = new FlowPane(6, 4);
            if (item.tags() != null) {
                for (String tag : item.tags()) {
                    if (tags.getChildren().size() >= 4) break;
                    if (tag == null || tag.isBlank()) continue;
                    Label chip = new Label(tag);
                    chip.getStyleClass().add("history-row-tag");
                    tags.getChildren().add(chip);
                }
            }
            if (item.reportAvailable()) {
                Label reportTag = new Label("已生成报告");
                reportTag.getStyleClass().addAll("history-row-tag", "success");
                tags.getChildren().add(reportTag);
            }

            HBox scoreAndTags = new HBox(10, score, tagsPrefix, tags);
            scoreAndTags.setAlignment(Pos.CENTER_LEFT);
            scoreAndTags.setMinWidth(0);
            scoreAndTags.setMaxWidth(Double.MAX_VALUE);

            VBox identity = new VBox(5, titleRow, meta, resume, scoreAndTags);
            identity.setMinWidth(0);
            identity.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(identity, Priority.ALWAYS);

            Button reportButton = new Button("查看报告", new FontIcon("mdi2f-file-document-outline"));
            reportButton.getStyleClass().add("history-row-primary");
            reportButton.setDisable(!item.reportAvailable());
            reportButton.setOnAction(event -> {
                getListView().getSelectionModel().select(item);
                openReport();
            });

            Button continueButton = new Button("继续面试", new FontIcon("mdi2p-play-outline"));
            continueButton.getStyleClass().add("history-row-action");
            boolean canContinue = item.status() == InterviewStatus.RUNNING
                    || item.status() == InterviewStatus.PAUSED
                    || item.status() == InterviewStatus.CREATED;
            continueButton.setDisable(!canContinue);
            continueButton.setOnAction(event -> {
                getListView().getSelectionModel().select(item);
                continueInterview();
            });

            Button recordButton = new Button("查看记录", new FontIcon("mdi2c-comment-text-outline"));
            recordButton.getStyleClass().add("history-row-action");
            recordButton.setOnAction(event -> {
                getListView().getSelectionModel().select(item);
                openDetail();
            });

            Button moreButton = new Button("", new FontIcon("mdi2d-dots-horizontal"));
            moreButton.getStyleClass().add("history-row-more");
            moreButton.setOnAction(event -> {
                getListView().getSelectionModel().select(item);
                showItemMenu(moreButton, item);
            });

            HBox operations = new HBox(8);
            operations.setAlignment(Pos.CENTER_RIGHT);
            operations.setMinWidth(186);
            if (item.reportAvailable()) {
                operations.getChildren().add(reportButton);
            } else if (canContinue) {
                VBox actionStack = new VBox(6, continueButton, recordButton);
                actionStack.setAlignment(Pos.CENTER_RIGHT);
                operations.getChildren().add(actionStack);
            } else {
                operations.getChildren().add(recordButton);
            }
            operations.getChildren().add(moreButton);

            HBox card = new HBox(14, coverBox, identity, operations);
            card.setAlignment(Pos.CENTER_LEFT);
            card.setPadding(new Insets(12, 14, 12, 12));
            card.getStyleClass().add("history-row-card");
            card.setMinWidth(0);
            card.setMaxWidth(Double.MAX_VALUE);
            card.prefWidthProperty().bind(historyList.widthProperty().subtract(28));
            card.setOnMouseClicked(event -> {
                getListView().getSelectionModel().select(item);
                if (event.getClickCount() == 2) openDetail();
            });
            continueButton.setOnMouseClicked(event -> event.consume());
            reportButton.setOnMouseClicked(event -> event.consume());
            recordButton.setOnMouseClicked(event -> event.consume());
            moreButton.setOnMouseClicked(event -> event.consume());
            return card;
        }

        private void showItemMenu(Button anchor, InterviewHistoryItemDto item) {
            ContextMenu menu = new ContextMenu();
            MenuItem openDetail = new MenuItem("查看记录");
            openDetail.setOnAction(event -> openDetail());
            MenuItem openReport = new MenuItem("查看报告");
            openReport.setDisable(!item.reportAvailable());
            openReport.setOnAction(event -> openReport());
            if (item.status() == InterviewStatus.RUNNING || item.status() == InterviewStatus.PAUSED
                    || item.status() == InterviewStatus.CREATED) {
                MenuItem cont = new MenuItem("继续面试");
                cont.setOnAction(event -> continueInterview());
                menu.getItems().add(cont);
            }
            menu.getItems().addAll(openDetail, openReport, new SeparatorMenuItem());
            MenuItem delete = new MenuItem("删除记录");
            delete.setOnAction(event -> deleteRecord());
            menu.getItems().add(delete);
            menu.show(anchor, javafx.geometry.Side.BOTTOM, 0, 4);
        }

        private void applyStatusTone(Label label, String text) {
            switch (text) {
                case "进行中" -> label.getStyleClass().add("active");
                case "已完成" -> label.getStyleClass().add("completed");
                case "已暂停" -> label.getStyleClass().add("paused");
                case "异常中止" -> label.getStyleClass().add("failed");
                default -> { }
            }
        }

        private Image loadPlanImage(InterviewHistoryItemDto item) {
            String path = item.planIconPath();
            if (path != null && !path.isBlank()) {
                Path filePath = Path.of(path);
                if (Files.isRegularFile(filePath)) {
                    return new Image(filePath.toUri().toString());
                }
            }
            return new Image(getClass().getResource("/images/plan/plan-placeholder.png").toExternalForm());
        }
    }
}
