package com.inin.aiinterviewer.ui.controller;

import com.inin.aiinterviewer.application.service.BackgroundTaskService;
import com.inin.aiinterviewer.config.properties.LlmProperties;
import com.inin.aiinterviewer.domain.enums.BackgroundTaskStatus;
import com.inin.aiinterviewer.domain.enums.BackgroundTaskType;
import com.inin.aiinterviewer.ui.navigation.ContentNavigator;
import com.inin.aiinterviewer.ui.navigation.JavaFxViewManager;
import com.inin.aiinterviewer.ui.navigation.Route;
import com.inin.aiinterviewer.ui.state.TaskNotificationCenter;
import com.inin.aiinterviewer.ui.state.TaskNotificationCenter.TaskNotification;
import com.inin.aiinterviewer.ui.state.UserSessionState;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class MainWindowController {

    private static final PseudoClass SELECTED = PseudoClass.getPseudoClass("selected");
    private static final PseudoClass ACTIVE = PseudoClass.getPseudoClass("active");
    private static final PseudoClass FAILED = PseudoClass.getPseudoClass("failed");

    private final UserSessionState sessionState;
    private final JavaFxViewManager viewManager;
    private final LlmProperties llmProperties;
    private final ContentNavigator contentNavigator;
    private final BackgroundTaskService taskService;
    private final TaskNotificationCenter notificationCenter;

    @FXML private BorderPane mainRoot;
    @FXML
    private Label usernameLabel;
    @FXML
    private Label aiStatusLabel;
    @FXML
    private Label contentTitleLabel;
    @FXML
    private StackPane contentHost;
    @FXML private Button dashboardNavButton;
    @FXML private Button plansNavButton;
    @FXML private Button historyNavButton;
    @FXML private Button resumesNavButton;
    @FXML private Button profilesNavButton;
    @FXML private Button knowledgeNavButton;
    @FXML private Button tasksNavButton;
    @FXML private Button settingsNavButton;
    @FXML private Button taskStatusButton;
    @FXML private HBox activityReceipt;
    @FXML private Label activityTitleLabel;
    @FXML private Label activityDetailLabel;

    private TaskNotificationCenter.Registration notificationRegistration;
    private PauseTransition receiptTimeout;
    private long notificationTaskId;

    public MainWindowController(
            UserSessionState sessionState,
            JavaFxViewManager viewManager,
            LlmProperties llmProperties,
            ContentNavigator contentNavigator,
            BackgroundTaskService taskService,
            TaskNotificationCenter notificationCenter
    ) {
        this.sessionState = sessionState;
        this.viewManager = viewManager;
        this.llmProperties = llmProperties;
        this.contentNavigator = contentNavigator;
        this.taskService = taskService;
        this.notificationCenter = notificationCenter;
    }

    @FXML
    private void initialize() {
        usernameLabel.setText(sessionState.requireCurrentUser().nickname());
        aiStatusLabel.setText(llmProperties.isConfigured() ? "AI 配置已检测" : "AI 尚未配置");
        contentNavigator.attach(contentHost, contentTitleLabel);
        mainRoot.sceneProperty().addListener((observable, previous, current) -> {
            if (current == null) unsubscribeFromNotifications();
            else subscribeToNotifications();
        });
        refreshTaskIndicator();
        showSection(Route.DASHBOARD);
    }

    @FXML private void showDashboard() { showSection(Route.DASHBOARD); }
    @FXML private void showPlans() { showSection(Route.PLAN); }
    @FXML private void showHistory() { showSection(Route.HISTORY); }
    @FXML private void showResumes() { showSection(Route.RESUME); }
    @FXML private void showProfiles() { showSection(Route.PROFILE); }
    @FXML private void showKnowledge() { showSection(Route.KNOWLEDGE); }
    @FXML private void showTasks() { showSection(Route.TASK); }
    @FXML private void showSettings() { showSection(Route.SETTING); }

    @FXML
    private void logout() {
        unsubscribeFromNotifications();
        sessionState.logOut();
        viewManager.switchView(Route.LOGIN);
    }

    private void showSection(Route route) {
        contentNavigator.showRoute(route);
        dashboardNavButton.pseudoClassStateChanged(SELECTED, route == Route.DASHBOARD);
        plansNavButton.pseudoClassStateChanged(SELECTED, route == Route.PLAN);
        historyNavButton.pseudoClassStateChanged(SELECTED, route == Route.HISTORY);
        resumesNavButton.pseudoClassStateChanged(SELECTED, route == Route.RESUME);
        profilesNavButton.pseudoClassStateChanged(SELECTED, route == Route.PROFILE);
        knowledgeNavButton.pseudoClassStateChanged(SELECTED, route == Route.KNOWLEDGE);
        tasksNavButton.pseudoClassStateChanged(SELECTED, route == Route.TASK);
        settingsNavButton.pseudoClassStateChanged(SELECTED, route == Route.SETTING);
    }

    @FXML
    private void showNotificationTask() {
        if (notificationTaskId <= 0) return;
        long taskId = notificationTaskId;
        dismissActivityReceipt();
        showSection(Route.TASK);
        contentNavigator.showSubPage("/fxml/task-detail-view.fxml", "任务详情", taskId);
    }

    @FXML
    private void dismissActivityReceipt() {
        if (receiptTimeout != null) receiptTimeout.stop();
        activityReceipt.setVisible(false);
        activityReceipt.setManaged(false);
        notificationTaskId = 0;
    }

    private void subscribeToNotifications() {
        if (notificationRegistration != null) return;
        notificationRegistration = notificationCenter.subscribe(
                sessionState.requireCurrentUser().id(),
                notification -> Platform.runLater(() -> handleTaskNotification(notification)));
    }

    private void unsubscribeFromNotifications() {
        if (notificationRegistration == null) return;
        notificationRegistration.close();
        notificationRegistration = null;
        if (receiptTimeout != null) receiptTimeout.stop();
    }

    private void handleTaskNotification(TaskNotification notification) {
        if (mainRoot.getScene() == null) return;
        refreshTaskIndicator();
        if (notification.outcome() == TaskNotificationCenter.Outcome.QUEUED
                || notification.outcome() == TaskNotificationCenter.Outcome.RUNNING) return;
        notificationTaskId = notification.taskId();
        boolean failed = notification.outcome() == TaskNotificationCenter.Outcome.FAILED;
        activityReceipt.pseudoClassStateChanged(FAILED, failed);
        activityTitleLabel.setText(notificationTitle(notification));
        activityDetailLabel.setText(notificationDetail(notification));
        activityReceipt.setManaged(true);
        activityReceipt.setVisible(true);
        if (receiptTimeout != null) receiptTimeout.stop();
        receiptTimeout = new PauseTransition(Duration.seconds(failed ? 12 : 8));
        receiptTimeout.setOnFinished(event -> dismissActivityReceipt());
        receiptTimeout.play();
    }

    private void refreshTaskIndicator() {
        var tasks = taskService.listDtos(sessionState.requireCurrentUser().id());
        long active = tasks.stream().filter(task -> task.status() == BackgroundTaskStatus.PENDING
                || task.status() == BackgroundTaskStatus.RUNNING).count();
        long failed = tasks.stream().filter(task -> task.status() == BackgroundTaskStatus.FAILED).count();
        taskStatusButton.pseudoClassStateChanged(ACTIVE, active > 0);
        taskStatusButton.pseudoClassStateChanged(FAILED, active == 0 && failed > 0);
        if (active > 0) {
            taskStatusButton.setText("后台处理中 · " + active);
            taskStatusButton.setAccessibleText(active + " 个后台任务正在处理，打开任务中心");
        } else if (failed > 0) {
            taskStatusButton.setText("任务待处理 · " + failed);
            taskStatusButton.setAccessibleText(failed + " 个后台任务失败，打开任务中心");
        } else {
            taskStatusButton.setText("后台任务");
            taskStatusButton.setAccessibleText("打开任务中心");
        }
    }

    private String notificationTitle(TaskNotification notification) {
        String type = taskTypeText(notification.taskType());
        return notification.outcome() == TaskNotificationCenter.Outcome.COMPLETED
                ? type + "已完成" : type + "失败";
    }

    private String notificationDetail(TaskNotification notification) {
        if (notification.outcome() == TaskNotificationCenter.Outcome.COMPLETED) {
            return notification.taskType() == BackgroundTaskType.REPORT_GENERATE
                    ? "评分与报告已保存，可从面试记录查看。"
                    : "处理结果已保存到本地。";
        }
        return "自动重试已停止，进入任务详情检查后可重新排队。";
    }

    private String taskTypeText(BackgroundTaskType type) {
        return switch (type) {
            case RESUME_PARSE -> "简历解析";
            case PROFILE_GENERATE -> "候选人画像生成";
            case DOCUMENT_PARSE -> "知识文档处理";
            case EMBEDDING_GENERATE -> "向量生成";
            case VECTOR_UPDATE -> "索引更新";
            case REPORT_GENERATE -> "面试报告生成";
        };
    }
}
