package com.inin.aiinterviewer.ui.controller;

import com.inin.aiinterviewer.config.properties.AppProperties;
import com.inin.aiinterviewer.config.properties.LlmProperties;
import com.inin.aiinterviewer.ui.state.UserSessionState;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class DashboardController {

    private final UserSessionState sessionState;
    private final AppProperties appProperties;
    private final LlmProperties llmProperties;

    @FXML private Label greetingLabel;
    @FXML private Label dataPathLabel;
    @FXML private Label aiDetailLabel;

    public DashboardController(
            UserSessionState sessionState,
            AppProperties appProperties,
            LlmProperties llmProperties
    ) {
        this.sessionState = sessionState;
        this.appProperties = appProperties;
        this.llmProperties = llmProperties;
    }

    @FXML
    private void initialize() {
        greetingLabel.setText("你好，" + sessionState.requireCurrentUser().nickname());
        dataPathLabel.setText(appProperties.dataRoot());
        aiDetailLabel.setText(llmProperties.isConfigured() ? "AI 配置已检测" : "待配置 AI 服务");
    }
}

