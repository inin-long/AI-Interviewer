package com.inin.aiinterviewer.ui.controller;

import com.inin.aiinterviewer.application.dto.InterviewMessageDto;
import com.inin.aiinterviewer.application.exception.BusinessException;
import com.inin.aiinterviewer.application.exception.ErrorCode;
import com.inin.aiinterviewer.application.service.InterviewResultService;
import com.inin.aiinterviewer.application.service.InterviewSessionService;
import com.inin.aiinterviewer.application.service.SessionBranchService;
import com.inin.aiinterviewer.application.service.TrainingRecommendationService;
import com.inin.aiinterviewer.application.dto.TrainingRecommendationDto;
import com.inin.aiinterviewer.application.exception.GlobalExceptionHandler;
import com.inin.aiinterviewer.domain.model.Message;
import com.inin.aiinterviewer.ui.component.MarkdownView;
import com.inin.aiinterviewer.ui.navigation.ContentNavigator;
import com.inin.aiinterviewer.ui.navigation.ContextAwareController;
import com.inin.aiinterviewer.ui.navigation.InterviewTranscriptContext;
import com.inin.aiinterviewer.ui.navigation.JavaFxViewManager;
import com.inin.aiinterviewer.ui.state.UserSessionState;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.Cursor;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.stage.FileChooser;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Scope("prototype")
public class InterviewReportController implements ContextAwareController<Long> {

    private final InterviewResultService resultService;
    private final InterviewSessionService sessionService;
    private final SessionBranchService branchService;
    private final TrainingRecommendationService trainingService;
    private final UserSessionState sessionState;
    private final ContentNavigator contentNavigator;
    private final JavaFxViewManager viewManager;
    private final GlobalExceptionHandler exceptionHandler;
    private long interviewId;
    private String markdown = "";

    @FXML private Label titleLabel;
    @FXML private Label overallScoreLabel;
    @FXML private Label technicalScoreLabel;
    @FXML private Label problemSolvingScoreLabel;
    @FXML private Label projectScoreLabel;
    @FXML private Label systemDesignScoreLabel;
    @FXML private Label communicationScoreLabel;
    @FXML private Label comprehensiveScoreLabel;
    @FXML private MarkdownView reportView;
    @FXML private VBox evidenceNavigationContainer;
    @FXML private VBox branchNavigationContainer;
    @FXML private VBox trainingRecommendationContainer;
    @FXML private Button trainingPlanButton;
    @FXML private VBox citationNavigationContainer;

    public InterviewReportController(
            InterviewResultService resultService,
            InterviewSessionService sessionService,
            SessionBranchService branchService,
            TrainingRecommendationService trainingService,
            UserSessionState sessionState,
            ContentNavigator contentNavigator,
            JavaFxViewManager viewManager,
            GlobalExceptionHandler exceptionHandler
    ) {
        this.resultService = resultService;
        this.sessionService = sessionService;
        this.branchService = branchService;
        this.trainingService = trainingService;
        this.sessionState = sessionState;
        this.contentNavigator = contentNavigator;
        this.viewManager = viewManager;
        this.exceptionHandler = exceptionHandler;
    }

    @Override
    public void initializeContext(Long interviewId) {
        if (interviewId == null) throw new IllegalArgumentException("Report requires an interview id");
        this.interviewId = interviewId;
        long userId = sessionState.requireCurrentUser().id();
        var report = resultService.find(userId, interviewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERVIEW_NOT_FOUND));
        titleLabel.setText(report.title());
        configureOverallScore(overallScoreLabel, report);
        configureScore(technicalScoreLabel, report, "technical");
        configureScore(problemSolvingScoreLabel, report, "problemSolving");
        configureScore(projectScoreLabel, report, "project");
        configureScore(systemDesignScoreLabel, report, "systemDesign");
        configureScore(communicationScoreLabel, report, "communication");
        configureScore(comprehensiveScoreLabel, report, "comprehensive");
        markdown = report.contentMarkdown() == null ? "" : report.contentMarkdown();
        reportView.setMarkdown(markdown);
        renderEvidenceNavigation(report.evidence());
        renderBranchNavigation(branchService.list(userId, interviewId));
        renderTrainingRecommendation(trainingService.recommend(userId, interviewId));
        renderCitationNavigation(sessionService.messages(userId, interviewId));
    }

    @FXML
    private void back() {
        contentNavigator.back();
    }

    @FXML
    private void openTranscript() {
        contentNavigator.showSubPage(
                "/fxml/interview-history-detail-view.fxml", "面试记录", interviewId);
    }

    private void renderCitationNavigation(List<InterviewMessageDto> messages) {
        citationNavigationContainer.getChildren().clear();
        int questionNumber = 0;
        for (var message : messages) {
            if (message.role() != Message.Role.ASSISTANT) continue;
            questionNumber++;
            if (message.citations().isEmpty()) continue;
            int targetQuestion = questionNumber;
            Button link = new Button("Q" + targetQuestion + " · " + message.citations().size() + " 条依据");
            link.setMaxWidth(Double.MAX_VALUE);
            link.getStyleClass().add("report-question-link");
            link.setAccessibleText("查看第 " + targetQuestion + " 题及其参考依据");
            String sourceNames = message.citations().stream()
                    .map(citation -> citation.documentName())
                    .distinct()
                    .collect(Collectors.joining("、"));
            link.setTooltip(new Tooltip(preview(message.content()) + "\n来源：" + sourceNames));
            link.setOnAction(event -> openTranscriptAt(targetQuestion));
            citationNavigationContainer.getChildren().add(link);
        }
        if (citationNavigationContainer.getChildren().isEmpty()) {
            Label empty = new Label("本次问答没有知识引用");
            empty.setWrapText(true);
            empty.getStyleClass().add("citation-empty");
            citationNavigationContainer.getChildren().add(empty);
        }
    }

    private void renderEvidenceNavigation(
            List<com.inin.aiinterviewer.application.dto.EvaluationEvidenceDto> evidence
    ) {
        evidenceNavigationContainer.getChildren().clear();
        evidence.stream()
                .filter(item -> item.questionNumber() > 0)
                .forEach(item -> {
                    Button link = new Button("Q" + item.questionNumber() + " · "
                            + evidenceSignalText(item.signal()) + " · " + item.competencyCode());
                    link.setMaxWidth(Double.MAX_VALUE);
                    link.getStyleClass().add("report-question-link");
                    link.setAccessibleText("查看第 " + item.questionNumber() + " 题的评分证据");
                    link.setTooltip(new Tooltip(preview(item.reason())));
                    link.setOnAction(event -> openTranscriptAt(item.questionNumber()));
                    Button replay = new Button("重答");
                    replay.getStyleClass().add("secondary-button");
                    replay.setAccessibleText("重新回答第 " + item.questionNumber() + " 题");
                    replay.setOnAction(event -> createBranch(item.questionNumber()));
                    HBox row = new HBox(6, link, replay);
                    HBox.setHgrow(link, Priority.ALWAYS);
                    evidenceNavigationContainer.getChildren().add(row);
                });
        if (evidenceNavigationContainer.getChildren().isEmpty()) {
            Label empty = new Label("暂无可定位的评分证据");
            empty.setWrapText(true);
            empty.getStyleClass().add("citation-empty");
            evidenceNavigationContainer.getChildren().add(empty);
        }
    }

    private void renderBranchNavigation(
            List<com.inin.aiinterviewer.application.dto.SessionBranchDto> branches
    ) {
        branchNavigationContainer.getChildren().clear();
        branches.stream().limit(6).forEach(branch -> {
            Button link = new Button("Q" + branch.sourceQuestionNumber() + " · "
                    + branchStatusText(branch.status()));
            link.setMaxWidth(Double.MAX_VALUE);
            link.getStyleClass().add("report-question-link");
            link.setOnAction(event -> openBranch(branch.id()));
            branchNavigationContainer.getChildren().add(link);
        });
        if (branchNavigationContainer.getChildren().isEmpty()) {
            Label empty = new Label("从评分证据点击“重答”创建分支");
            empty.setWrapText(true);
            empty.getStyleClass().add("citation-empty");
            branchNavigationContainer.getChildren().add(empty);
        }
    }

    private void createBranch(int questionNumber) {
        try {
            long userId = sessionState.requireCurrentUser().id();
            var branch = branchService.create(userId, interviewId, questionNumber, null);
            openBranch(branch.id());
        } catch (RuntimeException exception) {
            viewManager.showError(exceptionHandler.toUserMessage(exception));
        }
    }

    private void openBranch(String branchId) {
        contentNavigator.showSubPage(
                "/fxml/session-branch-view.fxml", "分支复盘", branchId);
    }

    private void renderTrainingRecommendation(TrainingRecommendationDto recommendation) {
        trainingRecommendationContainer.getChildren().clear();
        recommendation.topics().stream().limit(3).forEach(topic -> {
            String questions = topic.sourceQuestionNumbers().isEmpty() ? ""
                    : " · Q" + topic.sourceQuestionNumbers().stream()
                    .map(String::valueOf).collect(Collectors.joining("/"));
            Label label = new Label("• " + topic.title() + questions);
            label.setWrapText(true);
            label.setTooltip(new Tooltip(topic.rationale()));
            label.getStyleClass().add("secondary-text");
            trainingRecommendationContainer.getChildren().add(label);
        });
        if (!recommendation.knowledgeResources().isEmpty()) {
            Label knowledge = new Label("已关联 " + recommendation.knowledgeResources().size() + " 份知识资料");
            knowledge.getStyleClass().add("secondary-text");
            trainingRecommendationContainer.getChildren().add(knowledge);
        }
    }

    @FXML
    private void createTrainingPlan() {
        trainingPlanButton.setDisable(true);
        try {
            long userId = sessionState.requireCurrentUser().id();
            var plan = trainingService.createTrainingPlan(userId, interviewId);
            contentNavigator.showSubPage(
                    "/fxml/plan-editor-view.fxml", "专项训练方案", plan.id());
        } catch (RuntimeException exception) {
            trainingPlanButton.setDisable(false);
            viewManager.showError(exceptionHandler.toUserMessage(exception));
        }
    }

    private String branchStatusText(com.inin.aiinterviewer.domain.enums.SessionBranchStatus status) {
        return switch (status) {
            case DRAFT -> "待重答";
            case PROCESSING -> "比较中";
            case COMPLETED -> "已完成";
            case FAILED -> "可重试";
        };
    }

    private String evidenceSignalText(com.inin.aiinterviewer.domain.enums.EvidenceSignal signal) {
        return switch (signal) {
            case POSITIVE -> "正向";
            case NEGATIVE -> "负向";
            case NEUTRAL -> "中性";
            case INSUFFICIENT -> "证据不足";
        };
    }

    private void openTranscriptAt(int questionNumber) {
        contentNavigator.showSubPage(
                "/fxml/interview-history-detail-view.fxml", "面试记录",
                new InterviewTranscriptContext(interviewId, questionNumber));
    }

    @FXML private void scrollHome() { reportView.scrollToText(titleLabel.getText()); }
    @FXML private void scrollScores() { reportView.scrollToText("综合得分"); }
    @FXML private void scrollSummary() { reportView.scrollToHeading("综合评价"); }
    @FXML private void scrollTranscriptSummary() { reportView.scrollToHeading("问答摘要"); }
    @FXML private void scrollSources() { reportView.scrollToHeading("参考依据"); }

    @FXML
    private void copyMarkdown() {
        ClipboardContent content = new ClipboardContent();
        content.putString(markdown);
        Clipboard.getSystemClipboard().setContent(content);
        viewManager.showInfo("复制完成", "Markdown 报告已复制到剪贴板。");
    }

    @FXML
    private void exportMarkdown() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("导出 Markdown 报告");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Markdown 文件", "*.md"));
        chooser.setInitialFileName(safeFileName(titleLabel.getText()) + ".md");
        File target = chooser.showSaveDialog(reportView.getScene().getWindow());
        if (target == null) return;
        try {
            Files.writeString(target.toPath(), markdown, StandardCharsets.UTF_8);
            viewManager.showInfo("导出完成", "报告已保存到：\n" + target.toPath().toAbsolutePath());
        } catch (Exception exception) {
            viewManager.showError(exceptionHandler.toUserMessage(exception));
        }
    }

    private String safeFileName(String value) {
        String safe = value == null ? "面试报告" : value.replaceAll("[\\\\/:*?\"<>|]", "_").strip();
        return safe.isBlank() ? "面试报告" : safe;
    }

    private String preview(String value) {
        String text = value == null ? "" : value.replaceAll("\\s+", " ").strip();
        return text.length() > 90 ? text.substring(0, 90) + "…" : text;
    }

    private void configureOverallScore(
            Label label,
            com.inin.aiinterviewer.application.dto.InterviewReportDto report
    ) {
        if (report.scoreEvidence().isEmpty()) {
            label.setText(report.overallScore() + " / 100");
            return;
        }
        label.setText(report.overallScored()
                ? report.overallScore() + " / 100 · " + confidenceText(report.overallConfidence())
                : "证据不足");
        configureScoreNavigation(label, report, "overall");
    }

    private void configureScore(
            Label label,
            com.inin.aiinterviewer.application.dto.InterviewReportDto report,
            String key
    ) {
        var trace = report.scoreEvidence().get(key);
        if (trace == null) {
            label.setText(report.dimensions().getOrDefault(key, 0) + " 分");
            return;
        }
        label.setText(trace.scored()
                ? report.dimensions().getOrDefault(key, 0) + " 分 · " + confidenceText(trace.confidence())
                : "证据不足");
        configureScoreNavigation(label, report, key);
    }

    private void configureScoreNavigation(
            Label label,
            com.inin.aiinterviewer.application.dto.InterviewReportDto report,
            String key
    ) {
        var trace = report.scoreEvidence().get(key);
        if (trace == null || trace.evidenceIds().isEmpty()) return;
        int question = trace.evidenceIds().stream()
                .flatMap(id -> report.evidence().stream().filter(item -> item.id().equals(id)))
                .mapToInt(com.inin.aiinterviewer.application.dto.EvaluationEvidenceDto::questionNumber)
                .filter(value -> value > 0).findFirst().orElse(0);
        if (question == 0) return;
        label.setCursor(Cursor.HAND);
        label.getStyleClass().add("score-evidence-link");
        label.setTooltip(new Tooltip("点击查看该评分的首条证据（Q" + question + "）"));
        label.setOnMouseClicked(event -> openTranscriptAt(question));
    }

    private String confidenceText(double value) {
        if (value >= 0.7) return "高置信度";
        if (value >= 0.45) return "中置信度";
        return "低置信度";
    }
}
