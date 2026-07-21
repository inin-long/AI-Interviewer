package com.inin.aiinterviewer.ui.controller;

import com.inin.aiinterviewer.application.dto.CandidateProfileDto;
import com.inin.aiinterviewer.application.dto.ResumeDetailDto;
import com.inin.aiinterviewer.application.exception.GlobalExceptionHandler;
import com.inin.aiinterviewer.application.service.CandidateProfileService;
import com.inin.aiinterviewer.application.service.CandidateProfileTaskService;
import com.inin.aiinterviewer.application.service.BackgroundTaskService;
import com.inin.aiinterviewer.application.service.ResumeService;
import com.inin.aiinterviewer.domain.enums.ProfileSource;
import com.inin.aiinterviewer.domain.enums.BackgroundTaskStatus;
import com.inin.aiinterviewer.domain.enums.ResumeStatus;
import com.inin.aiinterviewer.domain.model.CandidateProfileContent;
import com.inin.aiinterviewer.ui.navigation.ContentNavigator;
import com.inin.aiinterviewer.ui.navigation.ContextAwareController;
import com.inin.aiinterviewer.ui.navigation.JavaFxViewManager;
import com.inin.aiinterviewer.ui.navigation.Route;
import com.inin.aiinterviewer.ui.state.UserSessionState;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.util.Duration;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@Scope("prototype")
public class ResumeDetailController implements ContextAwareController<Long> {

    private final ResumeService resumeService;
    private final CandidateProfileService profileService;
    private final CandidateProfileTaskService profileTaskService;
    private final BackgroundTaskService backgroundTaskService;
    private final UserSessionState sessionState;
    private final ContentNavigator contentNavigator;
    private final JavaFxViewManager viewManager;
    private final GlobalExceptionHandler exceptionHandler;

    @FXML private Label resumeNameLabel;
    @FXML private Label resumeStatusLabel;
    @FXML private Label profileStatusLabel;
    @FXML private Label sourceNoticeLabel;
    @FXML private TextField fullNameField;
    @FXML private TextField targetRoleField;
    @FXML private TextField yearsField;
    @FXML private TextField educationField;
    @FXML private TextField skillsField;
    @FXML private TextArea projectsArea;
    @FXML private TextArea experienceArea;
    @FXML private TextArea strengthsArea;
    @FXML private TextArea risksArea;
    @FXML private TextArea summaryArea;
    @FXML private TextArea rawTextArea;
    @FXML private Button generateButton;
    @FXML private Button confirmButton;

    private long resumeId;
    private CandidateProfileDto currentProfile;
    private Timeline generationWatcher;

    public ResumeDetailController(
            ResumeService resumeService,
            CandidateProfileService profileService,
            CandidateProfileTaskService profileTaskService,
            BackgroundTaskService backgroundTaskService,
            UserSessionState sessionState,
            ContentNavigator contentNavigator,
            JavaFxViewManager viewManager,
            GlobalExceptionHandler exceptionHandler
    ) {
        this.resumeService = resumeService;
        this.profileService = profileService;
        this.profileTaskService = profileTaskService;
        this.backgroundTaskService = backgroundTaskService;
        this.sessionState = sessionState;
        this.contentNavigator = contentNavigator;
        this.viewManager = viewManager;
        this.exceptionHandler = exceptionHandler;
    }

    @Override
    public void initializeContext(Long context) {
        if (context == null) {
            viewManager.showError("缺少简历标识，无法打开详情。");
            return;
        }
        resumeId = context;
        try {
            load();
        } catch (RuntimeException exception) {
            viewManager.showError(exceptionHandler.toUserMessage(exception));
        }
    }

    @FXML
    private void generateProfile() {
        long userId = sessionState.requireCurrentUser().id();
        try {
            long taskId = profileTaskService.enqueue(userId, resumeId);
            generateButton.setDisable(true);
            sourceNoticeLabel.setText("画像生成已进入后台任务队列，可安全等待或离开页面。 ");
            watchGeneration(taskId);
        } catch (RuntimeException exception) {
            viewManager.showError(exceptionHandler.toUserMessage(exception));
        }
    }

    @FXML
    private void saveDraft() {
        try {
            currentProfile = saveManualProfile();
            populate(currentProfile);
        } catch (RuntimeException exception) {
            viewManager.showError(exceptionHandler.toUserMessage(exception));
        }
    }

    @FXML
    private void confirmProfile() {
        try {
            currentProfile = saveManualProfile();
            currentProfile = profileService.confirm(sessionState.requireCurrentUser().id(), resumeId);
            contentNavigator.showRoute(Route.RESUME);
        } catch (RuntimeException exception) {
            viewManager.showError(exceptionHandler.toUserMessage(exception));
        }
    }

    @FXML
    private void back() {
        contentNavigator.back();
    }

    private void load() {
        long userId = sessionState.requireCurrentUser().id();
        ResumeDetailDto detail = resumeService.getDetail(userId, resumeId);
        resumeNameLabel.setText(detail.resume().originalName());
        resumeStatusLabel.setText(switch (detail.resume().status()) {
            case UPLOADED -> "已上传";
            case PARSING -> "解析中";
            case COMPLETED -> "已完成";
            case FAILED -> "失败";
        });
        rawTextArea.setText(detail.parsedText() == null ? "" : detail.parsedText());
        boolean parsed = detail.resume().status() == ResumeStatus.COMPLETED;
        generateButton.setDisable(!parsed);
        currentProfile = profileService.find(userId, resumeId).orElse(null);
        if (currentProfile == null) {
            profileStatusLabel.setText("尚未生成");
            sourceNoticeLabel.setText(parsed
                    ? "点击生成画像；未配置 AI 时将生成明确标记的本地草稿，需人工确认。 "
                    : "简历仍在后台解析，完成后才能生成候选人画像。 ");
            confirmButton.setDisable(true);
        } else {
            populate(currentProfile);
        }
    }

    private void watchGeneration(long taskId) {
        if (generationWatcher != null) generationWatcher.stop();
        generationWatcher = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            var task = backgroundTaskService.requireDto(
                    sessionState.requireCurrentUser().id(), taskId);
            if (task.status() == BackgroundTaskStatus.SUCCESS) {
                generationWatcher.stop();
                generateButton.setDisable(false);
                currentProfile = profileService.find(
                        sessionState.requireCurrentUser().id(), resumeId).orElse(null);
                if (currentProfile != null) populate(currentProfile);
            } else if (task.status() == BackgroundTaskStatus.FAILED) {
                generationWatcher.stop();
                generateButton.setDisable(false);
                profileStatusLabel.setText("生成失败");
                sourceNoticeLabel.setText("画像生成失败：" + safeError(task.errorMessage()) + "。可点击重新分析。 ");
            } else if (task.status() == BackgroundTaskStatus.PENDING && task.attemptCount() > 0) {
                sourceNoticeLabel.setText("画像生成失败后等待第 " + (task.attemptCount() + 1) + " 次重试…");
            } else {
                sourceNoticeLabel.setText("正在后台分析简历并生成结构化画像…");
            }
        }));
        generationWatcher.setCycleCount(120);
        generationWatcher.play();
    }

    private String safeError(String value) {
        if (value == null || value.isBlank()) return "未知错误";
        return value.length() <= 160 ? value : value.substring(0, 160) + "…";
    }

    private void populate(CandidateProfileDto profile) {
        CandidateProfileContent content = profile.content();
        fullNameField.setText(content.fullName());
        targetRoleField.setText(content.targetRole());
        yearsField.setText(content.yearsExperience());
        educationField.setText(content.education());
        skillsField.setText(String.join(", ", content.skills()));
        projectsArea.setText(String.join("\n", content.projects()));
        experienceArea.setText(String.join("\n", content.experience()));
        strengthsArea.setText(String.join("\n", content.strengths()));
        risksArea.setText(String.join("\n", content.risks()));
        summaryArea.setText(content.summary());
        profileStatusLabel.setText(profile.confirmed() ? "已确认" : "待确认");
        sourceNoticeLabel.setText(sourceText(profile.source()));
        confirmButton.setDisable(false);
    }

    private CandidateProfileContent contentFromForm() {
        return new CandidateProfileContent(fullNameField.getText(), targetRoleField.getText(),
                yearsField.getText(), educationField.getText(), list(skillsField.getText()),
                lines(projectsArea.getText()), lines(experienceArea.getText()),
                lines(strengthsArea.getText()), lines(risksArea.getText()), summaryArea.getText());
    }

    private CandidateProfileDto saveManualProfile() {
        return profileService.saveManual(
                sessionState.requireCurrentUser().id(), resumeId, contentFromForm());
    }

    private List<String> list(String value) {
        return Arrays.stream(value.split("[,，\\n]"))
                .map(String::strip).filter(item -> !item.isBlank()).distinct().toList();
    }

    private List<String> lines(String value) {
        return Arrays.stream(value.split("\\R"))
                .map(String::strip).filter(item -> !item.isBlank()).toList();
    }

    private String sourceText(ProfileSource source) {
        return switch (source) {
            case AI -> "由已配置的 AI 服务结构化提取，请人工核对后确认。";
            case LOCAL_DRAFT -> "AI 未配置：当前为本地关键词草稿，不代表 AI 分析结果。";
            case MANUAL -> "当前画像已由用户手动编辑。";
        };
    }
}
