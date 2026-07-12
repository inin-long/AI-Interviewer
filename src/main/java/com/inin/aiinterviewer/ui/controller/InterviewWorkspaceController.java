package com.inin.aiinterviewer.ui.controller;

import com.inin.aiinterviewer.application.dto.InterviewMessageDto;
import com.inin.aiinterviewer.application.dto.InterviewSessionDto;
import com.inin.aiinterviewer.application.exception.GlobalExceptionHandler;
import com.inin.aiinterviewer.application.service.InterviewSessionService;
import com.inin.aiinterviewer.config.properties.LlmProperties;
import com.inin.aiinterviewer.domain.enums.InterviewStage;
import com.inin.aiinterviewer.domain.enums.InterviewStatus;
import com.inin.aiinterviewer.domain.model.Message;
import com.inin.aiinterviewer.ui.navigation.ContentNavigator;
import com.inin.aiinterviewer.ui.navigation.ContextAwareController;
import com.inin.aiinterviewer.ui.navigation.JavaFxViewManager;
import com.inin.aiinterviewer.ui.state.UserSessionState;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@Scope("prototype")
public class InterviewWorkspaceController implements ContextAwareController<Long> {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final InterviewSessionService sessionService;
    private final UserSessionState sessionState;
    private final ContentNavigator contentNavigator;
    private final JavaFxViewManager viewManager;
    private final GlobalExceptionHandler exceptionHandler;
    private final LlmProperties llmProperties;

    @FXML private Label titleLabel;
    @FXML private Label jobLabel;
    @FXML private Label stageLabel;
    @FXML private Label statusLabel;
    @FXML private Label aiNoticeLabel;
    @FXML private TextArea transcriptArea;
    @FXML private TextArea answerArea;
    @FXML private Button pauseButton;
    @FXML private Button submitButton;

    private long sessionId;
    private InterviewSessionDto currentSession;

    public InterviewWorkspaceController(
            InterviewSessionService sessionService,
            UserSessionState sessionState,
            ContentNavigator contentNavigator,
            JavaFxViewManager viewManager,
            GlobalExceptionHandler exceptionHandler,
            LlmProperties llmProperties
    ) {
        this.sessionService = sessionService;
        this.sessionState = sessionState;
        this.contentNavigator = contentNavigator;
        this.viewManager = viewManager;
        this.exceptionHandler = exceptionHandler;
        this.llmProperties = llmProperties;
    }

    @Override
    public void initializeContext(Long context) {
        if (context == null) {
            throw new IllegalArgumentException("Interview workspace requires a session id");
        }
        sessionId = context;
        refresh();
    }

    @FXML
    private void submitAnswer() {
        try {
            sessionService.appendUserAnswer(userId(), sessionId, answerArea.getText());
            answerArea.clear();
            refresh();
        } catch (RuntimeException exception) {
            viewManager.showError(exceptionHandler.toUserMessage(exception));
        }
    }

    @FXML
    private void togglePause() {
        try {
            if (currentSession.status() == InterviewStatus.RUNNING) {
                sessionService.pause(userId(), sessionId);
            } else {
                sessionService.resume(userId(), sessionId);
            }
            refresh();
        } catch (RuntimeException exception) {
            viewManager.showError(exceptionHandler.toUserMessage(exception));
        }
    }

    @FXML
    private void back() {
        if (currentSession != null && currentSession.status() == InterviewStatus.RUNNING) {
            Alert confirmation = new Alert(
                    Alert.AlertType.CONFIRMATION,
                    "离开面试页面前将自动暂停，并保存当前 Checkpoint。",
                    ButtonType.CANCEL, ButtonType.OK);
            confirmation.setHeaderText("暂停并离开面试？");
            if (confirmation.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
                return;
            }
            try {
                sessionService.pause(userId(), sessionId);
            } catch (RuntimeException exception) {
                viewManager.showError(exceptionHandler.toUserMessage(exception));
                return;
            }
        }
        contentNavigator.back();
    }

    private void refresh() {
        currentSession = sessionService.require(userId(), sessionId);
        List<InterviewMessageDto> messages = sessionService.messages(userId(), sessionId);
        titleLabel.setText(currentSession.title());
        jobLabel.setText(currentSession.jobTitle());
        stageLabel.setText(stageText(currentSession.stage()));
        statusLabel.setText(statusText(currentSession.status()));
        transcriptArea.setText(transcript(messages));

        boolean running = currentSession.status() == InterviewStatus.RUNNING;
        answerArea.setDisable(!running);
        submitButton.setDisable(!running);
        pauseButton.setText(running ? "暂停面试" : "继续面试");
        aiNoticeLabel.setText(llmProperties.isConfigured()
                ? "AI 已配置。下一迭代将把提问、回答分析和流式输出接入当前会话。"
                : "AI 尚未配置。当前可验证会话、回答保存及暂停恢复，不会生成伪造的 AI 提问。");
    }

    private String transcript(List<InterviewMessageDto> messages) {
        if (messages.isEmpty()) {
            return "会话已创建，初始状态和 Checkpoint 已保存。\n等待 Agent 提问流程接入。";
        }
        StringBuilder transcript = new StringBuilder();
        for (InterviewMessageDto message : messages) {
            String role = message.role() == Message.Role.USER ? "你" : "AI 面试官";
            String time = message.createTime() == null ? "" : "  " + TIME_FORMAT.format(message.createTime());
            transcript.append(role).append(time).append("\n")
                    .append(message.content()).append("\n\n");
        }
        return transcript.toString().stripTrailing();
    }

    private long userId() {
        return sessionState.requireCurrentUser().id();
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
