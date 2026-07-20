package com.inin.aiinterviewer.ui.controller;

import com.inin.aiinterviewer.application.dto.CandidateProfileDto;
import com.inin.aiinterviewer.application.dto.CandidateProfileListItemDto;
import com.inin.aiinterviewer.application.dto.ResumeDetailDto;
import com.inin.aiinterviewer.application.dto.ResumeDto;
import com.inin.aiinterviewer.application.exception.GlobalExceptionHandler;
import com.inin.aiinterviewer.application.service.BackgroundTaskService;
import com.inin.aiinterviewer.application.service.CandidateProfileService;
import com.inin.aiinterviewer.application.service.CandidateProfileTaskService;
import com.inin.aiinterviewer.application.service.InterviewPlanService;
import com.inin.aiinterviewer.application.service.InterviewSessionService;
import com.inin.aiinterviewer.application.service.ResumeService;
import com.inin.aiinterviewer.application.service.ResumeTaskService;
import com.inin.aiinterviewer.domain.enums.BackgroundTaskStatus;
import com.inin.aiinterviewer.domain.enums.ResumeStatus;
import com.inin.aiinterviewer.ui.component.AppDialogs;
import com.inin.aiinterviewer.ui.component.AppSelect;
import com.inin.aiinterviewer.ui.component.DrawerPane;
import com.inin.aiinterviewer.ui.component.ResumeProfileDrawerView;
import com.inin.aiinterviewer.ui.dialog.FileDialogService;
import com.inin.aiinterviewer.ui.navigation.ContentNavigator;
import com.inin.aiinterviewer.ui.navigation.JavaFxViewManager;
import com.inin.aiinterviewer.ui.navigation.Route;
import com.inin.aiinterviewer.ui.state.UserSessionState;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
@Scope("prototype")
public class ResumeController {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter ROW_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String SORT_UPDATE = "按更新时间";
    private static final String SORT_NAME = "按名称";
    private static final String SORT_SIZE = "按大小";

    private final ResumeService resumeService;
    private final ResumeTaskService resumeTaskService;
    private final BackgroundTaskService backgroundTaskService;
    private final CandidateProfileService profileService;
    private final CandidateProfileTaskService profileTaskService;
    private final InterviewPlanService planService;
    private final InterviewSessionService interviewSessionService;
    private final UserSessionState sessionState;
    private final JavaFxViewManager viewManager;
    private final ContentNavigator contentNavigator;
    private final FileDialogService fileDialogService;
    private final GlobalExceptionHandler exceptionHandler;

    @FXML private ListView<ResumeDto> resumeList;
    @FXML private TextField searchField;
    @FXML private AppSelect<String> sortSelect;
    @FXML private ToggleButton allFilterButton;
    @FXML private ToggleButton confirmedFilterButton;
    @FXML private ToggleButton pendingFilterButton;
    @FXML private ToggleButton failedFilterButton;
    @FXML private Label totalCountLabel;
    @FXML private Label confirmedCountLabel;
    @FXML private Label pendingCountLabel;
    @FXML private Label latestDateLabel;
    @FXML private Label latestTimeLabel;
    @FXML private Label processResumeLabel;
    @FXML private Label uploadStateLabel;
    @FXML private Label extractStateLabel;
    @FXML private Label analysisStateLabel;
    @FXML private Label portraitStateLabel;
    @FXML private Button uploadButton;
    @FXML private DrawerPane profileDrawer;

    private final List<ResumeDto> allResumes = new ArrayList<>();
    private final Map<Long, CandidateProfileListItemDto> profilesByResume = new HashMap<>();
    private Timeline resumeStatusWatcher;
    private Timeline profileStatusWatcher;
    private ResumeDto drawerResume;
    private CandidateProfileDto drawerProfile;
    private ResumeProfileDrawerView drawerView;

    public ResumeController(
            ResumeService resumeService,
            ResumeTaskService resumeTaskService,
            BackgroundTaskService backgroundTaskService,
            CandidateProfileService profileService,
            CandidateProfileTaskService profileTaskService,
            InterviewPlanService planService,
            InterviewSessionService interviewSessionService,
            UserSessionState sessionState,
            ContentNavigator contentNavigator,
            FileDialogService fileDialogService,
            JavaFxViewManager viewManager,
            GlobalExceptionHandler exceptionHandler
    ) {
        this.resumeService = resumeService;
        this.resumeTaskService = resumeTaskService;
        this.backgroundTaskService = backgroundTaskService;
        this.profileService = profileService;
        this.profileTaskService = profileTaskService;
        this.planService = planService;
        this.interviewSessionService = interviewSessionService;
        this.sessionState = sessionState;
        this.contentNavigator = contentNavigator;
        this.fileDialogService = fileDialogService;
        this.viewManager = viewManager;
        this.exceptionHandler = exceptionHandler;
    }

    @FXML
    private void initialize() {
        profileDrawer.setDrawerWidth(470);
        sortSelect.getItems().setAll(SORT_UPDATE, SORT_NAME, SORT_SIZE);
        sortSelect.setValue(SORT_UPDATE);
        sortSelect.valueProperty().addListener((observable, previous, current) -> applyFilters());
        resumeList.setCellFactory(ignored -> new ResumeCardCell());
        resumeList.getSelectionModel().selectedItemProperty().addListener(
                (observable, previous, current) -> updateProcess(current));
        refresh();
    }

    @FXML
    private void uploadResume() {
        chooseAndUpload();
    }

    @FXML
    private void importResume() {
        chooseAndUpload();
    }

    private void chooseAndUpload() {
        Path selected = fileDialogService.chooseResume(uploadButton.getScene().getWindow()).orElse(null);
        if (selected == null) return;

        long userId = sessionState.requireCurrentUser().id();
        Task<ResumeTaskService.QueuedResume> task = new Task<>() {
            @Override
            protected ResumeTaskService.QueuedResume call() {
                return resumeTaskService.uploadAndEnqueue(userId, selected);
            }
        };
        uploadButton.setDisable(true);
        processResumeLabel.setText("正在导入 " + selected.getFileName());
        uploadStateLabel.setText("保存中");
        task.setOnSucceeded(event -> {
            uploadButton.setDisable(false);
            ResumeTaskService.QueuedResume queued = task.getValue();
            refresh();
            selectResume(queued.resume().id());
            watchResumeStatus(queued.taskId(), queued.resume().id());
        });
        task.setOnFailed(event -> {
            uploadButton.setDisable(false);
            uploadStateLabel.setText("上传失败");
            viewManager.showError(exceptionHandler.toUserMessage(task.getException()));
        });
        Thread worker = new Thread(task, "resume-upload");
        worker.setDaemon(true);
        worker.start();
    }

    @FXML
    private void createCandidateProfile() {
        ResumeDto candidate = allResumes.stream()
                .filter(resume -> resume.status() == ResumeStatus.COMPLETED)
                .filter(resume -> !profilesByResume.containsKey(resume.id()))
                .findFirst()
                .orElseGet(() -> allResumes.stream()
                        .filter(resume -> resume.status() == ResumeStatus.COMPLETED)
                        .findFirst().orElse(null));
        if (candidate == null) {
            viewManager.showInfo("新建候选人画像", "请先上传并完成至少一份简历的文本解析。");
            return;
        }
        resumeList.getSelectionModel().select(candidate);
        openProfile(candidate);
    }

    @FXML
    private void applyFilters() {
        if (resumeList == null) return;
        if (!allFilterButton.isSelected() && !confirmedFilterButton.isSelected()
                && !pendingFilterButton.isSelected() && !failedFilterButton.isSelected()) {
            allFilterButton.setSelected(true);
        }
        String query = searchField == null || searchField.getText() == null
                ? "" : searchField.getText().strip().toLowerCase(Locale.ROOT);
        List<ResumeDto> filtered = allResumes.stream()
                .filter(this::matchesSelectedFilter)
                .filter(resume -> matchesSearch(resume, query))
                .sorted(selectedComparator())
                .toList();
        ResumeDto selected = resumeList.getSelectionModel().getSelectedItem();
        resumeList.getItems().setAll(filtered);
        if (selected != null && filtered.stream().anyMatch(item -> item.id().equals(selected.id()))) {
            resumeList.getSelectionModel().select(selected);
        } else if (!filtered.isEmpty()) {
            resumeList.getSelectionModel().selectFirst();
        } else {
            updateProcess(null);
        }
    }

    @FXML
    private void refresh() {
        long userId = sessionState.requireCurrentUser().id();
        allResumes.clear();
        allResumes.addAll(resumeService.list(userId));
        profilesByResume.clear();
        profileService.list(userId).forEach(profile -> profilesByResume.put(profile.resumeId(), profile));
        updateStatistics();
        applyFilters();
    }

    private void updateStatistics() {
        long confirmed = profilesByResume.values().stream().filter(CandidateProfileListItemDto::confirmed).count();
        long pending = allResumes.stream().filter(resume -> {
            CandidateProfileListItemDto profile = profilesByResume.get(resume.id());
            return resume.status() != ResumeStatus.FAILED && (profile == null || !profile.confirmed());
        }).count();
        totalCountLabel.setText(String.valueOf(allResumes.size()));
        confirmedCountLabel.setText(String.valueOf(confirmed));
        pendingCountLabel.setText(String.valueOf(pending));
        LocalDateTime latest = allResumes.stream().map(ResumeDto::updateTime).max(LocalDateTime::compareTo).orElse(null);
        latestDateLabel.setText(latest == null ? "--" : DATE_FORMAT.format(latest));
        latestTimeLabel.setText(latest == null ? "暂无数据" : TIME_FORMAT.format(latest));
    }

    private void updateProcess(ResumeDto resume) {
        if (resume == null) {
            processResumeLabel.setText("选择一份简历查看处理进度");
            uploadStateLabel.setText("等待中");
            extractStateLabel.setText("等待中");
            analysisStateLabel.setText("等待中");
            portraitStateLabel.setText("等待中");
            return;
        }
        CandidateProfileListItemDto profile = profilesByResume.get(resume.id());
        processResumeLabel.setText("当前简历：" + resume.originalName());
        uploadStateLabel.setText("已完成");
        extractStateLabel.setText(switch (resume.status()) {
            case UPLOADED -> "排队中";
            case PARSING -> "进行中";
            case COMPLETED -> "已完成";
            case FAILED -> "失败";
        });
        analysisStateLabel.setText(profile == null
                ? resume.status() == ResumeStatus.COMPLETED ? "等待生成" : "等待中"
                : "已完成");
        portraitStateLabel.setText(profile == null ? "等待中" : profile.confirmed() ? "已确认" : "待确认");
    }

    private boolean matchesSelectedFilter(ResumeDto resume) {
        CandidateProfileListItemDto profile = profilesByResume.get(resume.id());
        if (confirmedFilterButton.isSelected()) return profile != null && profile.confirmed();
        if (failedFilterButton.isSelected()) return resume.status() == ResumeStatus.FAILED;
        if (pendingFilterButton.isSelected()) {
            return resume.status() != ResumeStatus.FAILED && (profile == null || !profile.confirmed());
        }
        return allFilterButton.isSelected();
    }

    private boolean matchesSearch(ResumeDto resume, String query) {
        if (query.isBlank()) return true;
        CandidateProfileListItemDto profile = profilesByResume.get(resume.id());
        String searchable = resume.originalName() + " "
                + (profile == null ? "" : profile.candidateName() + " " + profile.targetRole());
        return searchable.toLowerCase(Locale.ROOT).contains(query);
    }

    private Comparator<ResumeDto> selectedComparator() {
        return switch (sortSelect.getValue()) {
            case SORT_NAME -> Comparator.comparing(ResumeDto::originalName, String.CASE_INSENSITIVE_ORDER);
            case SORT_SIZE -> Comparator.comparingLong(ResumeDto::fileSize).reversed();
            default -> Comparator.comparing(ResumeDto::updateTime, Comparator.reverseOrder())
                    .thenComparing(ResumeDto::id, Comparator.reverseOrder());
        };
    }

    private void openProfile(ResumeDto resume) {
        try {
            long userId = sessionState.requireCurrentUser().id();
            drawerResume = resume;
            ResumeDetailDto detail = resumeService.getDetail(userId, resume.id());
            drawerProfile = profileService.find(userId, resume.id()).orElse(null);
            drawerView = new ResumeProfileDrawerView();
            drawerView.setOnGenerate(() -> generateProfile(resume));
            drawerView.setOnEdit(() -> editProfile(resume));
            drawerView.setOnConfirm(this::confirmProfile);
            drawerView.setOnInterview(this::startInterview);
            drawerView.render(detail, drawerProfile, false);
            profileDrawer.setDrawerWidth(470);
            profileDrawer.open("候选人画像预览工作区", drawerView);
        } catch (RuntimeException exception) {
            viewManager.showError(exceptionHandler.toUserMessage(exception));
        }
    }

    private void openOriginal(ResumeDto resume) {
        ResumeDetailDto detail = resumeService.getDetail(sessionState.requireCurrentUser().id(), resume.id());
        Label fileName = new Label(resume.originalName());
        fileName.getStyleClass().add("resume-original-name");
        Label meta = new Label(resume.fileType().toUpperCase(Locale.ROOT) + "  ·  "
                + formatSize(resume.fileSize()) + "  ·  " + ROW_TIME_FORMAT.format(resume.updateTime()));
        meta.getStyleClass().add("resume-original-meta");
        VBox summary = new VBox(4, fileName, meta);
        summary.getStyleClass().add("resume-original-summary");
        Button backToProfile = new Button("返回候选人画像", new FontIcon("mdi2a-arrow-left"));
        backToProfile.getStyleClass().add("resume-drawer-outline-button");
        backToProfile.setOnAction(event -> openProfile(resume));
        TextArea rawText = new TextArea(detail.parsedText() == null || detail.parsedText().isBlank()
                ? "当前简历尚未完成文本提取。" : detail.parsedText());
        rawText.setEditable(false);
        rawText.setWrapText(true);
        rawText.getStyleClass().add("resume-original-text");
        VBox.setVgrow(rawText, Priority.ALWAYS);
        VBox original = new VBox(14, backToProfile, summary, rawText);
        original.setPadding(new Insets(18, 20, 20, 20));
        original.getStyleClass().add("resume-original-drawer");
        profileDrawer.open("简历原文", original);
    }

    private void generateProfile(ResumeDto resume) {
        try {
            long userId = sessionState.requireCurrentUser().id();
            long taskId = profileTaskService.enqueue(userId, resume.id());
            drawerView.render(resumeService.getDetail(userId, resume.id()), null, true);
            watchProfileStatus(taskId, resume.id());
        } catch (RuntimeException exception) {
            viewManager.showError(exceptionHandler.toUserMessage(exception));
        }
    }

    private void watchProfileStatus(long taskId, long resumeId) {
        if (profileStatusWatcher != null) profileStatusWatcher.stop();
        profileStatusWatcher = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            var task = backgroundTaskService.requireDto(sessionState.requireCurrentUser().id(), taskId);
            if (task.status() == BackgroundTaskStatus.SUCCESS) {
                profileStatusWatcher.stop();
                drawerProfile = profileService.find(sessionState.requireCurrentUser().id(), resumeId).orElse(null);
                ResumeDetailDto detail = resumeService.getDetail(sessionState.requireCurrentUser().id(), resumeId);
                drawerView.render(detail, drawerProfile, false);
                refresh();
                selectResume(resumeId);
            } else if (task.status() == BackgroundTaskStatus.FAILED) {
                profileStatusWatcher.stop();
                drawerView.render(resumeService.getDetail(sessionState.requireCurrentUser().id(), resumeId), null, false);
                viewManager.showError("候选人画像生成失败，可稍后重新分析。");
            }
        }));
        profileStatusWatcher.setCycleCount(120);
        profileStatusWatcher.play();
    }

    private void confirmProfile() {
        if (drawerResume == null || drawerProfile == null) return;
        try {
            long userId = sessionState.requireCurrentUser().id();
            drawerProfile = profileService.confirm(userId, drawerResume.id());
            drawerView.render(resumeService.getDetail(userId, drawerResume.id()), drawerProfile, false);
            refresh();
            selectResume(drawerResume.id());
        } catch (RuntimeException exception) {
            viewManager.showError(exceptionHandler.toUserMessage(exception));
        }
    }

    private void editProfile(ResumeDto resume) {
        profileDrawer.close();
        contentNavigator.showSubPage("/fxml/resume-detail-view.fxml", "编辑候选人画像", resume.id());
    }

    private void startInterview() {
        if (drawerProfile == null || !drawerProfile.confirmed()) return;
        long userId = sessionState.requireCurrentUser().id();
        var plan = planService.list(userId).stream()
                .filter(item -> drawerProfile.id().equals(item.profileId()))
                .findFirst().orElse(null);
        if (plan == null) {
            profileDrawer.close();
            contentNavigator.showRoute(Route.PLAN);
            viewManager.showInfo("进入模拟面试", "画像已确认。请新建或选择一个关联该画像的面试方案后开始面试。");
            return;
        }
        try {
            var session = interviewSessionService.startOrResume(userId, plan.id());
            profileDrawer.close();
            contentNavigator.showSubPage("/fxml/interview-workspace-view.fxml", "模拟面试", session.id());
        } catch (RuntimeException exception) {
            viewManager.showError(exceptionHandler.toUserMessage(exception));
        }
    }

    private void deleteResume(ResumeDto resume) {
        if (!AppDialogs.confirm(
                resumeList.getScene() == null ? null : resumeList.getScene().getWindow(),
                "删除简历",
                "确认删除简历",
                "将删除“" + resume.originalName() + "”及其本地文件，此操作无法撤销。",
                "删除简历",
                true)) return;
        try {
            resumeService.delete(sessionState.requireCurrentUser().id(), resume.id());
            if (drawerResume != null && drawerResume.id().equals(resume.id())) profileDrawer.close();
            refresh();
        } catch (RuntimeException exception) {
            viewManager.showError(exceptionHandler.toUserMessage(exception));
        }
    }

    private void watchResumeStatus(long taskId, long resumeId) {
        if (resumeStatusWatcher != null) resumeStatusWatcher.stop();
        resumeStatusWatcher = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            refresh();
            selectResume(resumeId);
            var task = backgroundTaskService.requireDto(sessionState.requireCurrentUser().id(), taskId);
            if (task.status() == BackgroundTaskStatus.SUCCESS || task.status() == BackgroundTaskStatus.FAILED) {
                resumeStatusWatcher.stop();
            }
        }));
        resumeStatusWatcher.setCycleCount(60);
        resumeStatusWatcher.play();
    }

    private void selectResume(long resumeId) {
        resumeList.getItems().stream()
                .filter(item -> item.id().equals(resumeId))
                .findFirst()
                .ifPresent(item -> resumeList.getSelectionModel().select(item));
    }

    private String statusText(ResumeDto resume, CandidateProfileListItemDto profile) {
        if (resume.status() == ResumeStatus.FAILED) return "解析失败";
        if (resume.status() == ResumeStatus.UPLOADED || resume.status() == ResumeStatus.PARSING) return "解析中";
        if (profile == null) return "待生成";
        return profile.confirmed() ? "已确认" : "待确认";
    }

    private String statusClass(ResumeDto resume, CandidateProfileListItemDto profile) {
        if (resume.status() == ResumeStatus.FAILED) return "failed";
        if (resume.status() == ResumeStatus.UPLOADED || resume.status() == ResumeStatus.PARSING) return "parsing";
        if (profile != null && profile.confirmed()) return "confirmed";
        return "pending";
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    private final class ResumeCardCell extends ListCell<ResumeDto> {
        @Override
        protected void updateItem(ResumeDto resume, boolean empty) {
            super.updateItem(resume, empty);
            if (empty || resume == null) {
                setText(null);
                setGraphic(null);
                return;
            }
            setText(null);
            setGraphic(buildCard(resume));
        }

        private HBox buildCard(ResumeDto resume) {
            CandidateProfileListItemDto profile = profilesByResume.get(resume.id());
            VBox fileBadge = fileBadge(resume.fileType());

            Label name = new Label(resume.originalName());
            name.setTextOverrun(OverrunStyle.ELLIPSIS);
            name.getStyleClass().add("resume-row-name");
            String role = profile == null || profile.targetRole() == null || profile.targetRole().isBlank()
                    ? "目标岗位：待生成候选人画像" : "目标岗位：" + profile.targetRole();
            Label roleLabel = new Label(role);
            roleLabel.getStyleClass().add("resume-row-role");
            FlowPane skills = new FlowPane(6, 4);
            skills.getStyleClass().add("resume-row-skills");
            List<String> visibleSkills = profile == null ? List.of() : profile.skills().stream().limit(4).toList();
            for (String skill : visibleSkills) {
                Label chip = new Label(skill);
                chip.getStyleClass().add("resume-row-skill");
                skills.getChildren().add(chip);
            }
            VBox identity = new VBox(2, name, roleLabel, skills);
            identity.setMinWidth(280);
            identity.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(identity, Priority.ALWAYS);

            Label time = new Label(ROW_TIME_FORMAT.format(resume.updateTime()));
            time.getStyleClass().add("resume-row-time");
            Label timeHint = new Label("上传时间");
            timeHint.getStyleClass().add("resume-row-time-hint");
            VBox timestamp = new VBox(2, time, timeHint);
            timestamp.setMinWidth(145);

            Label status = new Label(statusText(resume, profile), statusIcon(resume, profile));
            status.getStyleClass().addAll("resume-status-chip", statusClass(resume, profile));
            status.setMinWidth(92);

            Button original = rowAction("查看原文", "mdi2e-eye-outline", "resume-original-action");
            original.setOnAction(event -> openOriginal(resume));
            Button portrait = rowAction("查看画像", "mdi2a-account-outline", "resume-profile-action");
            portrait.setOnAction(event -> openProfile(resume));
            Button more = rowAction("更多", "mdi2d-dots-horizontal", "resume-more-action");
            more.setOnAction(event -> deleteResume(resume));
            HBox actions = new HBox(4, original, portrait, more);
            actions.setAlignment(Pos.CENTER_RIGHT);

            HBox card = new HBox(14, fileBadge, identity, timestamp, status, actions);
            card.setAlignment(Pos.CENTER_LEFT);
            card.setPadding(new Insets(9, 10, 9, 12));
            card.getStyleClass().add("resume-row-card");
            card.setOnMouseClicked(event -> {
                getListView().getSelectionModel().select(resume);
                openProfile(resume);
            });
            original.setOnMouseClicked(event -> event.consume());
            portrait.setOnMouseClicked(event -> event.consume());
            more.setOnMouseClicked(event -> event.consume());
            return card;
        }

        private VBox fileBadge(String fileType) {
            String normalized = fileType == null ? "" : fileType.toLowerCase(Locale.ROOT);
            String iconLiteral = normalized.equals("pdf") ? "mdi2f-file-pdf-box" : "mdi2f-file-word-box";
            FontIcon icon = new FontIcon(iconLiteral);
            icon.setIconSize(28);
            icon.getStyleClass().addAll("resume-file-icon", normalized.equals("pdf") ? "pdf" : "word");
            Label type = new Label(normalized.isBlank() ? "FILE" : normalized.toUpperCase(Locale.ROOT));
            type.getStyleClass().add("resume-file-type");
            VBox badge = new VBox(1, icon, type);
            badge.setAlignment(Pos.CENTER);
            badge.getStyleClass().addAll("resume-file-badge", normalized.equals("pdf") ? "pdf" : "word");
            return badge;
        }

        private Button rowAction(String text, String iconLiteral, String extraClass) {
            Button button = new Button(text, new FontIcon(iconLiteral));
            button.getStyleClass().addAll("resume-row-action", extraClass);
            return button;
        }

        private FontIcon statusIcon(ResumeDto resume, CandidateProfileListItemDto profile) {
            String literal = switch (statusClass(resume, profile)) {
                case "confirmed" -> "mdi2c-check-circle-outline";
                case "failed" -> "mdi2a-alert-circle-outline";
                case "parsing" -> "mdi2l-loading";
                default -> "mdi2c-clock-outline";
            };
            FontIcon icon = new FontIcon(literal);
            icon.setIconSize(15);
            return icon;
        }
    }
}
