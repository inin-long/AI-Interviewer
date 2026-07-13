package com.inin.aiinterviewer.ui.controller;

import com.inin.aiinterviewer.config.properties.AppProperties;
import com.inin.aiinterviewer.config.properties.LlmProperties;
import com.inin.aiinterviewer.application.service.BackgroundTaskService;
import com.inin.aiinterviewer.application.dto.BackgroundTaskDto;
import com.inin.aiinterviewer.domain.enums.BackgroundTaskStatus;
import com.inin.aiinterviewer.domain.enums.BackgroundTaskType;
import com.inin.aiinterviewer.ui.state.UserSessionState;
import com.inin.aiinterviewer.ui.navigation.ContentNavigator;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class DashboardController {

    private final UserSessionState sessionState;
    private final AppProperties appProperties;
    private final LlmProperties llmProperties;
    private final BackgroundTaskService taskService;
    private final ContentNavigator contentNavigator;

    @FXML private Label greetingLabel;
    @FXML private Label dataPathLabel;
    @FXML private Label aiDetailLabel;
    @FXML private Label taskSummaryLabel;
    @FXML private Label latestTaskLabel;

    public DashboardController(
            UserSessionState sessionState,
            AppProperties appProperties,
            LlmProperties llmProperties,
            BackgroundTaskService taskService,
            ContentNavigator contentNavigator
    ) {
        this.sessionState = sessionState;
        this.appProperties = appProperties;
        this.llmProperties = llmProperties;
        this.taskService = taskService;
        this.contentNavigator = contentNavigator;
    }

    @FXML
    private void initialize() {
        greetingLabel.setText("你好，" + sessionState.requireCurrentUser().nickname());
        dataPathLabel.setText(appProperties.dataRoot());
        aiDetailLabel.setText(llmProperties.isConfigured() ? "AI 配置已检测" : "待配置 AI 服务");
        refreshTasks();
    }

    @FXML
    private void refreshTasks() {
        var tasks = taskService.listDtos(sessionState.requireCurrentUser().id());
        long pending = tasks.stream().filter(task -> task.status() == BackgroundTaskStatus.PENDING).count();
        long running = tasks.stream().filter(task -> task.status() == BackgroundTaskStatus.RUNNING).count();
        long failed = tasks.stream().filter(task -> task.status() == BackgroundTaskStatus.FAILED).count();
        taskSummaryLabel.setText("等待 " + pending + " · 执行中 " + running + " · 失败 " + failed);
        if (tasks.isEmpty()) {
            latestTaskLabel.setText("暂无后台任务。上传知识文档后，可在这里查看处理状态。");
            return;
        }
        BackgroundTaskDto latest = tasks.getFirst();
        latestTaskLabel.setText("最近任务：" + typeText(latest.type()) + " · " + statusText(latest));
    }

    @FXML
    private void showAllTasks() {
        contentNavigator.showSubPage("/fxml/task-view.fxml", "后台任务", null);
    }

    private String typeText(BackgroundTaskType type) {
        return switch (type) {
            case RESUME_PARSE -> "简历解析";
            case PROFILE_GENERATE -> "候选人画像生成";
            case DOCUMENT_PARSE -> "知识文档处理";
            case EMBEDDING_GENERATE -> "向量生成";
            case VECTOR_UPDATE -> "索引更新";
            case REPORT_GENERATE -> "报告生成";
        };
    }

    private String statusText(BackgroundTaskDto task) {
        return switch (task.status()) {
            case PENDING -> "等待执行";
            case RUNNING -> "执行中（第 " + task.attemptCount() + " 次）";
            case SUCCESS -> "已完成";
            case FAILED -> "失败：" + (task.errorMessage() == null ? "未知错误" : task.errorMessage());
        };
    }
}
