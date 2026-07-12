package com.inin.aiinterviewer.ui.controller;

import com.inin.aiinterviewer.application.service.KnowledgeDocumentService;
import com.inin.aiinterviewer.ui.navigation.ContentNavigator;
import com.inin.aiinterviewer.ui.navigation.ContextAwareController;
import com.inin.aiinterviewer.ui.state.UserSessionState;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class KnowledgeDetailController implements ContextAwareController<Long> {

    private final KnowledgeDocumentService service;
    private final UserSessionState sessionState;
    private final ContentNavigator contentNavigator;

    @FXML private Label nameLabel;
    @FXML private Label metadataLabel;
    @FXML private Label statusLabel;
    @FXML private Label errorLabel;
    @FXML private TextArea chunksArea;

    public KnowledgeDetailController(
            KnowledgeDocumentService service,
            UserSessionState sessionState,
            ContentNavigator contentNavigator
    ) {
        this.service = service;
        this.sessionState = sessionState;
        this.contentNavigator = contentNavigator;
    }

    @Override
    public void initializeContext(Long documentId) {
        if (documentId == null) throw new IllegalArgumentException("Knowledge detail requires document id");
        var detail = service.detail(sessionState.requireCurrentUser().id(), documentId);
        var document = detail.document();
        nameLabel.setText(document.name());
        metadataLabel.setText(document.originalName() + " · " + document.category()
                + " · " + detail.chunks().size() + " 个片段");
        statusLabel.setText(document.status().name());
        errorLabel.setText(document.errorMessage() == null ? "" : document.errorMessage());
        errorLabel.setVisible(document.errorMessage() != null);
        errorLabel.setManaged(errorLabel.isVisible());
        StringBuilder content = new StringBuilder();
        for (int index = 0; index < detail.chunks().size(); index++) {
            content.append("## 片段 ").append(index + 1).append("\n")
                    .append(detail.chunks().get(index)).append("\n\n");
        }
        chunksArea.setText(content.toString().stripTrailing());
    }

    @FXML private void back() { contentNavigator.back(); }
}
