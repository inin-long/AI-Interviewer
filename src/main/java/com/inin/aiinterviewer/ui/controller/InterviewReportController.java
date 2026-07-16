package com.inin.aiinterviewer.ui.controller;

import com.inin.aiinterviewer.application.dto.InterviewMessageDto;
import com.inin.aiinterviewer.application.exception.BusinessException;
import com.inin.aiinterviewer.application.exception.ErrorCode;
import com.inin.aiinterviewer.application.service.InterviewResultService;
import com.inin.aiinterviewer.application.service.InterviewSessionService;
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
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.stage.FileChooser;
import javafx.scene.layout.VBox;
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
    @FXML private VBox citationNavigationContainer;

    public InterviewReportController(
            InterviewResultService resultService,
            InterviewSessionService sessionService,
            UserSessionState sessionState,
            ContentNavigator contentNavigator,
            JavaFxViewManager viewManager,
            GlobalExceptionHandler exceptionHandler
    ) {
        this.resultService = resultService;
        this.sessionService = sessionService;
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
        overallScoreLabel.setText(report.overallScore() + " / 100");
        technicalScoreLabel.setText(score(report, "technical"));
        problemSolvingScoreLabel.setText(score(report, "problemSolving"));
        projectScoreLabel.setText(score(report, "project"));
        systemDesignScoreLabel.setText(score(report, "systemDesign"));
        communicationScoreLabel.setText(score(report, "communication"));
        comprehensiveScoreLabel.setText(score(report, "comprehensive"));
        markdown = report.contentMarkdown() == null ? "" : report.contentMarkdown();
        reportView.setMarkdown(markdown);
        renderEvidenceNavigation(report.evidence());
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
                    evidenceNavigationContainer.getChildren().add(link);
                });
        if (evidenceNavigationContainer.getChildren().isEmpty()) {
            Label empty = new Label("暂无可定位的评分证据");
            empty.setWrapText(true);
            empty.getStyleClass().add("citation-empty");
            evidenceNavigationContainer.getChildren().add(empty);
        }
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

    private String score(com.inin.aiinterviewer.application.dto.InterviewReportDto report, String key) {
        return report.dimensions().getOrDefault(key, 0) + " 分";
    }
}
