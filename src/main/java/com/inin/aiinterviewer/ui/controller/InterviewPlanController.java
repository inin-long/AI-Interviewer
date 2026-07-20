package com.inin.aiinterviewer.ui.controller;

import com.inin.aiinterviewer.application.dto.InterviewHistoryItemDto;
import com.inin.aiinterviewer.application.dto.InterviewPlanDto;
import com.inin.aiinterviewer.application.dto.InterviewSessionDto;
import com.inin.aiinterviewer.application.dto.ResumeDto;
import com.inin.aiinterviewer.application.exception.GlobalExceptionHandler;
import com.inin.aiinterviewer.application.service.InterviewHistoryService;
import com.inin.aiinterviewer.application.service.InterviewPlanAssetService;
import com.inin.aiinterviewer.application.service.InterviewPlanService;
import com.inin.aiinterviewer.application.service.InterviewPlanTransferService;
import com.inin.aiinterviewer.application.service.InterviewSessionService;
import com.inin.aiinterviewer.application.service.ResumeService;
import com.inin.aiinterviewer.domain.enums.InterviewDifficulty;
import com.inin.aiinterviewer.ui.component.AppSelect;
import com.inin.aiinterviewer.ui.dialog.FileDialogService;
import com.inin.aiinterviewer.ui.navigation.ContentNavigator;
import com.inin.aiinterviewer.ui.navigation.JavaFxViewManager;
import com.inin.aiinterviewer.ui.navigation.Route;
import com.inin.aiinterviewer.ui.state.UserSessionState;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
@Scope("prototype")
public class InterviewPlanController {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String FILTER_ALL = "全部方案";
    private static final String FILTER_DEFAULT = "默认方案";
    private static final String FILTER_PROFILE = "已关联画像";
    private static final String FILTER_KNOWLEDGE = "已关联知识库";
    private static final String SORT_UPDATE = "最近更新";
    private static final String SORT_NAME = "方案名称";
    private static final String SORT_DURATION = "面试时长";

    private final InterviewPlanService planService;
    private final InterviewSessionService sessionService;
    private final InterviewHistoryService historyService;
    private final ResumeService resumeService;
    private final InterviewPlanTransferService transferService;
    private final UserSessionState sessionState;
    private final ContentNavigator contentNavigator;
    private final FileDialogService fileDialogService;
    private final JavaFxViewManager viewManager;
    private final GlobalExceptionHandler exceptionHandler;

    @FXML private ListView<InterviewPlanDto> planList;
    @FXML private TextField searchField;
    @FXML private AppSelect<String> filterSelect;
    @FXML private AppSelect<String> sortSelect;
    @FXML private Label summaryLabel;
    @FXML private Label footerCountLabel;
    @FXML private Label previewNameLabel;
    @FXML private Label previewJobLabel;
    @FXML private Label previewDifficultyLabel;
    @FXML private Label previewDurationLabel;
    @FXML private Label previewQuestionLabel;
    @FXML private Label previewResumeLabel;
    @FXML private Label previewKnowledgeLabel;
    @FXML private Label previewStagesLabel;
    @FXML private Label previewRulesLabel;
    @FXML private Label weeklyUseLabel;
    @FXML private Label averageScoreLabel;
    @FXML private Label latestUseLabel;
    @FXML private Button editButton;
    @FXML private Button duplicateButton;

    private final List<InterviewPlanDto> allPlans = new ArrayList<>();
    private final Map<Long, String> resumeNames = new HashMap<>();
    private final List<InterviewSessionDto> sessions = new ArrayList<>();

    public InterviewPlanController(
            InterviewPlanService planService,
            InterviewSessionService sessionService,
            InterviewHistoryService historyService,
            ResumeService resumeService,
            InterviewPlanTransferService transferService,
            UserSessionState sessionState,
            ContentNavigator contentNavigator,
            FileDialogService fileDialogService,
            JavaFxViewManager viewManager,
            GlobalExceptionHandler exceptionHandler
    ) {
        this.planService = planService;
        this.sessionService = sessionService;
        this.historyService = historyService;
        this.resumeService = resumeService;
        this.transferService = transferService;
        this.sessionState = sessionState;
        this.contentNavigator = contentNavigator;
        this.fileDialogService = fileDialogService;
        this.viewManager = viewManager;
        this.exceptionHandler = exceptionHandler;
    }

    @FXML
    private void initialize() {
        filterSelect.getItems().setAll(FILTER_ALL, FILTER_DEFAULT, FILTER_PROFILE, FILTER_KNOWLEDGE);
        filterSelect.setValue(FILTER_ALL);
        sortSelect.getItems().setAll(SORT_UPDATE, SORT_NAME, SORT_DURATION);
        sortSelect.setValue(SORT_UPDATE);
        filterSelect.valueProperty().addListener((observable, oldValue, value) -> applyFilters());
        sortSelect.valueProperty().addListener((observable, oldValue, value) -> applyFilters());
        planList.setCellFactory(ignored -> new PlanCardCell());
        planList.getSelectionModel().selectedItemProperty().addListener(
                (observable, previous, current) -> renderPreview(current));
        editButton.disableProperty().bind(planList.getSelectionModel().selectedItemProperty().isNull());
        duplicateButton.disableProperty().bind(planList.getSelectionModel().selectedItemProperty().isNull());
        refresh();
    }

    @FXML
    private void createPlan() {
        contentNavigator.showSubPage("/fxml/plan-editor-view.fxml", "新建面试方案", null);
    }

    @FXML
    private void editSelected() {
        InterviewPlanDto selected = planList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            contentNavigator.showSubPage("/fxml/plan-editor-view.fxml", "编辑面试方案", selected.id());
        }
    }

    private void openDetail(InterviewPlanDto plan) {
        if (plan != null) {
            contentNavigator.showSubPage("/fxml/plan-detail-view.fxml", "面试方案详情", plan.id());
        }
    }

    @FXML
    private void duplicateSelected() {
        InterviewPlanDto selected = planList.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        try {
            InterviewPlanDto copy = planService.duplicate(userId(), selected.id());
            refresh();
            select(copy.id());
        } catch (RuntimeException exception) {
            showError(exception);
        }
    }

    @FXML
    private void quickStart() {
        InterviewPlanDto selected = planList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            selected = allPlans.stream().filter(InterviewPlanDto::defaultPlan).findFirst()
                    .orElse(allPlans.isEmpty() ? null : allPlans.getFirst());
        }
        if (selected == null) {
            viewManager.showInfo("快速开始", "请先创建至少一个面试方案。");
            return;
        }
        start(selected);
    }

    @FXML
    private void createFromRecent() {
        InterviewPlanDto source = sessions.stream().map(InterviewSessionDto::planId)
                .filter(id -> id != null)
                .map(id -> allPlans.stream().filter(plan -> plan.id().equals(id)).findFirst().orElse(null))
                .filter(java.util.Objects::nonNull).findFirst().orElse(null);
        if (source == null) {
            viewManager.showInfo("从最近面试生成", "暂无可复用的最近面试，请先选择现有方案或新建方案。");
            return;
        }
        try {
            InterviewPlanDto copy = planService.duplicate(userId(), source.id());
            contentNavigator.showSubPage("/fxml/plan-editor-view.fxml", "编辑面试方案", copy.id());
        } catch (RuntimeException exception) {
            showError(exception);
        }
    }

    @FXML
    private void importPlan() {
        Path source = fileDialogService.choosePlanImport(planList.getScene().getWindow()).orElse(null);
        if (source == null) return;
        try {
            InterviewPlanDto imported = transferService.importPlan(userId(), source);
            refresh();
            select(imported.id());
            openDetail(imported);
        } catch (RuntimeException exception) {
            showError(exception);
        }
    }

    @FXML
    private void openResumes() {
        contentNavigator.showRoute(Route.RESUME);
    }

    @FXML
    private void refresh() {
        try {
            long userId = userId();
            allPlans.clear();
            allPlans.addAll(planService.list(userId));
            sessions.clear();
            sessions.addAll(sessionService.list(userId));
            resumeNames.clear();
            for (ResumeDto resume : resumeService.list(userId)) resumeNames.put(resume.id(), resume.originalName());
            applyFilters();
            renderUsageStats();
        } catch (RuntimeException exception) {
            showError(exception);
        }
    }

    @FXML
    private void applyFilters() {
        if (planList == null) return;
        String query = searchField == null || searchField.getText() == null
                ? "" : searchField.getText().strip().toLowerCase(Locale.ROOT);
        List<InterviewPlanDto> filtered = allPlans.stream()
                .filter(this::matchesFilter)
                .filter(plan -> query.isBlank() || searchableText(plan).contains(query))
                .sorted(selectedComparator())
                .toList();
        InterviewPlanDto selected = planList.getSelectionModel().getSelectedItem();
        planList.getItems().setAll(filtered);
        summaryLabel.setText("（" + filtered.size() + "）");
        footerCountLabel.setText("共 " + filtered.size() + " 条");
        if (selected != null && filtered.stream().anyMatch(plan -> plan.id().equals(selected.id()))) {
            select(selected.id());
        } else if (!filtered.isEmpty()) {
            planList.getSelectionModel().selectFirst();
        } else {
            renderPreview(null);
        }
    }

    private void start(InterviewPlanDto plan) {
        try {
            var session = sessionService.startOrResume(userId(), plan.id());
            contentNavigator.showSubPage("/fxml/interview-workspace-view.fxml", "模拟面试", session.id());
        } catch (RuntimeException exception) {
            showError(exception);
        }
    }

    private void renderPreview(InterviewPlanDto plan) {
        if (plan == null) {
            previewNameLabel.setText("请选择方案");
            previewJobLabel.setText("—");
            previewDifficultyLabel.setText("—");
            previewDurationLabel.setText("—");
            previewQuestionLabel.setText("—");
            previewResumeLabel.setText("—");
            previewKnowledgeLabel.setText("—");
            previewStagesLabel.setText("—");
            previewRulesLabel.setText("请选择方案后查看规则");
            return;
        }
        previewNameLabel.setText(plan.name());
        previewJobLabel.setText(plan.jobTitle());
        previewDifficultyLabel.setText(difficultyText(plan.difficulty()));
        previewDurationLabel.setText(plan.durationMinutes() + " 分钟");
        previewQuestionLabel.setText(plan.questionCount() + " 题");
        previewResumeLabel.setText(plan.resumeId() == null ? "未关联" : resumeNames.getOrDefault(plan.resumeId(), "已关联简历"));
        previewKnowledgeLabel.setText(plan.knowledgeCategories().isEmpty()
                ? "未关联" : plan.knowledgeCategories().size() + " 个分类");
        previewStagesLabel.setText(plan.stages().stream().map(this::stageText).reduce((a, b) -> a + " / " + b).orElse("默认流程"));
        String focus = String.valueOf(plan.rules().getOrDefault("focus", "按岗位能力模型动态追问"));
        previewRulesLabel.setText("• " + focus + "\n• 基于简历与知识库证据追问\n• 面试结束生成结构化评估报告");
    }

    private void renderUsageStats() {
        LocalDate weekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        long weekly = sessions.stream().filter(session -> session.startedTime() != null
                && !session.startedTime().toLocalDate().isBefore(weekStart)).count();
        weeklyUseLabel.setText(weekly + " 次");
        List<InterviewHistoryItemDto> histories = historyService.list(userId(), null, null);
        int[] scores = histories.stream().filter(item -> item.score() != null).mapToInt(InterviewHistoryItemDto::score).toArray();
        averageScoreLabel.setText(scores.length == 0 ? "—" : Math.round(java.util.Arrays.stream(scores).average().orElse(0)) + " 分");
        LocalDateTime latest = sessions.stream().map(InterviewSessionDto::updateTime)
                .filter(java.util.Objects::nonNull).max(LocalDateTime::compareTo).orElse(null);
        latestUseLabel.setText(relativeDate(latest));
    }

    private String relativeDate(LocalDateTime time) {
        if (time == null) return "—";
        LocalDate date = time.toLocalDate();
        if (date.equals(LocalDate.now())) return "今天";
        if (date.equals(LocalDate.now().minusDays(1))) return "昨天";
        return DATE_FORMAT.format(date);
    }

    private boolean matchesFilter(InterviewPlanDto plan) {
        String filter = filterSelect == null ? FILTER_ALL : filterSelect.getValue();
        if (FILTER_DEFAULT.equals(filter)) return plan.defaultPlan();
        if (FILTER_PROFILE.equals(filter)) return plan.profileId() != null;
        if (FILTER_KNOWLEDGE.equals(filter)) return !plan.knowledgeCategories().isEmpty();
        return true;
    }

    private Comparator<InterviewPlanDto> selectedComparator() {
        String sort = sortSelect == null ? SORT_UPDATE : sortSelect.getValue();
        if (SORT_NAME.equals(sort)) return Comparator.comparing(InterviewPlanDto::name, String.CASE_INSENSITIVE_ORDER);
        if (SORT_DURATION.equals(sort)) return Comparator.comparingInt(InterviewPlanDto::durationMinutes).reversed();
        return Comparator.comparing(InterviewPlanDto::updateTime, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private String searchableText(InterviewPlanDto plan) {
        return (plan.name() + " " + plan.jobTitle() + " " + plan.jobDescription() + " "
                + plan.rules().getOrDefault("focus", "")).toLowerCase(Locale.ROOT);
    }

    private void select(long planId) {
        planList.getItems().stream().filter(plan -> plan.id().equals(planId)).findFirst()
                .ifPresent(planList.getSelectionModel()::select);
    }

    private long userId() {
        return sessionState.requireCurrentUser().id();
    }

    private void showError(RuntimeException exception) {
        viewManager.showError(exceptionHandler.toUserMessage(exception));
    }

    private String difficultyText(InterviewDifficulty difficulty) {
        return switch (difficulty) {
            case JUNIOR -> "初级";
            case MEDIUM -> "中级";
            case SENIOR -> "高级";
            case EXPERT -> "专家";
        };
    }

    private String stageText(String stage) {
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

    private final class PlanCardCell extends ListCell<InterviewPlanDto> {
        @Override
        protected void updateItem(InterviewPlanDto plan, boolean empty) {
            super.updateItem(plan, empty);
            setText(null);
            setGraphic(empty || plan == null ? null : card(plan));
        }

        private HBox card(InterviewPlanDto plan) {
            ImageView cover = new ImageView(loadPlanImage(plan));
            cover.setFitWidth(64);
            cover.setFitHeight(64);
            cover.setPreserveRatio(true);
            cover.setSmooth(true);
            StackPane coverBox = new StackPane(cover);
            coverBox.getStyleClass().add("plan-cover-box");

            Label name = new Label(plan.name());
            name.getStyleClass().add("plan-row-title");
            name.setTextOverrun(OverrunStyle.ELLIPSIS);
            name.setMinWidth(160);
            name.setMaxWidth(Double.MAX_VALUE);
            FlowPane tags = new FlowPane(6, 4);
            if (plan.defaultPlan()) tags.getChildren().add(tag("默认方案", "primary"));
            if (sessions.stream().anyMatch(session -> plan.id().equals(session.planId()))) {
                tags.getChildren().add(tag("最近使用", "success"));
            }
            HBox title = new HBox(8, name, tags);
            title.setAlignment(Pos.CENTER_LEFT);
            title.setMinWidth(0);
            title.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(name, Priority.ALWAYS);
            Label meta = new Label(plan.jobTitle() + "  ·  " + difficultyText(plan.difficulty()) + "  ·  "
                    + plan.durationMinutes() + " 分钟  ·  " + plan.questionCount() + " 题");
            meta.getStyleClass().add("plan-row-meta");
            meta.setMinWidth(0);
            meta.setMaxWidth(Double.MAX_VALUE);
            meta.setTextOverrun(OverrunStyle.ELLIPSIS);
            String focus = String.valueOf(plan.rules().getOrDefault("focus", "按岗位能力模型动态追问"));
            Label focusLabel = new Label("重点：" + focus);
            focusLabel.getStyleClass().add("plan-row-focus");
            focusLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
            focusLabel.setMinWidth(0);
            focusLabel.setMaxWidth(Double.MAX_VALUE);
            VBox identity = new VBox(5, title, meta, focusLabel);
            identity.setMinWidth(0);
            identity.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(identity, Priority.ALWAYS);

            Label updated = new Label("更新于 " + (plan.updateTime() == null ? "—" : DATE_FORMAT.format(plan.updateTime())));
            updated.getStyleClass().add("plan-row-updated");
            Button start = action("开始面试", "mdi2p-play-outline", true);
            start.setMinWidth(94);
            start.setOnAction(event -> start(plan));
            Button edit = action("编辑", "mdi2p-pencil-outline", false);
            edit.setMinWidth(64);
            edit.setOnAction(event -> {
                getListView().getSelectionModel().select(plan);
                editSelected();
            });
            Button detail = action("", "mdi2d-dots-vertical", false);
            detail.setMinWidth(36);
            detail.setOnAction(event -> openDetail(plan));
            VBox operations = new VBox(13, updated, new HBox(9, start, edit, detail));
            operations.setAlignment(Pos.CENTER_RIGHT);
            operations.setMinWidth(220);

            HBox card = new HBox(14, coverBox, identity, operations);
            card.setAlignment(Pos.CENTER_LEFT);
            card.setPadding(new Insets(12, 14, 12, 12));
            card.getStyleClass().add("plan-row-card");
            card.setMinWidth(0);
            card.setMaxWidth(Double.MAX_VALUE);
            card.prefWidthProperty().bind(planList.widthProperty().subtract(32));
            card.setOnMouseClicked(event -> {
                getListView().getSelectionModel().select(plan);
                if (event.getClickCount() == 2) openDetail(plan);
            });
            start.setOnMouseClicked(event -> event.consume());
            edit.setOnMouseClicked(event -> event.consume());
            detail.setOnMouseClicked(event -> event.consume());
            return card;
        }

        private Label tag(String text, String tone) {
            Label tag = new Label(text);
            tag.getStyleClass().addAll("plan-row-tag", tone);
            return tag;
        }

        private Button action(String text, String iconLiteral, boolean primary) {
            Button button = new Button(text, new FontIcon(iconLiteral));
            button.getStyleClass().add(primary ? "plan-row-primary" : "plan-row-action");
            return button;
        }
    }

    private Image loadPlanImage(InterviewPlanDto plan) {
        Object value = plan.rules().get(InterviewPlanAssetService.ICON_PATH_RULE);
        if (value != null && !String.valueOf(value).isBlank()) {
            Path path = Path.of(String.valueOf(value));
            if (Files.isRegularFile(path)) return new Image(path.toUri().toString());
        }
        return new Image(getClass().getResource("/images/plan/plan-placeholder.png").toExternalForm());
    }
}
