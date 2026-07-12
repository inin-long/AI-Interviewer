package com.inin.aiinterviewer.ui.controller;

import com.inin.aiinterviewer.application.dto.InterviewMessageDto;
import com.inin.aiinterviewer.application.dto.InterviewSessionDto;
import com.inin.aiinterviewer.application.exception.GlobalExceptionHandler;
import com.inin.aiinterviewer.application.service.InterviewAgentService;
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
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;
import reactor.core.publisher.Flux;

@Component
@Scope("prototype")
public class InterviewWorkspaceController implements ContextAwareController<Long> {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final InterviewSessionService sessionService;
    private final InterviewAgentService agentService;
    private final UserSessionState sessionState;
    private final ContentNavigator contentNavigator;
    private final JavaFxViewManager viewManager;
    private final GlobalExceptionHandler exceptionHandler;
    private final LlmProperties llmProperties;

    @FXML private Label titleLabel;
    @FXML private Label jobLabel;
    @FXML private Label stageLabel;
    @FXML private Label statusLabel;
    @FXML private Label progressLabel;
    @FXML private Label aiNoticeLabel;
    @FXML private TextArea transcriptArea;
    @FXML private TextArea answerArea;
    @FXML private Button pauseButton;
    @FXML private Button submitButton;
    @FXML private Button retryQuestionButton;
    @FXML private Button reportButton;

    private long sessionId;
    private InterviewSessionDto currentSession;
    private boolean generationInProgress;

    public InterviewWorkspaceController(
            InterviewSessionService sessionService,
            InterviewAgentService agentService,
            UserSessionState sessionState,
            ContentNavigator contentNavigator,
            JavaFxViewManager viewManager,
            GlobalExceptionHandler exceptionHandler,
            LlmProperties llmProperties
    ) {
        this.sessionService = sessionService;
        this.agentService = agentService;
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
        if (llmProperties.isConfigured()
                && currentSession.status() == InterviewStatus.RUNNING
                && sessionService.messages(userId(), sessionId).isEmpty()) {
            generateInitialQuestion();
        }
    }

    @FXML
    private void submitAnswer() {
        String answer = answerArea.getText();
        if (llmProperties.isConfigured()) {
            stream(agentService.answer(userId(), sessionId, answer), true, "正在分析回答并准备下一题…");
            return;
        }
        try {
            sessionService.appendUserAnswer(userId(), sessionId, answer);
            answerArea.clear();
            refresh();
        } catch (RuntimeException exception) {
            viewManager.showError(exceptionHandler.toUserMessage(exception));
        }
    }

    @FXML
    private void togglePause() {
        if (generationInProgress) {
            viewManager.showInfo("正在生成", "请等待本轮 AI 输出完成后再暂停。");
            return;
        }
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
        if (generationInProgress) {
            viewManager.showInfo("正在生成", "请等待本轮 AI 输出完成后再离开面试页面。");
            return;
        }
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
        long askedQuestions = messages.stream().filter(message -> message.role() == Message.Role.ASSISTANT).count();
        progressLabel.setText("第 " + Math.min(askedQuestions, currentSession.planSnapshot().questionCount())
                + " / " + currentSession.planSnapshot().questionCount() + " 题");

        boolean running = currentSession.status() == InterviewStatus.RUNNING;
        boolean hasQuestion = sessionService.loadLatestState(userId(), sessionId)
                .map(state -> state.currentQuestion() != null && !state.currentQuestion().isBlank())
                .orElse(false);
        boolean canAnswer = running && !generationInProgress
                && (!llmProperties.isConfigured() || hasQuestion);
        answerArea.setDisable(!canAnswer);
        submitButton.setDisable(!canAnswer);
        pauseButton.setDisable(generationInProgress);
        pauseButton.setText(running ? "暂停面试" : "继续面试");
        pauseButton.setVisible(currentSession.status() != InterviewStatus.COMPLETED);
        pauseButton.setManaged(pauseButton.isVisible());
        retryQuestionButton.setVisible(llmProperties.isConfigured() && running && !hasQuestion);
        retryQuestionButton.setManaged(retryQuestionButton.isVisible());
        retryQuestionButton.setDisable(generationInProgress || !messages.isEmpty());
        reportButton.setVisible(currentSession.status() == InterviewStatus.COMPLETED);
        reportButton.setManaged(reportButton.isVisible());
        if (currentSession.status() == InterviewStatus.COMPLETED) {
            aiNoticeLabel.setText("面试已完成，六维评分和 Markdown 报告已保存。可从面试记录进入报告页查看。 ");
        } else {
            aiNoticeLabel.setText(llmProperties.isConfigured()
                    ? "AI 已配置：回答会先保存，再执行分析、受控阶段决策和流式提问。"
                    : "AI 尚未配置。当前可验证会话、回答保存及暂停恢复，不会生成伪造的 AI 提问。");
        }
    }

    @FXML
    private void openReport() {
        contentNavigator.showSubPage(
                "/fxml/report-detail-view.fxml", "面试报告", sessionId);
    }

    @FXML
    private void generateInitialQuestion() {
        if (!llmProperties.isConfigured() || generationInProgress) return;
        stream(agentService.generateInitialQuestion(userId(), sessionId), false, "正在生成第一题…");
    }

    private void stream(Flux<String> output, boolean clearInputOnSuccess, String progressText) {
        if (generationInProgress) return;
        generationInProgress = true;
        setBusyState(progressText);
        String existing = transcriptArea.getText();
        transcriptArea.setText(existing.isBlank() ? "AI 面试官\n" : existing + "\n\nAI 面试官\n");
        output.subscribe(
                chunk -> Platform.runLater(() -> transcriptArea.appendText(chunk)),
                throwable -> Platform.runLater(() -> {
                    generationInProgress = false;
                    refresh();
                    viewManager.showError(exceptionHandler.toUserMessage(throwable));
                }),
                () -> Platform.runLater(() -> {
                    generationInProgress = false;
                    if (clearInputOnSuccess) answerArea.clear();
                    refresh();
                }));
    }

    private void setBusyState(String progressText) {
        aiNoticeLabel.setText(progressText);
        answerArea.setDisable(true);
        submitButton.setDisable(true);
        pauseButton.setDisable(true);
        retryQuestionButton.setDisable(true);
        statusLabel.setText("生成中");
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
