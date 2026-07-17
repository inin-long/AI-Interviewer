package com.inin.aiinterviewer.ui;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;

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
}
