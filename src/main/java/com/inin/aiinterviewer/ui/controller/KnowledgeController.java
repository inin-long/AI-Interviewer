package com.inin.aiinterviewer.ui.controller;

import com.inin.aiinterviewer.application.dto.KnowledgeDocumentDto;
import com.inin.aiinterviewer.application.exception.GlobalExceptionHandler;
import com.inin.aiinterviewer.application.service.KnowledgeDocumentService;
import com.inin.aiinterviewer.application.service.KnowledgeDocumentTaskService;
import com.inin.aiinterviewer.ui.navigation.ContentNavigator;
import com.inin.aiinterviewer.ui.navigation.JavaFxViewManager;
import com.inin.aiinterviewer.ui.dialog.FileDialogService;
import com.inin.aiinterviewer.ui.state.UserSessionState;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
@Scope("prototype")
public class KnowledgeController {

    private final KnowledgeDocumentService knowledgeService;
    private final KnowledgeDocumentTaskService knowledgeTaskService;
    private final UserSessionState sessionState;
    private final ContentNavigator contentNavigator;
    private final FileDialogService fileDialogService;
    private final JavaFxViewManager viewManager;
    private final GlobalExceptionHandler exceptionHandler;

    @FXML private TableView<KnowledgeDocumentDto> documentTable;
    @FXML private TableColumn<KnowledgeDocumentDto, String> nameColumn;
    @FXML private TableColumn<KnowledgeDocumentDto, String> categoryColumn;
    @FXML private TableColumn<KnowledgeDocumentDto, String> typeColumn;
    @FXML private TableColumn<KnowledgeDocumentDto, String> statusColumn;
    @FXML private TableColumn<KnowledgeDocumentDto, String> sizeColumn;
    @FXML private TextField searchField;
    @FXML private TextArea searchResultArea;
    @FXML private Label summaryLabel;
    @FXML private Button uploadButton;
    @FXML private Button viewButton;
    @FXML private Button deleteButton;

    public KnowledgeController(
            KnowledgeDocumentService knowledgeService,
            KnowledgeDocumentTaskService knowledgeTaskService,
            UserSessionState sessionState,
            ContentNavigator contentNavigator,
            FileDialogService fileDialogService,
            JavaFxViewManager viewManager,
            GlobalExceptionHandler exceptionHandler
    ) {
        this.knowledgeService = knowledgeService;
        this.knowledgeTaskService = knowledgeTaskService;
        this.sessionState = sessionState;
        this.contentNavigator = contentNavigator;
        this.fileDialogService = fileDialogService;
        this.viewManager = viewManager;
        this.exceptionHandler = exceptionHandler;
    }

    @FXML
    private void initialize() {
        nameColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().name()));
        categoryColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().category()));
        typeColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().fileType().toUpperCase()));
        statusColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(statusText(cell.getValue())));
        sizeColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(sizeText(cell.getValue().fileSize())));
        viewButton.disableProperty().bind(documentTable.getSelectionModel().selectedItemProperty().isNull());
        deleteButton.disableProperty().bind(documentTable.getSelectionModel().selectedItemProperty().isNull());
        documentTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && documentTable.getSelectionModel().getSelectedItem() != null) viewSelected();
        });
        refresh();
    }

    @FXML
    private void upload() {
        Path selected = fileDialogService.chooseKnowledgeDocument(
                documentTable.getScene().getWindow()).orElse(null);
        if (selected == null) return;
        Task<KnowledgeDocumentTaskService.QueuedKnowledgeDocument> task = new Task<>() {
            @Override protected KnowledgeDocumentTaskService.QueuedKnowledgeDocument call() {
                return knowledgeTaskService.uploadAndEnqueue(userId(), selected, "技术资料");
            }
        };
        uploadButton.setDisable(true);
        summaryLabel.setText("正在保存文档并创建后台任务…");
        task.setOnSucceeded(event -> {
            uploadButton.setDisable(false);
            refresh();
            summaryLabel.setText("文档已加入后台处理队列，可继续使用其他功能");
        });
        task.setOnFailed(event -> {
            uploadButton.setDisable(false);
            refresh();
            viewManager.showError(exceptionHandler.toUserMessage(task.getException()));
        });
        Thread worker = new Thread(task, "knowledge-document-indexing");
        worker.setDaemon(true);
        worker.start();
    }

    @FXML
    private void search() {
        try {
            var results = knowledgeService.search(userId(), searchField.getText(), 5);
            if (results.isEmpty()) {
                searchResultArea.setText("没有找到相关知识片段。");
                return;
            }
            StringBuilder text = new StringBuilder();
            for (var result : results) {
                text.append(result.documentName()).append(" · 相似度 ")
                        .append(String.format("%.3f", result.score())).append("\n")
                        .append(result.content()).append("\n\n");
            }
            searchResultArea.setText(text.toString().stripTrailing());
        } catch (RuntimeException exception) {
            viewManager.showError(exceptionHandler.toUserMessage(exception));
        }
    }

    @FXML
    private void viewSelected() {
        KnowledgeDocumentDto selected = documentTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            contentNavigator.showSubPage(
                    "/fxml/knowledge-detail-view.fxml", "知识文档", selected.id());
        }
    }

    @FXML
    private void deleteSelected() {
        KnowledgeDocumentDto selected = documentTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                "删除知识文档 “" + selected.name() + "” 及其向量索引？", ButtonType.CANCEL, ButtonType.OK);
        confirmation.setHeaderText("确认删除知识文档");
        if (confirmation.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
        try {
            knowledgeService.delete(userId(), selected.id());
            refresh();
        } catch (RuntimeException exception) {
            viewManager.showError(exceptionHandler.toUserMessage(exception));
        }
    }

    @FXML
    private void refresh() {
        var documents = knowledgeService.list(userId());
        documentTable.getItems().setAll(documents);
        long ready = documents.stream().filter(item -> item.status().name().equals("READY")).count();
        summaryLabel.setText("共 " + documents.size() + " 个文档，" + ready + " 个已就绪");
    }

    private long userId() { return sessionState.requireCurrentUser().id(); }

    private String statusText(KnowledgeDocumentDto dto) {
        return switch (dto.status()) {
            case UPLOADED -> "已上传";
            case PARSING -> "解析中";
            case INDEXING -> "向量化中";
            case READY -> "已就绪";
            case FAILED -> "失败";
        };
    }

    private String sizeText(long bytes) {
        return bytes < 1024 * 1024 ? String.format("%.1f KB", bytes / 1024.0)
                : String.format("%.1f MB", bytes / 1024.0 / 1024.0);
    }
}
