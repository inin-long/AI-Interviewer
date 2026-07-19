package com.inin.aiinterviewer.ui.navigation;

import com.inin.aiinterviewer.application.exception.GlobalExceptionHandler;
import com.inin.aiinterviewer.ui.component.AppDialog;
import com.inin.aiinterviewer.ui.component.AppDialogs;
import com.inin.aiinterviewer.ui.state.UserSessionState;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

@Component
public class JavaFxViewManager implements ViewManager {

    private final ApplicationContext applicationContext;
    private final UserSessionState sessionState;
    private final GlobalExceptionHandler exceptionHandler;
    private Stage primaryStage;

    public JavaFxViewManager(
            ApplicationContext applicationContext,
            UserSessionState sessionState,
            GlobalExceptionHandler exceptionHandler
    ) {
        this.applicationContext = applicationContext;
        this.sessionState = sessionState;
        this.exceptionHandler = exceptionHandler;
    }

    public void attachStage(Stage stage) {
        this.primaryStage = Objects.requireNonNull(stage, "stage");
        stage.setMinWidth(1024);
        stage.setMinHeight(720);
        URL icon = getClass().getResource("/images/home/app-icon.png");
        if (icon != null) {
            stage.getIcons().setAll(new Image(icon.toExternalForm()));
        }
    }

    @Override
    public void switchView(Route requestedRoute) {
        requireStage();
        Route route = authorize(requestedRoute);
        if (!route.isImplemented()) {
            showInfo(route.title(), "该功能将在后续里程碑中实现。");
            return;
        }

        try {
            Parent root = load(route);
            Scene scene = primaryStage.getScene();
            if (scene == null) {
                scene = new Scene(root, 1440, 820);
                URL stylesheet = getClass().getResource("/css/app.css");
                if (stylesheet != null) {
                    scene.getStylesheets().add(stylesheet.toExternalForm());
                }
                primaryStage.setScene(scene);
            } else {
                scene.setRoot(root);
            }
            primaryStage.setTitle("AI Interviewer");
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load view: " + route, exception);
        }
    }

    public void showUnexpectedError(Throwable throwable) {
        showError(exceptionHandler.toUserMessage(throwable));
    }

    public void showError(String message) {
        AppDialogs.showMessage(primaryStage, "操作失败", "操作未能完成", message, AppDialog.Tone.DANGER);
    }

    public void showInfo(String title, String message) {
        AppDialogs.showMessage(primaryStage, title, title, message, AppDialog.Tone.INFORMATION);
    }

    public void maximizePrimaryStage() {
        requireStage();
        primaryStage.setMaximized(true);
    }

    private Parent load(Route route) throws IOException {
        URL location = Objects.requireNonNull(getClass().getResource(route.fxmlPath()),
                () -> "Missing FXML resource: " + route.fxmlPath());
        FXMLLoader loader = new FXMLLoader(location);
        loader.setControllerFactory(applicationContext::getBean);
        return loader.load();
    }

    private Route authorize(Route requestedRoute) {
        boolean publicRoute = requestedRoute == Route.LOGIN || requestedRoute == Route.REGISTER;
        if (!publicRoute && sessionState.currentUser().isEmpty()) {
            return Route.LOGIN;
        }
        return requestedRoute;
    }

    private void requireStage() {
        if (primaryStage == null) {
            throw new IllegalStateException("Primary JavaFX stage is not attached");
        }
    }
}
