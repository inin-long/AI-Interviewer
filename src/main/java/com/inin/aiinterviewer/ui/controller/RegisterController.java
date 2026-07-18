package com.inin.aiinterviewer.ui.controller;

import com.inin.aiinterviewer.application.exception.GlobalExceptionHandler;
import com.inin.aiinterviewer.application.service.UserService;
import com.inin.aiinterviewer.ui.animation.AuthIllustrationMotion;
import com.inin.aiinterviewer.ui.navigation.JavaFxViewManager;
import com.inin.aiinterviewer.ui.navigation.Route;
import javafx.animation.Animation;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class RegisterController {

    private final UserService userService;
    private final JavaFxViewManager viewManager;
    private final GlobalExceptionHandler exceptionHandler;

    @FXML
    private Parent authShowcase;
    @FXML
    private TextField usernameField;
    @FXML
    private TextField nicknameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private PasswordField confirmPasswordField;
    @FXML
    private TextField visiblePasswordField;
    @FXML
    private TextField visibleConfirmPasswordField;
    @FXML
    private FontIcon passwordVisibilityIcon;
    @FXML
    private FontIcon confirmPasswordVisibilityIcon;
    @FXML
    private CheckBox agreementCheckBox;
    @FXML
    private Label errorLabel;

    private Animation illustrationMotion;
    private boolean passwordVisible;
    private boolean confirmPasswordVisible;

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
        visiblePasswordField.textProperty().bindBidirectional(passwordField.textProperty());
        visibleConfirmPasswordField.textProperty().bindBidirectional(confirmPasswordField.textProperty());
        illustrationMotion = AuthIllustrationMotion.start(authShowcase);
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

    @FXML
    private void togglePasswordVisibility() {
        passwordVisible = togglePasswordField(
                passwordVisible, passwordField, visiblePasswordField, passwordVisibilityIcon);
    }

    @FXML
    private void toggleConfirmPasswordVisibility() {
        confirmPasswordVisible = togglePasswordField(
                confirmPasswordVisible,
                confirmPasswordField,
                visibleConfirmPasswordField,
                confirmPasswordVisibilityIcon
        );
    }

    @FXML
    private void showLocalUsage() {
        viewManager.showInfo(
                "本地使用说明",
                "账户、简历、知识库、面试记录和报告默认保存在本机。配置 AI 服务后，请根据所选服务商的隐私条款使用。"
        );
    }

    private boolean togglePasswordField(
            boolean currentlyVisible,
            PasswordField hiddenField,
            TextField visibleField,
            FontIcon icon
    ) {
        boolean show = !currentlyVisible;
        hiddenField.setVisible(!show);
        hiddenField.setManaged(!show);
        visibleField.setVisible(show);
        visibleField.setManaged(show);
        icon.setIconLiteral(show ? "mdi2e-eye-outline" : "mdi2e-eye-off-outline");
        TextField activeField = show ? visibleField : hiddenField;
        activeField.requestFocus();
        activeField.positionCaret(activeField.getText().length());
        return show;
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
