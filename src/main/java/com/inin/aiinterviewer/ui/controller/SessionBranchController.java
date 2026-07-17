package com.inin.aiinterviewer.ui.controller;

import com.inin.aiinterviewer.application.dto.SessionBranchDto;
import com.inin.aiinterviewer.application.exception.GlobalExceptionHandler;
import com.inin.aiinterviewer.application.service.SessionBranchService;
import com.inin.aiinterviewer.domain.enums.SessionBranchStatus;
import com.inin.aiinterviewer.ui.component.MarkdownView;
import com.inin.aiinterviewer.ui.navigation.ContentNavigator;
import com.inin.aiinterviewer.ui.navigation.ContextAwareController;
import com.inin.aiinterviewer.ui.navigation.JavaFxViewManager;
import com.inin.aiinterviewer.ui.state.UserSessionState;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
@Scope("prototype")
public class SessionBranchController implements ContextAwareController<String> {

    private final SessionBranchService branchService;
    private final UserSessionState sessionState;
    private final ContentNavigator contentNavigator;
    private final JavaFxViewManager viewManager;
    private final GlobalExceptionHandler exceptionHandler;

    @FXML private Label titleLabel;
    @FXML private Label questionNumberLabel;
    @FXML private Label statusLabel;
    @FXML private Label originalQuestionLabel;
    @FXML private Label originalAnswerLabel;
    @FXML private TextArea newAnswerArea;
    @FXML private Label progressLabel;
    @FXML private Button submitButton;
    @FXML private MarkdownView comparisonView;

    private String branchId;
    private boolean processing;

    public SessionBranchController(
            SessionBranchService branchService,
            UserSessionState sessionState,
            ContentNavigator contentNavigator,
            JavaFxViewManager viewManager,
            GlobalExceptionHandler exceptionHandler
    ) {
        this.branchService = branchService;
        this.sessionState = sessionState;
        this.contentNavigator = contentNavigator;
        this.viewManager = viewManager;
        this.exceptionHandler = exceptionHandler;
    }

    @Override
    public void initializeContext(String context) {
        if (context == null || context.isBlank()) {
            throw new IllegalArgumentException("Session branch requires an id");
        }
        branchId = context;
        refresh();
    }

    @FXML
    private void back() {
        if (processing) {
            viewManager.showInfo("正在生成", "请等待分支比较完成后再离开。");
            return;
        }
        contentNavigator.back();
    }

    @FXML
    private void submitAnswer() {
        if (processing) return;
        String answer = newAnswerArea.getText();
        processing = true;
        submitButton.setDisable(true);
        newAnswerArea.setDisable(true);
        progressLabel.setText("正在比较两次回答的逻辑链、证据和缺口…");
        Mono.fromCallable(() -> branchService.submitAnswer(userId(), branchId, answer))
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(
                        ignored -> Platform.runLater(() -> {
                            processing = false;
                            refresh();
                        }),
                        error -> Platform.runLater(() -> {
                            processing = false;
                            refresh();
                            viewManager.showError(exceptionHandler.toUserMessage(error));
                        }));
    }

    private void refresh() {
        SessionBranchDto branch = branchService.require(userId(), branchId);
        titleLabel.setText(branch.title());
        questionNumberLabel.setText("Q" + branch.sourceQuestionNumber());
        statusLabel.setText(statusText(branch.status()));
        originalQuestionLabel.setText(branch.originalQuestion());
        originalAnswerLabel.setText(branch.originalAnswer());
        if (!branch.newAnswer().isBlank() && newAnswerArea.getText().isBlank()) {
            newAnswerArea.setText(branch.newAnswer());
        }
        boolean completed = branch.status() == SessionBranchStatus.COMPLETED;
        boolean busy = branch.status() == SessionBranchStatus.PROCESSING || processing;
        newAnswerArea.setDisable(completed || busy);
        submitButton.setDisable(completed || busy);
        submitButton.setText(branch.status() == SessionBranchStatus.FAILED ? "重新比较" : "生成局部对比");
        comparisonView.setMarkdown(completed ? branch.comparisonMarkdown()
                : "# 分支局部对比\n\n提交新的回答后，这里将展示逻辑链完整度、证据数量、证据质量分、追问变化、观点修正和缺口解决情况。");
        progressLabel.setText(branch.status() == SessionBranchStatus.FAILED
                ? "上次比较失败：" + branch.errorMessage() + "。可以直接修改回答后重试。"
                : completed ? "分支比较已保存，原面试和原报告保持不变。"
                : busy ? "正在比较两次回答…"
                : "提交后将比较逻辑链、证据、追问和缺口变化");
    }

    private String statusText(SessionBranchStatus status) {
        return switch (status) {
            case DRAFT -> "待重答";
            case PROCESSING -> "比较中";
            case COMPLETED -> "已完成";
            case FAILED -> "比较失败";
        };
    }

    private long userId() {
        return sessionState.requireCurrentUser().id();
    }
}
