package com.inin.aiinterviewer.ui.controller;

import com.inin.aiinterviewer.config.properties.LlmProperties;
import com.inin.aiinterviewer.ui.navigation.JavaFxViewManager;
import com.inin.aiinterviewer.ui.navigation.Route;
import com.inin.aiinterviewer.ui.state.UserSessionState;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class MainWindowController {

    private final UserSessionState sessionState;
    private final JavaFxViewManager viewManager;
    private final LlmProperties llmProperties;
    private final ApplicationContext applicationContext;

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
            ApplicationContext applicationContext
    ) {
        this.sessionState = sessionState;
        this.viewManager = viewManager;
        this.llmProperties = llmProperties;
        this.applicationContext = applicationContext;
    }

    @FXML
    private void initialize() {
        usernameLabel.setText(sessionState.requireCurrentUser().nickname());
        aiStatusLabel.setText(llmProperties.isConfigured() ? "AI 配置已检测" : "AI 尚未配置");
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
        contentTitleLabel.setText(route.title());
        if (route.contentPath() == null) {
            Label title = new Label("“" + route.title() + "”将在后续里程碑中实现");
            title.getStyleClass().add("page-title");
            Label description = new Label("当前版本不会填充演示数据或伪造业务结果。");
            description.getStyleClass().add("secondary-text");
            VBox placeholder = new VBox(12, title, description);
            placeholder.setAlignment(Pos.CENTER);
            contentHost.getChildren().setAll(placeholder);
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(route.contentPath()));
            loader.setControllerFactory(applicationContext::getBean);
            Parent content = loader.load();
            contentHost.getChildren().setAll(content);
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot load content view: " + route, exception);
        }
    }
}

