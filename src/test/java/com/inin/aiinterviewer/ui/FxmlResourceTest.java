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
            "/fxml/plan-view.fxml", "/fxml/plan-editor-view.fxml",
            "/fxml/interview-workspace-view.fxml"
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
