package com.inin.aiinterviewer.ui.controller;

import com.inin.aiinterviewer.application.dto.InterviewHistoryItemDto;
import com.inin.aiinterviewer.application.dto.InterviewPlanDto;
import com.inin.aiinterviewer.application.dto.InterviewReportDto;
import com.inin.aiinterviewer.application.exception.GlobalExceptionHandler;
import com.inin.aiinterviewer.application.service.InterviewHistoryService;
import com.inin.aiinterviewer.application.service.InterviewPlanService;
import com.inin.aiinterviewer.application.service.InterviewResultService;
import com.inin.aiinterviewer.application.service.InterviewSessionService;
import com.inin.aiinterviewer.application.service.KnowledgeDocumentService;
import com.inin.aiinterviewer.application.service.ResumeService;
import com.inin.aiinterviewer.domain.enums.InterviewStatus;
import com.inin.aiinterviewer.ui.navigation.ContentNavigator;
import com.inin.aiinterviewer.ui.navigation.JavaFxViewManager;
import com.inin.aiinterviewer.ui.navigation.Route;
import com.inin.aiinterviewer.ui.state.UserSessionState;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Arc;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@Scope("prototype")
public class DashboardController {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final PseudoClass COMPLETED = PseudoClass.getPseudoClass("completed");
    private static final PseudoClass ACTIVE = PseudoClass.getPseudoClass("active");
    private static final PseudoClass FAILED = PseudoClass.getPseudoClass("failed");
    private static final PseudoClass SCORED = PseudoClass.getPseudoClass("scored");

    private final UserSessionState sessionState;
    private final ResumeService resumeService;
    private final KnowledgeDocumentService knowledgeService;
    private final InterviewPlanService planService;
    private final InterviewHistoryService historyService;
    private final InterviewResultService resultService;
    private final InterviewSessionService sessionService;
    private final ContentNavigator contentNavigator;
    private final JavaFxViewManager viewManager;
    private final GlobalExceptionHandler exceptionHandler;

    @FXML private Label greetingLabel;
    @FXML private Label resumeCountLabel;
    @FXML private Label resumeHintLabel;
    @FXML private Label knowledgeCountLabel;
    @FXML private Label knowledgeHintLabel;
    @FXML private Label activeInterviewCountLabel;
    @FXML private Label activeInterviewHintLabel;
    @FXML private Label latestScoreLabel;
    @FXML private Label latestScoreHintLabel;
    @FXML private VBox recentInterviewList;
    @FXML private VBox recentPlanList;
    @FXML private Arc scoreArc;
    @FXML private Label scoreValueLabel;
    @FXML private VBox dimensionList;
    @FXML private Label scoreHeadlineLabel;
    @FXML private Label scoreSummaryLabel;

    private List<InterviewHistoryItemDto> interviewHistory = List.of();
    private List<InterviewPlanDto> interviewPlans = List.of();

    public DashboardController(
            UserSessionState sessionState,
            ResumeService resumeService,
            KnowledgeDocumentService knowledgeService,
            InterviewPlanService planService,
            InterviewHistoryService historyService,
            InterviewResultService resultService,
            InterviewSessionService sessionService,
            ContentNavigator contentNavigator,
            JavaFxViewManager viewManager,
            GlobalExceptionHandler exceptionHandler
    ) {
        this.sessionState = sessionState;
        this.resumeService = resumeService;
        this.knowledgeService = knowledgeService;
        this.planService = planService;
        this.historyService = historyService;
        this.resultService = resultService;
        this.sessionService = sessionService;
        this.contentNavigator = contentNavigator;
        this.viewManager = viewManager;
        this.exceptionHandler = exceptionHandler;
    }

    @FXML
    private void initialize() {
        refreshDashboard();
    }

    private void refreshDashboard() {
        long userId = userId();
        greetingLabel.setText(greetingText() + "，" + sessionState.requireCurrentUser().nickname());

        int resumeCount = resumeService.list(userId).size();
        int knowledgeCount = knowledgeService.list(userId).size();
        interviewPlans = planService.list(userId);
        interviewHistory = historyService.list(userId, "", null);

        long activeCount = interviewHistory.stream().filter(this::isActive).count();
        Optional<InterviewHistoryItemDto> latestScored = interviewHistory.stream()
                .filter(item -> item.score() != null)
                .findFirst();

        resumeCountLabel.setText(String.valueOf(resumeCount));
        resumeHintLabel.setText(resumeCount == 0 ? "去上传第一份简历" : "本地资料已同步");
        knowledgeCountLabel.setText(String.valueOf(knowledgeCount));
        knowledgeHintLabel.setText(knowledgeCount == 0 ? "上传资料增强追问" : "面试知识范围已就绪");
        activeInterviewCountLabel.setText(String.valueOf(activeCount));
        activeInterviewHintLabel.setText(activeCount == 0 ? "新面试等你开始" : "继续加油 💪");
        latestScoreLabel.setText(latestScored.map(item -> String.valueOf(item.score())).orElse("--"));
        latestScoreHintLabel.setText(latestScored.isPresent() ? "最近一次正式评估" : "完成面试后生成");

        renderRecentInterviews();
        renderRecentPlans();
        renderLatestReport();
    }

    private void renderRecentInterviews() {
        recentInterviewList.getChildren().clear();
        List<InterviewHistoryItemDto> recent = interviewHistory.stream().limit(3).toList();
        if (recent.isEmpty()) {
            recentInterviewList.getChildren().add(emptyState(
                    "还没有面试记录", "创建方案并开始模拟面试后，最近进度会显示在这里。"));
            return;
        }
        recent.stream().map(this::interviewRow).forEach(recentInterviewList.getChildren()::add);
    }

    private Button interviewRow(InterviewHistoryItemDto item) {
        StackPane iconBubble = iconBubble(interviewIcon(item.title()));
        VBox copy = new VBox(3,
                label(textOr(item.title(), "未命名面试"), "dashboard-row-title"),
                label(textOr(item.jobTitle(), "通用技术岗位"), "dashboard-row-meta"));
        copy.setMinWidth(230);
        copy.setPrefWidth(230);

        Label date = label(timeText(item.updateTime()), "dashboard-row-date");
        Label status = label(statusText(item.status()), "dashboard-status-chip");
        status.pseudoClassStateChanged(COMPLETED, item.status() == InterviewStatus.COMPLETED);
        status.pseudoClassStateChanged(ACTIVE, isActive(item));
        status.pseudoClassStateChanged(FAILED, item.status() == InterviewStatus.FAILED);

        Label score = label(item.score() == null ? "--" : item.score() + "/100", "dashboard-row-score");
        score.pseudoClassStateChanged(SCORED, item.score() != null);
        Label action = label(rowActionText(item), "dashboard-row-action");
        FontIcon more = icon("mdi2d-dots-vertical", 19);
        more.getStyleClass().add("dashboard-plan-more");

        HBox row = new HBox(12, iconBubble, copy, date, fixedGap(19), status,
                fixedGap(18), score, fixedGap(10), action, more);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMaxWidth(Double.MAX_VALUE);

        Button button = new Button();
        button.setGraphic(row);
        button.setMaxWidth(Double.MAX_VALUE);
        button.getStyleClass().add("dashboard-list-row");
        button.setOnAction(event -> openInterview(item));
        return button;
    }

    private void renderRecentPlans() {
        recentPlanList.getChildren().clear();
        List<InterviewPlanDto> recent = interviewPlans.stream().limit(3).toList();
        if (recent.isEmpty()) {
            recentPlanList.getChildren().add(emptyState(
                    "暂无面试方案", "创建方案后即可一键开始面试。"));
            return;
        }
        recent.stream().map(this::planRow).forEach(recentPlanList.getChildren()::add);
    }

    private Button planRow(InterviewPlanDto plan) {
        FontIcon documentIcon = icon("mdi2f-file-document-outline", 22);
        VBox copy = new VBox(2,
                label(textOr(plan.name(), "未命名方案"), "dashboard-plan-title"),
                label("更新时间：" + timeText(plan.updateTime(), "yyyy-MM-dd"), "dashboard-plan-meta"));
        HBox.setHgrow(copy, Priority.ALWAYS);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        FontIcon more = icon("mdi2d-dots-vertical", 20);
        more.getStyleClass().add("dashboard-plan-more");
        HBox row = new HBox(12, documentIcon, copy, spacer, more);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMaxWidth(Double.MAX_VALUE);

        Button button = new Button();
        button.setGraphic(row);
        button.setMaxWidth(Double.MAX_VALUE);
        button.getStyleClass().add("dashboard-plan-row");
        button.setOnAction(event -> contentNavigator.showSubPage(
                "/fxml/plan-editor-view.fxml", "编辑面试方案", plan.id()));
        return button;
    }

    private void renderLatestReport() {
        Optional<InterviewReportDto> report = interviewHistory.stream()
                .filter(InterviewHistoryItemDto::reportAvailable)
                .findFirst()
                .flatMap(item -> resultService.find(userId(), item.sessionId()));

        if (report.isEmpty()) {
            scoreArc.setLength(0);
            scoreValueLabel.setText("--");
            scoreHeadlineLabel.setText("完成一次面试后生成能力分析");
            scoreSummaryLabel.setText("系统会结合回答质量、项目经验和沟通表现给出结构化评估。");
            renderDimensions(Map.of());
            return;
        }

        InterviewReportDto value = report.get();
        int score = boundedScore(value.overallScore());
        scoreArc.setLength(-360d * score / 100d);
        scoreArc.setAccessibleText("综合评分 " + score + " 分");
        scoreValueLabel.setText(String.valueOf(score));
        scoreHeadlineLabel.setText("整体表现 " + score + " 分，能力画像已更新");
        scoreSummaryLabel.setText(value.summary() == null || value.summary().isBlank()
                ? "本次评估已完成，可进入报告中心查看完整分析与证据。"
                : value.summary());
        renderDimensions(value.dimensions());
    }

    private void renderDimensions(Map<String, Integer> scores) {
        LinkedHashMap<String, String> dimensions = new LinkedHashMap<>();
        dimensions.put("technical", "技术能力");
        dimensions.put("problemSolving", "问题解决");
        dimensions.put("project", "项目经验");
        dimensions.put("systemDesign", "系统设计");
        dimensions.put("communication", "沟通能力");

        dimensionList.getChildren().clear();
        dimensions.forEach((key, name) -> {
            Integer rawScore = scores.get(key);
            int score = rawScore == null ? 0 : boundedScore(rawScore);
            Label nameLabel = label(name, "dashboard-dimension-name");
            ProgressBar bar = new ProgressBar(rawScore == null ? 0 : score / 100d);
            bar.getStyleClass().add("dashboard-dimension-bar");
            bar.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(bar, Priority.ALWAYS);
            Label scoreLabel = label(rawScore == null ? "--" : String.valueOf(score), "dashboard-dimension-score");
            HBox row = new HBox(10, nameLabel, bar, scoreLabel);
            row.setAlignment(Pos.CENTER_LEFT);
            dimensionList.getChildren().add(row);
        });
    }

    @FXML
    private void startInterview() {
        Optional<InterviewHistoryItemDto> active = interviewHistory.stream().filter(this::isActive).findFirst();
        if (active.isPresent()) {
            openInterview(active.get());
            return;
        }
        if (interviewPlans.isEmpty()) {
            createPlan();
            return;
        }
        InterviewPlanDto plan = interviewPlans.stream().filter(InterviewPlanDto::defaultPlan)
                .findFirst().orElse(interviewPlans.getFirst());
        try {
            var session = sessionService.startOrResume(userId(), plan.id());
            contentNavigator.showSubPage(
                    "/fxml/interview-workspace-view.fxml", "模拟面试", session.id());
        } catch (RuntimeException exception) {
            viewManager.showError(exceptionHandler.toUserMessage(exception));
        }
    }

    @FXML
    private void createPlan() {
        contentNavigator.showSubPage("/fxml/plan-editor-view.fxml", "新建面试方案", null);
    }

    @FXML private void showHistory() { contentNavigator.showRoute(Route.HISTORY); }
    @FXML private void showPlans() { contentNavigator.showRoute(Route.PLAN); }
    @FXML private void showResumes() { contentNavigator.showRoute(Route.RESUME); }
    @FXML private void showKnowledge() { contentNavigator.showRoute(Route.KNOWLEDGE); }
    @FXML private void showSettings() { contentNavigator.showRoute(Route.SETTING); }

    @FXML
    private void openReportCenter() {
        interviewHistory.stream().filter(InterviewHistoryItemDto::reportAvailable).findFirst()
                .ifPresentOrElse(
                        item -> contentNavigator.showSubPage(
                                "/fxml/report-detail-view.fxml", "面试报告", item.sessionId()),
                        () -> contentNavigator.showRoute(Route.HISTORY));
    }

    private void openInterview(InterviewHistoryItemDto item) {
        if (isActive(item)) {
            contentNavigator.showSubPage(
                    "/fxml/interview-workspace-view.fxml", "模拟面试", item.sessionId());
        } else if (item.reportAvailable()) {
            contentNavigator.showSubPage(
                    "/fxml/report-detail-view.fxml", "面试报告", item.sessionId());
        } else {
            contentNavigator.showSubPage(
                    "/fxml/interview-history-detail-view.fxml", "面试记录", item.sessionId());
        }
    }

    private boolean isActive(InterviewHistoryItemDto item) {
        return item.status() == InterviewStatus.CREATED
                || item.status() == InterviewStatus.RUNNING
                || item.status() == InterviewStatus.PAUSED;
    }

    private String rowActionText(InterviewHistoryItemDto item) {
        if (isActive(item)) return "继续";
        return item.reportAvailable() ? "查看报告" : "查看记录";
    }

    private String interviewIcon(String title) {
        String value = title == null ? "" : title.toLowerCase();
        if (value.contains("redis") || value.contains("数据库")) return "mdi2d-database-outline";
        if (value.contains("系统") || value.contains("架构")) return "mdi2s-source-branch";
        return "mdi2l-language-java";
    }

    private String statusText(InterviewStatus status) {
        return switch (status) {
            case CREATED -> "待开始";
            case RUNNING -> "进行中";
            case PAUSED -> "已暂停";
            case COMPLETED -> "已完成";
            case FAILED -> "已中止";
        };
    }

    private String greetingText() {
        int hour = LocalTime.now().getHour();
        if (hour < 11) return "早上好";
        if (hour < 18) return "下午好";
        return "晚上好";
    }

    private VBox emptyState(String title, String copy) {
        VBox empty = new VBox(5,
                label(title, "dashboard-empty-title"),
                label(copy, "dashboard-empty-copy"));
        empty.getStyleClass().add("dashboard-empty");
        empty.setAlignment(Pos.CENTER);
        empty.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(empty, Priority.ALWAYS);
        return empty;
    }

    private StackPane iconBubble(String literal) {
        StackPane bubble = new StackPane(icon(literal, 22));
        bubble.getStyleClass().add("dashboard-row-icon");
        return bubble;
    }

    private Region fixedGap(double width) {
        Region gap = new Region();
        gap.setMinWidth(width);
        gap.setPrefWidth(width);
        gap.setMaxWidth(width);
        return gap;
    }

    private FontIcon icon(String literal, int size) {
        FontIcon icon = new FontIcon(literal);
        icon.setIconSize(size);
        return icon;
    }

    private Label label(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        return label;
    }

    private String textOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String timeText(LocalDateTime time) {
        return time == null ? "--" : TIME_FORMAT.format(time);
    }

    private String timeText(LocalDateTime time, String pattern) {
        return time == null ? "--" : DateTimeFormatter.ofPattern(pattern).format(time);
    }

    private int boundedScore(int score) {
        return Math.max(0, Math.min(100, score));
    }

    private long userId() {
        return sessionState.requireCurrentUser().id();
    }
}
