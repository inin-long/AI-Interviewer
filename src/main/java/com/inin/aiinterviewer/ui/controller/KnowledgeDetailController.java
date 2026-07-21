package com.inin.aiinterviewer.ui.controller;

import com.inin.aiinterviewer.application.dto.KnowledgeDetailDto;
import com.inin.aiinterviewer.application.dto.KnowledgeDocumentDto;
import com.inin.aiinterviewer.application.exception.GlobalExceptionHandler;
import com.inin.aiinterviewer.application.service.KnowledgeDocumentService;
import com.inin.aiinterviewer.ui.navigation.ContentNavigator;
import com.inin.aiinterviewer.domain.enums.KnowledgeStatus;
import com.inin.aiinterviewer.ui.navigation.ContextAwareController;
import com.inin.aiinterviewer.ui.state.UserSessionState;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Scope("prototype")
public class KnowledgeDetailController implements ContextAwareController<Long> {

    private final KnowledgeDocumentService service;
    private final UserSessionState sessionState;
    private final ContentNavigator contentNavigator;
    private final GlobalExceptionHandler exceptionHandler;
    private final com.inin.aiinterviewer.ui.navigation.JavaFxViewManager viewManager;

    @FXML private Label nameLabel;
    @FXML private TextField nameField;
    @FXML private Label metadataLabel;
    @FXML private Label statusLabel;
    @FXML private Label errorLabel;
    @FXML private TextArea chunksArea;
    @FXML private ComboBox<String> categoryBox;
    @FXML private HBox editActions;
    @FXML private Button enterEditButton;
    @FXML private Button saveButton;
    @FXML private Button cancelEditButton;
    @FXML private Label chunkTitle;
    @FXML private Label editHintLabel;

    private long currentDocId = -1;
    private List<Long> chunkIds = new ArrayList<>();
    private boolean editing = false;
    private String originalText = "";
    private String originalName = "";

    private static final String[] CATEGORIES = {"技术资料", "业务文档", "行业报告", "面试指南", "其他"};

    public KnowledgeDetailController(
            KnowledgeDocumentService service,
            UserSessionState sessionState,
            ContentNavigator contentNavigator,
            GlobalExceptionHandler exceptionHandler,
            com.inin.aiinterviewer.ui.navigation.JavaFxViewManager viewManager
    ) {
        this.service = service;
        this.sessionState = sessionState;
        this.contentNavigator = contentNavigator;
        this.exceptionHandler = exceptionHandler;
        this.viewManager = viewManager;
    }

    @Override
    public void initializeContext(Long documentId) {
        if (documentId == null) {
            viewManager.showError("缺少文档标识，无法打开详情。");
            return;
        }
        currentDocId = documentId;
        try {
            loadDetail();
        } catch (RuntimeException exception) {
            viewManager.showError(exceptionHandler.toUserMessage(exception));
        }
    }

    private void loadDetail() {
        var detail = service.detail(sessionState.requireCurrentUser().id(), currentDocId);
        var document = detail.document();

        nameLabel.setText(document.name());
        nameField.setText(document.name());
        originalName = document.name();

        metadataLabel.setText(document.originalName() + " · " + document.category()
                + " · " + detail.chunks().size() + " 个片段");
        statusLabel.setText(statusText(document.status()));
        categoryBox.setValue(document.category());

        errorLabel.setVisible(document.errorMessage() != null);
        errorLabel.setManaged(errorLabel.isVisible());
        errorLabel.setText(document.errorMessage() == null ? "" : document.errorMessage());

        // Load chunk IDs for later editing
        chunkIds = service.findChunkIds(currentDocId, sessionState.requireCurrentUser().id());

        StringBuilder content = new StringBuilder();
        for (int index = 0; index < detail.chunks().size(); index++) {
            content.append("## 片段 ").append(index + 1).append("\n")
                    .append(detail.chunks().get(index)).append("\n\n");
        }
        String text = content.toString().stripTrailing();
        chunksArea.setText(text);
        originalText = text;
    }

    /* ── 编辑模式切换 ── */

    @FXML
    private void enterEditMode() {
        editing = true;
        // Show editable fields, hide read-only ones
        nameLabel.setVisible(false);
        nameLabel.setManaged(false);
        nameField.setVisible(true);
        nameField.setManaged(true);

        categoryBox.setVisible(true);
        categoryBox.setManaged(true);

        editActions.setVisible(true);
        editActions.setManaged(true);
        enterEditButton.setVisible(false);
        enterEditButton.setManaged(false);

        chunksArea.setEditable(true);
        editHintLabel.setVisible(true);
        editHintLabel.setManaged(true);
        chunkTitle.setText("解析与切片内容（编辑模式）");

        nameField.requestFocus();
    }

    @FXML
    private void cancelEdit() {
        exitEditMode(false);
    }

    @FXML
    private void saveChanges() {
        try {
            long userId = sessionState.requireCurrentUser().id();

            // 1) Update metadata (name / category)
            String newName = nameField.getText().trim();
            String newCat = categoryBox.getValue();
            if (!newName.equals(originalName)) {
                service.updateMetadata(userId, currentDocId, newName, newCat);
                originalName = newName;
                nameLabel.setText(newName);
            } else if (newCat != null && !newCat.isBlank()) {
                service.updateMetadata(userId, currentDocId, newName, newCat);
            }

            // 2) Parse TextArea back into individual chunks and update each
            String editedText = chunksArea.getText();
            List<String> parsedChunks = parseChunksFromText(editedText);
            if (parsedChunks.size() != chunkIds.size()) {
                Alert warn = new Alert(Alert.AlertType.WARNING,
                        "片段数量已变化（原" + chunkIds.size() + " → 现" + parsedChunks.size()
                                + "）。将按顺序匹配更新前" + Math.min(parsedChunks.size(), chunkIds.size())
                                + "个片段，超出部分将被忽略。",
                        ButtonType.OK);
                warn.setHeaderText("片段数量不匹配");
                warn.showAndWait();
            }

            int updateCount = Math.min(parsedChunks.size(), chunkIds.size());
            for (int i = 0; i < updateCount; i++) {
                service.updateChunk(userId, chunkIds.get(i), parsedChunks.get(i));
            }
            originalText = editedText;

            exitEditMode(true);

            // Reload to reflect updated data
            loadDetail();
        } catch (RuntimeException ex) {
            viewManager.showError(exceptionHandler.toUserMessage(ex));
        }
    }

    private void exitEditMode(boolean saved) {
        editing = false;
        nameLabel.setVisible(true);
        nameLabel.setManaged(true);
        nameField.setVisible(false);
        nameField.setManaged(false);
        categoryBox.setVisible(false);
        categoryBox.setManaged(false);
        editActions.setVisible(false);
        editActions.setManaged(false);
        enterEditButton.setVisible(true);
        enterEditButton.setManaged(true);
        chunksArea.setEditable(false);
        editHintLabel.setVisible(false);
        editHintLabel.setManaged(false);
        chunkTitle.setText("解析与切片内容");
        if (!saved) {
            chunksArea.setText(originalText);
            nameField.setText(originalName);
        }
    }

    /**
     * Split the TextArea text back into individual chunk strings.
     * The format is: ## 片段 N\n<content>\n\n## 片段 N+1\n<content>\n\n...
     */
    private List<String> parseChunksFromText(String text) {
        List<String> result = new ArrayList<>();
        String[] parts = text.split("(?m)^##\\s*片段\\s+\\d+\\s*$");
        for (String part : parts) {
            String trimmed = part.strip();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result.isEmpty() ? List.of(text.strip()) : result;
    }

    @FXML
    private void back() { contentNavigator.back(); }

    /* ── 初始化分类下拉（仅编辑时可见）── */
    @FXML
    private void initialize() {
        categoryBox.getItems().setAll(CATEGORIES);
    }

    private String statusText(KnowledgeStatus status) {
        return switch (status) {
            case UPLOADED -> "已上传";
            case PARSING -> "解析中";
            case INDEXING -> "向量化中";
            case READY -> "已就绪";
            case FAILED -> "失败";
        };
    }
}
