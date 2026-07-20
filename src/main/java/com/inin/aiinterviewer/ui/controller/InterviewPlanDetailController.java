package com.inin.aiinterviewer.ui.controller;

import com.inin.aiinterviewer.application.dto.InterviewPlanDto;
import com.inin.aiinterviewer.application.dto.InterviewSessionDto;
import com.inin.aiinterviewer.application.exception.GlobalExceptionHandler;
import com.inin.aiinterviewer.application.service.DomainPackService;
import com.inin.aiinterviewer.application.service.InterviewPlanAssetService;
import com.inin.aiinterviewer.application.service.InterviewPlanService;
import com.inin.aiinterviewer.application.service.InterviewPlanTransferService;
import com.inin.aiinterviewer.application.service.InterviewSessionService;
import com.inin.aiinterviewer.application.service.ResumeService;
import com.inin.aiinterviewer.domain.enums.InterviewDifficulty;
import com.inin.aiinterviewer.domain.enums.InterviewMode;
import com.inin.aiinterviewer.domain.enums.InterviewerPersona;
import com.inin.aiinterviewer.domain.enums.PressureLevel;
import com.inin.aiinterviewer.domain.enums.VerificationStrictness;
import com.inin.aiinterviewer.domain.model.InterviewPlanSettings;
import com.inin.aiinterviewer.ui.component.AppDialogs;
import com.inin.aiinterviewer.ui.dialog.FileDialogService;
import com.inin.aiinterviewer.ui.navigation.ContentNavigator;
import com.inin.aiinterviewer.ui.navigation.ContextAwareController;
import com.inin.aiinterviewer.ui.navigation.JavaFxViewManager;
import com.inin.aiinterviewer.ui.state.UserSessionState;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
@Scope("prototype")
public class InterviewPlanDetailController implements ContextAwareController<Long> {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final InterviewPlanService planService;
    private final InterviewSessionService sessionService;
    private final ResumeService resumeService;
    private final DomainPackService domainPackService;
    private final InterviewPlanTransferService transferService;
    private final UserSessionState sessionState;
    private final ContentNavigator contentNavigator;
    private final FileDialogService fileDialogService;
    private final JavaFxViewManager viewManager;
    private final GlobalExceptionHandler exceptionHandler;

    @FXML private Label breadcrumbLabel;
    @FXML private ImageView iconView;
    @FXML private Label nameLabel;
    @FXML private Label stateLabel;
    @FXML private FlowPane tagContainer;
    @FXML private Label descriptionLabel;
    @FXML private Label suitableLabel;
    @FXML private Label createdLabel;
    @FXML private Label updatedLabel;
    @FXML private Label questionLabel;
    @FXML private Label resumeCountLabel;
    @FXML private Label knowledgeCountLabel;
    @FXML private FlowPane stageContainer;
    @FXML private Label durationTotalLabel;
    @FXML private Label jobLabel;
    @FXML private Label difficultyLabel;
    @FXML private Label modeLabel;
    @FXML private Label personaLabel;
    @FXML private Label pressureLabel;
    @FXML private Label strictnessLabel;
    @FXML private Label scenarioLabel;
    @FXML private Label domainPackLabel;
    @FXML private Label resumeLabel;
    @FXML private Label profileLabel;
    @FXML private FlowPane knowledgeContainer;
    @FXML private Label documentCountLabel;
    @FXML private FlowPane focusContainer;
    @FXML private Label ruleSummaryLabel;
    @FXML private Label usageCountLabel;
    @FXML private VBox usageContainer;
    @FXML private ProgressIndicator completenessIndicator;
    @FXML private Label completenessLabel;
    @FXML private Label stageCountLabel;
    @FXML private Label sideQuestionLabel;
    @FXML private Label sideResumeLabel;
    @FXML private Label sideKnowledgeLabel;
    @FXML private Label ruleCountLabel;
    @FXML private VBox sceneContainer;
    @FXML private VBox directionContainer;
    @FXML private VBox riskContainer;
    @FXML private VBox adviceContainer;

    private InterviewPlanDto plan;

    public InterviewPlanDetailController(
            InterviewPlanService planService,
            InterviewSessionService sessionService,
            ResumeService resumeService,
            DomainPackService domainPackService,
            InterviewPlanTransferService transferService,
            UserSessionState sessionState,
            ContentNavigator contentNavigator,
            FileDialogService fileDialogService,
            JavaFxViewManager viewManager,
            GlobalExceptionHandler exceptionHandler
    ) {
        this.planService = planService;
        this.sessionService = sessionService;
        this.resumeService = resumeService;
        this.domainPackService = domainPackService;
        this.transferService = transferService;
        this.sessionState = sessionState;
        this.contentNavigator = contentNavigator;
        this.fileDialogService = fileDialogService;
        this.viewManager = viewManager;
        this.exceptionHandler = exceptionHandler;
    }

    @Override
    public void initializeContext(Long planId) {
        if (planId == null) throw new IllegalArgumentException("Plan id is required");
        plan = planService.require(planId, userId());
        render();
    }

    @FXML
    private void back() {
        contentNavigator.back();
    }

    @FXML
    private void editPlan() {
        if (plan != null) contentNavigator.showSubPage("/fxml/plan-editor-view.fxml", "编辑面试方案", plan.id());
    }

    @FXML
    private void duplicatePlan() {
        if (plan == null) return;
        try {
            InterviewPlanDto copy = planService.duplicate(userId(), plan.id());
            contentNavigator.showSubPage("/fxml/plan-editor-view.fxml", "编辑面试方案", copy.id());
        } catch (RuntimeException exception) {
            showError(exception);
        }
    }

    @FXML
    private void exportSnapshot() {
        if (plan == null) return;
        Path target = fileDialogService.choosePlanExport(iconView.getScene().getWindow(), plan.name()).orElse(null);
        if (target == null) return;
        try {
            transferService.exportPlan(plan, target);
            viewManager.showInfo("导出方案快照", "方案快照已保存到：\n" + target.toAbsolutePath());
        } catch (RuntimeException exception) {
            showError(exception);
        }
    }

    @FXML
    private void startInterview() {
        if (plan == null) return;
        try {
            var session = sessionService.startOrResume(userId(), plan.id());
            contentNavigator.showSubPage("/fxml/interview-workspace-view.fxml", "模拟面试", session.id());
        } catch (RuntimeException exception) {
            showError(exception);
        }
    }

    @FXML
    private void deletePlan() {
        if (plan == null) return;
        if (!AppDialogs.confirm(iconView.getScene().getWindow(), "删除面试方案", "确认删除面试方案",
                "将删除“" + plan.name() + "”。历史面试记录会继续保留。", "删除方案", true)) return;
        try {
            planService.delete(userId(), plan.id());
            contentNavigator.back();
        } catch (RuntimeException exception) {
            showError(exception);
        }
    }

    private void render() {
        breadcrumbLabel.setText("面试方案 / " + plan.name() + " / 方案详情");
        iconView.setImage(loadImage());
        nameLabel.setText(plan.name());
        stateLabel.setText(plan.defaultPlan() ? "默认方案" : "已启用");
        tagContainer.getChildren().setAll(
                chip(plan.jobTitle()), chip(difficultyText(plan.difficulty())), chip(plan.durationMinutes() + " 分钟"));
        descriptionLabel.setText(plan.jobDescription() == null || plan.jobDescription().isBlank()
                ? "该方案围绕" + plan.jobTitle() + "的核心能力进行结构化提问、证据追问与综合评估。"
                : plan.jobDescription());
        suitableLabel.setText("适用岗位：" + plan.jobTitle() + " / 同方向候选人");
        createdLabel.setText(plan.createTime() == null ? "—" : TIME_FORMAT.format(plan.createTime()));
        updatedLabel.setText(plan.updateTime() == null ? "—" : TIME_FORMAT.format(plan.updateTime()));
        questionLabel.setText(plan.questionCount() + " 题");
        resumeCountLabel.setText(plan.resumeId() == null ? "0" : "1");
        knowledgeCountLabel.setText(Integer.toString(plan.knowledgeCategories().size()));
        durationTotalLabel.setText(Integer.toString(plan.durationMinutes()));
        renderStages();
        renderParameters();
        renderResources();
        renderFocus();
        renderRules();
        renderUsage();
        renderSummary();
    }

    private void renderStages() {
        stageContainer.getChildren().clear();
        int count = Math.max(1, plan.stages().size());
        int baseMinutes = Math.max(2, plan.durationMinutes() / count);
        int index = 1;
        for (String stage : plan.stages()) {
            Label number = new Label(Integer.toString(index++));
            number.getStyleClass().add("plan-stage-number");
            Label name = new Label(stageText(stage));
            name.getStyleClass().add("plan-stage-name");
            int minutes = blueprintMetric(stage, "minutes", baseMinutes);
            int questions = blueprintMetric(stage, "questions", Math.max(1, plan.questionCount() / count));
            Label time = new Label(minutes + " 分钟  |  " + questions + " 题");
            time.getStyleClass().add("plan-stage-time");
            VBox item = new VBox(3, number, name, time);
            item.setAlignment(Pos.CENTER);
            item.getStyleClass().add("plan-detail-stage-item");
            stageContainer.getChildren().add(item);
        }
    }

    private int blueprintMetric(String stage, String key, int fallback) {
        Object raw = plan.rules().get("stageBlueprint");
        if (!(raw instanceof List<?> rows)) return fallback;
        for (Object item : rows) {
            if (!(item instanceof Map<?, ?> values) || !stage.equals(String.valueOf(values.get("stage")))) continue;
            Object value = values.get(key);
            if (value instanceof Number number) return number.intValue();
            try { return Integer.parseInt(String.valueOf(value)); } catch (NumberFormatException ignored) { return fallback; }
        }
        return fallback;
    }

    private void renderParameters() {
        InterviewPlanSettings settings = InterviewPlanSettings.fromRules(plan.rules());
        jobLabel.setText(plan.jobTitle());
        difficultyLabel.setText(difficultyText(plan.difficulty()));
        modeLabel.setText(modeText(settings.mode()));
        personaLabel.setText(personaText(settings.persona()));
        pressureLabel.setText(pressureText(settings.pressureLevel()));
        strictnessLabel.setText(settings.strictness() == VerificationStrictness.STRICT ? "严格" : "标准");
        scenarioLabel.setText(settings.scenarioRatio() + "%");
        String pack = domainPackService.list().stream().filter(item -> item.id().equals(plan.domainPackId()))
                .map(item -> item.displayName()).findFirst().orElse("通用岗位能力包");
        domainPackLabel.setText(pack);
    }

    private void renderResources() {
        String resumeName = plan.resumeId() == null ? null : resumeService.list(userId()).stream()
                .filter(item -> item.id().equals(plan.resumeId())).map(item -> item.originalName()).findFirst().orElse("已关联简历");
        resumeLabel.setText(resumeName == null ? "未关联简历" : resumeName);
        profileLabel.setText(plan.profileId() == null ? "未关联已确认候选人画像" : "已关联已确认候选人画像");
        knowledgeContainer.getChildren().clear();
        if (plan.knowledgeCategories().isEmpty()) {
            knowledgeContainer.getChildren().add(chip("未选择分类"));
        } else {
            plan.knowledgeCategories().forEach(category -> knowledgeContainer.getChildren().add(chip(category)));
        }
        documentCountLabel.setText(plan.knowledgeDocumentIds().isEmpty()
                ? "当前分类下暂无已就绪文档" : "已纳入 " + plan.knowledgeDocumentIds().size() + " 份已就绪文档");
    }

    private void renderFocus() {
        focusContainer.getChildren().clear();
        String focus = String.valueOf(plan.rules().getOrDefault("focus", "技术基础、项目深度、系统设计、沟通表达"));
        Arrays.stream(focus.split("[,，、/]"))
                .map(String::strip).filter(value -> !value.isBlank()).limit(8)
                .forEach(value -> focusContainer.getChildren().add(dimension(value)));
    }

    private void renderRules() {
        InterviewPlanSettings settings = InterviewPlanSettings.fromRules(plan.rules());
        boolean followup = Boolean.parseBoolean(String.valueOf(plan.rules().getOrDefault("adaptiveFollowup", true)));
        boolean report = Boolean.parseBoolean(String.valueOf(plan.rules().getOrDefault("generateReport", true)));
        ruleSummaryLabel.setText("✓ 按候选人回答动态调整追问深度：" + (followup ? "开启" : "关闭")
                + "\n✓ 回答按" + (settings.strictness() == VerificationStrictness.STRICT ? "严格" : "标准") + "证据规则进行核验"
                + "\n✓ 知识库引用按所选分类动态解析，不绑定单个文档"
                + "\n✓ 面试结束生成结构化评估报告：" + (report ? "开启" : "关闭"));
    }

    private void renderUsage() {
        List<InterviewSessionDto> used = sessionService.list(userId()).stream()
                .filter(session -> plan.id().equals(session.planId())).toList();
        usageCountLabel.setText("共 " + used.size() + " 次");
        usageContainer.getChildren().clear();
        if (used.isEmpty()) {
            Label empty = new Label("该方案尚未使用，点击“立即开始面试”创建第一场面试。");
            empty.getStyleClass().add("plan-detail-hint");
            usageContainer.getChildren().add(empty);
            return;
        }
        used.stream().limit(3).forEach(session -> usageContainer.getChildren().add(usageRow(session)));
    }

    private HBox usageRow(InterviewSessionDto session) {
        Label time = new Label(session.updateTime() == null ? "—" : TIME_FORMAT.format(session.updateTime()));
        time.getStyleClass().add("plan-usage-time");
        Label title = new Label(session.title());
        title.getStyleClass().add("plan-usage-title");
        Label status = new Label(session.status().name());
        status.getStyleClass().add("plan-usage-status");
        Button detail = new Button("查看记录");
        detail.getStyleClass().add("plan-small-quiet");
        detail.setOnAction(event -> contentNavigator.showSubPage(
                "/fxml/interview-history-detail-view.fxml", "面试记录详情", session.id()));
        HBox row = new HBox(12, time, title, status, detail);
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(title, Priority.ALWAYS);
        row.getStyleClass().add("plan-usage-row");
        return row;
    }

    private void renderSummary() {
        int completeness = 45;
        if (plan.resumeId() != null) completeness += 10;
        if (plan.profileId() != null) completeness += 10;
        if (!plan.knowledgeCategories().isEmpty()) completeness += 10;
        if (plan.jobDescription() != null && !plan.jobDescription().isBlank()) completeness += 10;
        if (plan.stages().size() >= 5) completeness += 10;
        if (plan.rules().containsKey(InterviewPlanAssetService.ICON_PATH_RULE)) completeness += 5;
        completeness = Math.min(100, completeness);
        completenessIndicator.setProgress(completeness / 100.0);
        completenessLabel.setText(completeness + "%");
        stageCountLabel.setText(Integer.toString(plan.stages().size()));
        sideQuestionLabel.setText(Integer.toString(plan.questionCount()));
        sideResumeLabel.setText(plan.resumeId() == null ? "0" : "1");
        sideKnowledgeLabel.setText(Integer.toString(plan.knowledgeCategories().size()));
        ruleCountLabel.setText(Integer.toString(plan.rules().size()));

        sceneContainer.getChildren().setAll(
                bullet("用于" + plan.jobTitle() + "候选人筛选"),
                bullet(difficultyText(plan.difficulty()) + "岗位晋升或能力校准"),
                bullet("项目复盘型与技术深挖型模拟面试"));

        directionContainer.getChildren().clear();
        List<String> focuses = Arrays.stream(String.valueOf(plan.rules().getOrDefault("focus", "项目细节、技术取舍、故障恢复"))
                        .split("[,，、/]"))
                .map(String::strip).filter(value -> !value.isBlank()).limit(5).toList();
        int index = 1;
        for (String focus : focuses) directionContainer.getChildren().add(numbered(index++, focus + "的实践细节与取舍"));

        riskContainer.getChildren().clear();
        if (plan.resumeId() == null) riskContainer.getChildren().add(warning("未关联简历，个性化追问依据较少。"));
        if (plan.profileId() == null) riskContainer.getChildren().add(warning("未关联已确认画像，候选人能力基线不完整。"));
        if (plan.knowledgeCategories().isEmpty()) riskContainer.getChildren().add(warning("未选择知识库分类，专业证据覆盖有限。"));
        if (riskContainer.getChildren().isEmpty()) riskContainer.getChildren().add(success("核心资源已配置，可直接开始面试。"));

        adviceContainer.getChildren().setAll(
                bullet("首次使用前预览阶段与题量是否匹配时长。"),
                bullet("知识库只关联分类，新增文档可自动进入后续面试。"),
                bullet("定期根据面试报告调整重点方向与难度。"));
    }

    private Label chip(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("plan-detail-chip");
        return label;
    }

    private VBox dimension(String text) {
        Label name = new Label(text);
        name.getStyleClass().add("plan-dimension-name");
        ProgressIndicator dot = new ProgressIndicator(0.82);
        dot.setPrefSize(22, 22);
        dot.getStyleClass().add("plan-dimension-dot");
        VBox box = new VBox(4, new HBox(7, dot, name), new Label("重点考察"));
        box.getStyleClass().add("plan-dimension-box");
        return box;
    }

    private Label bullet(String text) {
        Label label = new Label(text, new FontIcon("mdi2c-circle-small"));
        label.setWrapText(true);
        label.getStyleClass().add("plan-detail-bullet");
        return label;
    }

    private Label numbered(int index, String text) {
        Label label = new Label(index + "  " + text);
        label.setWrapText(true);
        label.getStyleClass().add("plan-direction-item");
        return label;
    }

    private Label warning(String text) {
        Label label = new Label(text, new FontIcon("mdi2a-alert-circle-outline"));
        label.setWrapText(true);
        label.getStyleClass().add("plan-risk-item");
        return label;
    }

    private Label success(String text) {
        Label label = new Label(text, new FontIcon("mdi2c-check-circle-outline"));
        label.setWrapText(true);
        label.getStyleClass().add("plan-success-item");
        return label;
    }

    private Image loadImage() {
        Object raw = plan.rules().get(InterviewPlanAssetService.ICON_PATH_RULE);
        if (raw != null) {
            try {
                Path path = Path.of(String.valueOf(raw));
                if (Files.isRegularFile(path)) return new Image(path.toUri().toString());
            } catch (RuntimeException ignored) {
                // Fall through to the product placeholder.
            }
        }
        return new Image(getClass().getResource("/images/plan/plan-placeholder.png").toExternalForm());
    }

    private long userId() {
        return sessionState.requireCurrentUser().id();
    }

    private void showError(RuntimeException exception) {
        viewManager.showError(exceptionHandler.toUserMessage(exception));
    }

    private String difficultyText(InterviewDifficulty value) {
        return switch (value) { case JUNIOR -> "初级"; case MEDIUM -> "中级"; case SENIOR -> "高级"; case EXPERT -> "专家"; };
    }

    private String stageText(String value) {
        return switch (value) {
            case "INTRODUCTION" -> "自我介绍"; case "RESUME_REVIEW" -> "简历回顾";
            case "PROJECT_EXPERIENCE" -> "项目经历"; case "TECHNICAL_DEEP_DIVE" -> "技术深挖";
            case "SYSTEM_DESIGN" -> "系统设计"; case "CODING" -> "代码题";
            case "BEHAVIORAL" -> "行为面试"; case "SUMMARY" -> "总结"; default -> value;
        };
    }

    private String modeText(InterviewMode value) {
        return switch (value) { case FORMAL_SIMULATION -> "正式模拟"; case COACHING -> "教练训练"; case SCENARIO_SIMULATION -> "情境沙盘"; };
    }

    private String pressureText(PressureLevel value) {
        return switch (value) { case RELAXED -> "轻松"; case STANDARD -> "标准"; case CHALLENGING -> "挑战"; case HIGH_PRESSURE -> "高压"; };
    }

    private String personaText(InterviewerPersona value) {
        return switch (value) {
            case PROFESSIONAL_INTERVIEWER -> "专业面试官"; case FUTURE_PEER -> "未来同事";
            case TECH_LEAD -> "技术负责人"; case ARCHITECT -> "架构师";
            case INCIDENT_COMMANDER -> "事故指挥者"; case PRODUCT_LEADER -> "产品负责人";
        };
    }
}
