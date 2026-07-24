package com.inin.aiinterviewer.ui;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class UiDesignStandardTest {

    private static final Path FXML_DIRECTORY = Path.of("src/main/resources/fxml");
    private static final Path STYLESHEET = Path.of("src/main/resources/css/app.css");

    @Test
    void fxmlUsesSemanticClassesInsteadOfInlineOrCharacterIcons() throws IOException {
        try (Stream<Path> files = Files.list(FXML_DIRECTORY)) {
            List<Path> violations = files
                    .filter(path -> path.getFileName().toString().endsWith(".fxml"))
                    .filter(this::containsForbiddenPresentation)
                    .toList();

            assertThat(violations)
                    .as("FXML must use shared semantic classes and Material Design icons")
                    .isEmpty();
        }
    }

    @Test
    void lockedStylesheetDefinesRequiredTokensAndComponentFamilies() throws IOException {
        String css = Files.readString(STYLESHEET);

        assertThat(css)
                .contains("AI Interviewer UI Design Standard v1.0 — LOCKED")
                .contains("-app-background: #F8FAFD;")
                .contains("-primary: #5364F2;")
                .contains("-border: #E1E5EC;")
                .contains("-radius-card: 10;")
                .contains(".primary-button,")
                .contains(".secondary-button,")
                .contains(".status-badge")
                .contains(".back-icon-button");
    }

    @Test
    void visualTruthScreenshotsAreVersionedWithTheProject() {
        assertThat(Path.of("docs/ui-reference/ui-standard-resume.png")).exists();
        assertThat(Path.of("docs/ui-reference/ui-standard-skills.png")).exists();
        assertThat(Path.of("docs/product/ui-design.md")).exists();
    }

    private boolean containsForbiddenPresentation(Path path) {
        try {
            String fxml = Files.readString(path);
            return fxml.contains("style=\"")
                    || fxml.contains("text=\"←\"")
                    || fxml.contains("text=\"&lt; 返回\"");
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to inspect " + path, exception);
        }
    }
}
