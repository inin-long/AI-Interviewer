package com.inin.aiinterviewer.ui.controller;

import com.inin.aiinterviewer.application.dto.InterviewHistoryDetailDto;
import com.inin.aiinterviewer.application.service.InterviewHistoryService;
import com.inin.aiinterviewer.domain.enums.InterviewStage;
import com.inin.aiinterviewer.domain.enums.InterviewStatus;
import com.inin.aiinterviewer.ui.component.InterviewTranscriptView;
import com.inin.aiinterviewer.ui.navigation.ContentNavigator;
import com.inin.aiinterviewer.ui.navigation.ContextAwareController;
import com.inin.aiinterviewer.ui.navigation.InterviewTranscriptContext;
import com.inin.aiinterviewer.ui.state.UserSessionState;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

@Component
@Scope("prototype")
public class InterviewHistoryDetailController implements ContextAwareController<Object> {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final InterviewHistoryService historyService;
    private final UserSessionState sessionState;
    private final ContentNavigator contentNavigator;

    @FXML private Label titleLabel;
    @FXML private Label jobLabel;
    @FXML private Label statusLabel;
    @FXML private Label stageLabel;
    @FXML private Label startedLabel;
    @FXML private Label completedLabel;
    @FXML private Label messageCountLabel;
    @FXML private InterviewTranscriptView transcriptView;
    @FXML private Button continueButton;
    @FXML private Button reportButton;

    private long sessionId;

    public InterviewHistoryDetailController(
            InterviewHistoryService historyService,
            UserSessionState sessionState,
            ContentNavigator contentNavigator
    ) {
        this.historyService = historyService;
        this.sessionState = sessionState;
        this.contentNavigator = contentNavigator;
    }

    @Override
    public void initializeContext(Object context) {
        if (context == null) throw new IllegalArgumentException("Interview history detail requires a session id");
        int targetQuestion = 0;
        if (context instanceof InterviewTranscriptContext target) {
            sessionId = target.sessionId();
            targetQuestion = target.questionNumber();
        } else if (context instanceof Number number) {
            sessionId = number.longValue();
        } else {
            throw new IllegalArgumentException("Unsupported interview history context");
        }
        InterviewHistoryDetailDto detail = historyService.detail(userId(), sessionId);
        var session = detail.session();
        titleLabel.setText(session.title());
        jobLabel.setText(session.jobTitle() == null ? "未填写岗位" : session.jobTitle());
        statusLabel.setText(statusText(session.status()));
        stageLabel.setText(stageText(session.stage()));
        startedLabel.setText(session.startedTime() == null ? "—" : TIME_FORMAT.format(session.startedTime()));
        completedLabel.setText(session.completedTime() == null ? "—" : TIME_FORMAT.format(session.completedTime()));
        messageCountLabel.setText(detail.messages().size() + " 条");
        transcriptView.setEmptyMessage("本次面试尚未产生问答记录。");
        transcriptView.setMessages(detail.messages());
        if (targetQuestion > 0) transcriptView.scrollToQuestion(targetQuestion);
        boolean active = session.status() == InterviewStatus.RUNNING
                || session.status() == InterviewStatus.PAUSED || session.status() == InterviewStatus.CREATED;
        continueButton.setVisible(active);
        continueButton.setManaged(active);
        reportButton.setVisible(detail.report().isPresent());
        reportButton.setManaged(detail.report().isPresent());
    }

    @FXML private void back() { contentNavigator.back(); }

    @FXML
    private void continueInterview() {
        contentNavigator.showSubPage("/fxml/interview-workspace-view.fxml", "模拟面试", sessionId);
    }

    @FXML
    private void openReport() {
        contentNavigator.showSubPage("/fxml/report-detail-view.fxml", "面试报告", sessionId);
    }

    private long userId() { return sessionState.requireCurrentUser().id(); }

    private String statusText(InterviewStatus status) {
        return switch (status) {
            case CREATED -> "待开始";
            case RUNNING -> "进行中";
            case PAUSED -> "已暂停";
            case COMPLETED -> "已完成";
            case FAILED -> "异常中止";
        };
    }

    private String stageText(InterviewStage stage) {
        return switch (stage) {
            case INTRODUCTION -> "开场介绍";
            case RESUME_REVIEW -> "简历回顾";
            case PROJECT_EXPERIENCE -> "项目经历";
            case TECHNICAL_DEEP_DIVE -> "技术深挖";
            case SYSTEM_DESIGN -> "系统设计";
            case CODING -> "编码能力";
            case BEHAVIORAL -> "行为面试";
            case SUMMARY -> "总结";
            case COMPLETED -> "已完成";
        };
    }
}
