package com.inin.aiinterviewer.ui;

import com.inin.aiinterviewer.application.dto.UserDto;
import com.inin.aiinterviewer.ui.state.UserSessionState;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
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
    }

    private Parent loadOnFxThread(String resource) throws Exception {
        FutureTask<Parent> task = new FutureTask<>(() -> {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(resource));
            loader.setControllerFactory(applicationContext::getBean);
            return loader.load();
        });
        Platform.runLater(task);
        return task.get(15, TimeUnit.SECONDS);
    }
}

