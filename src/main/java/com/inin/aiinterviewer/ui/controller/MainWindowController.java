package com.inin.aiinterviewer.ui.controller;

import com.inin.aiinterviewer.config.properties.LlmProperties;
import com.inin.aiinterviewer.ui.navigation.ContentNavigator;
import com.inin.aiinterviewer.ui.navigation.JavaFxViewManager;
import com.inin.aiinterviewer.ui.navigation.Route;
import com.inin.aiinterviewer.ui.state.UserSessionState;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class MainWindowController {

    private final UserSessionState sessionState;
    private final JavaFxViewManager viewManager;
    private final LlmProperties llmProperties;
    private final ContentNavigator contentNavigator;

    @FXML
    private Label usernameLabel;
    @FXML
    private Label aiStatusLabel;
    @FXML
    private Label contentTitleLabel;
    @FXML
    private StackPane contentHost;

    public MainWindowController(
            UserSessionState sessionState,
            JavaFxViewManager viewManager,
            LlmProperties llmProperties,
            ContentNavigator contentNavigator
    ) {
        this.sessionState = sessionState;
        this.viewManager = viewManager;
        this.llmProperties = llmProperties;
        this.contentNavigator = contentNavigator;
    }

    @FXML
    private void initialize() {
        usernameLabel.setText(sessionState.requireCurrentUser().nickname());
        aiStatusLabel.setText(llmProperties.isConfigured() ? "AI 配置已检测" : "AI 尚未配置");
        contentNavigator.attach(contentHost, contentTitleLabel);
        showSection(Route.DASHBOARD);
    }

    @FXML private void showDashboard() { showSection(Route.DASHBOARD); }
    @FXML private void showPlans() { showSection(Route.PLAN); }
    @FXML private void showHistory() { showSection(Route.HISTORY); }
    @FXML private void showResumes() { showSection(Route.RESUME); }
    @FXML private void showKnowledge() { showSection(Route.KNOWLEDGE); }
    @FXML private void showSettings() { showSection(Route.SETTING); }

    @FXML
    private void logout() {
        sessionState.logOut();
        viewManager.switchView(Route.LOGIN);
    }

    private void showSection(Route route) {
        contentNavigator.showRoute(route);
    }
}
