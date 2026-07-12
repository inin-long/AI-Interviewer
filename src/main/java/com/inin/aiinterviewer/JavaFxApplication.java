package com.inin.aiinterviewer;

import com.inin.aiinterviewer.ui.navigation.JavaFxViewManager;
import com.inin.aiinterviewer.ui.navigation.Route;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public class JavaFxApplication extends Application {

    private ConfigurableApplicationContext applicationContext;

    public static void launchApplication(String[] args) {
        Application.launch(JavaFxApplication.class, args);
    }

    @Override
    public void init() {
        applicationContext = new SpringApplicationBuilder(AiInterviewerApplication.class)
                .headless(false)
                .run(getParameters().getRaw().toArray(String[]::new));
    }

    @Override
    public void start(Stage primaryStage) {
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            error.printStackTrace();
            Platform.runLater(() -> applicationContext
                    .getBean(JavaFxViewManager.class)
                    .showUnexpectedError(error));
        });

        JavaFxViewManager viewManager = applicationContext.getBean(JavaFxViewManager.class);
        viewManager.attachStage(primaryStage);
        viewManager.switchView(Route.LOGIN);
        primaryStage.show();
    }

    @Override
    public void stop() {
        if (applicationContext != null) {
            applicationContext.close();
        }
        Platform.exit();
    }
}

