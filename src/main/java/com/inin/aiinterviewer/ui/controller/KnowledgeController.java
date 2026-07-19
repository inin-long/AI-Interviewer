package com.inin.aiinterviewer.ui.controller;

import com.inin.aiinterviewer.application.dto.KnowledgeCategoryDto;
import com.inin.aiinterviewer.application.dto.KnowledgeDetailDto;
import com.inin.aiinterviewer.application.dto.KnowledgeDocumentDto;
import com.inin.aiinterviewer.application.exception.GlobalExceptionHandler;
import com.inin.aiinterviewer.application.service.KnowledgeDocumentService;
import com.inin.aiinterviewer.application.service.KnowledgeDocumentTaskService;
import com.inin.aiinterviewer.domain.enums.KnowledgeStatus;
import com.inin.aiinterviewer.ui.component.AppDialog;
import com.inin.aiinterviewer.ui.component.AppDialogs;
import com.inin.aiinterviewer.ui.component.AppSelect;
import com.inin.aiinterviewer.ui.component.DrawerPane;
import com.inin.aiinterviewer.ui.dialog.FileDialogService;
import com.inin.aiinterviewer.ui.navigation.JavaFxViewManager;
import com.inin.aiinterviewer.ui.state.UserSessionState;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.util.StringConverter;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Predicate;

@Component
@Scope("prototype")
public class KnowledgeController {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final KnowledgeDocumentService knowledgeService;
    private final KnowledgeDocumentTaskService knowledgeTaskService;
    private final UserSessionState sessionState;
    private final FileDialogService fileDialogService;
    private final JavaFxViewManager viewManager;
    private final GlobalExceptionHandler exceptionHandler;
    private final ObservableList<KnowledgeDocumentDto> allDocuments = FXCollections.observableArrayList();
    private final ObservableList<KnowledgeCategoryDto> allCategories = FXCollections.observableArrayList();

    @FXML private ListView<CategoryFilterItem> categoryList;
    @FXML private ListView<KnowledgeDocumentDto> documentList;
    @FXML private TextField categorySearchField;
    @FXML private TextField documentSearchField;
    @FXML private AppSelect<SortOption> sortBox;
    @FXML private ToggleGroup statusToggleGroup;
    @FXML private ToggleButton allStatusButton;
    @FXML private ToggleButton readyStatusButton;
    @FXML private ToggleButton processingStatusButton;
    @FXML private ToggleButton failedStatusButton;
    @FXML private Label summaryLabel;
    @FXML private Label totalCountLabel;
    @FXML private Label totalHintLabel;
    @FXML private Label readyCountLabel;
    @FXML private Label readyHintLabel;
    @FXML private Label categoryCountLabel;
    @FXML private Label processingCountLabel;
    @FXML private Label failedCountLabel;
    @FXML private Button uploadButton;
    @FXML private DrawerPane documentDrawer;

    private String selectedCategory;

    public KnowledgeController(
            KnowledgeDocumentService knowledgeService,
            KnowledgeDocumentTaskService knowledgeTaskService,
            UserSessionState sessionState,
            FileDialogService fileDialogService,
            JavaFxViewManager viewManager,
            GlobalExceptionHandler exceptionHandler
    ) {
        this.knowledgeService = knowledgeService;
        this.knowledgeTaskService = knowledgeTaskService;
        this.sessionState = sessionState;
        this.fileDialogService = fileDialogService;
        this.viewManager = viewManager;
        this.exceptionHandler = exceptionHandler;
    }

    @FXML
    private void initialize() {
        categoryList.setFixedCellSize(45);
        categoryList.setCellFactory(ignored -> new CategoryCell());
        documentList.setFixedCellSize(99);
        documentList.setCellFactory(ignored -> new DocumentCell());
        documentList.setOnMouseClicked(event -> {
            if (event.getButton() != MouseButton.PRIMARY || event.getClickCount() != 1
                    || eventTargetInsideButton(event.getTarget())) return;
            KnowledgeDocumentDto selected = documentList.getSelectionModel().getSelectedItem();
            if (selected != null) showDocument(selected);
        });

        sortBox.getItems().setAll(SortOption.values());
        sortBox.setConverter(new StringConverter<>() {
            @Override public String toString(SortOption value) { return value == null ? "" : value.label; }
            @Override public SortOption fromString(String value) { return null; }
        });
        sortBox.setValue(SortOption.UPDATED_DESC);

        categorySearchField.textProperty().addListener((observable, previous, current) -> refreshCategoryFilters());
        documentSearchField.textProperty().addListener((observable, previous, current) -> applyFilters());
        sortBox.valueProperty().addListener((observable, previous, current) -> applyFilters());
        categoryList.getSelectionModel().selectedItemProperty().addListener((observable, previous, current) -> {
            selectedCategory = current == null || current.allDocuments ? null : current.name;
            applyFilters();
        });
        statusToggleGroup.selectedToggleProperty().addListener((observable, previous, current) -> {
            if (current == null) {
                allStatusButton.setSelected(true);
            } else {
                applyFilters();
            }
        });
        refresh();
    }

    @FXML
    private void createCategory() {
        showCreateCategoryDialog(documentList.getScene().getWindow()).ifPresent(category -> {
            refresh();
            selectCategory(category.name());
        });
    }

    @FXML
    private void upload() {
        if (allCategories.isEmpty()) {
            viewManager.showInfo("请先新建分类", "上传文档前必须选择分类，知识库不提供“未分类”选项。");
            return;
        }
        Path selected = fileDialogService.chooseKnowledgeDocument(
                documentList.getScene().getWindow()).orElse(null);
        if (selected == null) return;
        String category = chooseUploadCategory(selected).orElse(null);
        if (category == null) return;

        Task<KnowledgeDocumentTaskService.QueuedKnowledgeDocument> task = new Task<>() {
            @Override protected KnowledgeDocumentTaskService.QueuedKnowledgeDocument call() {
                return knowledgeTaskService.uploadAndEnqueue(userId(), selected, category);
            }
        };
        uploadButton.setDisable(true);
        summaryLabel.setText("正在保存文档并创建后台任务…");
        task.setOnSucceeded(event -> {
            uploadButton.setDisable(false);
            refresh();
            selectCategory(category);
            summaryLabel.setText("文档已加入后台处理队列 · 分类：" + category);
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
    private void refresh() {
        try {
            allDocuments.setAll(knowledgeService.list(userId()));
            allCategories.setAll(knowledgeService.listCategories(userId()));
            updateStats();
            refreshCategoryFilters();
            applyFilters();
        } catch (RuntimeException exception) {
            viewManager.showError(exceptionHandler.toUserMessage(exception));
        }
    }

    private Optional<KnowledgeCategoryDto> showCreateCategoryDialog(javafx.stage.Window owner) {
        return AppDialogs.textInput(
                owner,
                "新建分类",
                "创建文档分类",
                "分类名称",
                "例如：技术资料、项目文档、学习笔记"
        ).flatMap(name -> {
            try {
                return Optional.of(knowledgeService.createCategory(userId(), name));
            } catch (RuntimeException exception) {
                viewManager.showError(exceptionHandler.toUserMessage(exception));
                return Optional.empty();
            }
        });
    }

    private Optional<String> chooseUploadCategory(Path selectedFile) {
        AppDialog<String> dialog = new AppDialog<>(
                documentList.getScene().getWindow(),
                "上传文档",
                "为文档选择分类",
                "分类为必选项，面试方案将按分类自动使用其中已就绪的文档。",
                AppDialog.Tone.INFORMATION);

        Label fileName = new Label(selectedFile.getFileName().toString());
        fileName.getStyleClass().add("upload-file-name");
        AppSelect<String> categoryBox = new AppSelect<>();
        categoryBox.setPromptText("请选择分类（必选）");
        categoryBox.setMaxWidth(Double.MAX_VALUE);
        categoryBox.getItems().setAll(allCategories.stream().map(KnowledgeCategoryDto::name).toList());
        Button create = new Button("新建分类");
        create.getStyleClass().add("secondary-button");
        create.setOnAction(event -> showCreateCategoryDialog(categoryBox.getScene().getWindow()).ifPresent(category -> {
            if (!categoryBox.getItems().contains(category.name())) categoryBox.getItems().add(category.name());
            categoryBox.setValue(category.name());
            allCategories.setAll(knowledgeService.listCategories(userId()));
        }));
        HBox categoryRow = new HBox(10, categoryBox, create);
        HBox.setHgrow(categoryBox, Priority.ALWAYS);
        VBox content = new VBox(8,
                new Label("已选择文档"), fileName,
                new Label("文档分类"), categoryRow);
        dialog.setBody(content);

        dialog.addCancelAction("取消");
        Button confirm = dialog.addAction("上传", categoryBox::getValue, AppDialog.ActionStyle.PRIMARY);
        confirm.disableProperty().bind(categoryBox.valueProperty().isNull());
        dialog.setInitialFocus(categoryBox);
        return dialog.showAndWait();
    }

    private void showDocument(KnowledgeDocumentDto document) {
        try {
            KnowledgeDetailDto detail = knowledgeService.detail(userId(), document.id());
            documentDrawer.open(document.name(), createDrawerContent(detail));
        } catch (RuntimeException exception) {
            viewManager.showError(exceptionHandler.toUserMessage(exception));
        }
    }

    private Node createDrawerContent(KnowledgeDetailDto detail) {
        KnowledgeDocumentDto document = detail.document();
        FontIcon fileIcon = new FontIcon(fileIconLiteral(document.fileType()));
        fileIcon.setIconSize(32);
        fileIcon.getStyleClass().addAll("knowledge-drawer-file-icon", fileTypeStyle(document.fileType()));
        Label category = new Label(document.category());
        category.getStyleClass().add("knowledge-category-chip");
        Label metadata = new Label(document.fileType().toUpperCase(Locale.ROOT) + " · "
                + sizeText(document.fileSize()) + " · " + detail.chunks().size() + " 个片段");
        metadata.getStyleClass().add("secondary-text");
        VBox identity = new VBox(7, category, metadata);
        HBox fileSummary = new HBox(14, fileIcon, identity);
        fileSummary.setAlignment(Pos.CENTER_LEFT);
        fileSummary.getStyleClass().add("knowledge-drawer-summary");

        GridPane overview = new GridPane();
        overview.setHgap(12);
        overview.setVgap(12);
        overview.getColumnConstraints().setAll(
                percentColumn(50), percentColumn(50));
        overview.add(infoCard("索引状态", statusText(document), "mdi2c-check-circle-outline"), 0, 0);
        overview.add(infoCard("文档分类", document.category(), "mdi2t-tag-outline"), 1, 0);
        overview.add(infoCard("文件大小", sizeText(document.fileSize()), "mdi2f-file-outline"), 0, 1);
        overview.add(infoCard("更新时间", timeText(document.updateTime()), "mdi2c-clock-outline"), 1, 1);

        VBox overviewTabContent = new VBox(14, fileSummary, overview);
        if (document.errorMessage() != null && !document.errorMessage().isBlank()) {
            Label error = new Label(document.errorMessage());
            error.setWrapText(true);
            error.getStyleClass().add("form-error");
            overviewTabContent.getChildren().add(error);
        }
        overviewTabContent.setPadding(new Insets(18));

        TextArea contentArea = new TextArea(renderChunks(detail.chunks()));
        contentArea.setEditable(false);
        contentArea.setWrapText(true);
        contentArea.getStyleClass().add("knowledge-chunk-content");
        VBox.setVgrow(contentArea, Priority.ALWAYS);
        VBox contentTabContent = new VBox(contentArea);
        contentTabContent.setPadding(new Insets(14, 16, 16, 16));

        ListView<String> chunkList = new ListView<>();
        chunkList.getItems().setAll(detail.chunks());
        chunkList.setCellFactory(ignored -> new ListCell<>() {
            @Override protected void updateItem(String chunk, boolean empty) {
                super.updateItem(chunk, empty);
                setText(empty || chunk == null ? null : chunk);
                setWrapText(true);
            }
        });
        chunkList.getStyleClass().add("knowledge-chunk-list");
        VBox indexTabContent = new VBox(chunkList);
        indexTabContent.setPadding(new Insets(14, 16, 16, 16));
        VBox.setVgrow(chunkList, Priority.ALWAYS);

        TabPane tabs = new TabPane(
                tab("概览", overviewTabContent),
                tab("文档内容", contentTabContent),
                tab("索引详情", indexTabContent));
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getStyleClass().add("knowledge-drawer-tabs");
        VBox.setVgrow(tabs, Priority.ALWAYS);
        VBox root = new VBox(tabs);
        root.getStyleClass().add("knowledge-drawer-content");
        return root;
    }

    private ColumnConstraints percentColumn(double percent) {
        ColumnConstraints constraint = new ColumnConstraints();
        constraint.setPercentWidth(percent);
        constraint.setHgrow(Priority.ALWAYS);
        return constraint;
    }

    private VBox infoCard(String label, String value, String iconLiteral) {
        FontIcon icon = new FontIcon(iconLiteral);
        icon.setIconSize(18);
        icon.getStyleClass().add("knowledge-info-card-icon");
        Label labelNode = new Label(label);
        labelNode.getStyleClass().add("knowledge-info-card-label");
        HBox heading = new HBox(7, icon, labelNode);
        heading.setAlignment(Pos.CENTER_LEFT);
        Label valueNode = new Label(value);
        valueNode.setWrapText(true);
        valueNode.getStyleClass().add("knowledge-info-card-value");
        VBox card = new VBox(8, heading, valueNode);
        card.getStyleClass().add("knowledge-info-card");
        return card;
    }

    private Tab tab(String title, Node content) {
        Tab tab = new Tab(title, content);
        tab.setClosable(false);
        return tab;
    }

    private String renderChunks(List<String> chunks) {
        if (chunks.isEmpty()) return "文档尚未生成可查看的解析片段。";
        StringBuilder content = new StringBuilder();
        for (int index = 0; index < chunks.size(); index++) {
            if (index > 0) content.append("\n\n");
            content.append("片段 ").append(index + 1).append("\n").append(chunks.get(index));
        }
        return content.toString();
    }

    private void deleteDocument(KnowledgeDocumentDto document) {
        if (!AppDialogs.confirm(
                documentList.getScene().getWindow(),
                "删除文档",
                "确认删除知识文档",
                "将删除“" + document.name() + "”及其向量索引，此操作无法撤销。",
                "删除",
                true)) return;
        try {
            if (documentDrawer.isOpen()) documentDrawer.close();
            knowledgeService.delete(userId(), document.id());
            refresh();
        } catch (RuntimeException exception) {
            viewManager.showError(exceptionHandler.toUserMessage(exception));
        }
    }

    private void updateStats() {
        long ready = allDocuments.stream().filter(item -> item.status() == KnowledgeStatus.READY).count();
        long processing = allDocuments.stream().filter(item -> processing(item.status())).count();
        long failed = allDocuments.stream().filter(item -> item.status() == KnowledgeStatus.FAILED).count();
        totalCountLabel.setText(Long.toString(allDocuments.size()));
        totalHintLabel.setText(allDocuments.isEmpty() ? "等待上传" : "本地知识资料");
        readyCountLabel.setText(Long.toString(ready));
        readyHintLabel.setText("占比 " + percentage(ready, allDocuments.size()) + "%");
        categoryCountLabel.setText(Integer.toString(allCategories.size()));
        processingCountLabel.setText(Long.toString(processing));
        failedCountLabel.setText(Long.toString(failed));
    }

    private void refreshCategoryFilters() {
        String query = normalizedQuery(categorySearchField.getText());
        List<CategoryFilterItem> filters = allCategories.stream()
                .filter(category -> query.isEmpty() || category.name().toLowerCase(Locale.ROOT).contains(query))
                .map(category -> new CategoryFilterItem(category.name(), category.documentCount(), false))
                .toList();
        categoryList.getItems().setAll(new CategoryFilterItem("全部文档", allDocuments.size(), true));
        categoryList.getItems().addAll(filters);
        selectCategory(selectedCategory);
    }

    private void selectCategory(String category) {
        CategoryFilterItem target = categoryList.getItems().stream()
                .filter(item -> category == null ? item.allDocuments : item.name.equals(category))
                .findFirst()
                .orElseGet(() -> categoryList.getItems().isEmpty() ? null : categoryList.getItems().getFirst());
        categoryList.getSelectionModel().select(target);
    }

    private void applyFilters() {
        String query = normalizedQuery(documentSearchField.getText());
        Predicate<KnowledgeDocumentDto> statusFilter = statusFilter();
        Comparator<KnowledgeDocumentDto> comparator = sortBox.getValue() == null
                ? SortOption.UPDATED_DESC.comparator : sortBox.getValue().comparator;
        List<KnowledgeDocumentDto> filtered = allDocuments.stream()
                .filter(document -> selectedCategory == null || selectedCategory.equals(document.category()))
                .filter(statusFilter)
                .filter(document -> query.isEmpty()
                        || document.name().toLowerCase(Locale.ROOT).contains(query)
                        || document.originalName().toLowerCase(Locale.ROOT).contains(query)
                        || document.category().toLowerCase(Locale.ROOT).contains(query))
                .sorted(comparator)
                .toList();
        documentList.getItems().setAll(filtered);
        String scope = selectedCategory == null ? "全部分类" : selectedCategory;
        summaryLabel.setText("共 " + filtered.size() + " 个文档 · " + scope);
    }

    private Predicate<KnowledgeDocumentDto> statusFilter() {
        if (statusToggleGroup.getSelectedToggle() == readyStatusButton) {
            return document -> document.status() == KnowledgeStatus.READY;
        }
        if (statusToggleGroup.getSelectedToggle() == processingStatusButton) {
            return document -> processing(document.status());
        }
        if (statusToggleGroup.getSelectedToggle() == failedStatusButton) {
            return document -> document.status() == KnowledgeStatus.FAILED;
        }
        return document -> true;
    }

    private boolean processing(KnowledgeStatus status) {
        return status == KnowledgeStatus.UPLOADED
                || status == KnowledgeStatus.PARSING
                || status == KnowledgeStatus.INDEXING;
    }

    private boolean eventTargetInsideButton(Object target) {
        if (!(target instanceof Node node)) return false;
        Node current = node;
        while (current != null) {
            if (current instanceof Button) return true;
            Parent parent = current.getParent();
            current = parent;
        }
        return false;
    }

    private long userId() {
        return sessionState.requireCurrentUser().id();
    }

    private String statusText(KnowledgeDocumentDto dto) {
        return switch (dto.status()) {
            case UPLOADED -> "待处理";
            case PARSING -> "解析中";
            case INDEXING -> "向量化中";
            case READY -> "已就绪";
            case FAILED -> "解析失败";
        };
    }

    private String statusStyle(KnowledgeStatus status) {
        return switch (status) {
            case READY -> "ready";
            case FAILED -> "failed";
            case UPLOADED, PARSING, INDEXING -> "processing";
        };
    }

    private String fileIconLiteral(String fileType) {
        return switch (fileType.toLowerCase(Locale.ROOT)) {
            case "pdf" -> "mdi2f-file-pdf-box";
            case "doc", "docx" -> "mdi2f-file-word-box";
            default -> "mdi2f-file-document-outline";
        };
    }

    private String fileTypeStyle(String fileType) {
        return switch (fileType.toLowerCase(Locale.ROOT)) {
            case "pdf" -> "pdf";
            case "doc", "docx" -> "word";
            default -> "text";
        };
    }

    private String sizeText(long bytes) {
        return bytes < 1024 * 1024 ? String.format(Locale.ROOT, "%.1f KB", bytes / 1024.0)
                : String.format(Locale.ROOT, "%.1f MB", bytes / 1024.0 / 1024.0);
    }

    private String timeText(LocalDateTime value) {
        return value == null ? "—" : TIME_FORMAT.format(value);
    }

    private int percentage(long part, long total) {
        return total == 0 ? 0 : (int) Math.round(part * 100.0 / total);
    }

    private String normalizedQuery(String text) {
        return text == null ? "" : text.strip().toLowerCase(Locale.ROOT);
    }

    private final class DocumentCell extends ListCell<KnowledgeDocumentDto> {
        private final FontIcon fileIcon = new FontIcon();
        private final Label name = new Label();
        private final Label category = new Label();
        private final Label metadata = new Label();
        private final Label updated = new Label();
        private final Label status = new Label();
        private final Button action = new Button();
        private final HBox root;

        private DocumentCell() {
            fileIcon.setIconSize(29);
            StackPane iconBox = new StackPane(fileIcon);
            iconBox.getStyleClass().add("knowledge-file-icon-box");
            name.getStyleClass().add("knowledge-document-name");
            category.getStyleClass().add("knowledge-category-chip");
            HBox titleRow = new HBox(9, name, category);
            titleRow.setAlignment(Pos.CENTER_LEFT);
            metadata.getStyleClass().add("knowledge-document-meta");
            VBox identity = new VBox(8, titleRow, metadata);
            HBox.setHgrow(identity, Priority.ALWAYS);
            updated.getStyleClass().add("knowledge-document-updated");
            status.getStyleClass().add("knowledge-status-chip");
            VBox state = new VBox(9, updated, status);
            state.setAlignment(Pos.CENTER_RIGHT);
            state.setMinWidth(136);
            action.setGraphic(new FontIcon("mdi2d-dots-horizontal"));
            action.getStyleClass().add("knowledge-row-action");
            action.setAccessibleText("文档操作");
            action.setOnAction(event -> showActions());
            root = new HBox(13, iconBox, identity, state, action);
            root.setAlignment(Pos.CENTER_LEFT);
            root.getStyleClass().add("knowledge-document-row");
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        }

        @Override
        protected void updateItem(KnowledgeDocumentDto document, boolean empty) {
            super.updateItem(document, empty);
            if (empty || document == null) {
                setGraphic(null);
                return;
            }
            fileIcon.setIconLiteral(fileIconLiteral(document.fileType()));
            fileIcon.getStyleClass().setAll("knowledge-file-icon", fileTypeStyle(document.fileType()));
            name.setText(document.name());
            category.setText(document.category());
            metadata.setText(document.originalName() + "  ·  " + document.fileType().toUpperCase(Locale.ROOT)
                    + "  ·  " + sizeText(document.fileSize()));
            updated.setText(timeText(document.updateTime()));
            status.setText(statusText(document));
            status.getStyleClass().setAll("knowledge-status-chip", statusStyle(document.status()));
            setGraphic(root);
        }

        private void showActions() {
            KnowledgeDocumentDto document = getItem();
            if (document == null) return;
            documentList.getSelectionModel().select(document);
            MenuItem view = new MenuItem("查看文档", new FontIcon("mdi2e-eye-outline"));
            view.setOnAction(event -> showDocument(document));
            MenuItem delete = new MenuItem("删除文档", new FontIcon("mdi2d-delete-outline"));
            delete.setOnAction(event -> deleteDocument(document));
            ContextMenu menu = new ContextMenu(view, new SeparatorMenuItem(), delete);
            menu.show(action, javafx.geometry.Side.BOTTOM, 0, 4);
        }
    }

    private static final class CategoryCell extends ListCell<CategoryFilterItem> {
        private final FontIcon icon = new FontIcon();
        private final Label name = new Label();
        private final Label count = new Label();
        private final HBox root = new HBox(9);

        private CategoryCell() {
            icon.setIconSize(17);
            name.getStyleClass().add("knowledge-category-name");
            count.getStyleClass().add("knowledge-category-count");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            root.getChildren().setAll(icon, name, spacer, count);
            root.setAlignment(Pos.CENTER_LEFT);
            root.getStyleClass().add("knowledge-category-row");
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        }

        @Override protected void updateItem(CategoryFilterItem item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                return;
            }
            icon.setIconLiteral(item.allDocuments ? "mdi2f-folder-multiple-outline" : "mdi2f-folder-outline");
            name.setText(item.name);
            count.setText(Long.toString(item.count));
            setGraphic(root);
        }
    }

    private record CategoryFilterItem(String name, long count, boolean allDocuments) {
    }

    private enum SortOption {
        UPDATED_DESC("按更新时间", Comparator.comparing(KnowledgeDocumentDto::updateTime,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(Comparator.comparingLong(KnowledgeDocumentDto::id).reversed())),
        NAME_ASC("按名称", Comparator.comparing(KnowledgeDocumentDto::name,
                String.CASE_INSENSITIVE_ORDER)),
        SIZE_DESC("按大小", Comparator.comparingLong(KnowledgeDocumentDto::fileSize).reversed());

        private final String label;
        private final Comparator<KnowledgeDocumentDto> comparator;

        SortOption(String label, Comparator<KnowledgeDocumentDto> comparator) {
            this.label = label;
            this.comparator = comparator;
        }
    }
}
