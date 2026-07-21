package com.inin.aiinterviewer.ui.controller;

import com.inin.aiinterviewer.application.dto.AssessmentResultDto;
import com.inin.aiinterviewer.application.exception.GlobalExceptionHandler;
import com.inin.aiinterviewer.application.service.CareerAssessmentService;
import com.inin.aiinterviewer.ui.component.MarkdownView;
import com.inin.aiinterviewer.ui.navigation.ContextAwareController;
import com.inin.aiinterviewer.ui.navigation.ContentNavigator;
import com.inin.aiinterviewer.ui.navigation.JavaFxViewManager;
import com.inin.aiinterviewer.ui.state.UserSessionState;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class CareerReportController implements ContextAwareController<Long> {

    private final CareerAssessmentService assessmentService;
    private final UserSessionState sessionState;
    private final ContentNavigator contentNavigator;
    private final JavaFxViewManager viewManager;
    private final GlobalExceptionHandler exceptionHandler;

    @FXML private Label codeLabel;
    @FXML private MarkdownView reportView;

    public CareerReportController(
            CareerAssessmentService assessmentService,
            UserSessionState sessionState,
            ContentNavigator contentNavigator,
            JavaFxViewManager viewManager,
            GlobalExceptionHandler exceptionHandler
    ) {
        this.assessmentService = assessmentService;
        this.sessionState = sessionState;
        this.contentNavigator = contentNavigator;
        this.viewManager = viewManager;
        this.exceptionHandler = exceptionHandler;
    }

    @Override
    public void initializeContext(Long context) {
        try {
            if (context == null) {
                viewManager.showError("测评报告需要结果 id");
                return;
            }
            AssessmentResultDto result = assessmentService.getResult(
                    sessionState.requireCurrentUser().id(), context);
            codeLabel.setText("类型代码：" + result.resultCode());
            reportView.setMarkdown(result.reportMarkdown() == null ? "" : result.reportMarkdown());
        } catch (RuntimeException exception) {
            viewManager.showError(exceptionHandler.toUserMessage(exception));
        }
    }

    @FXML
    private void back() {
        contentNavigator.back();
    }
}
