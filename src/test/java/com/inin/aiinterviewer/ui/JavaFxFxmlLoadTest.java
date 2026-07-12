package com.inin.aiinterviewer.ui;

import com.inin.aiinterviewer.application.dto.UserDto;
import com.inin.aiinterviewer.ui.state.UserSessionState;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.css.PseudoClass;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EnabledOnOs(OS.WINDOWS)
class JavaFxFxmlLoadTest {

    @TempDir
    static Path applicationHome;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private UserSessionState sessionState;

    @DynamicPropertySource
    static void applicationProperties(DynamicPropertyRegistry registry) {
        registry.add("ai.interviewer.home", () -> applicationHome.toString());
    }

    @BeforeAll
    static void startJavaFxToolkit() throws InterruptedException {
        CountDownLatch started = new CountDownLatch(1);
        Platform.startup(started::countDown);
        assertThat(started.await(10, TimeUnit.SECONDS)).isTrue();
    }

    @AfterAll
    static void stopJavaFxToolkit() {
        Platform.exit();
    }

    @Test
    void loadsPublicAuthenticationViewsWithSpringControllers() throws Exception {
        assertThat(loadOnFxThread("/fxml/login.fxml")).isNotNull();
        assertThat(loadOnFxThread("/fxml/register.fxml")).isNotNull();
    }

    @Test
    void loadsMainWindowForAuthenticatedUser() throws Exception {
        sessionState.logIn(new UserDto(1L, "test-user", "测试用户", LocalDateTime.now()));
        assertThat(loadOnFxThread("/fxml/main-window.fxml")).isNotNull();
        assertThat(loadOnFxThread("/fxml/resume-view.fxml")).isNotNull();
        assertThat(loadOnFxThread("/fxml/resume-detail-view.fxml")).isNotNull();
        assertThat(loadOnFxThread("/fxml/plan-view.fxml")).isNotNull();
        assertThat(loadOnFxThread("/fxml/plan-editor-view.fxml")).isNotNull();
        assertThat(loadOnFxThread("/fxml/interview-workspace-view.fxml")).isNotNull();
        assertThat(loadOnFxThread("/fxml/report-detail-view.fxml")).isNotNull();
        assertThat(loadOnFxThread("/fxml/knowledge-view.fxml")).isNotNull();
        assertThat(loadOnFxThread("/fxml/knowledge-detail-view.fxml")).isNotNull();
        assertThat(loadOnFxThread("/fxml/history-view.fxml")).isNotNull();
        assertThat(loadOnFxThread("/fxml/interview-history-detail-view.fxml")).isNotNull();
        assertThat(loadOnFxThread("/fxml/settings-view.fxml")).isNotNull();
    }

    @Test
    void selectAndTextInputHaveMatchingRenderedHeight() throws Exception {
        if (sessionState.currentUser().isEmpty()) {
            sessionState.logIn(new UserDto(1L, "style-test-user", "样式测试用户", LocalDateTime.now()));
        }
        FutureTask<double[]> task = new FutureTask<>(() -> {
            Parent root = load("/fxml/plan-editor-view.fxml");
            Scene scene = new Scene(root, 1200, 800);
            scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
            root.applyCss();
            root.layout();
            ComboBox<?> difficulty = (ComboBox<?>) root.lookup("#difficultyBox");
            TextField name = (TextField) root.lookup("#nameField");
            return new double[]{difficulty.getHeight(), name.getHeight()};
        });
        Platform.runLater(task);
        double[] heights = task.get(15, TimeUnit.SECONDS);
        assertThat(heights[0]).isEqualTo(52.0);
        assertThat(heights[0]).isEqualTo(heights[1]);
    }

    @Test
    void sidebarSpansWindowAndNavigationTracksActiveRoute() throws Exception {
        if (sessionState.currentUser().isEmpty()) {
            sessionState.logIn(new UserDto(1L, "navigation-test-user", "导航测试用户", LocalDateTime.now()));
        }
        FutureTask<boolean[]> task = new FutureTask<>(() -> {
            BorderPane root = (BorderPane) load("/fxml/main-window.fxml");
            Scene scene = new Scene(root, 1200, 800);
            scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
            root.applyCss();
            root.layout();
            Button dashboard = (Button) root.lookup("#dashboardNavButton");
            Button plans = (Button) root.lookup("#plansNavButton");
            Button settings = (Button) root.lookup("#settingsNavButton");
            PseudoClass selected = PseudoClass.getPseudoClass("selected");
            boolean initialSelection = dashboard.getPseudoClassStates().contains(selected);
            plans.fire();
            boolean switchedSelection = plans.getPseudoClassStates().contains(selected)
                    && !dashboard.getPseudoClassStates().contains(selected);
            boolean fullHeight = root.getTop() == null
                    && Math.abs(root.getLeft().getBoundsInParent().getHeight() - root.getHeight()) < 0.5;
            boolean topbarInsideContent = root.getCenter() instanceof BorderPane content
                    && content.getTop() != null;
            settings.fire();
            boolean settingsLoaded = settings.getPseudoClassStates().contains(selected)
                    && !plans.getPseudoClassStates().contains(selected)
                    && root.lookup("#generalNavButton") != null;
            return new boolean[]{initialSelection, switchedSelection, fullHeight,
                    topbarInsideContent, settingsLoaded};
        });
        Platform.runLater(task);
        assertThat(task.get(15, TimeUnit.SECONDS)).containsOnly(true);
    }

    @Test
    void settingsMasksSecretAndSwitchesCategoryWithActiveState() throws Exception {
        if (sessionState.currentUser().isEmpty()) {
            sessionState.logIn(new UserDto(1L, "settings-test-user", "设置测试用户", LocalDateTime.now()));
        }
        FutureTask<boolean[]> task = new FutureTask<>(() -> {
            Parent root = load("/fxml/settings-view.fxml");
            Scene scene = new Scene(root, 1100, 760);
            scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
            root.applyCss();
            root.layout();
            Button general = (Button) root.lookup("#generalNavButton");
            Button ai = (Button) root.lookup("#aiNavButton");
            VBox generalPane = (VBox) root.lookup("#generalPane");
            VBox aiPane = (VBox) root.lookup("#aiPane");
            PasswordField key = (PasswordField) root.lookup("#apiKeyField");
            PseudoClass selected = PseudoClass.getPseudoClass("selected");
            boolean initial = general.getPseudoClassStates().contains(selected)
                    && generalPane.isVisible() && !aiPane.isVisible();
            ai.fire();
            boolean switched = ai.getPseudoClassStates().contains(selected)
                    && !general.getPseudoClassStates().contains(selected)
                    && aiPane.isVisible() && !generalPane.isVisible();
            boolean masked = key.getText() == null || !key.getText().startsWith("sk-");
            return new boolean[]{initial, switched, masked};
        });
        Platform.runLater(task);
        assertThat(task.get(15, TimeUnit.SECONDS)).containsOnly(true);
    }

    private Parent loadOnFxThread(String resource) throws Exception {
        FutureTask<Parent> task = new FutureTask<>(() -> {
            return load(resource);
        });
        Platform.runLater(task);
        return task.get(15, TimeUnit.SECONDS);
    }

    private Parent load(String resource) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(resource));
        loader.setControllerFactory(applicationContext::getBean);
        return loader.load();
    }
}
