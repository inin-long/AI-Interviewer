package com.inin.aiinterviewer.ui.controller;

import com.inin.aiinterviewer.config.properties.LlmProperties;
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
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.geometry.Side;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
@Scope("prototype")
public class MainWindowController {

    private static final PseudoClass SELECTED = PseudoClass.getPseudoClass("selected");
    private static final PseudoClass FAILED = PseudoClass.getPseudoClass("failed");
    private static final PseudoClass INTERVIEW_MODE = PseudoClass.getPseudoClass("interview-mode");

    private final UserSessionState sessionState;
    private final JavaFxViewManager viewManager;
    private final LlmProperties llmProperties;
    private final ContentNavigator contentNavigator;
    private final TaskNotificationCenter notificationCenter;

    @FXML private BorderPane mainRoot;
    @FXML private Label usernameLabel;
    @FXML private Label avatarLabel;
    @FXML private Label aiStatusLabel;
    @FXML private FontIcon aiStatusIcon;
    @FXML private Label contentTitleLabel;
    @FXML private Label contentSubtitleLabel;
    @FXML private Label sessionClockLabel;
    @FXML private Label interviewStorageNote;
    @FXML private StackPane contentHost;
    @FXML private Button dashboardNavButton;
    @FXML private Button plansNavButton;
    @FXML private ToggleButton interviewNavButton;
    @FXML private Button historyNavButton;
    @FXML private Button resumesNavButton;
    @FXML private Button knowledgeNavButton;
    @FXML private Button questionBankNavButton;
    @FXML private Button careerAssessmentNavButton;
    @FXML private Button skillsLibraryNavButton;
    @FXML private Button careerPlanningNavButton;
    @FXML private Button profilesNavButton;
    @FXML private Button tasksNavButton;
    @FXML private Button settingsNavButton;
    @FXML private Button userMenuButton;
    @FXML private ImageView defaultBrandImage;
    @FXML private ImageView interviewBrandImage;
    @FXML private ImageView interviewUserAvatar;
    @FXML private FontIcon topbarMenuIcon;
    @FXML private HBox activityReceipt;
    @FXML private Label activityTitleLabel;
    @FXML private Label activityDetailLabel;

    private TaskNotificationCenter.Registration notificationRegistration;
    private PauseTransition receiptTimeout;
    private long notificationTaskId;
    private ContextMenu userMenu;

    public MainWindowController(
            UserSessionState sessionState,
            JavaFxViewManager viewManager,
            LlmProperties llmProperties,
            ContentNavigator contentNavigator,
            TaskNotificationCenter notificationCenter
    ) {
        this.sessionState = sessionState;
        this.viewManager = viewManager;
        this.llmProperties = llmProperties;
        this.contentNavigator = contentNavigator;
        this.notificationCenter = notificationCenter;
    }

    @FXML
    private void initialize() {
        String nickname = sessionState.requireCurrentUser().nickname();
        usernameLabel.setText(nickname);
        avatarLabel.setText(avatarInitial(nickname));
        boolean aiConfigured = llmProperties.isConfigured();
        aiStatusLabel.setText(aiConfigured ? "AI 服务状态：正常" : "AI 服务状态：待配置");
        aiStatusIcon.pseudoClassStateChanged(FAILED, !aiConfigured);
        contentNavigator.attach(contentHost, contentTitleLabel, this::selectNavigation, this::configurePageMode);
        mainRoot.sceneProperty().addListener((observable, previous, current) -> {
            if (current == null) unsubscribeFromNotifications();
            else subscribeToNotifications();
        });
        showSection(Route.DASHBOARD);
    }

    @FXML private void showDashboard() { showSection(Route.DASHBOARD); }
    @FXML private void showPlans() { showSection(Route.PLAN); }
    @FXML private void showInterviewEntry() { showSection(Route.PLAN); }
    @FXML private void showHistory() { showSection(Route.HISTORY); }
    @FXML private void showResumes() { showSection(Route.RESUME); }
    @FXML private void showProfiles() { showSection(Route.PROFILE); }
    @FXML private void showKnowledge() { showSection(Route.KNOWLEDGE); }
    @FXML private void showQuestionBank() { showSection(Route.QUESTION_BANK); }
    @FXML private void showCareerAssessment() { showSection(Route.CAREER_ASSESSMENT); }
    @FXML private void showSkillsLibrary() { showSection(Route.SKILLS_LIBRARY); }
    @FXML private void showCareerPlanning() { showSection(Route.CAREER_PLANNING); }
    @FXML private void showTasks() { showSection(Route.TASK); }
    @FXML private void showSettings() { showSection(Route.SETTING); }

    @FXML
    private void showUserMenu() {
        if (userMenu == null) {
            MenuItem settings = new MenuItem("设置", new FontIcon("mdi2c-cog-outline"));
            settings.setOnAction(event -> showSettings());
            MenuItem logout = new MenuItem("退出登录", new FontIcon("mdi2l-logout-variant"));
            logout.setOnAction(event -> logout());
            userMenu = new ContextMenu(settings, logout);
        }
        if (userMenu.isShowing()) {
            userMenu.hide();
        } else {
            userMenu.show(userMenuButton, Side.BOTTOM, 0, 6);
        }
    }

    @FXML
    private void logout() {
        if (!contentNavigator.prepareForExternalNavigation()) return;
        unsubscribeFromNotifications();
        sessionState.logOut();
        viewManager.switchView(Route.LOGIN);
    }

    private void showSection(Route route) {
        contentNavigator.showRoute(route);
    }

    private void selectNavigation(Route route) {
        dashboardNavButton.pseudoClassStateChanged(SELECTED, route == Route.DASHBOARD);
        plansNavButton.pseudoClassStateChanged(SELECTED, route == Route.PLAN);
        historyNavButton.pseudoClassStateChanged(SELECTED, route == Route.HISTORY);
        resumesNavButton.pseudoClassStateChanged(SELECTED, route == Route.RESUME);
        profilesNavButton.pseudoClassStateChanged(SELECTED, route == Route.PROFILE);
        knowledgeNavButton.pseudoClassStateChanged(SELECTED, route == Route.KNOWLEDGE);
        questionBankNavButton.pseudoClassStateChanged(SELECTED, route == Route.QUESTION_BANK);
        careerAssessmentNavButton.pseudoClassStateChanged(SELECTED, route == Route.CAREER_ASSESSMENT);
        skillsLibraryNavButton.pseudoClassStateChanged(SELECTED, route == Route.SKILLS_LIBRARY);
        careerPlanningNavButton.pseudoClassStateChanged(SELECTED, route == Route.CAREER_PLANNING);
        tasksNavButton.pseudoClassStateChanged(SELECTED, route == Route.TASK);
        settingsNavButton.pseudoClassStateChanged(SELECTED, route == Route.SETTING);
    }

    private void configurePageMode(String fxmlPath) {
        boolean interviewMode = fxmlPath != null && fxmlPath.endsWith("/interview-workspace-view.fxml");
        mainRoot.pseudoClassStateChanged(INTERVIEW_MODE, interviewMode);
        showNode(interviewNavButton, interviewMode);
        showNode(profilesNavButton, !interviewMode);
        showNode(questionBankNavButton, !interviewMode);
        showNode(careerAssessmentNavButton, !interviewMode);
        showNode(skillsLibraryNavButton, !interviewMode);
        showNode(careerPlanningNavButton, !interviewMode);
        showNode(tasksNavButton, !interviewMode);
        showNode(interviewStorageNote, interviewMode);
        showNode(contentSubtitleLabel, interviewMode);
        showNode(sessionClockLabel, interviewMode);
        showNode(interviewBrandImage, interviewMode);
        showNode(defaultBrandImage, !interviewMode);
        showNode(interviewUserAvatar, interviewMode);
        showNode(avatarLabel, !interviewMode);
        showNode(topbarMenuIcon, !interviewMode);
        aiStatusLabel.setText(interviewMode
                ? "本地数据已同步"
                : (llmProperties.isConfigured() ? "AI 服务状态：正常" : "AI 服务状态：待配置"));
        if (!interviewMode) {
            contentSubtitleLabel.setText("");
            sessionClockLabel.setText("00:00:00");
            return;
        }
        dashboardNavButton.pseudoClassStateChanged(SELECTED, false);
        plansNavButton.pseudoClassStateChanged(SELECTED, false);
        historyNavButton.pseudoClassStateChanged(SELECTED, false);
        resumesNavButton.pseudoClassStateChanged(SELECTED, false);
        profilesNavButton.pseudoClassStateChanged(SELECTED, false);
        knowledgeNavButton.pseudoClassStateChanged(SELECTED, false);
        settingsNavButton.pseudoClassStateChanged(SELECTED, false);
        interviewNavButton.pseudoClassStateChanged(SELECTED, true);
    }

    private void showNode(javafx.scene.Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
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
        if (notification.outcome() == TaskNotificationCenter.Outcome.QUEUED
                || notification.outcome() == TaskNotificationCenter.Outcome.RUNNING
                || notification.outcome() == TaskNotificationCenter.Outcome.DELETED) return;
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

    private String avatarInitial(String nickname) {
        if (nickname == null || nickname.isBlank()) return "U";
        return nickname.strip().substring(0, 1).toUpperCase(Locale.ROOT);
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
