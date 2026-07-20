package com.inin.aiinterviewer.ui;

import com.inin.aiinterviewer.application.dto.UserDto;
import com.inin.aiinterviewer.ui.state.UserSessionState;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Region;
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

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

@SpringBootTest
@EnabledOnOs(OS.WINDOWS)
class DashboardSnapshotTest {

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
    static void startJavaFx() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException alreadyStarted) {
            latch.countDown();
        }
        latch.await(10, TimeUnit.SECONDS);
    }

    @Test
    void captureDashboard() throws Exception {
        String outputPath = System.getProperty("dashboard.snapshot.path");
        assumeTrue(outputPath != null && !outputPath.isBlank());
        sessionState.logIn(new UserDto(1L, "snapshot-user", "inin", LocalDateTime.now()));
        FutureTask<Void> task = new FutureTask<>(() -> {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main-window.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();
            Scene scene = new Scene(root, 1672, 901);
            scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
            if (root instanceof Region region) region.resize(1672, 901);
            root.applyCss();
            root.layout();

            int width = 1672;
            int height = 901;
            WritableImage snapshot = new WritableImage(width, height);
            root.snapshot(null, snapshot);
            int[] pixels = new int[width * height];
            snapshot.getPixelReader().getPixels(
                    0, 0, width, height,
                    PixelFormat.getIntArgbPreInstance(), pixels, 0, width);
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB_PRE);
            image.setRGB(0, 0, width, height, pixels, 0, width);
            Path output = Path.of(outputPath);
            Files.createDirectories(output.getParent());
            ImageIO.write(image, "png", output.toFile());
            return null;
        });
        Platform.runLater(task);
        task.get(30, TimeUnit.SECONDS);
    }
}
