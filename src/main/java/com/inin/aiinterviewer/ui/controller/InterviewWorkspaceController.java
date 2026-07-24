package com.inin.aiinterviewer.ui.controller;

import com.inin.aiinterviewer.application.dto.InterviewMessageDto;
import com.inin.aiinterviewer.application.dto.InterviewSessionDto;
import com.inin.aiinterviewer.application.dto.ReportGenerationTaskStateDto;
import com.inin.aiinterviewer.application.dto.CoachingFeedbackDto;
import com.inin.aiinterviewer.application.exception.GlobalExceptionHandler;
import com.inin.aiinterviewer.application.service.InterviewAgentService;
import com.inin.aiinterviewer.application.service.ReportGenerationTaskService;
import com.inin.aiinterviewer.application.service.InterviewSessionService;
import com.inin.aiinterviewer.application.service.CoachingFeedbackService;
import com.inin.aiinterviewer.application.service.SessionBranchService;
import com.inin.aiinterviewer.config.properties.LlmProperties;
import com.inin.aiinterviewer.domain.enums.BackgroundTaskStatus;
import com.inin.aiinterviewer.domain.enums.InterviewStage;
import com.inin.aiinterviewer.domain.enums.InterviewStatus;
import com.inin.aiinterviewer.domain.enums.InterviewMode;
import com.inin.aiinterviewer.domain.enums.InterviewerPersona;
import com.inin.aiinterviewer.domain.enums.PressureLevel;
import com.inin.aiinterviewer.domain.enums.ReportStatus;
import com.inin.aiinterviewer.domain.enums.ScenarioStatus;
import com.inin.aiinterviewer.domain.model.InterviewPlanSettings;
import com.inin.aiinterviewer.domain.model.Message;
import com.inin.aiinterviewer.ui.component.AppDialogs;
import com.inin.aiinterviewer.ui.component.InterviewTranscriptView;
import com.inin.aiinterviewer.ui.navigation.ContentNavigator;
import com.inin.aiinterviewer.ui.navigation.ContextAwareController;
import com.inin.aiinterviewer.ui.navigation.JavaFxViewManager;
import com.inin.aiinterviewer.ui.navigation.NavigationGuard;
import com.inin.aiinterviewer.ui.navigation.Route;
import com.inin.aiinterviewer.ui.state.UserSessionState;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import reactor.core.publisher.Flux;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Scope("prototype")
public class InterviewWorkspaceController implements ContextAwareController<Long>, NavigationGuard {

    private static final Logger log = LoggerFactory.getLogger(InterviewWorkspaceController.class);
    private static final Map<Long, String> SESSION_DRAFTS = new ConcurrentHashMap<>();

    private final InterviewSessionService sessionService;
    private final InterviewAgentService agentService;
    private final ReportGenerationTaskService reportTaskService;
    private final CoachingFeedbackService coachingFeedbackService;
    private final SessionBranchService branchService;
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
    @FXML private Label modeLabel;
    @FXML private Label personaLabel;
    @FXML private Label pressureLabel;
    @FXML private Label scenarioLabel;
    @FXML private Label progressLabel;
    @FXML private Label remainingTimeLabel;
    @FXML private Label answerTimeLimitLabel;
    @FXML private Label aiNoticeLabel;
    @FXML private Label citationCountLabel;
    @FXML private Label infoJobLabel;
    @FXML private Label planNameLabel;
    @FXML private Label resumeNameLabel;
    @FXML private Label durationLabel;
    @FXML private Label stageRailLabel;
    @FXML private Label stageDescriptionLabel;
    @FXML private Label scoreStateLabel;
    @FXML private Label agentConnectionLabel;
    @FXML private Label agentGraphStatusLabel;
    @FXML private Label agentCheckpointLabel;
    @FXML private Label agentStreamingLabel;
    @FXML private Label agentRetryLabel;
    @FXML private Label currentCitationLabel;
    @FXML private VBox citationContainer;
    @FXML private VBox coachingPanel;
    @FXML private VBox scenarioPanel;
    @FXML private VBox scorePanel;
    @FXML private VBox agentPanel;
    @FXML private VBox tipPanel;
    @FXML private Label coachCoveredLabel;
    @FXML private Label coachMissingLabel;
    @FXML private Label coachLogicLabel;
    @FXML private Label coachStructureLabel;
    @FXML private Label coachHintLabel;
    @FXML private Label scenarioObjectiveLabel;
    @FXML private Label scenarioFactsLabel;
    @FXML private Label scenarioEventLabel;
    @FXML private Label scenarioDecisionLabel;
    @FXML private InterviewTranscriptView transcriptView;
    @FXML private TextArea answerArea;
    @FXML private ToggleButton textModeToggle;
    @FXML private ToggleButton codeModeToggle;
    @FXML private ProgressBar overallProgressBar;
    @FXML private Label overallProgressLabel;
    @FXML private Button pauseButton;
    @FXML private Button endButton;
    @FXML private Button submitButton;
    @FXML private Button retryQuestionButton;
    @FXML private Button retryReportButton;
    @FXML private Button reportButton;
    @FXML private Button coachHintButton;
    @FXML private Button coachReanswerButton;

    private long sessionId;
    private InterviewSessionDto currentSession;
    private boolean generationInProgress;
    private Timeline reportStatePoller;
    private Timeline workspaceClock;
    private Timeline answerTimer;
    private int answerRemainingSeconds;
    private int answerTotalSeconds;
    private int lastTimerQuestionCount = -1;
    private CoachingFeedbackDto currentCoachingFeedback = CoachingFeedbackDto.unavailable();

    public InterviewWorkspaceController(
            InterviewSessionService sessionService,
            InterviewAgentService agentService,
            ReportGenerationTaskService reportTaskService,
            CoachingFeedbackService coachingFeedbackService,
            SessionBranchService branchService,
            UserSessionState sessionState,
            ContentNavigator contentNavigator,
            JavaFxViewManager viewManager,
            GlobalExceptionHandler exceptionHandler,
            LlmProperties llmProperties
    ) {
        this.sessionService = sessionService;
        this.agentService = agentService;
        this.reportTaskService = reportTaskService;
        this.coachingFeedbackService = coachingFeedbackService;
        this.branchService = branchService;
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
        answerArea.setText(SESSION_DRAFTS.getOrDefault(sessionId, ""));
        viewManager.maximizePrimaryStage();
        workspaceRoot.sceneProperty().addListener((observable, previous, current) -> {
            if (previous != null && current == null) stopWorkspaceTimers();
            if (current != null) updateShellContext();
        });
        transcriptView.setEmptyMessage("会话已创建，等待 AI 面试官生成第一道问题。");
        transcriptView.setCitationHandler(this::openCitation);
        answerArea.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER && event.isControlDown()) {
                submitAnswer();
                event.consume();
            }
        });
        refresh();
        startWorkspaceClock();
        if (llmProperties.isConfigured()
                && currentSession.status() == InterviewStatus.RUNNING
                && sessionService.messages(userId(), sessionId).isEmpty()) {
            generateInitialQuestion();
        }
    }

    @FXML
    private void submitAnswer() {
        String answer = answerArea.getText();
        if (answer == null || answer.isBlank()) {
            viewManager.showInfo("请输入回答", "请先填写你的回答再提交。");
            return;
        }
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
            SESSION_DRAFTS.remove(sessionId);
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
        contentNavigator.back();
    }

    @Override
    public boolean allowNavigationAway() {
        if (generationInProgress) {
            viewManager.showInfo("正在生成", "请等待本轮 AI 输出完成后再离开面试页面。");
            return false;
        }
        if (currentSession != null && currentSession.status() == InterviewStatus.RUNNING) {
            if (!AppDialogs.confirm(
                    workspaceRoot.getScene() == null ? null : workspaceRoot.getScene().getWindow(),
                    "暂停面试",
                    "暂停并离开面试？",
                    "离开前将自动暂停面试并保存当前进度，之后可以继续。",
                    "暂停并离开",
                    false)) return false;
            try {
                sessionService.pause(userId(), sessionId);
                currentSession = sessionService.require(userId(), sessionId);
            } catch (RuntimeException exception) {
                viewManager.showError(exceptionHandler.toUserMessage(exception));
                return false;
            }
        }
        saveDraftSilently();
        stopWorkspaceTimers();
        return true;
    }

    @FXML
    private void endInterview() {
        if (generationInProgress) {
            viewManager.showInfo("正在生成", "请等待本轮 AI 输出完成后再结束面试。");
            return;
        }
        if (currentSession == null
                || (currentSession.status() != InterviewStatus.RUNNING
                    && currentSession.status() != InterviewStatus.PAUSED)) {
            return;
        }
        if (!com.inin.aiinterviewer.ui.component.AppDialogs.confirm(
                answerArea.getScene() == null ? null : answerArea.getScene().getWindow(),
                "结束面试",
                "确认结束面试",
                "结束面试后将保存当前进度，并在后台生成报告（可稍后在「面试记录」中查看）。是否继续？",
                "结束面试", false)) {
            return;
        }
        try {
            if (currentSession.status() == InterviewStatus.PAUSED) {
                sessionService.resume(userId(), sessionId);
            }
            String pending = answerArea.getText();
            if (pending != null && !pending.isBlank()) {
                sessionService.appendUserAnswer(userId(), sessionId, pending.strip());
                answerArea.clear();
                SESSION_DRAFTS.remove(sessionId);
            }
            sessionService.endInterview(userId(), sessionId);
            currentSession = sessionService.require(userId(), sessionId);
            try {
                reportTaskService.enqueue(userId(), sessionId);
                viewManager.showInfo("面试已结束", "报告正在后台生成，你可以稍后在「面试记录」中查看。");
            } catch (RuntimeException enqueueFailure) {
                viewManager.showInfo("面试已结束",
                        "当前回答较少，未能生成评分报告；你仍可在「面试记录」中查看本次对话。");
            }
            stopWorkspaceTimers();
            contentNavigator.showRoute(Route.DASHBOARD);
        } catch (RuntimeException exception) {
            viewManager.showError(exceptionHandler.toUserMessage(exception));
        }
    }

    private void refresh() {
        currentSession = sessionService.require(userId(), sessionId);
        List<InterviewMessageDto> messages = sessionService.messages(userId(), sessionId);
        ReportGenerationTaskStateDto reportTaskState = reportTaskService.state(userId(), sessionId);
        boolean reportReady = reportTaskState.completion().reportStatus() == ReportStatus.COMPLETED;
        titleLabel.setText(currentSession.title());
        jobLabel.setText(currentSession.jobTitle());
        stageLabel.setText(stageText(currentSession.stage()));
        stageRailLabel.setText(stageText(currentSession.stage()));
        stageDescriptionLabel.setText(stageDescription(currentSession.stage()));
        infoJobLabel.setText(currentSession.jobTitle());
        planNameLabel.setText(currentSession.planSnapshot().name());
        resumeNameLabel.setText(currentSession.resumeId() == null
                ? "未关联简历" : "已关联简历 #" + currentSession.resumeId());
        durationLabel.setText(currentSession.planSnapshot().durationMinutes() + " 分钟");
        statusLabel.setText(reportReady ? "已完成" : statusText(currentSession.status()));
        InterviewPlanSettings settings = InterviewPlanSettings.fromRules(
                currentSession.planSnapshot().rules());
        modeLabel.setText(modeText(settings.mode()));
        personaLabel.setText(personaText(settings.persona()));
        var latestState = sessionService.loadLatestState(userId(), sessionId);
        PressureLevel pressure = latestState.map(state -> state.pressureState().level())
                .orElse(settings.pressureLevel());
        pressureLabel.setText(pressureText(pressure));
        latestState.map(state -> state.activeScenario())
                .filter(scenario -> scenario.status() == ScenarioStatus.ACTIVE)
                .ifPresentOrElse(scenario -> {
                    scenarioLabel.setText("场景 " + scenario.currentRound() + " / " + scenario.maxRounds());
                    scenarioLabel.setVisible(true);
                    scenarioLabel.setManaged(true);
                }, () -> {
                    scenarioLabel.setVisible(false);
                    scenarioLabel.setManaged(false);
                });
        renderModeDetails(settings, latestState.orElse(null));
        transcriptView.setMessages(messages);
        renderCitations(messages);
        var completionState = reportTaskState.completion();
        long askedQuestions = messages.stream().filter(message -> message.role() == Message.Role.ASSISTANT).count();
        progressLabel.setText("第 " + Math.min(askedQuestions, currentSession.planSnapshot().questionCount())
                + " / " + currentSession.planSnapshot().questionCount() + " 题");
        int totalQuestions = Math.max(1, currentSession.planSnapshot().questionCount());
        double overallProgress = Math.min(1.0, askedQuestions / (double) totalQuestions);
        overallProgressBar.setProgress(overallProgress);
        overallProgressLabel.setText(Math.round(overallProgress * 100) + "%");
        long answeredQuestions = messages.stream().filter(message -> message.role() == Message.Role.USER).count();
        scoreStateLabel.setText(answeredQuestions == 0 ? "等待回答" : "已收集 " + answeredQuestions + " 条回答");
        refreshRemainingTime();

        boolean running = currentSession.status() == InterviewStatus.RUNNING;
        boolean hasQuestion = sessionService.loadLatestState(userId(), sessionId)
                .map(state -> state.currentQuestion() != null && !state.currentQuestion().isBlank())
                .orElse(false);
        boolean awaitingReport = completionState.finalAnswerSaved()
                && currentSession.status() != InterviewStatus.COMPLETED;
        boolean canAnswer = running && !generationInProgress && !awaitingReport && !reportReady
                && (!llmProperties.isConfigured() || hasQuestion);
        updateAnswerTimerForTurn(canAnswer);
        answerArea.setDisable(!canAnswer);
        submitButton.setDisable(!canAnswer);
        pauseButton.setDisable(generationInProgress);
        pauseButton.setText(running ? "暂停面试" : "继续面试");
        pauseButton.setVisible(currentSession.status() != InterviewStatus.COMPLETED && !awaitingReport && !reportReady);
        pauseButton.setManaged(pauseButton.isVisible());
        boolean canEnd = running && !awaitingReport && !generationInProgress && !reportReady;
        endButton.setVisible(canEnd);
        endButton.setManaged(canEnd);
        retryQuestionButton.setVisible(llmProperties.isConfigured() && running && !hasQuestion);
        retryQuestionButton.setManaged(retryQuestionButton.isVisible());
        retryQuestionButton.setDisable(generationInProgress || !messages.isEmpty());
        retryReportButton.setVisible(awaitingReport);
        retryReportButton.setManaged(awaitingReport);
        retryReportButton.setDisable(generationInProgress
                || !llmProperties.isConfigured()
                || reportTaskState.active());
        retryReportButton.setText(reportActionText(reportTaskState));
        reportButton.setVisible(reportReady);
        reportButton.setManaged(reportReady);
        if (reportReady) {
            aiNoticeLabel.setText("面试已结束，六维评分和 Markdown 报告已生成。可点击上方「查看报告」，或稍后在「面试记录」中查看。");
        } else if (awaitingReport) {
            aiNoticeLabel.setText(completionNotice(reportTaskState));
        } else if (currentSession.status() == InterviewStatus.COMPLETED) {
            aiNoticeLabel.setText("面试已完成，六维评分和 Markdown 报告已保存。可从面试记录进入报告页查看。 ");
        } else {
            aiNoticeLabel.setText(llmProperties.isConfigured()
                    ? "AI 已配置：回答会先保存，再执行分析、受控阶段决策和流式提问。"
                    : "AI 尚未配置。当前可验证会话、回答保存及暂停恢复，不会生成伪造的 AI 提问。");
        }
        renderAgentStatus(running, reportReady);
        updateShellContext();
        updateReportStatePolling(reportTaskState);
    }

    @FXML
    private void openReport() {
        stopWorkspaceTimers();
        contentNavigator.showSubPage(
                "/fxml/report-detail-view.fxml", "面试报告", sessionId);
    }

    @FXML
    private void selectTextMode() {
        textModeToggle.setSelected(true);
        answerArea.getStyleClass().remove("code-answer-input");
        answerArea.setPromptText("输入你的回答，支持普通文本与代码回答…");
        answerArea.requestFocus();
    }

    @FXML
    private void selectCodeMode() {
        codeModeToggle.setSelected(true);
        if (!answerArea.getStyleClass().contains("code-answer-input")) {
            answerArea.getStyleClass().add("code-answer-input");
        }
        answerArea.setPromptText("输入代码或伪代码，可说明复杂度与关键取舍…");
        answerArea.requestFocus();
    }

    @FXML
    private void saveDraft() {
        String draft = saveDraftSilently();
        viewManager.showInfo("草稿已保存", draft.isBlank()
                ? "当前草稿为空。" : "本轮回答草稿已保留，重新进入本次面试时会自动恢复。");
    }

    @FXML
    private void clearDraft() {
        answerArea.clear();
        SESSION_DRAFTS.remove(sessionId);
        answerArea.requestFocus();
    }

    private String saveDraftSilently() {
        String draft = answerArea.getText() == null ? "" : answerArea.getText();
        if (draft.isBlank()) SESSION_DRAFTS.remove(sessionId);
        else SESSION_DRAFTS.put(sessionId, draft);
        return draft;
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
        output.timeout(java.time.Duration.ofMinutes(3))
                .subscribe(
                        chunk -> Platform.runLater(() -> {
                            if (expectsQuestion) transcriptView.appendAssistantChunk(chunk);
                        }),
                        throwable -> Platform.runLater(() -> handleStreamFailure(throwable)),
                        () -> Platform.runLater(() -> handleStreamSuccess(clearInputOnSuccess)));
    }

    private void handleStreamFailure(Throwable throwable) {
        log.error("Interview stream failed for session {}", sessionId, throwable);
        generationInProgress = false;
        String message = buildStreamErrorMessage(throwable);
        try {
            refresh();
        } catch (RuntimeException refreshFailure) {
            log.error("Refresh failed after stream error for session {}", sessionId, refreshFailure);
        }
        viewManager.showError(message);
    }

    private void handleStreamSuccess(boolean clearInputOnSuccess) {
        try {
            generationInProgress = false;
            if (clearInputOnSuccess) {
                answerArea.clear();
                SESSION_DRAFTS.remove(sessionId);
            }
            refresh();
        } catch (RuntimeException exception) {
            log.error("Refresh failed after stream completion for session {}", sessionId, exception);
            generationInProgress = false;
            try {
                refresh();
            } catch (RuntimeException ignored) {
                // best effort: keep UI usable even if refresh keeps failing
            }
            viewManager.showError(exceptionHandler.toUserMessage(exception));
        }
    }

    private String buildStreamErrorMessage(Throwable throwable) {
        Throwable root = throwable;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String detail = root.getMessage();
        boolean lock = root.getClass().getName().toLowerCase().contains("lock")
                || (detail != null && detail.toLowerCase().contains("locked"));
        StringBuilder message = new StringBuilder(exceptionHandler.toUserMessage(throwable));
        if (detail != null && !detail.isBlank()) {
            message.append("\n\n原始信息：").append(detail);
        }
        if (lock) {
            message.append("\n\n检测到数据库被占用，请完全关闭应用（任务管理器中结束所有 java.exe）后重新启动再试。");
        }
        return message.toString();
    }

    private void setBusyState(String progressText) {
        stopAnswerTimer();
        aiNoticeLabel.setText(progressText);
        agentStreamingLabel.setText("运行中");
        agentStreamingLabel.getStyleClass().remove("rail-agent-ready");
        if (!agentStreamingLabel.getStyleClass().contains("rail-agent-running")) {
            agentStreamingLabel.getStyleClass().add("rail-agent-running");
        }
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

    private void startWorkspaceClock() {
        if (workspaceClock != null) return;
        workspaceClock = new Timeline(new KeyFrame(Duration.seconds(30), event -> refreshRemainingTime()));
        workspaceClock.setCycleCount(Timeline.INDEFINITE);
        workspaceClock.play();
    }

    private void stopWorkspaceClock() {
        if (workspaceClock == null) return;
        workspaceClock.stop();
        workspaceClock = null;
    }

    private void stopWorkspaceTimers() {
        stopReportStatePolling();
        stopWorkspaceClock();
        stopAnswerTimer();
    }

    private void updateAnswerTimerForTurn(boolean canAnswer) {
        Integer limit = InterviewPlanSettings.answerTimeLimitSecondsOf(
                currentSession.planSnapshot().rules());
        if (limit == null || !canAnswer) {
            stopAnswerTimer();
            lastTimerQuestionCount = -1;
            if (answerTimeLimitLabel != null) {
                answerTimeLimitLabel.setVisible(false);
                answerTimeLimitLabel.setManaged(false);
            }
            return;
        }
        int questionCount = transcriptView.getQuestionCount();
        if (questionCount != lastTimerQuestionCount) {
            lastTimerQuestionCount = questionCount;
            answerTotalSeconds = limit;
            answerRemainingSeconds = limit;
        }
        if (answerTimeLimitLabel != null) {
            answerTimeLimitLabel.setVisible(true);
            answerTimeLimitLabel.setManaged(true);
        }
        updateAnswerTimerLabel();
        startAnswerTimer();
    }

    private void startAnswerTimer() {
        if (answerTimer != null) return;
        answerTimer = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            if (answerRemainingSeconds > 0) answerRemainingSeconds--;
            updateAnswerTimerLabel();
        }));
        answerTimer.setCycleCount(Timeline.INDEFINITE);
        answerTimer.play();
    }

    private void stopAnswerTimer() {
        if (answerTimer == null) return;
        answerTimer.stop();
        answerTimer = null;
    }

    private void updateAnswerTimerLabel() {
        if (answerTimeLimitLabel == null) return;
        String total = formatSeconds(answerTotalSeconds);
        if (answerRemainingSeconds <= 0) {
            answerTimeLimitLabel.setText("⏰ 本题作答已超时（限时 " + total + "），仍可继续作答后提交");
            answerTimeLimitLabel.setStyle("-fx-text-fill: #d9534f; -fx-font-weight: bold; -fx-font-size: 13px;");
        } else {
            String remaining = formatSeconds(answerRemainingSeconds);
            int threshold = Math.max(10, answerTotalSeconds / 10);
            boolean urgent = answerRemainingSeconds <= threshold;
            String color = urgent ? "#d9534f" : "#e08a1e";
            answerTimeLimitLabel.setText("⏳ 本题作答倒计时 " + remaining + " / " + total);
            answerTimeLimitLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold; -fx-font-size: 13px;");
        }
    }

    private String formatSeconds(int seconds) {
        int minutes = seconds / 60;
        int secs = seconds % 60;
        return minutes + ":" + (secs < 10 ? "0" : "") + secs;
    }

    private void refreshRemainingTime() {
        if (currentSession == null) return;
        LocalDateTime started = currentSession.startedTime() == null
                ? currentSession.createTime() : currentSession.startedTime();
        LocalDateTime ended = currentSession.completedTime() == null
                ? LocalDateTime.now() : currentSession.completedTime();
        long elapsedSeconds = Math.max(0, java.time.Duration.between(started, ended).toSeconds());
        remainingTimeLabel.setText(formatCompactClock(elapsedSeconds));
        updateShellClock(elapsedSeconds);
    }

    private String formatCompactClock(long seconds) {
        long minutes = seconds / 60;
        long remainder = seconds % 60;
        return String.format("%02d:%02d", minutes, remainder);
    }

    private void updateShellClock(long elapsedSeconds) {
        if (workspaceRoot.getScene() == null) return;
        javafx.scene.Node node = workspaceRoot.getScene().lookup("#sessionClockLabel");
        if (!(node instanceof Label label)) return;
        long hours = elapsedSeconds / 3600;
        long minutes = (elapsedSeconds % 3600) / 60;
        long seconds = elapsedSeconds % 60;
        label.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
    }

    private void updateShellContext() {
        if (currentSession == null || workspaceRoot.getScene() == null) return;
        javafx.scene.Node title = workspaceRoot.getScene().lookup("#contentTitleLabel");
        if (title instanceof Label label) label.setText(currentSession.title());
        javafx.scene.Node subtitle = workspaceRoot.getScene().lookup("#contentSubtitleLabel");
        if (subtitle instanceof Label label) {
            InterviewPlanSettings settings = InterviewPlanSettings.fromRules(
                    currentSession.planSnapshot().rules());
            label.setText("方案：" + currentSession.planSnapshot().name()
                    + " · " + modeText(settings.mode())
                    + "  |  岗位：" + currentSession.jobTitle());
        }
    }

    private void renderAgentStatus(boolean running, boolean reportReady) {
        boolean configured = llmProperties.isConfigured();
        agentConnectionLabel.setText(configured ? "已连接" : "待配置");
        agentGraphStatusLabel.setText(configured && running ? "✓ 就绪" : (reportReady ? "✓ 完成" : "待配置"));
        agentCheckpointLabel.setText(running || reportReady ? "✓ 就绪" : "已暂停");
        agentStreamingLabel.setText(generationInProgress ? "运行中" : (configured ? "✓ 就绪" : "待配置"));
        agentRetryLabel.setText(configured ? "✓ 就绪" : "待配置");
        agentStreamingLabel.getStyleClass().removeAll("rail-agent-ready", "rail-agent-running");
        agentStreamingLabel.getStyleClass().add(generationInProgress
                ? "rail-agent-running" : "rail-agent-ready");
    }

    private String stageDescription(InterviewStage stage) {
        return switch (stage) {
            case INTRODUCTION -> "确认候选人背景与本次面试重点，建立清晰的交流上下文。";
            case RESUME_REVIEW -> "围绕简历中的关键经历核验职责、贡献与结果，补齐事实链路。";
            case PROJECT_EXPERIENCE -> "结合真实项目深挖目标、约束、个人决策与最终产出。";
            case TECHNICAL_DEEP_DIVE -> "深入考察技术广度与深度，结合项目经验进行追问和延展。";
            case SYSTEM_DESIGN -> "从容量、边界、可靠性与取舍角度完成系统方案推演。";
            case CODING -> "通过代码或伪代码检查建模、实现质量与复杂度意识。";
            case BEHAVIORAL -> "通过结构化事例评估协作、推动、复盘与影响力。";
            case SUMMARY -> "回顾关键回答，确认遗漏信息并完成最终收束。";
            case COMPLETED -> "本次面试已完成，报告与能力证据正在整理。";
        };
    }

    private void renderModeDetails(
            InterviewPlanSettings settings,
            com.inin.aiinterviewer.agent.state.InterviewState state
    ) {
        boolean coaching = settings.mode() == InterviewMode.COACHING;
        show(coachingPanel, coaching);
        if (coaching) renderCoachingFeedback();

        var scenario = state == null ? null : state.activeScenario();
        boolean scenarioMode = settings.mode() == InterviewMode.SCENARIO_SIMULATION;
        show(scenarioPanel, scenarioMode || (scenario != null && scenario.status() == ScenarioStatus.ACTIVE));
        boolean detailMode = coaching || scenarioMode
                || (scenario != null && scenario.status() == ScenarioStatus.ACTIVE);
        show(scorePanel, !detailMode);
        show(agentPanel, !detailMode);
        show(tipPanel, !detailMode);
        if (scenario == null) {
            scenarioObjectiveLabel.setText("等待按方案规则进入情境沙盘");
            scenarioFactsLabel.setText("场景开始后仅展示已公开事实、约束和变量。");
            scenarioEventLabel.setText("尚无场景事件");
            scenarioDecisionLabel.setText("尚无候选人决策");
            return;
        }
        scenarioObjectiveLabel.setText(scenario.objective() + "\n角色：" + scenario.candidateRole());
        List<String> publicState = new java.util.ArrayList<>(scenario.knownFacts());
        scenario.constraints().stream().filter(constraint -> constraint.active())
                .map(constraint -> "约束：" + constraint.description()).forEach(publicState::add);
        if (!scenario.variables().isEmpty()) publicState.add("当前变量：" + scenario.variables());
        scenarioFactsLabel.setText(bullets(publicState, "暂无公开事实"));
        scenarioEventLabel.setText(scenario.events().isEmpty() ? "尚无场景事件"
                : scenario.events().getLast().description());
        scenarioDecisionLabel.setText(scenario.decisions().isEmpty() ? "尚无候选人决策"
                : scenario.decisions().getLast().action() + "\n依据："
                + scenario.decisions().getLast().rationale());
    }

    private void renderCoachingFeedback() {
        currentCoachingFeedback = coachingFeedbackService.feedback(userId(), sessionId);
        coachHintLabel.setVisible(false);
        coachHintLabel.setManaged(false);
        if (!currentCoachingFeedback.available()) {
            coachCoveredLabel.setText("完成一轮回答后显示");
            coachMissingLabel.setText("完成一轮回答后显示");
            coachLogicLabel.setText("完成一轮回答后显示");
            coachStructureLabel.setText(bullets(List.of(
                    "背景与约束", "个人行动与取舍", "结果与验证"), ""));
            coachHintButton.setDisable(true);
            coachReanswerButton.setDisable(true);
            return;
        }
        coachCoveredLabel.setText(bullets(currentCoachingFeedback.coveredContent(), "暂无"));
        coachMissingLabel.setText(bullets(currentCoachingFeedback.missingContent(), "暂无"));
        coachLogicLabel.setText(bullets(currentCoachingFeedback.logicGaps(), "暂无"));
        coachStructureLabel.setText(bullets(currentCoachingFeedback.referenceStructure(), "暂无"));
        coachHintLabel.setText(currentCoachingFeedback.hint());
        coachHintButton.setDisable(generationInProgress);
        coachReanswerButton.setDisable(generationInProgress || !currentCoachingFeedback.canReanswer());
    }

    @FXML
    private void requestCoachHint() {
        if (!currentCoachingFeedback.available() || generationInProgress) return;
        coachHintLabel.setVisible(true);
        coachHintLabel.setManaged(true);
    }

    @FXML
    private void reanswerLatest() {
        if (!currentCoachingFeedback.canReanswer() || generationInProgress) return;
        try {
            if (currentSession.status() == InterviewStatus.RUNNING) {
                sessionService.pause(userId(), sessionId);
                currentSession = sessionService.require(userId(), sessionId);
            }
            var branch = branchService.create(
                    userId(), sessionId, currentCoachingFeedback.sourceQuestionNumber(), null);
            stopWorkspaceTimers();
            contentNavigator.showSubPage(
                    "/fxml/session-branch-view.fxml", "教练重答", branch.id());
        } catch (RuntimeException exception) {
            refresh();
            viewManager.showError(exceptionHandler.toUserMessage(exception));
        }
    }

    private void show(VBox node, boolean value) {
        node.setVisible(value);
        node.setManaged(value);
    }

    private String bullets(List<String> values, String emptyText) {
        if (values == null || values.isEmpty()) return emptyText;
        return values.stream().filter(value -> value != null && !value.isBlank())
                .map(value -> "• " + value).reduce((left, right) -> left + "\n" + right)
                .orElse(emptyText);
    }

    private void renderCitations(List<InterviewMessageDto> messages) {
        citationContainer.getChildren().clear();
        var documents = currentSession == null ? List.<com.inin.aiinterviewer.application.dto.KnowledgeDocumentSnapshotDto>of()
                : currentSession.knowledgeSnapshot();
        citationCountLabel.setText(documents.isEmpty() ? "暂无关联" : "查看全部（" + documents.size() + "）");
        if (documents.isEmpty()) {
            Label empty = new Label("当前方案未关联知识库文档");
            empty.setWrapText(true);
            empty.getStyleClass().add("citation-empty");
            citationContainer.getChildren().add(empty);
        } else {
            for (int index = 0; index < Math.min(3, documents.size()); index++) {
                var document = documents.get(index);
                FontIcon icon = new FontIcon("mdi2f-file-document-outline");
                icon.setIconSize(15);
                icon.getStyleClass().add("rail-document-icon");
                Button link = new Button(document.name());
                link.setAlignment(Pos.CENTER_LEFT);
                link.setMaxWidth(Double.MAX_VALUE);
                link.getStyleClass().add("rail-document-link");
                link.setOnAction(event -> openCitation(document.id()));
                HBox.setHgrow(link, Priority.ALWAYS);
                Label tag = new Label(index < 2 ? "核心" : "推荐");
                tag.getStyleClass().add(index < 2 ? "rail-document-tag" : "rail-document-tag-recommended");
                HBox row = new HBox(7, icon, link, tag);
                row.setAlignment(Pos.CENTER_LEFT);
                row.getStyleClass().add("rail-document-row");
                citationContainer.getChildren().add(row);
            }
        }

        String latestReferences = messages.stream()
                .filter(message -> message.role() == Message.Role.ASSISTANT && !message.citations().isEmpty())
                .reduce((left, right) -> right)
                .map(message -> message.citations().stream()
                        .map(citation -> citation.documentName().replaceFirst("\\.[^.]+$", ""))
                        .distinct().limit(2).reduce((left, right) -> left + " 与 " + right).orElse(""))
                .orElse("");
        currentCitationLabel.setText(latestReferences.isBlank()
                ? "当前问题尚未引用知识库" : latestReferences);
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

    private String modeText(InterviewMode mode) {
        return switch (mode) {
            case FORMAL_SIMULATION -> "正式模拟";
            case COACHING -> "教练训练";
            case SCENARIO_SIMULATION -> "情境沙盘";
            case RE_TEST -> "复试";
        };
    }

    private String personaText(InterviewerPersona persona) {
        return switch (persona) {
            case PROFESSIONAL_INTERVIEWER -> "专业面试官";
            case FUTURE_PEER -> "未来同事";
            case TECH_LEAD -> "技术负责人";
            case ARCHITECT -> "架构师";
            case INCIDENT_COMMANDER -> "事故指挥者";
            case PRODUCT_LEADER -> "产品负责人";
        };
    }

    private String pressureText(PressureLevel pressure) {
        return switch (pressure) {
            case RELAXED -> "轻松压力";
            case STANDARD -> "标准压力";
            case CHALLENGING -> "挑战压力";
            case HIGH_PRESSURE -> "高压";
        };
    }
}
