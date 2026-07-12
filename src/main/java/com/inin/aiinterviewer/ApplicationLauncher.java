package com.inin.aiinterviewer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Plain JVM entry point. Keeping this class separate from JavaFX Application
 * also makes classpath-based packaging with jpackage reliable.
 */
public final class ApplicationLauncher {

    public static final String HOME_SYSTEM_PROPERTY = "ai.interviewer.home";

    private ApplicationLauncher() {
    }

    public static void main(String[] args) {
        Path home = resolveApplicationHome();
        createRuntimeDirectories(home);
        System.setProperty(HOME_SYSTEM_PROPERTY, home.toString());
        JavaFxApplication.launchApplication(args);
    }

    static Path resolveApplicationHome() {
        String explicitHome = System.getenv("AI_INTERVIEWER_HOME");
        if (explicitHome != null && !explicitHome.isBlank()) {
            return Path.of(explicitHome).toAbsolutePath().normalize();
        }

        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData != null && !localAppData.isBlank()) {
            return Path.of(localAppData, "AI-Interviewer").toAbsolutePath().normalize();
        }

        String userHome = System.getProperty("user.home");
        String directoryName = System.getProperty("os.name", "")
                .toLowerCase(Locale.ROOT)
                .contains("win") ? "AI-Interviewer" : ".ai-interviewer";
        return Path.of(userHome, directoryName).toAbsolutePath().normalize();
    }

    static void createRuntimeDirectories(Path home) {
        try {
            Files.createDirectories(home.resolve("database"));
            Files.createDirectories(home.resolve("users"));
            Files.createDirectories(home.resolve("logs"));
            Files.createDirectories(home.resolve("temp"));
            Files.createDirectories(home.resolve("config"));
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot initialize application data directory: " + home, exception);
        }
    }
}

