package com.inin.aiinterviewer.ui.controller;

import com.inin.aiinterviewer.application.dto.InterviewPlanDto;
import com.inin.aiinterviewer.application.dto.ResumeDto;
import com.inin.aiinterviewer.application.dto.SaveInterviewPlanCommand;
import com.inin.aiinterviewer.application.dto.CandidateProfileListItemDto;
import com.inin.aiinterviewer.application.exception.BusinessException;
import com.inin.aiinterviewer.application.exception.ErrorCode;
import com.inin.aiinterviewer.application.exception.GlobalExceptionHandler;
import com.inin.aiinterviewer.application.service.InterviewPlanService;
import com.inin.aiinterviewer.application.service.ResumeService;
import com.inin.aiinterviewer.application.service.CandidateProfileService;
import com.inin.aiinterviewer.application.service.KnowledgeDocumentService;
import com.inin.aiinterviewer.domain.enums.InterviewDifficulty;
import com.inin.aiinterviewer.ui.navigation.ContentNavigator;
import com.inin.aiinterviewer.ui.navigation.ContextAwareController;
import com.inin.aiinterviewer.ui.navigation.JavaFxViewManager;
import com.inin.aiinterviewer.ui.state.UserSessionState;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ListView;
import javafx.scene.control.ListCell;
import javafx.scene.control.SelectionMode;
import javafx.util.StringConverter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.List;
import com.inin.aiinterviewer.application.dto.KnowledgeDocumentDto;

@Component
@Scope("prototype")
public class InterviewPlanEditorController implements ContextAwareController<Long> {

    private final InterviewPlanService planService;
    private final ResumeService resumeService;
    private final CandidateProfileService profileService;
    private final KnowledgeDocumentService knowledgeService;
    private final UserSessionState sessionState;
    private final ContentNavigator contentNavigator;
    private final JavaFxViewManager viewManager;
    private final GlobalExceptionHandler exceptionHandler;

    @FXML private TextField nameField;
    @FXML private TextField jobTitleField;
    @FXML private TextArea jobDescriptionArea;
    @FXML private ComboBox<InterviewDifficulty> difficultyBox;
    @FXML private TextField durationField;
    @FXML private TextField questionCountField;
    @FXML private ComboBox<ResumeDto> resumeBox;
    @FXML private ComboBox<CandidateProfileListItemDto> profileBox;
    @FXML private ListView<KnowledgeDocumentDto> knowledgeList;
    @FXML private TextField focusField;
    @FXML private Label pageHeadingLabel;
    @FXML private Label summaryJobLabel;
    @FXML private Label summaryDifficultyLabel;
    @FXML private Label summaryDurationLabel;
    @FXML private Label summaryQuestionsLabel;
    @FXML private Label summaryProfileLabel;
    @FXML private Label summaryKnowledgeLabel;

    private Long editingPlanId;

    public InterviewPlanEditorController(
            InterviewPlanService planService,
            ResumeService resumeService,
            CandidateProfileService profileService,
            KnowledgeDocumentService knowledgeService,
            UserSessionState sessionState,
            ContentNavigator contentNavigator,
            JavaFxViewManager viewManager,
            GlobalExceptionHandler exceptionHandler
    ) {
        this.planService = planService;
        this.resumeService = resumeService;
        this.profileService = profileService;
        this.knowledgeService = knowledgeService;
        this.sessionState = sessionState;
        this.contentNavigator = contentNavigator;
        this.viewManager = viewManager;
        this.exceptionHandler = exceptionHandler;
    }

    @FXML
    private void initialize() {
        difficultyBox.getItems().setAll(InterviewDifficulty.values());
        difficultyBox.setConverter(new StringConverter<>() {
            @Override public String toString(InterviewDifficulty value) { return value == null ? "" : difficultyText(value); }
            @Override public InterviewDifficulty fromString(String value) { return null; }
        });
        resumeBox.setConverter(new StringConverter<>() {
            @Override public String toString(ResumeDto value) { return value == null ? "不关联简历" : value.originalName(); }
            @Override public ResumeDto fromString(String value) { return null; }
        });
        profileBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(CandidateProfileListItemDto value) {
                if (value == null) return "不关联候选人画像";
                String name = value.candidateName() == null || value.candidateName().isBlank()
                        ? value.resumeName() : value.candidateName();
                String role = value.targetRole() == null || value.targetRole().isBlank()
                        ? "待补充岗位" : value.targetRole();
                return name + " · " + role;
            }
            @Override public CandidateProfileListItemDto fromString(String value) { return null; }
        });
        resumeBox.getItems().setAll(resumeService.list(sessionState.requireCurrentUser().id()));
        profileBox.getItems().setAll(profileService.listConfirmed(sessionState.requireCurrentUser().id()));
        knowledgeList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        knowledgeList.getItems().setAll(knowledgeService.listReady(sessionState.requireCurrentUser().id()));
        knowledgeList.setCellFactory(ignored -> new ListCell<>() {
            @Override
            protected void updateItem(KnowledgeDocumentDto document, boolean empty) {
                super.updateItem(document, empty);
                setText(empty || document == null ? null : document.name() + " · " + document.category());
            }
        });
        nameField.textProperty().addListener((observable, oldValue, value) -> refreshSummary());
        jobTitleField.textProperty().addListener((observable, oldValue, value) -> refreshSummary());
        difficultyBox.valueProperty().addListener((observable, oldValue, value) -> refreshSummary());
        durationField.textProperty().addListener((observable, oldValue, value) -> refreshSummary());
        questionCountField.textProperty().addListener((observable, oldValue, value) -> refreshSummary());
        profileBox.valueProperty().addListener((observable, oldValue, profile) -> {
            if (profile != null) {
                resumeBox.getItems().stream().filter(resume -> resume.id().equals(profile.resumeId()))
                        .findFirst().ifPresent(resumeBox::setValue);
            }
            refreshSummary();
        });
        resumeBox.valueProperty().addListener((observable, oldValue, resume) -> {
            CandidateProfileListItemDto profile = profileBox.getValue();
            if (profile != null && (resume == null || !profile.resumeId().equals(resume.id()))) {
                profileBox.setValue(null);
            }
        });
        knowledgeList.getSelectionModel().getSelectedItems()
                .addListener((javafx.collections.ListChangeListener<KnowledgeDocumentDto>) change -> refreshSummary());
    }

    @Override
    public void initializeContext(Long planId) {
        editingPlanId = planId;
        if (planId == null) {
            pageHeadingLabel.setText("新建面试方案");
            difficultyBox.setValue(InterviewDifficulty.MEDIUM);
            durationField.setText("45");
            questionCountField.setText("15");
        } else {
            pageHeadingLabel.setText("编辑面试方案");
            populate(planService.require(planId, sessionState.requireCurrentUser().id()));
        }
        refreshSummary();
    }

    @FXML
    private void savePlan() {
        try {
            SaveInterviewPlanCommand command = commandFromForm();
            long userId = sessionState.requireCurrentUser().id();
            if (editingPlanId == null) {
                planService.create(userId, command);
            } else {
                planService.update(userId, editingPlanId, command);
            }
            contentNavigator.back();
        } catch (RuntimeException exception) {
            viewManager.showError(exceptionHandler.toUserMessage(exception));
        }
    }

    @FXML
    private void cancel() {
        contentNavigator.back();
    }

    private void populate(InterviewPlanDto plan) {
        nameField.setText(plan.name());
        jobTitleField.setText(plan.jobTitle());
        jobDescriptionArea.setText(plan.jobDescription());
        difficultyBox.setValue(plan.difficulty());
        durationField.setText(Integer.toString(plan.durationMinutes()));
        questionCountField.setText(Integer.toString(plan.questionCount()));
        resumeBox.getItems().stream().filter(resume -> resume.id().equals(plan.resumeId()))
                .findFirst().ifPresent(resumeBox::setValue);
        profileBox.getItems().stream().filter(profile -> profile.id().equals(plan.profileId()))
                .findFirst().ifPresent(profileBox::setValue);
        for (KnowledgeDocumentDto document : knowledgeList.getItems()) {
            if (plan.knowledgeDocumentIds().contains(document.id())) {
                knowledgeList.getSelectionModel().select(document);
            }
        }
        focusField.setText(String.valueOf(plan.rules().getOrDefault("focus", "")));
    }

    private SaveInterviewPlanCommand commandFromForm() {
        try {
            int duration = Integer.parseInt(durationField.getText().trim());
            int questions = Integer.parseInt(questionCountField.getText().trim());
            ResumeDto resume = resumeBox.getValue();
            CandidateProfileListItemDto profile = profileBox.getValue();
            List<Long> documentIds = knowledgeList.getSelectionModel().getSelectedItems().stream()
                    .map(KnowledgeDocumentDto::id).toList();
            Map<String, Object> rules = focusField.getText().isBlank()
                    ? Map.of() : Map.of("focus", focusField.getText().trim());
            return new SaveInterviewPlanCommand(nameField.getText(), jobTitleField.getText(),
                    jobDescriptionArea.getText(), difficultyBox.getValue(), duration, questions,
                    resume == null ? null : resume.id(), profile == null ? null : profile.id(),
                    documentIds, rules, null);
        } catch (NumberFormatException exception) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
    }

    private void refreshSummary() {
        if (summaryJobLabel == null) return;
        summaryJobLabel.setText(jobTitleField.getText().isBlank() ? "待填写" : jobTitleField.getText());
        summaryDifficultyLabel.setText(difficultyBox.getValue() == null ? "待选择" : difficultyText(difficultyBox.getValue()));
        summaryDurationLabel.setText(durationField.getText().isBlank() ? "—" : durationField.getText() + " 分钟");
        summaryQuestionsLabel.setText(questionCountField.getText().isBlank() ? "—" : questionCountField.getText() + " 题");
        if (summaryProfileLabel != null) {
            CandidateProfileListItemDto profile = profileBox.getValue();
            summaryProfileLabel.setText(profile == null ? "未关联" : profileBox.getConverter().toString(profile));
        }
        if (summaryKnowledgeLabel != null && knowledgeList != null) {
            int count = knowledgeList.getSelectionModel().getSelectedItems().size();
            summaryKnowledgeLabel.setText(count == 0 ? "未选择" : count + " 个文档");
        }
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
