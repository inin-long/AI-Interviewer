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
import com.inin.aiinterviewer.application.service.InterviewPlanAssetService;
import com.inin.aiinterviewer.application.service.InterviewPlanTransferService;
import com.inin.aiinterviewer.application.service.InterviewSessionService;
import com.inin.aiinterviewer.domain.enums.InterviewDifficulty;
import com.inin.aiinterviewer.domain.enums.InterviewMode;
import com.inin.aiinterviewer.domain.enums.InterviewerPersona;
import com.inin.aiinterviewer.domain.enums.PressureLevel;
import com.inin.aiinterviewer.domain.enums.VerificationStrictness;
import com.inin.aiinterviewer.ui.component.AppSelect;
import com.inin.aiinterviewer.ui.component.AppMultiSelect;
import com.inin.aiinterviewer.ui.component.AppDialogs;
import com.inin.aiinterviewer.ui.dialog.FileDialogService;
import com.inin.aiinterviewer.domain.model.InterviewPlanSettings;
import com.inin.aiinterviewer.ui.navigation.ContentNavigator;
import com.inin.aiinterviewer.ui.navigation.ContextAwareController;
import com.inin.aiinterviewer.ui.navigation.JavaFxViewManager;
import com.inin.aiinterviewer.ui.state.UserSessionState;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.List;
import java.util.LinkedHashMap;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

@Component
@Scope("prototype")
public class InterviewPlanEditorController implements ContextAwareController<Long> {

    private final InterviewPlanService planService;
    private final ResumeService resumeService;
    private final CandidateProfileService profileService;
    private final KnowledgeDocumentService knowledgeService;
    private final InterviewPlanAssetService assetService;
    private final InterviewPlanTransferService transferService;
    private final InterviewSessionService sessionService;
    private final UserSessionState sessionState;
    private final ContentNavigator contentNavigator;
    private final FileDialogService fileDialogService;
    private final JavaFxViewManager viewManager;
    private final GlobalExceptionHandler exceptionHandler;

    @FXML private TextField nameField;
    @FXML private TextField jobTitleField;
    @FXML private TextArea jobDescriptionArea;
    @FXML private AppSelect<InterviewDifficulty> difficultyBox;
    @FXML private TextField durationField;
    @FXML private TextField questionCountField;
    @FXML private AppSelect<ResumeDto> resumeBox;
    @FXML private AppSelect<CandidateProfileListItemDto> profileBox;
    @FXML private AppMultiSelect<String> knowledgeSelect;
    @FXML private TextField focusField;
    @FXML private AppSelect<InterviewMode> modeBox;
    @FXML private AppSelect<InterviewerPersona> personaBox;
    @FXML private AppSelect<PressureLevel> pressureBox;
    @FXML private AppSelect<VerificationStrictness> strictnessBox;
    @FXML private AppSelect<Integer> scenarioRatioBox;
    @FXML private Label pageHeadingLabel;
    @FXML private Label summaryJobLabel;
    @FXML private Label summaryDifficultyLabel;
    @FXML private Label summaryDurationLabel;
    @FXML private Label summaryQuestionsLabel;
    @FXML private Label summaryProfileLabel;
    @FXML private Label summaryKnowledgeLabel;
    @FXML private Label summaryModeLabel;
    @FXML private Label summaryPressureLabel;
    @FXML private Label summaryScenarioRatioLabel;
    @FXML private Label summaryStagesLabel;
    @FXML private Label firstQuestionLabel;
    @FXML private ImageView iconPreview;
    @FXML private CheckBox introductionStageCheck;
    @FXML private CheckBox resumeStageCheck;
    @FXML private CheckBox projectStageCheck;
    @FXML private CheckBox technicalStageCheck;
    @FXML private CheckBox systemStageCheck;
    @FXML private CheckBox codingStageCheck;
    @FXML private CheckBox behavioralStageCheck;
    @FXML private CheckBox summaryStageCheck;
    @FXML private CheckBox adaptiveFollowupCheck;
    @FXML private CheckBox reportCheck;
    @FXML private Button duplicateTopButton;
    @FXML private Button deletePlanButton;
    @FXML private VBox stageConfigContainer;

    private Long editingPlanId;
    private Path pendingIcon;
    private String persistedIconPath;
    private boolean removePersistedIcon;
    private final List<StageEditorRow> stageRows = new ArrayList<>();

    public InterviewPlanEditorController(
            InterviewPlanService planService,
            ResumeService resumeService,
            CandidateProfileService profileService,
            KnowledgeDocumentService knowledgeService,
            InterviewPlanAssetService assetService,
            InterviewPlanTransferService transferService,
            InterviewSessionService sessionService,
            UserSessionState sessionState,
            ContentNavigator contentNavigator,
            FileDialogService fileDialogService,
            JavaFxViewManager viewManager,
            GlobalExceptionHandler exceptionHandler
    ) {
        this.planService = planService;
        this.resumeService = resumeService;
        this.profileService = profileService;
        this.knowledgeService = knowledgeService;
        this.assetService = assetService;
        this.transferService = transferService;
        this.sessionService = sessionService;
        this.sessionState = sessionState;
        this.contentNavigator = contentNavigator;
        this.fileDialogService = fileDialogService;
        this.viewManager = viewManager;
        this.exceptionHandler = exceptionHandler;
    }

    @FXML
    private void initialize() {
        buildStageEditor();
        difficultyBox.getItems().setAll(InterviewDifficulty.values());
        difficultyBox.setConverter(new StringConverter<>() {
            @Override public String toString(InterviewDifficulty value) { return value == null ? "" : difficultyText(value); }
            @Override public InterviewDifficulty fromString(String value) { return null; }
        });
        modeBox.getItems().setAll(InterviewMode.values());
        modeBox.setConverter(enumConverter(this::modeText));
        personaBox.getItems().setAll(InterviewerPersona.values());
        personaBox.setConverter(enumConverter(this::personaText));
        pressureBox.getItems().setAll(PressureLevel.values());
        pressureBox.setConverter(enumConverter(this::pressureText));
        strictnessBox.getItems().setAll(VerificationStrictness.values());
        strictnessBox.setConverter(enumConverter(this::strictnessText));
        scenarioRatioBox.getItems().setAll(0, 20, 30, 50);
        scenarioRatioBox.setConverter(new StringConverter<>() {
            @Override public String toString(Integer value) { return value == null ? "" : value + "%"; }
            @Override public Integer fromString(String value) { return null; }
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
        knowledgeSelect.getItems().setAll(knowledgeService.listCategories(
                sessionState.requireCurrentUser().id()).stream().map(category -> category.name()).toList());
        nameField.textProperty().addListener((observable, oldValue, value) -> refreshSummary());
        jobTitleField.textProperty().addListener((observable, oldValue, value) -> refreshSummary());
        difficultyBox.valueProperty().addListener((observable, oldValue, value) -> refreshSummary());
        durationField.textProperty().addListener((observable, oldValue, value) -> refreshSummary());
        durationField.focusedProperty().addListener((observable, oldValue, focused) -> {
            if (!focused) rebalanceStages();
        });
        questionCountField.textProperty().addListener((observable, oldValue, value) -> refreshSummary());
        questionCountField.focusedProperty().addListener((observable, oldValue, focused) -> {
            if (!focused) rebalanceStages();
        });
        modeBox.valueProperty().addListener((observable, oldValue, value) -> {
            if (value == InterviewMode.SCENARIO_SIMULATION
                    && Integer.valueOf(0).equals(scenarioRatioBox.getValue())) {
                scenarioRatioBox.setValue(50);
            }
            refreshSummary();
        });
        personaBox.valueProperty().addListener((observable, oldValue, value) -> refreshSummary());
        pressureBox.valueProperty().addListener((observable, oldValue, value) -> refreshSummary());
        strictnessBox.valueProperty().addListener((observable, oldValue, value) -> refreshSummary());
        scenarioRatioBox.valueProperty().addListener((observable, oldValue, value) -> refreshSummary());
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
        knowledgeSelect.getSelectedItems()
                .addListener((javafx.collections.SetChangeListener<String>) change -> refreshSummary());
        for (CheckBox stage : stageChecks()) {
            stage.selectedProperty().addListener((observable, oldValue, value) -> refreshSummary());
        }
        adaptiveFollowupCheck.setSelected(true);
        reportCheck.setSelected(true);
        showIcon(null);
    }

    @Override
    public void initializeContext(Long planId) {
        editingPlanId = planId;
        duplicateTopButton.setVisible(planId != null);
        duplicateTopButton.setManaged(planId != null);
        deletePlanButton.setVisible(planId != null);
        deletePlanButton.setManaged(planId != null);
        if (planId == null) {
            pageHeadingLabel.setText("新建面试方案");
            difficultyBox.setValue(InterviewDifficulty.MEDIUM);
            durationField.setText("45");
            questionCountField.setText("15");
            applySettings(InterviewPlanSettings.defaults());
            applyStages(List.of("INTRODUCTION", "RESUME_REVIEW", "PROJECT_EXPERIENCE",
                    "TECHNICAL_DEEP_DIVE", "SYSTEM_DESIGN", "SUMMARY"));
            rebalanceStages();
        } else {
            pageHeadingLabel.setText("编辑面试方案");
            populate(planService.require(planId, sessionState.requireCurrentUser().id()));
        }
        refreshSummary();
    }

    @FXML
    private void savePlan() {
        try {
            persist();
            contentNavigator.back();
        } catch (RuntimeException exception) {
            viewManager.showError(exceptionHandler.toUserMessage(exception));
        }
    }

    @FXML
    private void cancel() {
        contentNavigator.back();
    }

    @FXML
    private void saveAndStart() {
        try {
            InterviewPlanDto saved = persist();
            var session = sessionService.startOrResume(sessionState.requireCurrentUser().id(), saved.id());
            contentNavigator.showSubPage("/fxml/interview-workspace-view.fxml", "模拟面试", session.id());
        } catch (RuntimeException exception) {
            viewManager.showError(exceptionHandler.toUserMessage(exception));
        }
    }

    @FXML
    private void choosePlanIcon() {
        Path chosen = fileDialogService.choosePlanIcon(iconPreview.getScene().getWindow()).orElse(null);
        if (chosen == null) return;
        try {
            if (!Files.isRegularFile(chosen) || Files.size(chosen) > 5L * 1024 * 1024) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED);
            }
            pendingIcon = chosen;
            removePersistedIcon = false;
            showIcon(chosen.toUri().toString());
        } catch (java.io.IOException | RuntimeException exception) {
            viewManager.showError("请选择不超过 5 MB 的 PNG、JPG 或 WebP 图片。");
        }
    }

    @FXML
    private void removePlanIcon() {
        pendingIcon = null;
        removePersistedIcon = true;
        showIcon(null);
    }

    @FXML
    private void duplicatePlan() {
        if (editingPlanId == null) return;
        try {
            InterviewPlanDto copy = planService.duplicate(sessionState.requireCurrentUser().id(), editingPlanId);
            editingPlanId = copy.id();
            pageHeadingLabel.setText("编辑面试方案");
            populate(copy);
        } catch (RuntimeException exception) {
            viewManager.showError(exceptionHandler.toUserMessage(exception));
        }
    }

    @FXML
    private void exportPlan() {
        try {
            InterviewPlanDto plan = editingPlanId == null ? persist() : planService.require(
                    editingPlanId, sessionState.requireCurrentUser().id());
            Path target = fileDialogService.choosePlanExport(iconPreview.getScene().getWindow(), plan.name()).orElse(null);
            if (target == null) return;
            transferService.exportPlan(plan, target);
            viewManager.showInfo("导出方案", "方案已导出到：\n" + target.toAbsolutePath());
        } catch (RuntimeException exception) {
            viewManager.showError(exceptionHandler.toUserMessage(exception));
        }
    }

    @FXML
    private void importPlan() {
        Path source = fileDialogService.choosePlanImport(iconPreview.getScene().getWindow()).orElse(null);
        if (source == null) return;
        try {
            InterviewPlanDto imported = transferService.importPlan(sessionState.requireCurrentUser().id(), source);
            editingPlanId = imported.id();
            pageHeadingLabel.setText("编辑导入方案");
            duplicateTopButton.setVisible(true);
            duplicateTopButton.setManaged(true);
            deletePlanButton.setVisible(true);
            deletePlanButton.setManaged(true);
            populate(imported);
        } catch (RuntimeException exception) {
            viewManager.showError(exceptionHandler.toUserMessage(exception));
        }
    }

    @FXML
    private void deletePlan() {
        if (editingPlanId == null) return;
        if (!AppDialogs.confirm(iconPreview.getScene().getWindow(), "删除面试方案", "确认删除面试方案",
                "该方案将从列表中移除，已生成的面试记录不会受影响。", "删除方案", true)) return;
        try {
            planService.delete(sessionState.requireCurrentUser().id(), editingPlanId);
            contentNavigator.back();
        } catch (RuntimeException exception) {
            viewManager.showError(exceptionHandler.toUserMessage(exception));
        }
    }

    private InterviewPlanDto persist() {
        SaveInterviewPlanCommand command = commandFromForm();
        long userId = sessionState.requireCurrentUser().id();
        InterviewPlanDto saved = editingPlanId == null
                ? planService.create(userId, command)
                : planService.update(userId, editingPlanId, command);
        editingPlanId = saved.id();
        return saved;
    }

    private void populate(InterviewPlanDto plan) {
        pendingIcon = null;
        removePersistedIcon = false;
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
        knowledgeSelect.getSelectedItems().clear();
        for (String category : knowledgeSelect.getItems()) {
            if (plan.knowledgeCategories().contains(category)) {
                knowledgeSelect.getSelectedItems().add(category);
            }
        }
        focusField.setText(String.valueOf(plan.rules().getOrDefault("focus", "")));
        persistedIconPath = stringRule(plan.rules(), InterviewPlanAssetService.ICON_PATH_RULE);
        showIcon(persistedIconPath == null ? null : Path.of(persistedIconPath).toUri().toString());
        adaptiveFollowupCheck.setSelected(booleanRule(plan.rules(), "adaptiveFollowup", true));
        reportCheck.setSelected(booleanRule(plan.rules(), "generateReport", true));
        applyStages(plan.stages());
        if (!applyStageBlueprint(plan.rules().get("stageBlueprint"))) rebalanceStages();
        applySettings(InterviewPlanSettings.fromRules(plan.rules()));
    }

    private SaveInterviewPlanCommand commandFromForm() {
        try {
            int duration = Integer.parseInt(durationField.getText().trim());
            int questions = Integer.parseInt(questionCountField.getText().trim());
            ResumeDto resume = resumeBox.getValue();
            CandidateProfileListItemDto profile = profileBox.getValue();
            List<String> categories = knowledgeSelect.getItems().stream()
                    .filter(knowledgeSelect.getSelectedItems()::contains).toList();
            if (selectedStages().isEmpty()) throw new BusinessException(ErrorCode.VALIDATION_FAILED);
            validateStageTotals(duration, questions);
            LinkedHashMap<String, Object> baseRules = new LinkedHashMap<>();
            if (!focusField.getText().isBlank()) baseRules.put("focus", focusField.getText().trim());
            String iconPath = persistedIconPath;
            if (pendingIcon != null) {
                iconPath = assetService.storeIcon(sessionState.requireCurrentUser().id(), pendingIcon);
                persistedIconPath = iconPath;
                pendingIcon = null;
            }
            if (!removePersistedIcon && iconPath != null && !iconPath.isBlank()) {
                baseRules.put(InterviewPlanAssetService.ICON_PATH_RULE, iconPath);
            }
            baseRules.put("adaptiveFollowup", adaptiveFollowupCheck.isSelected());
            baseRules.put("generateReport", reportCheck.isSelected());
            baseRules.put("stageBlueprint", stageBlueprint());
            InterviewPlanSettings settings = new InterviewPlanSettings(
                    modeBox.getValue(), personaBox.getValue(), pressureBox.getValue(),
                    strictnessBox.getValue(), scenarioRatioBox.getValue() == null
                    ? 0 : scenarioRatioBox.getValue());
            Map<String, Object> rules = settings.mergeInto(baseRules);
            return new SaveInterviewPlanCommand(nameField.getText(), jobTitleField.getText(),
                    jobDescriptionArea.getText(), difficultyBox.getValue(), duration, questions,
                    resume == null ? null : resume.id(), profile == null ? null : profile.id(),
                    List.of(), rules, selectedStages(), null, categories);
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
        if (summaryKnowledgeLabel != null && knowledgeSelect != null) {
            int count = knowledgeSelect.getSelectedItems().size();
            summaryKnowledgeLabel.setText(count == 0 ? "未选择" : count + " 个分类");
        }
        if (summaryModeLabel != null) {
            summaryModeLabel.setText(modeBox.getValue() == null ? "待选择" : modeText(modeBox.getValue()));
        }
        if (summaryPressureLabel != null) {
            summaryPressureLabel.setText(pressureBox.getValue() == null
                    ? "待选择" : pressureText(pressureBox.getValue()));
        }
        if (summaryScenarioRatioLabel != null) {
            Integer ratio = scenarioRatioBox.getValue();
            summaryScenarioRatioLabel.setText(ratio == null ? "待选择" : "沙盘 " + ratio + "%");
        }
        if (summaryStagesLabel != null) {
            List<String> stages = selectedStages();
            summaryStagesLabel.setText(stages.isEmpty() ? "请至少选择一个面试阶段"
                    : stageRows.stream().filter(row -> row.enabled().isSelected())
                    .map(row -> stageText(row.code()) + " " + parsePositive(row.minutes().getText(), 0) + " 分")
                    .reduce((a, b) -> a + " / " + b).orElse(""));
        }
        if (firstQuestionLabel != null) {
            String role = jobTitleField.getText().isBlank() ? "目标岗位" : jobTitleField.getText().trim();
            firstQuestionLabel.setText("欢迎参加本次" + role + "面试。请先做一个简短的自我介绍，并重点说明与岗位相关的项目经验。");
        }
    }

    private void buildStageEditor() {
        introductionStageCheck = createStageCheck("introductionStageCheck", "自我介绍");
        resumeStageCheck = createStageCheck("resumeStageCheck", "简历回顾");
        projectStageCheck = createStageCheck("projectStageCheck", "项目经历");
        technicalStageCheck = createStageCheck("technicalStageCheck", "技术深挖");
        systemStageCheck = createStageCheck("systemStageCheck", "系统设计");
        codingStageCheck = createStageCheck("codingStageCheck", "代码题");
        behavioralStageCheck = createStageCheck("behavioralStageCheck", "行为面试");
        summaryStageCheck = createStageCheck("summaryStageCheck", "总结");

        stageRows.add(new StageEditorRow("INTRODUCTION", introductionStageCheck,
                "候选人自我介绍与背景确认", 1, 3, 5));
        stageRows.add(new StageEditorRow("RESUME_REVIEW", resumeStageCheck,
                "围绕简历经历进行证据追问", 2, 5, 10));
        stageRows.add(new StageEditorRow("PROJECT_EXPERIENCE", projectStageCheck,
                "深入项目细节与技术实现", 4, 13, 25));
        stageRows.add(new StageEditorRow("TECHNICAL_DEEP_DIVE", technicalStageCheck,
                "核心技术点追问与原理考察", 4, 12, 25));
        stageRows.add(new StageEditorRow("SYSTEM_DESIGN", systemStageCheck,
                "系统设计与架构能力评估", 2, 8, 20));
        stageRows.add(new StageEditorRow("CODING", codingStageCheck,
                "代码实现与边界分析", 2, 8, 15));
        stageRows.add(new StageEditorRow("BEHAVIORAL", behavioralStageCheck,
                "协作、冲突与复盘能力", 1, 5, 10));
        stageRows.add(new StageEditorRow("SUMMARY", summaryStageCheck,
                "面试总结与候选人提问", 2, 4, 15));

        HBox header = new HBox(8,
                stageHeader("阶段", 150), stageHeader("说明", 250),
                stageHeader("题目数", 62), stageHeader("时长", 62), stageHeader("权重 %", 68));
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("plan-stage-table-header");
        stageConfigContainer.getChildren().add(header);
        for (StageEditorRow row : stageRows) {
            row.enabled().selectedProperty().addListener((observable, oldValue, selected) -> {
                row.setControlsDisabled(!selected);
                refreshSummary();
            });
            row.setControlsDisabled(true);
            stageConfigContainer.getChildren().add(row.view());
        }
    }

    private CheckBox createStageCheck(String id, String text) {
        CheckBox check = new CheckBox(text);
        check.setId(id);
        check.getStyleClass().add("plan-stage-table-check");
        check.setMinWidth(150);
        check.setPrefWidth(150);
        return check;
    }

    private Label stageHeader(String text, double width) {
        Label label = new Label(text);
        label.setMinWidth(width);
        label.setPrefWidth(width);
        label.getStyleClass().add("plan-stage-table-heading");
        return label;
    }

    @FXML
    private void rebalanceStages() {
        int totalMinutes = parsePositive(durationField == null ? null : durationField.getText(), 45);
        int totalQuestions = parsePositive(questionCountField == null ? null : questionCountField.getText(), 15);
        List<StageEditorRow> enabled = stageRows.stream().filter(row -> row.enabled().isSelected()).toList();
        if (enabled.isEmpty()) return;
        int totalBase = enabled.stream().mapToInt(StageEditorRow::baseWeight).sum();
        int remainingMinutes = totalMinutes;
        int remainingQuestions = totalQuestions;
        int remainingWeight = 100;
        for (int index = 0; index < enabled.size(); index++) {
            StageEditorRow row = enabled.get(index);
            boolean last = index == enabled.size() - 1;
            int weight = last ? remainingWeight : Math.max(1, (int) Math.round(100.0 * row.baseWeight() / totalBase));
            int minutes = last ? remainingMinutes : Math.max(1, (int) Math.round(totalMinutes * weight / 100.0));
            int questions = last ? remainingQuestions : Math.max(0, (int) Math.round(totalQuestions * weight / 100.0));
            if (!last) {
                minutes = Math.min(minutes, Math.max(1, remainingMinutes - (enabled.size() - index - 1)));
                questions = Math.min(questions, Math.max(0, remainingQuestions));
            }
            row.questions().setText(Integer.toString(questions));
            row.minutes().setText(Integer.toString(minutes));
            row.weight().setText(Integer.toString(weight));
            remainingQuestions -= questions;
            remainingMinutes -= minutes;
            remainingWeight -= weight;
        }
        refreshSummary();
    }

    private int parsePositive(String text, int fallback) {
        try {
            int value = Integer.parseInt(text == null ? "" : text.strip());
            return value > 0 ? value : fallback;
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private void validateStageTotals(int duration, int questions) {
        int stageMinutes = stageRows.stream().filter(row -> row.enabled().isSelected())
                .mapToInt(row -> parsePositive(row.minutes().getText(), 0)).sum();
        int stageQuestions = stageRows.stream().filter(row -> row.enabled().isSelected())
                .mapToInt(row -> parsePositive(row.questions().getText(), 0)).sum();
        int stageWeight = stageRows.stream().filter(row -> row.enabled().isSelected())
                .mapToInt(row -> parsePositive(row.weight().getText(), 0)).sum();
        if (stageMinutes != duration || stageQuestions != questions || stageWeight != 100) {
            throw new BusinessException(ErrorCode.PLAN_STAGE_TOTAL_INVALID);
        }
    }

    private List<Map<String, Object>> stageBlueprint() {
        return stageRows.stream().map(row -> Map.<String, Object>of(
                "stage", row.code(), "enabled", row.enabled().isSelected(),
                "questions", parsePositive(row.questions().getText(), 0),
                "minutes", parsePositive(row.minutes().getText(), 0),
                "weight", parsePositive(row.weight().getText(), 0))).toList();
    }

    private boolean applyStageBlueprint(Object raw) {
        if (!(raw instanceof List<?> list) || list.isEmpty()) return false;
        boolean applied = false;
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> values)) continue;
            String stage = String.valueOf(values.get("stage"));
            StageEditorRow row = stageRows.stream().filter(candidate -> candidate.code().equals(stage)).findFirst().orElse(null);
            if (row == null) continue;
            row.enabled().setSelected(Boolean.parseBoolean(String.valueOf(
                    values.containsKey("enabled") ? values.get("enabled") : false)));
            row.questions().setText(String.valueOf(values.containsKey("questions") ? values.get("questions") : 0));
            row.minutes().setText(String.valueOf(values.containsKey("minutes") ? values.get("minutes") : 0));
            row.weight().setText(String.valueOf(values.containsKey("weight") ? values.get("weight") : 0));
            applied = true;
        }
        return applied;
    }

    private List<CheckBox> stageChecks() {
        return List.of(introductionStageCheck, resumeStageCheck, projectStageCheck, technicalStageCheck,
                systemStageCheck, codingStageCheck, behavioralStageCheck, summaryStageCheck);
    }

    private List<String> selectedStages() {
        List<String> stages = new ArrayList<>();
        if (introductionStageCheck.isSelected()) stages.add("INTRODUCTION");
        if (resumeStageCheck.isSelected()) stages.add("RESUME_REVIEW");
        if (projectStageCheck.isSelected()) stages.add("PROJECT_EXPERIENCE");
        if (technicalStageCheck.isSelected()) stages.add("TECHNICAL_DEEP_DIVE");
        if (systemStageCheck.isSelected()) stages.add("SYSTEM_DESIGN");
        if (codingStageCheck.isSelected()) stages.add("CODING");
        if (behavioralStageCheck.isSelected()) stages.add("BEHAVIORAL");
        if (summaryStageCheck.isSelected()) stages.add("SUMMARY");
        return List.copyOf(stages);
    }

    private void applyStages(List<String> stages) {
        List<String> safeStages = stages == null ? List.of() : stages;
        introductionStageCheck.setSelected(safeStages.contains("INTRODUCTION"));
        resumeStageCheck.setSelected(safeStages.contains("RESUME_REVIEW"));
        projectStageCheck.setSelected(safeStages.contains("PROJECT_EXPERIENCE"));
        technicalStageCheck.setSelected(safeStages.contains("TECHNICAL_DEEP_DIVE"));
        systemStageCheck.setSelected(safeStages.contains("SYSTEM_DESIGN"));
        codingStageCheck.setSelected(safeStages.contains("CODING"));
        behavioralStageCheck.setSelected(safeStages.contains("BEHAVIORAL"));
        summaryStageCheck.setSelected(safeStages.contains("SUMMARY"));
    }

    private final class StageEditorRow {
        private final String code;
        private final CheckBox enabled;
        private final int baseWeight;
        private final TextField questions = metricField();
        private final TextField minutes = metricField();
        private final TextField weight = metricField();
        private final HBox view;

        private StageEditorRow(String code, CheckBox enabled, String description,
                               int questions, int minutes, int baseWeight) {
            this.code = code;
            this.enabled = enabled;
            this.baseWeight = baseWeight;
            this.questions.setText(Integer.toString(questions));
            this.minutes.setText(Integer.toString(minutes));
            this.weight.setText(Integer.toString(baseWeight));
            Label descriptionLabel = new Label(description);
            descriptionLabel.setMinWidth(250);
            descriptionLabel.setPrefWidth(250);
            descriptionLabel.getStyleClass().add("plan-stage-description");
            view = new HBox(8, enabled, descriptionLabel, this.questions, this.minutes, this.weight);
            view.setAlignment(Pos.CENTER_LEFT);
            view.getStyleClass().add("plan-stage-table-row");
        }

        private TextField metricField() {
            TextField field = new TextField();
            field.setMinWidth(62);
            field.setPrefWidth(62);
            field.setMaxWidth(68);
            field.getStyleClass().add("plan-stage-metric-field");
            field.textProperty().addListener((observable, oldValue, value) -> refreshSummary());
            return field;
        }

        private void setControlsDisabled(boolean disabled) {
            questions.setDisable(disabled);
            minutes.setDisable(disabled);
            weight.setDisable(disabled);
        }

        private String code() { return code; }
        private CheckBox enabled() { return enabled; }
        private int baseWeight() { return baseWeight; }
        private TextField questions() { return questions; }
        private TextField minutes() { return minutes; }
        private TextField weight() { return weight; }
        private HBox view() { return view; }
    }

    private String stageText(String stage) {
        return switch (stage) {
            case "INTRODUCTION" -> "自我介绍";
            case "RESUME_REVIEW" -> "简历回顾";
            case "PROJECT_EXPERIENCE" -> "项目经历";
            case "TECHNICAL_DEEP_DIVE" -> "技术深挖";
            case "SYSTEM_DESIGN" -> "系统设计";
            case "CODING" -> "代码题";
            case "BEHAVIORAL" -> "行为面试";
            case "SUMMARY" -> "总结";
            default -> stage;
        };
    }

    private String stringRule(Map<String, Object> rules, String key) {
        Object value = rules.get(key);
        return value == null || String.valueOf(value).isBlank() ? null : String.valueOf(value);
    }

    private boolean booleanRule(Map<String, Object> rules, String key, boolean fallback) {
        Object value = rules.get(key);
        return value == null ? fallback : Boolean.parseBoolean(String.valueOf(value));
    }

    private void showIcon(String url) {
        try {
            String source = url == null || url.isBlank()
                    ? getClass().getResource("/images/plan/plan-placeholder.png").toExternalForm() : url;
            iconPreview.setImage(new Image(source));
        } catch (RuntimeException exception) {
            iconPreview.setImage(new Image(getClass().getResource(
                    "/images/plan/plan-placeholder.png").toExternalForm()));
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

    private void applySettings(InterviewPlanSettings settings) {
        modeBox.setValue(settings.mode());
        personaBox.setValue(settings.persona());
        pressureBox.setValue(settings.pressureLevel());
        strictnessBox.setValue(settings.strictness());
        scenarioRatioBox.setValue(settings.scenarioRatio());
    }

    private String modeText(InterviewMode mode) {
        return switch (mode) {
            case FORMAL_SIMULATION -> "正式模拟";
            case COACHING -> "教练训练";
            case SCENARIO_SIMULATION -> "情境沙盘";
            case RE_TEST -> "复试";
        };
    }

    private String personaText(InterviewerPersona persona) {
        return switch (persona) {
            case PROFESSIONAL_INTERVIEWER -> "专业面试官";
            case FUTURE_PEER -> "未来同事";
            case TECH_LEAD -> "技术负责人";
            case ARCHITECT -> "架构师";
            case INCIDENT_COMMANDER -> "事故指挥者";
            case PRODUCT_LEADER -> "产品负责人";
        };
    }

    private String pressureText(PressureLevel level) {
        return switch (level) {
            case RELAXED -> "轻松";
            case STANDARD -> "标准";
            case CHALLENGING -> "挑战";
            case HIGH_PRESSURE -> "高压";
        };
    }

    private String strictnessText(VerificationStrictness strictness) {
        return strictness == VerificationStrictness.STRICT ? "严格" : "标准";
    }

    private <T> StringConverter<T> enumConverter(java.util.function.Function<T, String> display) {
        return new StringConverter<>() {
            @Override public String toString(T value) { return value == null ? "" : display.apply(value); }
            @Override public T fromString(String value) { return null; }
        };
    }
}
