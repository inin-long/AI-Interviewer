package com.inin.aiinterviewer.ui.controller;

import com.inin.aiinterviewer.application.dto.InterviewPlanDto;
import com.inin.aiinterviewer.application.dto.ResumeDto;
import com.inin.aiinterviewer.application.dto.SaveInterviewPlanCommand;
import com.inin.aiinterviewer.application.dto.CandidateProfileListItemDto;
import com.inin.aiinterviewer.application.dto.DomainPackDto;
import com.inin.aiinterviewer.application.exception.BusinessException;
import com.inin.aiinterviewer.application.exception.ErrorCode;
import com.inin.aiinterviewer.application.exception.GlobalExceptionHandler;
import com.inin.aiinterviewer.application.service.InterviewPlanService;
import com.inin.aiinterviewer.application.service.ResumeService;
import com.inin.aiinterviewer.application.service.CandidateProfileService;
import com.inin.aiinterviewer.application.service.DomainPackService;
import com.inin.aiinterviewer.application.service.KnowledgeDocumentService;
import com.inin.aiinterviewer.domain.entity.JobPositionEntity;
import com.inin.aiinterviewer.infrastructure.database.mapper.JobPositionMapper;
import com.inin.aiinterviewer.domain.enums.InterviewDifficulty;
import com.inin.aiinterviewer.domain.enums.InterviewMode;
import com.inin.aiinterviewer.domain.enums.InterviewerPersona;
import com.inin.aiinterviewer.domain.enums.PressureLevel;
import com.inin.aiinterviewer.domain.enums.VerificationStrictness;
import com.inin.aiinterviewer.domain.model.InterviewPlanSettings;
import com.inin.aiinterviewer.domain.model.DomainPackSnapshot;
import com.inin.aiinterviewer.ui.navigation.ContentNavigator;
import com.inin.aiinterviewer.ui.navigation.ContextAwareController;
import com.inin.aiinterviewer.ui.navigation.JavaFxViewManager;
import com.inin.aiinterviewer.ui.state.UserSessionState;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ListView;
import javafx.scene.control.ListCell;
import javafx.scene.control.SelectionMode;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.StringConverter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import com.inin.aiinterviewer.application.dto.KnowledgeDocumentDto;

@Component
@Scope("prototype")
public class InterviewPlanEditorController implements ContextAwareController<Long> {

    private final InterviewPlanService planService;
    private final ResumeService resumeService;
    private final CandidateProfileService profileService;
    private final KnowledgeDocumentService knowledgeService;
    private final DomainPackService domainPackService;
    private final JobPositionMapper jobPositionMapper;
    private final UserSessionState sessionState;
    private final ContentNavigator contentNavigator;
    private final JavaFxViewManager viewManager;
    private final GlobalExceptionHandler exceptionHandler;
    private final ApplicationContext applicationContext;

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
    @FXML private ComboBox<DomainPackDto> domainPackBox;
    @FXML private ComboBox<InterviewMode> modeBox;
    @FXML private ComboBox<InterviewerPersona> personaBox;
    @FXML private ComboBox<PressureLevel> pressureBox;
    @FXML private ComboBox<VerificationStrictness> strictnessBox;
    @FXML private ComboBox<Integer> scenarioRatioBox;
    @FXML private Label pageHeadingLabel;
    @FXML private Label summaryJobLabel;
    @FXML private Label summaryDifficultyLabel;
    @FXML private Label summaryDurationLabel;
    @FXML private Label summaryQuestionsLabel;
    @FXML private Label summaryProfileLabel;
    @FXML private Label summaryKnowledgeLabel;
    @FXML private Label summaryDomainPackLabel;
    @FXML private Label summaryModeLabel;
    @FXML private Label summaryPressureLabel;
    @FXML private Label summaryScenarioRatioLabel;
    @FXML private TextField scenarioField;
    @FXML private ComboBox<String> answerTimeBox;
    @FXML private TextField customTimeField;
    @FXML private Label summaryScenarioLabel;
    @FXML private Label summaryTimeLimitLabel;
    @FXML private Label jdAutoFillHint;

    private Long editingPlanId;
    private boolean suppressAutoFill = false;
    private String lastAutoRole = null;
    private List<JobPositionEntity> allPositions = new ArrayList<>();
    private boolean jdUserEdited = false;

    public InterviewPlanEditorController(
            InterviewPlanService planService,
            ResumeService resumeService,
            CandidateProfileService profileService,
            KnowledgeDocumentService knowledgeService,
            DomainPackService domainPackService,
            JobPositionMapper jobPositionMapper,
            UserSessionState sessionState,
            ContentNavigator contentNavigator,
            JavaFxViewManager viewManager,
            GlobalExceptionHandler exceptionHandler,
            ApplicationContext applicationContext
    ) {
        this.planService = planService;
        this.resumeService = resumeService;
        this.profileService = profileService;
        this.knowledgeService = knowledgeService;
        this.domainPackService = domainPackService;
        this.jobPositionMapper = jobPositionMapper;
        this.sessionState = sessionState;
        this.contentNavigator = contentNavigator;
        this.viewManager = viewManager;
        this.exceptionHandler = exceptionHandler;
        this.applicationContext = applicationContext;
    }

    @FXML
    private void initialize() {
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
        answerTimeBox.getItems().setAll("不限时", "3 分钟", "5 分钟", "10 分钟", "自定义");
        answerTimeBox.setValue("不限时");
        customTimeField.setDisable(true);
        customTimeField.setPromptText("自定义分钟");
        answerTimeBox.valueProperty().addListener((observable, oldValue, value) -> {
            boolean custom = "自定义".equals(value);
            customTimeField.setDisable(!custom);
            if (!custom) customTimeField.clear();
            refreshSummary();
        });
        customTimeField.textProperty().addListener((observable, oldValue, value) -> refreshSummary());
        scenarioField.textProperty().addListener((observable, oldValue, value) -> refreshSummary());
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
        domainPackBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(DomainPackDto value) {
                if (value == null) return "请选择岗位知识包";
                if (DomainPackSnapshot.NONE_PACK_ID.equals(value.id())) return "无知识包（不使用领域包）";
                return value.displayName() + " · v" + value.version();
            }
            @Override public DomainPackDto fromString(String value) { return null; }
        });
        // 选项里始终包含「无知识包」模式，并默认选中它
        List<DomainPackDto> packItems = new ArrayList<>();
        packItems.add(new DomainPackDto(DomainPackSnapshot.NONE_PACK_ID, "", null, "", "无知识包（不使用领域包）", ""));
        packItems.addAll(domainPackService.list());
        domainPackBox.getItems().setAll(packItems);
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
        // 加载所有岗位用于 JD 自动匹配（develop 的岗位库自动填充能力）
        allPositions = jobPositionMapper.findAllByUserId(sessionState.requireCurrentUser().id());
        nameField.textProperty().addListener((observable, oldValue, value) -> refreshSummary());
        jobTitleField.textProperty().addListener((observable, oldValue, value) -> {
            if (lastAutoRole != null && !value.equals(lastAutoRole)) {
                lastAutoRole = null;
            }
            if (!suppressAutoFill && !jdUserEdited) {
                tryAutoFillJobDescription(value);
            }
            refreshSummary();
        });
        // 用户手动编辑 JD 后标记，不再自动覆盖
        jobDescriptionArea.textProperty().addListener((obs, old, val) -> {
            if (!suppressAutoFill && val != null && !val.isBlank() && !val.equals(jobDescriptionArea.getPromptText())) {
                jdUserEdited = true;
                if (jdAutoFillHint != null) jdAutoFillHint.setVisible(false);
            }
        });
        difficultyBox.valueProperty().addListener((observable, oldValue, value) -> refreshSummary());
        durationField.textProperty().addListener((observable, oldValue, value) -> refreshSummary());
        questionCountField.textProperty().addListener((observable, oldValue, value) -> refreshSummary());
        domainPackBox.valueProperty().addListener((observable, oldValue, value) -> refreshSummary());
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
            if (suppressAutoFill) { refreshSummary(); return; }
            if (profile != null) {
                resumeBox.getItems().stream().filter(resume -> resume.id().equals(profile.resumeId()))
                        .findFirst().ifPresent(resumeBox::setValue);
            }
            applyTargetRole(profile);
            refreshSummary();
        });
        resumeBox.valueProperty().addListener((observable, oldValue, resume) -> {
            if (suppressAutoFill) return;
            CandidateProfileListItemDto profile = profileBox.getValue();
            if (profile != null && (resume == null || !profile.resumeId().equals(resume.id()))) {
                profileBox.setValue(null);
            }
            applyTargetRole(resume);
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
            applySettings(InterviewPlanSettings.defaults());
            domainPackBox.getItems().stream()
                    .filter(pack -> DomainPackSnapshot.NONE_PACK_ID.equals(pack.id()))
                    .findFirst().ifPresent(domainPackBox::setValue);
        } else {
            pageHeadingLabel.setText("编辑面试方案");
            populate(planService.require(planId, sessionState.requireCurrentUser().id()));
        }
        refreshSummary();
    }

    @FXML
    private void onManagePacks() {
        try {
            URL location = getClass().getResource("/fxml/domain-pack-manager-view.fxml");
            FXMLLoader loader = new FXMLLoader(location);
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = (Parent) loader.load();
            DomainPackManagerController controller = loader.getController();
            controller.prefill(jobDescriptionArea.getText());
            Stage stage = new Stage();
            stage.setTitle("管理岗位知识包");
            stage.initModality(Modality.WINDOW_MODAL);
            Window owner = domainPackBox.getScene() == null ? null : domainPackBox.getScene().getWindow();
            if (owner != null) stage.initOwner(owner);
            Scene scene = new Scene(root, 800, 720);
            URL stylesheet = getClass().getResource("/css/app.css");
            if (stylesheet != null) scene.getStylesheets().add(stylesheet.toExternalForm());
            stage.setScene(scene);
            stage.showAndWait();
            refreshDomainPackBox();
        } catch (IOException exception) {
            viewManager.showError("无法打开知识包管理：" + exception.getMessage());
        }
    }

    private void refreshDomainPackBox() {
        List<DomainPackDto> packItems = new ArrayList<>();
        packItems.add(new DomainPackDto(DomainPackSnapshot.NONE_PACK_ID, "", null, "", "无知识包（不使用领域包）", ""));
        packItems.addAll(domainPackService.list());
        DomainPackDto current = domainPackBox.getValue();
        domainPackBox.getItems().setAll(packItems);
        if (current != null && current.id() != null) {
            packItems.stream().filter(pack -> current.id().equals(pack.id()))
                    .findFirst().ifPresent(domainPackBox::setValue);
        } else {
            packItems.stream().filter(pack -> DomainPackSnapshot.NONE_PACK_ID.equals(pack.id()))
                    .findFirst().ifPresent(domainPackBox::setValue);
        }
    }

    @FXML
    private void savePlan() {
        // 先做面向用户的具体校验：校验不过直接提示原因并返回，避免只弹「请检查输入内容」
        String formError = validatePlanForm();
        if (formError != null) {
            viewManager.showError(formError);
            return;
        }
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

    /**
     * 保存前的前端表单校验，返回面向用户的具体错误提示（多项错误以换行分隔）；全部通过返回 null。
     * 覆盖触发「请检查输入内容」最常见的必填项与取值范围，与服务层 toEntity 校验规则保持一致，后者仍保留兜底。
     */
    private String validatePlanForm() {
        List<String> errors = new ArrayList<>();

        String name = nameField.getText() == null ? "" : nameField.getText().trim();
        if (name.isBlank()) {
            errors.add("请填写方案名称");
        } else if (name.length() > 128) {
            errors.add("方案名称不能超过 128 个字");
        }

        String jobTitle = jobTitleField.getText() == null ? "" : jobTitleField.getText().trim();
        if (jobTitle.isBlank()) {
            errors.add("请填写岗位名称");
        } else if (jobTitle.length() > 128) {
            errors.add("岗位名称不能超过 128 个字");
        }

        String durationText = durationField.getText() == null ? "" : durationField.getText().trim();
        if (durationText.isEmpty()) {
            errors.add("请填写面试时长（10-240 分钟）");
        } else {
            try {
                int duration = Integer.parseInt(durationText);
                if (duration < 10 || duration > 240) {
                    errors.add("面试时长需在 10 到 240 分钟之间");
                }
            } catch (NumberFormatException ignore) {
                errors.add("面试时长需为数字（10-240 分钟）");
            }
        }

        String countText = questionCountField.getText() == null ? "" : questionCountField.getText().trim();
        if (countText.isEmpty()) {
            errors.add("请填写题目数量（1-100 题）");
        } else {
            try {
                int count = Integer.parseInt(countText);
                if (count < 1 || count > 100) {
                    errors.add("题目数量需在 1 到 100 题之间");
                }
            } catch (NumberFormatException ignore) {
                errors.add("题目数量需为数字（1-100 题）");
            }
        }

        if (errors.isEmpty()) return null;
        return String.join("\n", errors);
    }

    @FXML
    private void cancel() {
        try {
            contentNavigator.back();
        } catch (Exception ex) {
            viewManager.showError("返回失败：" + exceptionHandler.toUserMessage(ex));
        }
    }

    private void populate(InterviewPlanDto plan) {
        suppressAutoFill = true;
        jdUserEdited = true; // 编辑已有方案时不自动覆盖 JD
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
        applySettings(InterviewPlanSettings.fromRules(plan.rules()));
        scenarioField.setText(InterviewPlanSettings.scenarioOf(plan.rules()));
        setAnswerTimeLimitUi(InterviewPlanSettings.answerTimeLimitSecondsOf(plan.rules()));
        domainPackBox.getItems().stream().filter(pack -> pack.id().equals(plan.domainPackId()))
                .findFirst().ifPresent(domainPackBox::setValue);
        suppressAutoFill = false;
        lastAutoRole = jobTitleField.getText().isBlank() ? null : jobTitleField.getText();
    }

    private SaveInterviewPlanCommand commandFromForm() {
        try {
            int duration = Integer.parseInt(durationField.getText().trim());
            int questions = Integer.parseInt(questionCountField.getText().trim());
            ResumeDto resume = resumeBox.getValue();
            CandidateProfileListItemDto profile = profileBox.getValue();
            List<Long> documentIds = knowledgeList.getSelectionModel().getSelectedItems().stream()
                    .map(KnowledgeDocumentDto::id).toList();
            LinkedHashMap<String, Object> baseRules = new LinkedHashMap<>();
            if (!focusField.getText().isBlank()) baseRules.put("focus", focusField.getText().trim());
            if (!scenarioField.getText().isBlank()) baseRules.put("scenario", scenarioField.getText().trim());
            baseRules.put("answerTimeLimitSeconds", answerTimeLimitSecondsFromForm());
            InterviewPlanSettings settings = new InterviewPlanSettings(
                    modeBox.getValue(), personaBox.getValue(), pressureBox.getValue(),
                    strictnessBox.getValue(), scenarioRatioBox.getValue() == null
                    ? 0 : scenarioRatioBox.getValue());
            Map<String, Object> rules = settings.mergeInto(baseRules);
            DomainPackDto domainPack = domainPackBox.getValue();
            return new SaveInterviewPlanCommand(nameField.getText(), jobTitleField.getText(),
                    jobDescriptionArea.getText(), difficultyBox.getValue(), duration, questions,
                    resume == null ? null : resume.id(), profile == null ? null : profile.id(),
                    documentIds, rules, null, domainPack == null ? null : domainPack.id());
        } catch (NumberFormatException exception) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
    }

    /** 根据目标岗位名称模糊匹配 job_position 表，自动填充 JD（develop 岗位库自动填充能力） */
    private void tryAutoFillJobDescription(String title) {
        if (title == null || title.strip().length() < 2) return;
        try {
            doAutoFillJobDescription(title.strip());
        } catch (Exception ignored) {
            // 岗位匹配失败不应影响任何 UI 操作
        }
    }

    private void doAutoFillJobDescription(String trimmed) {
        String lower = trimmed.toLowerCase();
        // 1. 精确匹配（忽略大小写）
        for (JobPositionEntity pos : allPositions) {
            if (pos.getTitle() != null && pos.getTitle().toLowerCase().equals(lower)
                    && pos.getDescription() != null && !pos.getDescription().isBlank()) {
                setJdFromPosition(pos);
                return;
            }
        }
        // 2. 包含匹配（标题包含输入 或 输入包含标题）
        for (JobPositionEntity pos : allPositions) {
            if (pos.getTitle() == null || pos.getTitle().isBlank()) continue;
            String pt = pos.getTitle().toLowerCase();
            if (lower.contains(pt) || pt.contains(lower)) {
                if (pos.getDescription() != null && !pos.getDescription().isBlank()) {
                    setJdFromPosition(pos);
                    return;
                }
            }
        }
    }

    private void setJdFromPosition(JobPositionEntity pos) {
        suppressAutoFill = true;
        jobDescriptionArea.setText(pos.getDescription());
        suppressAutoFill = false;
        if (jdAutoFillHint != null) {
            jdAutoFillHint.setText("已从岗位库「" + pos.getTitle() + "」自动加载描述，可直接编辑修改");
            jdAutoFillHint.setVisible(true);
            jdAutoFillHint.setManaged(true);
        }
        jdUserEdited = false;
    }

    private void applyTargetRole(CandidateProfileListItemDto profile) {
        if (profile == null) return;
        String role = profile.targetRole();
        if (role == null || role.isBlank()) return;
        role = role.strip();
        String current = jobTitleField.getText();
        if (current.isBlank() || current.equals(lastAutoRole)) {
            jobTitleField.setText(role);
            lastAutoRole = role;
        }
    }

    private void applyTargetRole(ResumeDto resume) {
        if (resume == null) return;
        CandidateProfileListItemDto profile = profileBox.getItems().stream()
                .filter(p -> resume.id().equals(p.resumeId()))
                .findFirst().orElse(null);
        applyTargetRole(profile);
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
        if (summaryDomainPackLabel != null && domainPackBox != null) {
            DomainPackDto pack = domainPackBox.getValue();
            if (pack == null) {
                summaryDomainPackLabel.setText("待选择");
            } else if (DomainPackSnapshot.NONE_PACK_ID.equals(pack.id())) {
                summaryDomainPackLabel.setText("无知识包");
            } else {
                summaryDomainPackLabel.setText(pack.displayName() + " · v" + pack.version());
            }
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
            summaryScenarioRatioLabel.setText(ratio == null ? "待选择" : ratio + "%");
        }
        if (summaryScenarioLabel != null) {
            String sc = scenarioField.getText() == null ? "" : scenarioField.getText().strip();
            summaryScenarioLabel.setText(sc.isBlank() ? "未设置" : sc);
        }
        if (summaryTimeLimitLabel != null) {
            Integer secs = answerTimeLimitSecondsFromForm();
            summaryTimeLimitLabel.setText(secs == null ? "不限时" : (secs / 60) + " 分钟");
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
        scenarioField.setText("");
        setAnswerTimeLimitUi(null);
    }

    private Integer answerTimeLimitSecondsFromForm() {
        String selection = answerTimeBox.getValue();
        if (selection == null || "不限时".equals(selection)) return null;
        if ("自定义".equals(selection)) {
            String text = customTimeField.getText();
            if (text == null || text.strip().isBlank()) return null;
            try {
                int minutes = Integer.parseInt(text.strip());
                if (minutes <= 0) return null;
                return Math.min(minutes, 60) * 60;
            } catch (NumberFormatException exception) {
                return null;
            }
        }
        int minutes = Integer.parseInt(selection.replaceAll("\\D", ""));
        return minutes * 60;
    }

    private void setAnswerTimeLimitUi(Integer seconds) {
        if (seconds == null || seconds <= 0) {
            answerTimeBox.setValue("不限时");
            customTimeField.clear();
            customTimeField.setDisable(true);
            return;
        }
        int minutes = seconds / 60;
        if (minutes == 3 || minutes == 5 || minutes == 10) {
            answerTimeBox.setValue(minutes + " 分钟");
            customTimeField.clear();
            customTimeField.setDisable(true);
        } else {
            answerTimeBox.setValue("自定义");
            customTimeField.setText(String.valueOf(minutes));
            customTimeField.setDisable(false);
        }
    }

    private String modeText(InterviewMode mode) {
        return switch (mode) {
            case FORMAL_SIMULATION -> "正式模拟";
            case COACHING -> "教练训练";
            case SCENARIO_SIMULATION -> "情境沙盘";
        };
    }

    private String personaText(InterviewerPersona persona) {
        return switch (persona) {
            case FRIENDLY -> "友好型";
            case SERIOUS -> "严肃型";
            case PRESSURE -> "压力型";
            case TECHNICAL -> "技术性";
            case MENTOR -> "导师型";
            case HUMOROUS -> "幽默型";
            case PROFESSIONAL_INTERVIEWER -> "专业面试官";
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
