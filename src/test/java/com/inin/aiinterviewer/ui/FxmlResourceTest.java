package com.inin.aiinterviewer.ui;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class FxmlResourceTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "/fxml/login.fxml", "/fxml/register.fxml", "/fxml/main-window.fxml",
            "/fxml/dashboard-view.fxml", "/fxml/resume-view.fxml", "/fxml/resume-detail-view.fxml",
            "/fxml/profile-view.fxml", "/fxml/task-view.fxml", "/fxml/task-detail-view.fxml",
            "/fxml/plan-view.fxml", "/fxml/plan-editor-view.fxml",
            "/fxml/interview-workspace-view.fxml", "/fxml/report-detail-view.fxml",
            "/fxml/session-branch-view.fxml",
            "/fxml/knowledge-view.fxml", "/fxml/knowledge-detail-view.fxml",
            "/fxml/history-view.fxml", "/fxml/interview-history-detail-view.fxml",
            "/fxml/career-planning-view.fxml", "/fxml/career-plan-history-view.fxml",
            "/fxml/career-plan-detail-view.fxml", "/fxml/career-history-view.fxml",
            "/fxml/settings-view.fxml"
    })
    void fxmlResourceIsWellFormedAndDeclaresSpringController(String resource) throws Exception {
        try (InputStream input = getClass().getResourceAsStream(resource)) {
            assertThat(input).as("FXML resource %s", resource).isNotNull();
            var document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(input);
            String controller = document.getDocumentElement().getAttribute("fx:controller");
            assertThat(controller).startsWith("com.inin.aiinterviewer.ui.controller.");
        }
    }

    @Test
    void featureViewsUseSharedDialogsAndSelectsInsteadOfRawJavaFxControls() throws Exception {
        try (Stream<Path> fxmlFiles = Files.walk(Path.of("src/main/resources/fxml"));
             Stream<Path> javaFiles = Files.walk(Path.of("src/main/java"));
             Stream<Path> controllerFiles = Files.walk(Path.of("src/main/java/com/inin/aiinterviewer/ui/controller"))) {
            String fxml = readAll(fxmlFiles, ".fxml");
            String java = readAll(javaFiles, ".java");
            String controllers = readAll(controllerFiles, ".java");

            assertThat(fxml).doesNotContain("<ComboBox", "<ChoiceBox");
            assertThat(java).doesNotContain(
                    "new Alert(",
                    "new Dialog<", 
                    "new TextInputDialog(",
                    "new ChoiceDialog(",
                    "new ComboBox<",
                    "new javafx.scene.control.Alert(",
                    "new javafx.scene.control.Dialog<",
                    "new javafx.scene.control.TextInputDialog(",
                    "new javafx.scene.control.ChoiceDialog(",
                    "new javafx.scene.control.ComboBox<");
            assertThat(controllers).doesNotContain(
                    "ComboBox<",
                    "ChoiceBox<",
                    "import javafx.scene.control.ComboBox;",
                    "import javafx.scene.control.ChoiceBox;");
        }
    }

    private String readAll(Stream<Path> paths, String suffix) {
        return paths
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(suffix))
                .map(path -> {
                    try {
                        return Files.readString(path);
                    } catch (Exception exception) {
                        throw new IllegalStateException("Unable to read " + path, exception);
                    }
                })
                .reduce("", (left, right) -> left + "\n" + right);
    }
}
