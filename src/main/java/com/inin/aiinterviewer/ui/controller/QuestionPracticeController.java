package com.inin.aiinterviewer.ui.controller;

import com.inin.aiinterviewer.application.dto.InterviewQuestionDto;
import com.inin.aiinterviewer.application.exception.GlobalExceptionHandler;
import com.inin.aiinterviewer.application.service.QuestionBankService;
import com.inin.aiinterviewer.application.service.QuestionPracticeService;
import com.inin.aiinterviewer.application.service.ScoreEvent;
import com.inin.aiinterviewer.domain.enums.InterviewDifficulty;
import com.inin.aiinterviewer.domain.model.AnswerScore;
import com.inin.aiinterviewer.ui.component.MarkdownView;
import com.inin.aiinterviewer.ui.navigation.ContentNavigator;
import com.inin.aiinterviewer.ui.navigation.ContextAwareController;
import com.inin.aiinterviewer.ui.navigation.JavaFxViewManager;
import com.inin.aiinterviewer.ui.state.UserSessionState;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

@Component
@Scope("prototype")
public class QuestionPracticeController implements ContextAwareController<Long> {

    private static final Logger log = LoggerFactory.getLogger(QuestionPracticeController.class);

    private final QuestionBankService questionBankService;
    private final QuestionPracticeService questionPracticeService;
    private final UserSessionState sessionState;
    private final ContentNavigator contentNavigator;
    private final JavaFxViewManager viewManager;
    private final GlobalExceptionHandler exceptionHandler;

    @FXML private Label hintLabel;
    @FXML private Label jobBadge;
    @FXML private Label difficultyBadge;
    @FXML private Label titleLabel;
    @FXML private Label contentLabel;
    @FXML private FlowPane tagPane;
    @FXML private TextArea answerArea;
    @FXML private Label statusLabel;
    @FXML private Button clearButton;
    @FXML private Button submitButton;
    @FXML private VBox scoreCard;
    @FXML private Label scoreValueLabel;
    @FXML private ProgressBar correctnessBar;
    @FXML private Label correctnessLabel;
    @FXML private ProgressBar depthBar;
    @FXML private Label depthLabel;
    @FXML private VBox feedbackContainer;
    @FXML private TitledPane referencePane;
    @FXML private VBox referenceContainer;

    private Long questionId;
    private Disposable scoreSubscription;

    public QuestionPracticeController(
            QuestionBankService questionBankService,
            QuestionPracticeService questionPracticeService,
            UserSessionState sessionState,
            ContentNavigator contentNavigator,
            JavaFxViewManager viewManager,
            GlobalExceptionHandler exceptionHandler
    ) {
        this.questionBankService = questionBankService;
        this.questionPracticeService = questionPracticeService;
        this.sessionState = sessionState;
        this.contentNavigator = contentNavigator;
        this.viewManager = viewManager;
        this.exceptionHandler = exceptionHandler;
    }

    @Override
    public void initializeContext(Long context) {
        if (context == null) {
            viewManager.showError("未指定练习题目");
            contentNavigator.back();
            return;
        }
        this.questionId = context;
        long userId = sessionState.requireCurrentUser().id();
        InterviewQuestionDto question = questionBankService.listQuestions(userId).stream()
                .filter(item -> item.id() != null && item.id().equals(questionId))
                .findFirst()
                .orElse(null);
        if (question == null) {
            viewManager.showError("面试题不存在");
            contentNavigator.back();
            return;
        }
        renderQuestion(question);
    }

    private void renderQuestion(InterviewQuestionDto question) {
        titleLabel.setText(question.title());
        contentLabel.setText(question.content() == null ? "" : question.content());

        String job = question.jobTitle() == null || question.jobTitle().isBlank()
                ? "未归类" : question.jobTitle();
        jobBadge.setText(job);
        jobBadge.getStyleClass().setAll("badge", "badge-job");

        difficultyBadge.setText(difficultyText(question.difficulty()));
        difficultyBadge.getStyleClass().setAll("badge", difficultyBadgeClass(question.difficulty()));

        tagPane.getChildren().clear();
        for (String tag : question.tags()) {
            Label chip = new Label(tag);
            chip.getStyleClass().add("tag-chip");
            tagPane.getChildren().add(chip);
        }

        boolean hasReference = question.referenceAnswer() != null && !question.referenceAnswer().isBlank();
        referenceContainer.getChildren().clear();
        if (hasReference) {
            MarkdownView refMd = new MarkdownView();
            refMd.setMarkdown(formatReference(question.referenceAnswer()));
            referenceContainer.getChildren().add(refMd);
        } else {
            Label empty = new Label("本题暂无参考答案。");
            empty.getStyleClass().add("secondary-text");
            referenceContainer.getChildren().add(empty);
        }
        referencePane.setExpanded(false);
    }

    @FXML
    private void clearAnswer() {
        answerArea.clear();
        answerArea.requestFocus();
    }

    @FXML
    private void submit() {
        if (questionId == null) {
            return;
        }
        String answer = answerArea.getText();
        if (answer == null || answer.isBlank()) {
            viewManager.showError("请先写下你的答案再提交评分");
            return;
        }
        long userId = sessionState.requireCurrentUser().id();

        setBusy(true);
        statusLabel.setText("AI 正在评分，请稍候...");
        clearFeedback();

        Flux<ScoreEvent> events;
        try {
            events = questionPracticeService.streamScore(userId, questionId, answer);
        } catch (RuntimeException ex) {
            setBusy(false);
            statusLabel.setText("");
            viewManager.showError("评分失败：" + exceptionHandler.toUserMessage(ex));
            return;
        }
        scoreSubscription = events.subscribe(
                event -> Platform.runLater(() -> handleScoreEvent(event)),
                throwable -> Platform.runLater(() -> {
                    setBusy(false);
                    statusLabel.setText("");
                    log.error("[QuestionPractice] 评分失败", throwable);
                    viewManager.showError("评分失败：" + exceptionHandler.toUserMessage(
                            throwable instanceof RuntimeException re ? re : new RuntimeException(throwable)));
                }),
                () -> Platform.runLater(() -> {
                    setBusy(false);
                    statusLabel.setText("");
                }));
    }

    private void handleScoreEvent(ScoreEvent event) {
        if (event instanceof ScoreEvent.Scores) {
            // 分数先到，但按用户要求「评分与点评一起出」，此处不提前渲染，
            // 等 Done 事件到达时一次性渲染分数卡 + 完整点评。
            statusLabel.setText("AI 正在评分，请稍候...");
        } else if (event instanceof ScoreEvent.Done d) {
            renderComplete(d.result());
        } else if (event instanceof ScoreEvent.Error e) {
            viewManager.showError("评分失败：" + e.message());
        }
    }

    /** 一次性渲染分数卡与详细点评，保证「评分与点评同时出现」。 */
    private void renderComplete(AnswerScore result) {
        renderScoreNumbers(result.score(), result.correctness(), result.depth());
        renderFeedback(result);
    }

    private void renderScoreNumbers(int score, int correctness, int depth) {
        scoreValueLabel.setText(String.valueOf(score));

        correctnessBar.setProgress(correctness / 100.0);
        correctnessLabel.setText(correctness + " 分");
        depthBar.setProgress(depth / 100.0);
        depthLabel.setText(depth + " 分");

        scoreCard.setVisible(true);
        scoreCard.setManaged(true);
    }

    private void renderFeedback(AnswerScore result) {
        feedbackContainer.getChildren().clear();
        MarkdownView md = new MarkdownView();
        md.setMarkdown(buildFeedbackMarkdown(result));
        // feedbackContainer 直接平铺展开，内容多时由整页外层 ScrollPane 统一滚动
        feedbackContainer.getChildren().add(md);
        Platform.runLater(() -> scoreCard.requestFocus());
    }

    private void clearFeedback() {
        feedbackContainer.getChildren().clear();
    }

    /**
     * 把参考答案的纯文本拆成逐行要点：先在每个序号 N) / N、 / N. 前换行，
     * 再按中文/英文分号、中文句号切分，让「每点分行」展示；
     * 以冒号结尾的片段（如「常见失效场景：」）作为小标题不加点。
     * 若内容本身已是多行格式则原样返回。
     */
    private String formatReference(String raw) {
        if (raw == null || raw.isBlank()) return "";
        String text = raw.strip();
        if (text.contains("\n")) return text;
        // 1) 序号 N) 前先换行，使「标题：1) 点…」拆成两行
        //    仅用「数字 + 右括号」作标记；不用「、」「.」以免误拆「16、」「0.75」等
        text = text.replaceAll("(?<=\\S)(?=\\d+\\))", "\n");
        // 2) 按分号 / 句号 / 已有换行切分
        String[] segs = text.split("\\s*[；;。\\n]\\s*");
        StringBuilder sb = new StringBuilder();
        for (String seg : segs) {
            String s = seg.strip();
            if (s.isEmpty()) continue;
            if (s.endsWith("：") || s.endsWith(":")) {
                sb.append(s).append("\n\n");
            } else {
                sb.append("- ").append(s).append("\n");
            }
        }
        String result = sb.toString().strip();
        return result.isEmpty() ? text : result;
    }

    private String buildFeedbackMarkdown(AnswerScore result) {
        StringBuilder sb = new StringBuilder();

        // 1) 总评段落（feedback）— 作为开篇自然语言点评
        if (result.feedback() != null && !result.feedback().isBlank()) {
            sb.append(result.feedback().strip()).append("\n\n");
        }

        // 2) 具体优点（strengths）— 结构化列出，不与总评重复
        if (!result.strengths().isEmpty()) {
            sb.append("**亮点**\n\n");
            for (String s : result.strengths()) {
                sb.append("- ").append(s).append("\n");
            }
            sb.append("\n");
        }

        // 3) 不足之处（weaknesses）
        if (!result.weaknesses().isEmpty()) {
            sb.append("**需要补强**\n\n");
            for (String w : result.weaknesses()) {
                sb.append("- ").append(w).append("\n");
            }
            sb.append("\n");
        }

        // 4) 改进建议（suggestion）
        if (result.suggestion() != null && !result.suggestion().isBlank()) {
            sb.append("**改进建议**\n\n").append(result.suggestion().strip()).append("\n");
        }

        return sb.toString().isBlank() ? "AI 未返回有效点评。" : sb.toString();
    }

    private void setBusy(boolean busy) {
        submitButton.setDisable(busy);
        clearButton.setDisable(busy);
        answerArea.setDisable(busy);
    }

    @FXML
    private void back() {
        if (scoreSubscription != null && !scoreSubscription.isDisposed()) {
            scoreSubscription.dispose();
        }
        contentNavigator.back();
    }

    private String difficultyText(InterviewDifficulty difficulty) {
        if (difficulty == null) return "未标注";
        return switch (difficulty) {
            case JUNIOR -> "初级";
            case MEDIUM -> "中级";
            case SENIOR -> "高级";
            case EXPERT -> "专家";
        };
    }

    private String difficultyBadgeClass(InterviewDifficulty difficulty) {
        if (difficulty == null) return "badge-junior";
        return switch (difficulty) {
            case JUNIOR -> "badge-junior";
            case MEDIUM -> "badge-medium";
            case SENIOR -> "badge-senior";
            case EXPERT -> "badge-expert";
        };
    }
}
