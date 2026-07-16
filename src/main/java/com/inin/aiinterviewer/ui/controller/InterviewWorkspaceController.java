package com.inin.aiinterviewer.ui.controller;

import com.inin.aiinterviewer.application.dto.InterviewMessageDto;
import com.inin.aiinterviewer.application.dto.InterviewSessionDto;
import com.inin.aiinterviewer.application.dto.ReportGenerationTaskStateDto;
import com.inin.aiinterviewer.application.exception.GlobalExceptionHandler;
import com.inin.aiinterviewer.application.service.InterviewAgentService;
import com.inin.aiinterviewer.application.service.ReportGenerationTaskService;
import com.inin.aiinterviewer.application.service.InterviewSessionService;
import com.inin.aiinterviewer.config.properties.LlmProperties;
import com.inin.aiinterviewer.domain.enums.BackgroundTaskStatus;
import com.inin.aiinterviewer.domain.enums.InterviewStage;
import com.inin.aiinterviewer.domain.enums.InterviewStatus;
import com.inin.aiinterviewer.domain.enums.ReportStatus;
import com.inin.aiinterviewer.domain.model.Message;
import com.inin.aiinterviewer.ui.component.InterviewTranscriptView;
import com.inin.aiinterviewer.ui.navigation.ContentNavigator;
import com.inin.aiinterviewer.ui.navigation.ContextAwareController;
import com.inin.aiinterviewer.ui.navigation.JavaFxViewManager;
import com.inin.aiinterviewer.ui.state.UserSessionState;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import reactor.core.publisher.Flux;

@Component
@Scope("prototype")
public class InterviewWorkspaceController implements ContextAwareController<Long> {

    private final InterviewSessionService sessionService;
    private final InterviewAgentService agentService;
    private final ReportGenerationTaskService reportTaskService;
    private final UserSessionState sessionState;
    private final ContentNavigator contentNavigator;
    private final JavaFxViewManager viewManager;
    private final GlobalExceptionHandler exceptionHandler;
    private final LlmProperties llmProperties;

    @FXML private BorderPane workspaceRoot;
    @FXML private Label titleLabel;
    @FXML private Label jobLabel;
    @FXML private Label stageLabel;
    @FXML private Label statusLabel;
    @FXML private Label progressLabel;
    @FXML private Label aiNoticeLabel;
    @FXML private Label citationCountLabel;
    @FXML private VBox citationContainer;
    @FXML private InterviewTranscriptView transcriptView;
    @FXML private TextArea answerArea;
    @FXML private Button pauseButton;
    @FXML private Button submitButton;
    @FXML private Button retryQuestionButton;
    @FXML private Button retryReportButton;
    @FXML private Button reportButton;

    private long sessionId;
    private InterviewSessionDto currentSession;
    private boolean generationInProgress;
    private Timeline reportStatePoller;

    public InterviewWorkspaceController(
            InterviewSessionService sessionService,
            InterviewAgentService agentService,
            ReportGenerationTaskService reportTaskService,
            UserSessionState sessionState,
            ContentNavigator contentNavigator,
            JavaFxViewManager viewManager,
            GlobalExceptionHandler exceptionHandler,
            LlmProperties llmProperties
    ) {
        this.sessionService = sessionService;
        this.agentService = agentService;
        this.reportTaskService = reportTaskService;
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
        viewManager.maximizePrimaryStage();
        workspaceRoot.sceneProperty().addListener((observable, previous, current) -> {
            if (previous != null && current == null) stopReportStatePolling();
        });
        transcriptView.setEmptyMessage("会话已创建，等待 AI 面试官生成第一道问题。");
        transcriptView.setCitationHandler(this::openCitation);
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
            boolean expectsNextQuestion = currentSession != null
                    && transcriptView.getQuestionCount() < currentSession.planSnapshot().questionCount();
            stream(agentService.answer(userId(), sessionId, answer), true,
                    expectsNextQuestion ? "正在分析回答并准备下一题…" : "正在保存最终回答并创建报告任务…",
                    answer, expectsNextQuestion);
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
        stopReportStatePolling();
        contentNavigator.back();
    }

    private void refresh() {
        currentSession = sessionService.require(userId(), sessionId);
        List<InterviewMessageDto> messages = sessionService.messages(userId(), sessionId);
        titleLabel.setText(currentSession.title());
        jobLabel.setText(currentSession.jobTitle());
        stageLabel.setText(stageText(currentSession.stage()));
        statusLabel.setText(statusText(currentSession.status()));
        transcriptView.setMessages(messages);
        renderCitations(messages);
        ReportGenerationTaskStateDto reportTaskState = reportTaskService.state(userId(), sessionId);
        var completionState = reportTaskState.completion();
        long askedQuestions = messages.stream().filter(message -> message.role() == Message.Role.ASSISTANT).count();
        progressLabel.setText("第 " + Math.min(askedQuestions, currentSession.planSnapshot().questionCount())
                + " / " + currentSession.planSnapshot().questionCount() + " 题");

        boolean running = currentSession.status() == InterviewStatus.RUNNING;
        boolean hasQuestion = sessionService.loadLatestState(userId(), sessionId)
                .map(state -> state.currentQuestion() != null && !state.currentQuestion().isBlank())
                .orElse(false);
        boolean awaitingReport = completionState.finalAnswerSaved()
                && currentSession.status() != InterviewStatus.COMPLETED;
        boolean canAnswer = running && !generationInProgress && !awaitingReport
                && (!llmProperties.isConfigured() || hasQuestion);
        answerArea.setDisable(!canAnswer);
        submitButton.setDisable(!canAnswer);
        pauseButton.setDisable(generationInProgress);
        pauseButton.setText(running ? "暂停面试" : "继续面试");
        pauseButton.setVisible(currentSession.status() != InterviewStatus.COMPLETED && !awaitingReport);
        pauseButton.setManaged(pauseButton.isVisible());
        retryQuestionButton.setVisible(llmProperties.isConfigured() && running && !hasQuestion);
        retryQuestionButton.setManaged(retryQuestionButton.isVisible());
        retryQuestionButton.setDisable(generationInProgress || !messages.isEmpty());
        retryReportButton.setVisible(awaitingReport);
        retryReportButton.setManaged(awaitingReport);
        retryReportButton.setDisable(generationInProgress
                || !llmProperties.isConfigured()
                || reportTaskState.active());
        retryReportButton.setText(reportActionText(reportTaskState));
        reportButton.setVisible(currentSession.status() == InterviewStatus.COMPLETED);
        reportButton.setManaged(reportButton.isVisible());
        if (awaitingReport) {
            aiNoticeLabel.setText(completionNotice(reportTaskState));
        } else if (currentSession.status() == InterviewStatus.COMPLETED) {
            aiNoticeLabel.setText("面试已完成，六维评分和 Markdown 报告已保存。可从面试记录进入报告页查看。 ");
        } else {
            aiNoticeLabel.setText(llmProperties.isConfigured()
                    ? "AI 已配置：回答会先保存，再执行分析、受控阶段决策和流式提问。"
                    : "AI 尚未配置。当前可验证会话、回答保存及暂停恢复，不会生成伪造的 AI 提问。");
        }
        updateReportStatePolling(reportTaskState);
    }

    @FXML
    private void openReport() {
        stopReportStatePolling();
        contentNavigator.showSubPage(
                "/fxml/report-detail-view.fxml", "面试报告", sessionId);
    }

    @FXML
    private void generateInitialQuestion() {
        if (!llmProperties.isConfigured() || generationInProgress) return;
        stream(agentService.generateInitialQuestion(userId(), sessionId), false,
                "正在生成第一题…", null, true);
    }

    @FXML
    private void retryReport() {
        if (generationInProgress) return;
        if (!llmProperties.isConfigured()) {
            viewManager.showInfo("AI 尚未配置", "请先在设置页完成 AI 配置，再重新生成报告。");
            return;
        }
        try {
            reportTaskService.enqueue(userId(), sessionId);
            refresh();
            viewManager.showInfo("已加入后台队列", "可以继续使用应用，报告完成后本页会自动更新。");
        } catch (RuntimeException exception) {
            refresh();
            viewManager.showError(exceptionHandler.toUserMessage(exception));
        }
    }

    private void stream(
            Flux<String> output,
            boolean clearInputOnSuccess,
            String progressText,
            String pendingAnswer,
            boolean expectsQuestion
    ) {
        if (generationInProgress) return;
        generationInProgress = true;
        setBusyState(progressText);
        transcriptView.appendPendingUserAnswer(pendingAnswer);
        if (expectsQuestion) {
            transcriptView.beginAssistantStream(transcriptView.getQuestionCount() + 1);
        }
        output.subscribe(
                chunk -> Platform.runLater(() -> {
                    if (expectsQuestion) transcriptView.appendAssistantChunk(chunk);
                }),
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
        retryReportButton.setDisable(true);
        statusLabel.setText("生成中");
    }

    private String completionNotice(ReportGenerationTaskStateDto state) {
        if (!llmProperties.isConfigured()) {
            return "最终回答已保存。AI 当前未配置，请先前往设置页完成配置，再重新生成报告。";
        }
        if (state.taskStatus() == BackgroundTaskStatus.RUNNING) {
            return "最终回答已保存，后台正在生成六维评分与面试报告（第 "
                    + Math.max(1, state.attemptCount()) + " 次尝试）。可离开此页面，任务会继续执行。";
        }
        if (state.taskStatus() == BackgroundTaskStatus.PENDING) {
            if (state.attemptCount() > 0) {
                return "报告生成遇到短暂异常，后台任务已排队自动重试（已尝试 "
                        + state.attemptCount() + " 次）。最终回答不会重复保存。";
            }
            return "最终回答已保存，报告任务正在后台队列中等待执行。可离开此页面，任务不会丢失。";
        }
        if (state.completion().reportStatus() == ReportStatus.FAILED
                || state.taskStatus() == BackgroundTaskStatus.FAILED) {
            String reason = state.completion().failureMessage().isBlank()
                    ? state.taskErrorMessage()
                    : state.completion().failureMessage();
            if (reason.isBlank()) reason = "评分服务返回异常";
            return "最终回答已保存，报告生成失败：" + reason + "。可直接重新生成，不会重复保存回答。";
        }
        if (state.completion().reportStatus() == ReportStatus.GENERATING) {
            return "最终回答已保存，报告生成状态正在恢复。可以重新提交任务，不会重复保存回答。";
        }
        return "最终回答已保存，报告尚未生成。可直接重新生成，不会重复保存回答。";
    }

    private String reportActionText(ReportGenerationTaskStateDto state) {
        if (state.taskStatus() == BackgroundTaskStatus.RUNNING) return "报告生成中";
        if (state.taskStatus() == BackgroundTaskStatus.PENDING) return "等待生成";
        return state.taskId() == null ? "生成报告" : "重新生成报告";
    }

    private void updateReportStatePolling(ReportGenerationTaskStateDto state) {
        if (!state.active()) {
            stopReportStatePolling();
            return;
        }
        if (reportStatePoller != null) return;
        reportStatePoller = new Timeline(new KeyFrame(Duration.seconds(1.2), event -> pollReportState()));
        reportStatePoller.setCycleCount(Timeline.INDEFINITE);
        reportStatePoller.play();
    }

    private void pollReportState() {
        try {
            ReportGenerationTaskStateDto state = reportTaskService.state(userId(), sessionId);
            if (state.completion().reportStatus() == ReportStatus.COMPLETED) {
                stopReportStatePolling();
                refresh();
                return;
            }
            boolean awaitingReport = state.completion().finalAnswerSaved()
                    && currentSession.status() != InterviewStatus.COMPLETED;
            retryReportButton.setVisible(awaitingReport);
            retryReportButton.setManaged(awaitingReport);
            retryReportButton.setDisable(generationInProgress
                    || !llmProperties.isConfigured()
                    || state.active());
            retryReportButton.setText(reportActionText(state));
            if (awaitingReport) aiNoticeLabel.setText(completionNotice(state));
            if (!state.active()) stopReportStatePolling();
        } catch (RuntimeException ignored) {
            // A later tick can recover from a transient database read failure.
        }
    }

    private void stopReportStatePolling() {
        if (reportStatePoller == null) return;
        reportStatePoller.stop();
        reportStatePoller = null;
    }

    private void renderCitations(List<InterviewMessageDto> messages) {
        citationContainer.getChildren().clear();
        int citationCount = messages.stream().mapToInt(message -> message.citations().size()).sum();
        long citedQuestions = messages.stream()
                .filter(message -> message.role() == Message.Role.ASSISTANT && !message.citations().isEmpty())
                .count();
        citationCountLabel.setText(citationCount == 0
                ? "暂无引用"
                : citationCount + " 条引用 · " + citedQuestions + " 个问题");
        if (citationCount == 0) {
            Label empty = new Label("AI 使用知识库片段生成问题后，引用会按题目保存在这里。");
            empty.setWrapText(true);
            empty.getStyleClass().add("citation-empty");
            citationContainer.getChildren().add(empty);
            return;
        }

        int latestSequence = messages.stream()
                .filter(message -> message.role() == Message.Role.ASSISTANT && !message.citations().isEmpty())
                .mapToInt(InterviewMessageDto::sequenceNo)
                .max().orElse(-1);
        int questionNumber = 0;
        for (InterviewMessageDto message : messages) {
            if (message.role() != Message.Role.ASSISTANT) continue;
            questionNumber++;
            for (var citation : message.citations()) {
                VBox card = new VBox(8);
                card.getStyleClass().add("citation-card");
                if (message.sequenceNo() == latestSequence) {
                    card.getStyleClass().add("citation-card-latest");
                }

                Label marker = new Label("第 " + questionNumber + " 题 · 片段 " + (citation.chunkIndex() + 1));
                marker.getStyleClass().add("citation-kicker");
                Button documentLink = new Button(citation.documentName());
                documentLink.setAlignment(Pos.CENTER_LEFT);
                documentLink.setMaxWidth(Double.MAX_VALUE);
                documentLink.setWrapText(true);
                documentLink.setAccessibleText("查看来源文档 " + citation.documentName());
                documentLink.getStyleClass().add("citation-link");
                documentLink.setOnAction(event -> openCitation(citation.documentId()));

                Label excerpt = new Label(citation.excerpt());
                excerpt.setWrapText(true);
                excerpt.getStyleClass().add("citation-excerpt");
                card.getChildren().addAll(marker, documentLink, excerpt);
                citationContainer.getChildren().add(card);
            }
        }
    }

    private void openCitation(long documentId) {
        if (generationInProgress) {
            viewManager.showInfo("正在生成", "请等待本轮 AI 输出完成后再查看来源文档。");
            return;
        }
        try {
            contentNavigator.showSubPage("/fxml/knowledge-detail-view.fxml", "知识文档", documentId);
        } catch (RuntimeException exception) {
            viewManager.showError(exceptionHandler.toUserMessage(exception));
        }
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
