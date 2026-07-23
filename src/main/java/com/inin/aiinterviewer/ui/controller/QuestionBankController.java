package com.inin.aiinterviewer.ui.controller;

import com.inin.aiinterviewer.application.dto.InterviewQuestionDto;
import com.inin.aiinterviewer.application.dto.JobPositionDto;
import com.inin.aiinterviewer.application.exception.GlobalExceptionHandler;
import com.inin.aiinterviewer.application.service.QuestionBankService;
import com.inin.aiinterviewer.domain.enums.QuestionCategory;
import com.inin.aiinterviewer.ui.component.AppDialogs;
import com.inin.aiinterviewer.ui.component.AppSelect;
import com.inin.aiinterviewer.ui.navigation.ContentNavigator;
import com.inin.aiinterviewer.ui.navigation.JavaFxViewManager;
import com.inin.aiinterviewer.ui.navigation.Route;
import com.inin.aiinterviewer.ui.state.UserSessionState;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.Cursor;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Scope("prototype")
public class QuestionBankController {

    private final QuestionBankService questionBankService;
    private final UserSessionState sessionState;
    private final ContentNavigator contentNavigator;
    private final JavaFxViewManager viewManager;
    private final GlobalExceptionHandler exceptionHandler;

    @FXML private ListView<JobPositionDto> jobListView;
    @FXML private TextField newJobTitleField;
    @FXML private TextField newJobDeptField;
    @FXML private Button deleteJobButton;
    @FXML private AppSelect<String> categoryFilterCombo;
    @FXML private TextField searchField;
    @FXML private HBox searchBox;
    @FXML private GridPane questionTableGrid;
    @FXML private VBox emptyStateContainer;
    @FXML private Button deleteQuestionButton;
    @FXML private Button practiceButton;
    @FXML private FlowPane quickJobPane;

    private List<InterviewQuestionDto> allQuestions = new ArrayList<>();
    private final ObjectProperty<InterviewQuestionDto> selectedQuestion = new SimpleObjectProperty<>();
    private List<Node> selectedRowNodes = new ArrayList<>();

    public QuestionBankController(
            QuestionBankService questionBankService,
            UserSessionState sessionState,
            ContentNavigator contentNavigator,
            JavaFxViewManager viewManager,
            GlobalExceptionHandler exceptionHandler
    ) {
        this.questionBankService = questionBankService;
        this.sessionState = sessionState;
        this.contentNavigator = contentNavigator;
        this.viewManager = viewManager;
        this.exceptionHandler = exceptionHandler;
    }

    @FXML
    private void initialize() {
        // ---- Position list cell factory ----
        jobListView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(JobPositionDto item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                HBox root = new HBox(10);
                root.setAlignment(Pos.CENTER_LEFT);
                root.setPadding(new Insets(8, 12, 8, 4));

                FontIcon icon = new FontIcon("mdi2b-briefcase-outline");
                icon.setIconSize(18);
                icon.getStyleClass().add("qb-position-icon");

                VBox textCol = new VBox(2);
                Label title = new Label(item.title());
                title.getStyleClass().add("qb-position-title");
                if (item.department() != null && !item.department().isBlank()) {
                    Label dept = new Label(item.department());
                    dept.getStyleClass().add("qb-position-dept");
                    textCol.getChildren().addAll(title, dept);
                } else {
                    textCol.getChildren().add(title);
                }

                root.getChildren().addAll(icon, textCol);
                setGraphic(root);
            }
        });

        categoryFilterCombo.getItems().addAll("全部", "技术题", "行为题", "场景题");
        categoryFilterCombo.setValue("全部");
        categoryFilterCombo.setOnAction(event -> applyFilters());

        // Search field: real-time filtering on key release
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());

        // Bind search box focused appearance to inner text field
        searchField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                searchBox.getStyleClass().add("qb-search-focused");
            } else {
                searchBox.getStyleClass().remove("qb-search-focused");
            }
        });

        deleteJobButton.disableProperty().bind(jobListView.getSelectionModel().selectedItemProperty().isNull());
        deleteQuestionButton.disableProperty().bind(selectedQuestion.isNull());
        practiceButton.disableProperty().bind(selectedQuestion.isNull());

        loadAll();
    }

    /**
     * Add a single question row into the GridPane at the given row index.
     */
    private void addQuestionRow(int rowIndex, InterviewQuestionDto item) {
        List<Node> rowNodes = new ArrayList<>();

        // --- Column 0: 岗位 ---
        Label jobLabel = new Label(item.jobTitle() != null ? item.jobTitle() : "未归类");
        jobLabel.getStyleClass().add("qb-td-job");
        HBox jobCell = new HBox(jobLabel);
        jobCell.setAlignment(Pos.CENTER_LEFT);
        jobCell.getStyleClass().add("qb-table-row-cell");
        GridPane.setRowIndex(jobCell, rowIndex);
        GridPane.setColumnIndex(jobCell, 0);
        rowNodes.add(jobCell);

        // --- Column 1: 难度 ---
        String diffLabel = switch (item.difficulty()) {
            case JUNIOR -> "初级";
            case MEDIUM -> "中级";
            case SENIOR -> "高级";
            case EXPERT -> "专家";
        };
        String diffStyleClass = switch (item.difficulty()) {
            case JUNIOR -> "badge-junior";
            case MEDIUM -> "badge-medium";
            case SENIOR -> "badge-senior";
            case EXPERT -> "badge-expert";
        };
        Label diffBadge = new Label(diffLabel);
        diffBadge.getStyleClass().addAll("qb-difficulty-badge", diffStyleClass);
        HBox diffCell = new HBox(diffBadge);
        diffCell.setAlignment(Pos.CENTER);
        diffCell.getStyleClass().addAll("qb-table-row-cell", "qb-table-row-cell-difficulty");
        HBox.setHgrow(diffBadge, Priority.ALWAYS);
        GridPane.setRowIndex(diffCell, rowIndex);
        GridPane.setColumnIndex(diffCell, 1);
        rowNodes.add(diffCell);

        // --- Column 2: 题干 ---
        Label titleLabel = new Label(item.title());
        titleLabel.getStyleClass().add("qb-td-title");
        titleLabel.setWrapText(true);
        HBox titleCell = new HBox(titleLabel);
        titleCell.setAlignment(Pos.CENTER_LEFT);
        titleCell.getStyleClass().add("qb-table-row-cell");
        GridPane.setRowIndex(titleCell, rowIndex);
        GridPane.setColumnIndex(titleCell, 2);
        rowNodes.add(titleCell);

        // --- Column 3: 标签 ---
        FlowPane tagsRow = new FlowPane(Orientation.HORIZONTAL, 8, 6);
        tagsRow.setAlignment(Pos.CENTER_LEFT);
        tagsRow.setMaxWidth(Double.MAX_VALUE);
        tagsRow.setPrefWrapLength(360);
        if (item.tags() != null && !item.tags().isEmpty()) {
            final int MAX_VISIBLE_TAGS = 4;
            int total = item.tags().size();
            int show = Math.min(MAX_VISIBLE_TAGS, total);
            for (int i = 0; i < show; i++) {
                String tag = item.tags().get(i);
                Label tagLabel = new Label(tag);
                tagLabel.getStyleClass().add("qb-tag");
                tagLabel.setCursor(Cursor.HAND);
                Tooltip.install(tagLabel, new Tooltip("点击按“" + tag + "”筛选题目"));
                tagLabel.setOnMouseClicked(e -> {
                    e.consume();
                    filterByTag(tag);
                });
                tagsRow.getChildren().add(tagLabel);
            }
            // 超过 4 个的标签直接不显示；如需查看/搜索，点击已展示标签或在搜索框输入标签名即可
        } else {
            Label noTag = new Label("-");
            noTag.getStyleClass().add("qb-no-tag");
            tagsRow.getChildren().add(noTag);
        }
        HBox tagsCell = new HBox(tagsRow);
        HBox.setHgrow(tagsRow, Priority.ALWAYS);
        tagsCell.setAlignment(Pos.CENTER_LEFT);
        tagsCell.setStyle("-fx-padding: 11 0 11 18;");
        tagsCell.getStyleClass().addAll("qb-table-row-cell", "qb-table-row-cell-tags");
        GridPane.setRowIndex(tagsCell, rowIndex);
        GridPane.setColumnIndex(tagsCell, 3);
        rowNodes.add(tagsCell);

        // Selection handling: click any cell selects the whole row
        for (Node cell : rowNodes) {
            cell.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY) {
                    selectRow(item, rowNodes);
                    if (event.getClickCount() == 2) {
                        editSelectedQuestion();
                    }
                }
            });
        }

        questionTableGrid.getChildren().addAll(rowNodes);
    }

    private void selectRow(InterviewQuestionDto question, List<Node> rowNodes) {
        clearSelection();
        selectedQuestion.set(question);
        selectedRowNodes = rowNodes;
        for (Node cell : rowNodes) {
            cell.getStyleClass().add("qb-table-row-selected");
        }
    }

    private void clearSelection() {
        selectedQuestion.set(null);
        for (Node cell : selectedRowNodes) {
            cell.getStyleClass().remove("qb-table-row-selected");
        }
        selectedRowNodes.clear();
    }

    /**
     * Click a tag chip to filter the list to questions carrying that tag,
     * and scroll the search box into focus so the user can see / adjust it.
     */
    private void filterByTag(String tag) {
        searchField.setText(tag);
        applyFilters();
    }

    @FXML
    private void addJob() {
        String title = newJobTitleField.getText();
        if (title == null || title.isBlank()) {
            viewManager.showError("请填写岗位名称");
            return;
        }
        try {
            questionBankService.createJob(sessionState.requireCurrentUser().id(),
                    new com.inin.aiinterviewer.application.dto.SaveJobPositionCommand(
                            title, newJobDeptField.getText(), null));
            newJobTitleField.clear();
            newJobDeptField.clear();
            loadAll();
        } catch (RuntimeException exception) {
            viewManager.showError(exceptionHandler.toUserMessage(exception));
        }
    }

    @FXML
    private void addQuickJavaJob()    { addPresetJob("Java 后端开发工程师", "技术研发"); }
    @FXML
    private void addQuickFrontendJob(){ addPresetJob("前端开发工程师", "技术研发"); }
    @FXML
    private void addQuickPmJob()      { addPresetJob("产品经理", "产品"); }
    @FXML
    private void addQuickDataJob()    { addPresetJob("数据分析师", "数据"); }
    @FXML
    private void addQuickMedicalJob() { addPresetJob("医疗卫生", "医疗"); }

    private void addPresetJob(String title, String department) {
        try {
            long userId = sessionState.requireCurrentUser().id();
            questionBankService.createJob(userId,
                    new com.inin.aiinterviewer.application.dto.SaveJobPositionCommand(title, department, null));
            loadAll();
            // Auto-select the newly created job in the list
            jobListView.getItems().stream()
                    .filter(j -> title.equals(j.title()))
                    .findFirst()
                    .ifPresent(j -> jobListView.getSelectionModel().select(j));
        } catch (RuntimeException exception) {
            viewManager.showError(exceptionHandler.toUserMessage(exception));
        }
    }

    @FXML
    private void deleteSelectedJob() {
        JobPositionDto selected = jobListView.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        if (!AppDialogs.confirm(
                jobListView.getScene() == null ? null : jobListView.getScene().getWindow(),
                "删除岗位",
                "确认删除岗位",
                "将删除" + selected.title() + "及其下所有题目，此操作无法撤销。",
                "删除岗位",
                true)) return;
        try {
            long userId = sessionState.requireCurrentUser().id();
            for (InterviewQuestionDto question : allQuestions) {
                if (selected.id().equals(question.jobId())) {
                    questionBankService.deleteQuestion(userId, question.id());
                }
            }
            questionBankService.deleteJob(userId, selected.id());
            loadAll();
        } catch (RuntimeException exception) {
            viewManager.showError(exceptionHandler.toUserMessage(exception));
        }
    }

    @FXML
    private void newQuestion() {
        contentNavigator.showSubPage("/fxml/question-editor-view.fxml", "新建面试题", null);
    }

    @FXML
    private void editSelectedQuestion() {
        InterviewQuestionDto selected = selectedQuestion.get();
        if (selected != null) {
            contentNavigator.showSubPage("/fxml/question-editor-view.fxml", "编辑面试题", selected.id());
        }
    }

    @FXML
    private void deleteSelectedQuestion() {
        InterviewQuestionDto selected = selectedQuestion.get();
        if (selected == null) return;
        if (!AppDialogs.confirm(
                questionTableGrid.getScene() == null ? null : questionTableGrid.getScene().getWindow(),
                "删除面试题",
                "确认删除题目",
                "将永久删除" + selected.title() + "，此操作无法撤销。",
                "删除题目",
                true)) return;
        try {
            questionBankService.deleteQuestion(sessionState.requireCurrentUser().id(), selected.id());
            loadAll();
        } catch (RuntimeException exception) {
            viewManager.showError(exceptionHandler.toUserMessage(exception));
        }
    }

    @FXML
    private void refresh() {
        loadAll();
    }

    @FXML
    private void practiceSelectedQuestion() {
        InterviewQuestionDto selected = selectedQuestion.get();
        if (selected == null) {
            viewManager.showError("请先在列表中选择一道题目");
            return;
        }
        contentNavigator.showSubRoute(Route.QUESTION_PRACTICE, selected.id());
    }

    private void loadAll() {
        try {
            long userId = sessionState.requireCurrentUser().id();
            jobListView.getItems().setAll(questionBankService.listJobs(userId));
            allQuestions = questionBankService.listQuestions(userId);
            applyFilters();
        } catch (RuntimeException ex) {
            Throwable root = ex;
            while (root.getCause() != null && root.getCause() != root) root = root.getCause();
            String detail = root.getMessage();
            viewManager.showError("加载题库失败: " + exceptionHandler.toUserMessage(ex)
                    + (detail != null ? "\n详情: " + detail : ""));
        }
    }

    private void applyFilters() {
        String categoryText = categoryFilterCombo.getValue();
        QuestionCategory category = switch (categoryText) {
            case "技术题" -> QuestionCategory.TECHNICAL;
            case "行为题" -> QuestionCategory.BEHAVIORAL;
            case "场景题" -> QuestionCategory.SCENARIO;
            default -> null;
        };
        String keyword = searchField.getText();
        boolean hasKeyword = keyword != null && !keyword.isBlank();

        List<InterviewQuestionDto> filtered = new ArrayList<>();
        for (InterviewQuestionDto question : allQuestions) {
            if (category != null && question.category() != category) continue;
            if (hasKeyword) {
                String kw = keyword.toLowerCase();
                if (!question.title().toLowerCase().contains(kw)
                        && !(question.jobTitle() != null && question.jobTitle().toLowerCase().contains(kw))
                        && !(question.tags() != null && question.tags().stream().anyMatch(t -> t.toLowerCase().contains(kw)))) {
                    continue;
                }
            }
            filtered.add(question);
        }

        // Remove all data rows (rowIndex >= 1), keep header row (rowIndex 0)
        questionTableGrid.getChildren().removeIf(node -> {
            Integer row = GridPane.getRowIndex(node);
            return row != null && row >= 1;
        });
        clearSelection();

        if (filtered.isEmpty()) {
            questionTableGrid.setVisible(false);
            questionTableGrid.setManaged(false);
            emptyStateContainer.setVisible(true);
            emptyStateContainer.setManaged(true);
        } else {
            questionTableGrid.setVisible(true);
            questionTableGrid.setManaged(true);
            emptyStateContainer.setVisible(false);
            emptyStateContainer.setManaged(false);

            int row = 1;
            for (InterviewQuestionDto question : filtered) {
                addQuestionRow(row++, question);
            }
        }
    }
}
