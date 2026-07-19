package com.inin.aiinterviewer.ui.controller;

import com.inin.aiinterviewer.application.dto.CareerPlanDto;
import com.inin.aiinterviewer.application.dto.GeneratePlanCommand;
import com.inin.aiinterviewer.application.dto.OptimizeResumeCommand;
import com.inin.aiinterviewer.application.dto.ResumeOptimizationDto;
import com.inin.aiinterviewer.application.service.CareerPlanningService;
import com.inin.aiinterviewer.ui.component.AppDialog;
import com.inin.aiinterviewer.ui.component.AppDialogs;
import com.inin.aiinterviewer.ui.component.MarkdownView;
import com.inin.aiinterviewer.ui.navigation.ContentNavigator;
import com.inin.aiinterviewer.ui.navigation.ContextAwareController;
import com.inin.aiinterviewer.ui.navigation.Route;
import com.inin.aiinterviewer.ui.state.UserSessionState;
import jakarta.annotation.Resource;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class CareerPlanningController implements ContextAwareController<Object> {

    private static final Logger log = LoggerFactory.getLogger(CareerPlanningController.class);

    @Resource
    private ContentNavigator navigator;

    @Resource
    private UserSessionState userSessionState;

    @Resource
    private CareerPlanningService careerPlanningService;

    @FXML
    private TabPane tabPane;

    // --- Career Planning fields ---
    @FXML
    private TextField targetPositionField;
    @FXML
    private TextArea currentStatusArea;
    @FXML
    private TextArea careerGoalArea;
    @FXML
    private VBox planContentArea;

    // --- Resume Optimization fields ---
    @FXML
    private TextField resumeTargetField;
    @FXML
    private TextArea originalResumeArea;
    @FXML
    private TextArea optimizeDirectionArea;
    @FXML
    private VBox optimizationResultArea;

    public void initializeContext(Object context) {
        // no-op for root page
    }

    @FXML
    private void handleGeneratePlan() {
        String position = targetPositionField.getText().strip();
        if (position.isBlank()) {
            showWarning("请输入目标岗位");
            return;
        }
        Long userId = userSessionState.requireCurrentUser().id();

        planContentArea.getChildren().clear();
        ProgressBar progress = new ProgressBar();
        progress.setMaxWidth(Double.MAX_VALUE);
        Label status = new Label("AI 正在分析，请稍候...");
        planContentArea.getChildren().addAll(progress, status);

        Task<CareerPlanDto> task = new Task<>() {
            @Override
            protected CareerPlanDto call() {
                GeneratePlanCommand cmd = new GeneratePlanCommand(
                        currentStatusArea.getText().strip(),
                        position,
                        "",  // industry - optional field not in UI
                        ""   // experienceYears - optional
                );
                return careerPlanningService.generatePlan(userId, cmd);
            }
        };
        task.setOnSucceeded(e -> {
            CareerPlanDto result = task.getValue();
            planContentArea.getChildren().clear();
            if (result.planMarkdown() != null && !result.planMarkdown().isBlank()) {
                MarkdownView md = new MarkdownView();
                md.setMarkdown(result.planMarkdown());
                planContentArea.getChildren().add(md);
            } else {
                planContentArea.getChildren().add(new Label("未生成有效结果。"));
            }
        });
        task.setOnFailed(e -> {
            planContentArea.getChildren().clear();
            Throwable ex = task.getException();
            String msg = ex != null ? ex.getMessage() : "未知错误";
            log.error("[CareerPlan] 生成职业规划失败: {}", msg, ex);
            planContentArea.getChildren().add(
                    new Label("生成失败：" + msg));
        });
        Thread thread = new Thread(task, "CareerPlanTask");
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void handleOptimizeResume() {
        String original = originalResumeArea.getText().strip();
        if (original.isBlank()) {
            showWarning("请输入需要优化的简历内容");
            return;
        }
        Long userId = userSessionState.requireCurrentUser().id();

        optimizationResultArea.getChildren().clear();
        ProgressBar progress = new ProgressBar();
        progress.setMaxWidth(Double.MAX_VALUE);
        Label status = new Label("AI 正在优化，请稍候...");
        optimizationResultArea.getChildren().addAll(progress, status);

        Task<ResumeOptimizationDto> task = new Task<>() {
            @Override
            protected ResumeOptimizationDto call() {
                OptimizeResumeCommand cmd = new OptimizeResumeCommand(original);
                return careerPlanningService.optimizeResume(userId, cmd);
            }
        };
        task.setOnSucceeded(e -> {
            ResumeOptimizationDto result = task.getValue();
            optimizationResultArea.getChildren().clear();
            if (result.optimizedText() != null && !result.optimizedText().isBlank()) {
                MarkdownView md = new MarkdownView();
                md.setMarkdown(result.optimizedText());
                optimizationResultArea.getChildren().add(md);
            } else {
                optimizationResultArea.getChildren().add(new Label("未生成有效的优化结果。"));
            }
        });
        task.setOnFailed(e -> {
            optimizationResultArea.getChildren().clear();
            optimizationResultArea.getChildren().add(
                    new Label("优化失败：" + (task.getException() != null ? task.getException().getMessage() : "未知错误")));
        });
        Thread thread = new Thread(task, "ResumeOptimizeTask");
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void showPlanHistory() {
        navigator.showSubRoute(Route.CAREER_PLAN_HISTORY, null);
    }

    @FXML
    private void showOptimizationHistory() {
        navigator.showSubRoute(Route.RESUME_OPTIMIZATION_HISTORY, null);
    }

    private void showWarning(String message) {
        AppDialogs.showMessage(
                tabPane.getScene() == null ? null : tabPane.getScene().getWindow(),
                "请检查输入",
                "需要补充信息",
                message,
                AppDialog.Tone.WARNING);
    }
}
