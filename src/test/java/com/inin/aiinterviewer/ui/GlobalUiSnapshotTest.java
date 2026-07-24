package com.inin.aiinterviewer.ui;

import com.inin.aiinterviewer.application.dto.UserDto;
import com.inin.aiinterviewer.application.service.UserService;
import com.inin.aiinterviewer.ui.state.UserSessionState;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

@SpringBootTest
@EnabledOnOs(OS.WINDOWS)
class GlobalUiSnapshotTest {

    private static final int WIDTH = 1672;
    private static final int HEIGHT = 901;

    @TempDir
    static Path applicationHome;

    @Autowired private ApplicationContext applicationContext;
    @Autowired private UserService userService;
    @Autowired private UserSessionState sessionState;

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
    void captureReferenceAndRepresentativeRoutes() throws Exception {
        String outputDirectory = System.getProperty("global.ui.snapshot.dir");
        assumeTrue(outputDirectory != null && !outputDirectory.isBlank());

        UserDto user = userService.register("global-ui-snapshot", "inin", "Snapshot123!");
        sessionState.logIn(user);

        FutureTask<Void> capture = new FutureTask<>(() -> {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main-window.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();
            Scene scene = new Scene(root, WIDTH, HEIGHT);
            scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
            if (root instanceof Region region) region.resize(WIDTH, HEIGHT);

            Path directory = Path.of(outputDirectory);
            captureRoute(root, "#skillsLibraryNavButton", directory.resolve("skills-library.png"));
            captureRoute(root, "#questionBankNavButton", directory.resolve("question-bank.png"));
            captureRoute(root, "#settingsNavButton", directory.resolve("settings.png"));
            return null;
        });
        Platform.runLater(capture);
        capture.get(45, TimeUnit.SECONDS);
    }

    private void captureRoute(Parent root, String navigationButton, Path output) throws Exception {
        root.applyCss();
        root.layout();
        Button button = (Button) root.lookup(navigationButton);
        if (button == null) {
            throw new IllegalStateException("Missing navigation button: " + navigationButton);
        }
        button.fire();
        root.applyCss();
        root.layout();
        writeSnapshot(root, output);
    }

    private void writeSnapshot(Parent root, Path output) throws Exception {
        WritableImage snapshot = new WritableImage(WIDTH, HEIGHT);
        root.snapshot(null, snapshot);
        int[] pixels = new int[WIDTH * HEIGHT];
        snapshot.getPixelReader().getPixels(
                0, 0, WIDTH, HEIGHT,
                PixelFormat.getIntArgbPreInstance(), pixels, 0, WIDTH);
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB_PRE);
        image.setRGB(0, 0, WIDTH, HEIGHT, pixels, 0, WIDTH);
        Files.createDirectories(output.getParent());
        ImageIO.write(image, "png", output.toFile());
    }
}
