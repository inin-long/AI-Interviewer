package com.inin.aiinterviewer.ui.controller;

import com.inin.aiinterviewer.application.dto.InterviewQuestionDto;
import com.inin.aiinterviewer.application.dto.JobPositionDto;
import com.inin.aiinterviewer.application.dto.QuestionTagDto;
import com.inin.aiinterviewer.application.exception.GlobalExceptionHandler;
import com.inin.aiinterviewer.application.service.QuestionBankService;
import com.inin.aiinterviewer.domain.enums.InterviewDifficulty;
import com.inin.aiinterviewer.domain.enums.QuestionCategory;
import com.inin.aiinterviewer.ui.navigation.ContentNavigator;
import com.inin.aiinterviewer.ui.navigation.JavaFxViewManager;
import com.inin.aiinterviewer.ui.state.UserSessionState;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
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
    @FXML private ComboBox<String> categoryFilterCombo;
    @FXML private ComboBox<String> tagFilterCombo;
    @FXML private TableView<InterviewQuestionDto> questionTable;
    @FXML private TableColumn<InterviewQuestionDto, String> jobColumn;
    @FXML private TableColumn<InterviewQuestionDto, String> categoryColumn;
    @FXML private TableColumn<InterviewQuestionDto, String> difficultyColumn;
    @FXML private TableColumn<InterviewQuestionDto, String> titleColumn;
    @FXML private TableColumn<InterviewQuestionDto, String> tagsColumn;
    @FXML private Button deleteQuestionButton;

    private List<InterviewQuestionDto> allQuestions = new ArrayList<>();
    private List<QuestionTagDto> allTags = new ArrayList<>();

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
        jobListView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(JobPositionDto item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                String dept = item.department() == null || item.department().isBlank()
                        ? "" : " · " + item.department();
                setText(item.title() + dept);
            }
        });
        categoryFilterCombo.getItems().addAll("全部", "技术题", "行为题", "场景题");
        categoryFilterCombo.setValue("全部");
        categoryFilterCombo.setOnAction(event -> applyFilters());
        tagFilterCombo.setOnAction(event -> applyFilters());

        jobColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(
                cell.getValue().jobTitle() == null ? "（未归类）" : cell.getValue().jobTitle()));
        categoryColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().category().label()));
        difficultyColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(difficultyText(cell.getValue().difficulty())));
        titleColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().title()));
        tagsColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(String.join("、", cell.getValue().tags())));

        deleteJobButton.disableProperty().bind(jobListView.getSelectionModel().selectedItemProperty().isNull());
        deleteQuestionButton.disableProperty().bind(questionTable.getSelectionModel().selectedItemProperty().isNull());

        questionTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && questionTable.getSelectionModel().getSelectedItem() != null) {
                editSelectedQuestion();
            }
        });

        loadAll();
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
    private void deleteSelectedJob() {
        JobPositionDto selected = jobListView.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                "删除岗位「" + selected.title() + "」？其下题目将一并删除。",
                ButtonType.CANCEL, ButtonType.OK);
        confirmation.setHeaderText("确认删除岗位");
        if (confirmation.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
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
        InterviewQuestionDto selected = questionTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            contentNavigator.showSubPage("/fxml/question-editor-view.fxml", "编辑面试题", selected.id());
        }
    }

    @FXML
    private void deleteSelectedQuestion() {
        InterviewQuestionDto selected = questionTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                "删除面试题「" + selected.title() + "」？", ButtonType.CANCEL, ButtonType.OK);
        confirmation.setHeaderText("确认删除题目");
        if (confirmation.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
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

    private void loadAll() {
        try {
            long userId = sessionState.requireCurrentUser().id();
            jobListView.getItems().setAll(questionBankService.listJobs(userId));
            allQuestions = questionBankService.listQuestions(userId);
            allTags = questionBankService.listTags(userId);
            tagFilterCombo.getItems().setAll("全部");
            for (QuestionTagDto tag : allTags) {
                tagFilterCombo.getItems().add(tag.name());
            }
            tagFilterCombo.setValue("全部");
            applyFilters();
        } catch (RuntimeException ex) {
            // 解包根因，避免 MyBatis 等底层异常被泛化为"系统未知错误"
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
        String tag = tagFilterCombo.getValue();
        List<InterviewQuestionDto> filtered = new ArrayList<>();
        for (InterviewQuestionDto question : allQuestions) {
            if (category != null && question.category() != category) continue;
            if (tag != null && !"全部".equals(tag) && !question.tags().contains(tag)) continue;
            filtered.add(question);
        }
        questionTable.getItems().setAll(filtered);
    }

    private String difficultyText(InterviewDifficulty difficulty) {
        return switch (difficulty) {
            case JUNIOR -> "初级";
            case MEDIUM -> "中级";
            case SENIOR -> "高级";
            case EXPERT -> "专家";
        };
    }
}
