package com.inin.aiinterviewer.ui.controller;

import com.inin.aiinterviewer.application.exception.BusinessException;
import com.inin.aiinterviewer.application.exception.ErrorCode;
import com.inin.aiinterviewer.application.service.InterviewResultService;
import com.inin.aiinterviewer.application.exception.GlobalExceptionHandler;
import com.inin.aiinterviewer.ui.component.MarkdownView;
import com.inin.aiinterviewer.ui.navigation.ContentNavigator;
import com.inin.aiinterviewer.ui.navigation.ContextAwareController;
import com.inin.aiinterviewer.ui.navigation.JavaFxViewManager;
import com.inin.aiinterviewer.ui.state.UserSessionState;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.stage.FileChooser;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

@Component
@Scope("prototype")
public class InterviewReportController implements ContextAwareController<Long> {

    private final InterviewResultService resultService;
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

    public InterviewReportController(
            InterviewResultService resultService,
            UserSessionState sessionState,
            ContentNavigator contentNavigator,
            JavaFxViewManager viewManager,
            GlobalExceptionHandler exceptionHandler
    ) {
        this.resultService = resultService;
        this.sessionState = sessionState;
        this.contentNavigator = contentNavigator;
        this.viewManager = viewManager;
        this.exceptionHandler = exceptionHandler;
    }

    @Override
    public void initializeContext(Long interviewId) {
        if (interviewId == null) throw new IllegalArgumentException("Report requires an interview id");
        this.interviewId = interviewId;
        var report = resultService.find(sessionState.requireCurrentUser().id(), interviewId)
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

    private String score(com.inin.aiinterviewer.application.dto.InterviewReportDto report, String key) {
        return report.dimensions().getOrDefault(key, 0) + " 分";
    }
}
