package com.inin.aiinterviewer.ui.navigation;

import com.inin.aiinterviewer.application.exception.GlobalExceptionHandler;
import com.inin.aiinterviewer.ui.component.AppDialog;
import com.inin.aiinterviewer.ui.component.AppDialogs;
import com.inin.aiinterviewer.ui.state.UserSessionState;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Screen;
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
        stage.setMinWidth(1200);
        stage.setMinHeight(680);
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
            Rectangle2D screen = Screen.getPrimary().getVisualBounds();
            double width, height;
            if (route == Route.LOGIN || route == Route.REGISTER) {
                // 登录/注册：需容纳左侧展示区(760) + 卡片(440) + 间距，约需 1380 宽
                width = Math.min(1400, screen.getWidth() * 0.92);
                height = Math.min(820, screen.getHeight() * 0.88);
            } else {
                // 主界面：大窗口，类似浏览器页面尺寸
                width = Math.min(1480, screen.getWidth() * 0.92);
                height = Math.min(1060, screen.getHeight() * 0.95);
            }
            Scene scene = new Scene(root, width, height);
            URL stylesheet = getClass().getResource("/css/app.css");
            if (stylesheet != null) {
                scene.getStylesheets().add(stylesheet.toExternalForm());
            }
            primaryStage.setScene(scene);
            primaryStage.setWidth(width);
            primaryStage.setHeight(height);
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
        URL location = classLoaderResource(route.fxmlPath());
        if (location == null) {
            throw new IOException("Missing FXML resource: " + route.fxmlPath());
        }
        FXMLLoader loader = new FXMLLoader(location);
        loader.setControllerFactory(applicationContext::getBean);
        return loader.load();
    }

    private URL classLoaderResource(String path) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = getClass().getClassLoader();
        }
        String normalized = path.startsWith("/") ? path.substring(1) : path;
        URL resource = classLoader.getResource(normalized);
        if (resource == null) {
            resource = getClass().getResource(path);
        }
        return resource;
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
