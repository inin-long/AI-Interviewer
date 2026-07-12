package com.inin.aiinterviewer.ui.controller;

import com.inin.aiinterviewer.application.exception.BusinessException;
import com.inin.aiinterviewer.application.exception.ErrorCode;
import com.inin.aiinterviewer.application.service.InterviewResultService;
import com.inin.aiinterviewer.ui.navigation.ContentNavigator;
import com.inin.aiinterviewer.ui.navigation.ContextAwareController;
import com.inin.aiinterviewer.ui.state.UserSessionState;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class InterviewReportController implements ContextAwareController<Long> {

    private final InterviewResultService resultService;
    private final UserSessionState sessionState;
    private final ContentNavigator contentNavigator;

    @FXML private Label titleLabel;
    @FXML private Label overallScoreLabel;
    @FXML private Label technicalScoreLabel;
    @FXML private Label problemSolvingScoreLabel;
    @FXML private Label projectScoreLabel;
    @FXML private Label systemDesignScoreLabel;
    @FXML private Label communicationScoreLabel;
    @FXML private Label comprehensiveScoreLabel;
    @FXML private TextArea reportArea;

    public InterviewReportController(
            InterviewResultService resultService,
            UserSessionState sessionState,
            ContentNavigator contentNavigator
    ) {
        this.resultService = resultService;
        this.sessionState = sessionState;
        this.contentNavigator = contentNavigator;
    }

    @Override
    public void initializeContext(Long interviewId) {
        if (interviewId == null) throw new IllegalArgumentException("Report requires an interview id");
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
        reportArea.setText(report.contentMarkdown());
    }

    @FXML
    private void back() {
        contentNavigator.back();
    }

    private String score(com.inin.aiinterviewer.application.dto.InterviewReportDto report, String key) {
        return report.dimensions().getOrDefault(key, 0) + " 分";
    }
}
