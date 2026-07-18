package com.inin.aiinterviewer.ui.controller;

import com.inin.aiinterviewer.application.exception.GlobalExceptionHandler;
import com.inin.aiinterviewer.application.service.UserService;
import com.inin.aiinterviewer.ui.animation.AuthIllustrationMotion;
import com.inin.aiinterviewer.ui.navigation.JavaFxViewManager;
import com.inin.aiinterviewer.ui.navigation.Route;
import javafx.animation.Animation;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.prefs.Preferences;

@Component
@Scope("prototype")
public class LoginController {

    private static final String REMEMBERED_USERNAME = "rememberedUsername";

    private final UserService userService;
    private final JavaFxViewManager viewManager;
    private final GlobalExceptionHandler exceptionHandler;
    private final Preferences preferences = Preferences.userNodeForPackage(LoginController.class);

    @FXML
    private Parent authShowcase;
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private TextField visiblePasswordField;
    @FXML
    private CheckBox rememberCheckBox;
    @FXML
    private Button passwordVisibilityButton;
    @FXML
    private FontIcon passwordVisibilityIcon;
    @FXML
    private Label errorLabel;

    private Animation illustrationMotion;
    private boolean passwordVisible;

    public LoginController(
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
        restoreRememberedUsername();
        illustrationMotion = AuthIllustrationMotion.start(authShowcase);
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    @FXML
    private void handleLogin() {
        clearError();
        try {
            userService.login(usernameField.getText(), passwordField.getText());
            persistRememberedUsername();
            passwordField.clear();
            viewManager.switchView(Route.DASHBOARD);
        } catch (RuntimeException exception) {
            showError(exceptionHandler.toUserMessage(exception));
        }
    }

    @FXML
    private void showRegister() {
        viewManager.switchView(Route.REGISTER);
    }

    @FXML
    private void togglePasswordVisibility() {
        passwordVisible = !passwordVisible;
        passwordField.setVisible(!passwordVisible);
        passwordField.setManaged(!passwordVisible);
        visiblePasswordField.setVisible(passwordVisible);
        visiblePasswordField.setManaged(passwordVisible);
        passwordVisibilityIcon.setIconLiteral(passwordVisible
                ? "mdi2e-eye-outline" : "mdi2e-eye-off-outline");
        passwordVisibilityButton.setAccessibleText(passwordVisible ? "隐藏密码" : "显示密码");
        TextField activeField = passwordVisible ? visiblePasswordField : passwordField;
        activeField.requestFocus();
        activeField.positionCaret(activeField.getText().length());
    }

    @FXML
    private void showPasswordHelp() {
        viewManager.showInfo(
                "本地账户密码",
                "本地账户不连接邮箱或云端，因此无法在线找回密码。你可以创建新的本地账户继续使用。"
        );
    }

    private void restoreRememberedUsername() {
        try {
            String remembered = preferences.get(REMEMBERED_USERNAME, "");
            usernameField.setText(remembered);
            rememberCheckBox.setSelected(true);
        } catch (SecurityException ignored) {
            rememberCheckBox.setSelected(true);
        }
    }

    private void persistRememberedUsername() {
        try {
            if (rememberCheckBox.isSelected()) {
                preferences.put(REMEMBERED_USERNAME, usernameField.getText().strip());
            } else {
                preferences.remove(REMEMBERED_USERNAME);
            }
        } catch (SecurityException ignored) {
            // The login flow remains available when the OS preference store is restricted.
        }
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
