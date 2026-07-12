package com.inin.aiinterviewer.ui.controller;

import com.inin.aiinterviewer.application.exception.GlobalExceptionHandler;
import com.inin.aiinterviewer.application.service.UserService;
import com.inin.aiinterviewer.ui.navigation.JavaFxViewManager;
import com.inin.aiinterviewer.ui.navigation.Route;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class RegisterController {

    private final UserService userService;
    private final JavaFxViewManager viewManager;
    private final GlobalExceptionHandler exceptionHandler;

    @FXML
    private TextField usernameField;
    @FXML
    private TextField nicknameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private PasswordField confirmPasswordField;
    @FXML
    private CheckBox agreementCheckBox;
    @FXML
    private Label errorLabel;

    public RegisterController(
            UserService userService,
            JavaFxViewManager viewManager,
            GlobalExceptionHandler exceptionHandler
    ) {
        this.userService = userService;
        this.viewManager = viewManager;
        this.exceptionHandler = exceptionHandler;
    }

    @FXML
    private void initialize() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    @FXML
    private void handleRegister() {
        clearError();
        if (!agreementCheckBox.isSelected()) {
            showError("请先阅读并同意本地使用说明");
            return;
        }
        if (!passwordField.getText().equals(confirmPasswordField.getText())) {
            showError("两次输入的密码不一致");
            return;
        }

        try {
            userService.register(usernameField.getText(), nicknameField.getText(), passwordField.getText());
            passwordField.clear();
            confirmPasswordField.clear();
            viewManager.showInfo("账户创建成功", "本地账户已创建，请使用新账户登录。");
            viewManager.switchView(Route.LOGIN);
        } catch (RuntimeException exception) {
            showError(exceptionHandler.toUserMessage(exception));
        }
    }

    @FXML
    private void showLogin() {
        viewManager.switchView(Route.LOGIN);
    }

    private void clearError() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }
}

