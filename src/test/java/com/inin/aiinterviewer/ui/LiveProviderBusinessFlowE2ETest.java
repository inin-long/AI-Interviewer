package com.inin.aiinterviewer.ui;

import com.inin.aiinterviewer.application.service.CandidateProfileService;
import com.inin.aiinterviewer.application.service.BackgroundTaskService;
import com.inin.aiinterviewer.application.service.InterviewPlanService;
import com.inin.aiinterviewer.application.service.InterviewResultService;
import com.inin.aiinterviewer.application.service.InterviewSessionService;
import com.inin.aiinterviewer.application.service.KnowledgeDocumentService;
import com.inin.aiinterviewer.application.service.ResumeService;
import com.inin.aiinterviewer.domain.enums.InterviewDifficulty;
import com.inin.aiinterviewer.domain.enums.InterviewStatus;
import com.inin.aiinterviewer.domain.enums.KnowledgeStatus;
import com.inin.aiinterviewer.domain.enums.ResumeStatus;
import com.inin.aiinterviewer.domain.enums.BackgroundTaskStatus;
import com.inin.aiinterviewer.domain.enums.BackgroundTaskType;
import com.inin.aiinterviewer.ui.component.MarkdownView;
import com.inin.aiinterviewer.ui.dialog.FileDialogService;
import com.inin.aiinterviewer.ui.navigation.JavaFxViewManager;
import com.inin.aiinterviewer.ui.navigation.Route;
import com.inin.aiinterviewer.ui.state.UserSessionState;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputControl;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Opt-in acceptance test that exercises the production OpenAI-compatible chat
 * and embedding adapters. It is intentionally excluded unless every provider
 * variable is present and AI_LLM_LIVE_TEST is explicitly enabled.
 */
@SpringBootTest
@Import(LiveProviderBusinessFlowE2ETest.LiveFileDialogConfiguration.class)
@ExtendWith(ApplicationExtension.class)
@EnabledOnOs(OS.WINDOWS)
@EnabledIf("liveProviderConfigured")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class LiveProviderBusinessFlowE2ETest {

    private static final String USERNAME = "live_provider_e2e";
    private static final String PASSWORD = "LiveProvider-2026";
    private static final String PLAN_NAME = "真实 Provider 全栈工程师面试";
    private static final String FIRST_ANSWER =
            "我负责 Java/Spring Boot 服务、Vue 3 管理端和交付流水线。订单与 Outbox 事件在同一事务落库，"
                    + "使用 requestId 唯一约束保证幂等，再由发布器至少一次投递。";

    @TempDir
    static Path applicationHome;

    @Autowired private JavaFxViewManager viewManager;
    @Autowired private UserSessionState sessionState;
    @Autowired private BackgroundTaskService backgroundTaskService;
    @Autowired private ResumeService resumeService;
    @Autowired private CandidateProfileService profileService;
    @Autowired private KnowledgeDocumentService knowledgeService;
    @Autowired private InterviewPlanService planService;
    @Autowired private InterviewSessionService sessionService;
    @Autowired private InterviewResultService resultService;

    static boolean liveProviderConfigured() {
        return truthy(System.getenv("AI_LLM_LIVE_TEST"))
                && present("AI_LLM_API_KEY")
                && present("AI_LLM_BASE_URL")
                && present("AI_LLM_CHAT_MODEL")
                && present("AI_LLM_EMBEDDING_MODEL");
    }

    @DynamicPropertySource
    static void liveWorkflowProperties(DynamicPropertyRegistry registry) {
        registry.add("ai.interviewer.home", () -> applicationHome.toString());
        registry.add("task.enabled", () -> "true");
        registry.add("task.worker-count", () -> "1");
        registry.add("task.poll-interval", () -> "100ms");
        registry.add("task.retry-count", () -> "2");
        registry.add("task.retry-delay", () -> "500ms");
        registry.add("llm.timeout", () -> "300s");
        registry.add("llm.max-retries", () -> "0");
        registry.add("llm.max-tokens", () -> "2048");
        registry.add("test.resume-path", () -> fixture("full-stack-engineer-resume.md").toString());
        registry.add("test.knowledge-path", () -> fixture("outbox-and-cache-consistency.md").toString());
    }

    @Start
    void start(Stage stage) {
        viewManager.attachStage(stage);
        viewManager.switchView(Route.LOGIN);
        stage.show();
        stage.toFront();
        stage.requestFocus();
    }

    @Test
    void completesRealChatEmbeddingKnowledgeAndReportWorkflow(FxRobot robot) throws Exception {
        registerAndLogin(robot);
        long userId = sessionState.requireCurrentUser().id();

        long resumeId = uploadResume(robot, userId);
        long profileId = generateAndConfirmProfile(robot, userId, resumeId);
        long documentId = uploadAndSearchKnowledge(robot, userId);
        createPlan(robot, userId, resumeId, profileId, documentId);
        long sessionId = conductInterview(robot, userId);
        verifyReport(robot, userId, sessionId);
    }

    private void registerAndLogin(FxRobot robot) throws Exception {
        fire(robot, "#showRegisterButton");
        waitForNode(robot, "#registerButton");
        setText(robot, "#usernameField", USERNAME);
        setText(robot, "#nicknameField", "真实模型测试用户");
        setText(robot, "#passwordField", PASSWORD);
        setText(robot, "#confirmPasswordField", PASSWORD);
        CheckBox agreement = robot.lookup("#agreementCheckBox").queryAs(CheckBox.class);
        robot.interact(() -> agreement.setSelected(true));
        WaitForAsyncUtils.asyncFx(() -> button(robot, "#registerButton").fire());

        DialogPane dialog = waitForDialog(robot);
        Button ok = (Button) dialog.lookupButton(ButtonType.OK);
        robot.interact(ok::fire);
        waitForNode(robot, "#loginButton");
        setText(robot, "#usernameField", USERNAME);
        setText(robot, "#passwordField", PASSWORD);
        fire(robot, "#loginButton");
        waitForNode(robot, "#mainRoot");
        assertThat(sessionState.requireCurrentUser().username()).isEqualTo(USERNAME);
    }

    private long uploadResume(FxRobot robot, long userId) throws Exception {
        fire(robot, "#resumesNavButton");
        waitForNode(robot, "#resumeTable");
        fire(robot, "#uploadButton");
        waitUntil(() -> !resumeService.list(userId).isEmpty());
        long resumeId = resumeService.list(userId).getFirst().id();
        waitUntil(() -> resumeService.list(userId).getFirst().status() == ResumeStatus.COMPLETED);

        fire(robot, "#resumesNavButton");
        waitForNode(robot, "#resumeTable");
        assertThat(resumeService.getDetail(userId, resumeId).parsedText())
                .contains("高级全栈工程师", "Outbox 模式");
        return resumeId;
    }

    private long generateAndConfirmProfile(FxRobot robot, long userId, long resumeId) throws Exception {
        TableView<?> resumes = table(robot, "#resumeTable");
        robot.interact(() -> resumes.getSelectionModel().selectFirst());
        fire(robot, "#viewButton");
        waitForNode(robot, "#generateButton");
        fire(robot, "#generateButton");
        awaitSuccessfulTask(userId, BackgroundTaskType.PROFILE_GENERATE);
        assertThat(profileService.find(userId, resumeId)).isPresent();

        fire(robot, "#resumesNavButton");
        waitForNode(robot, "#resumeTable");
        TableView<?> refreshed = table(robot, "#resumeTable");
        robot.interact(() -> refreshed.getSelectionModel().selectFirst());
        fire(robot, "#viewButton");
        waitForNode(robot, "#fullNameField");
        assertThat(textInput(robot, "#skillsField").getText()).isNotBlank();
        assertThat(textInput(robot, "#summaryArea").getText()).isNotBlank();
        fire(robot, "#confirmButton");
        waitUntil(() -> profileService.find(userId, resumeId).orElseThrow().confirmed());
        return profileService.find(userId, resumeId).orElseThrow().id();
    }

    private long uploadAndSearchKnowledge(FxRobot robot, long userId) throws Exception {
        fire(robot, "#knowledgeNavButton");
        waitForNode(robot, "#documentTable");
        fire(robot, "#uploadButton");
        waitUntil(() -> !knowledgeService.list(userId).isEmpty());
        long documentId = knowledgeService.list(userId).getFirst().id();
        awaitSuccessfulTask(userId, BackgroundTaskType.DOCUMENT_PARSE);
        assertThat(knowledgeService.list(userId).getFirst().status()).isEqualTo(KnowledgeStatus.READY);

        fire(robot, "#knowledgeNavButton");
        waitForNode(robot, "#documentTable");
        setText(robot, "#searchField", "订单消息发送失败后如何通过 Outbox 恢复");
        fire(robot, "#searchButton");
        waitUntil(() -> !textInput(robot, "#searchResultArea").getText().isBlank());
        assertThat(textInput(robot, "#searchResultArea").getText())
                .contains("outbox-and-cache-consistency");
        return documentId;
    }

    private void createPlan(
            FxRobot robot, long userId, long resumeId, long profileId, long documentId
    ) throws Exception {
        fire(robot, "#plansNavButton");
        waitForNode(robot, "#planTable");
        fire(robot, "#createPlanButton");
        waitForNode(robot, "#savePlanButton");
        setText(robot, "#nameField", PLAN_NAME);
        setText(robot, "#jobTitleField", "高级全栈工程师");
        setText(robot, "#jobDescriptionArea",
                "负责 Java、Spring Boot、Vue 3、PostgreSQL、Redis，以及订单一致性和可观测性建设。");
        setText(robot, "#durationField", "30");
        setText(robot, "#questionCountField", "1");
        setText(robot, "#focusField", "项目深挖、事务消息、缓存一致性、全栈工程能力");
        selectCombo(robot, "#difficultyBox", InterviewDifficulty.SENIOR);
        selectFirst(robot, "#resumeBox");
        selectFirst(robot, "#profileBox");
        selectListFirst(robot, "#knowledgeList");
        fire(robot, "#savePlanButton");
        waitUntil(() -> planService.list(userId).size() == 1);

        var plan = planService.list(userId).getFirst();
        assertThat(plan.resumeId()).isEqualTo(resumeId);
        assertThat(plan.profileId()).isEqualTo(profileId);
        assertThat(plan.knowledgeDocumentIds()).containsExactly(documentId);
    }

    private long conductInterview(FxRobot robot, long userId) throws Exception {
        TableView<?> plans = table(robot, "#planTable");
        robot.interact(() -> plans.getSelectionModel().selectFirst());
        fire(robot, "#startButton");
        waitForNode(robot, "#workspaceRoot");
        waitUntil(() -> !sessionService.list(userId).isEmpty());
        long sessionId = sessionService.list(userId).getFirst().id();

        awaitInitialQuestion(robot, userId, sessionId);
        assertQuestion(sessionService.messages(userId, sessionId).getFirst().content());
        waitUntil(() -> !textInput(robot, "#answerArea").isDisabled());
        setText(robot, "#answerArea", FIRST_ANSWER);
        fire(robot, "#submitButton");

        awaitSuccessfulTask(userId, BackgroundTaskType.REPORT_GENERATE);
        assertThat(resultService.find(userId, sessionId)).isPresent();
        waitUntil(() -> button(robot, "#reportButton").isVisible());
        assertThat(sessionService.require(userId, sessionId).status()).isEqualTo(InterviewStatus.COMPLETED);
        return sessionId;
    }

    private void verifyReport(FxRobot robot, long userId, long sessionId) throws Exception {
        fire(robot, "#reportButton");
        waitForNode(robot, "#overallScoreLabel");
        var report = resultService.find(userId, sessionId).orElseThrow();
        assertThat(report.overallScore()).isBetween(0, 100);
        assertThat(report.dimensions()).hasSize(6)
                .allSatisfy((name, score) -> assertThat(score).isBetween(0, 100));
        assertThat(report.summary()).isNotBlank();

        MarkdownView reportView = robot.lookup("#reportView").queryAs(MarkdownView.class);
        assertThat(reportView.getMarkdown())
                .contains(PLAN_NAME, "综合得分", "综合评价", "问答摘要", "参考依据", FIRST_ANSWER);
        assertThat(label(robot, "#overallScoreLabel").getText()).endsWith(" / 100");
    }

    private void awaitInitialQuestion(FxRobot robot, long userId, long sessionId) throws Exception {
        for (int attempt = 1; attempt <= 2; attempt++) {
            waitUntil(() -> sessionService.messages(userId, sessionId).size() == 1
                    || (button(robot, "#retryQuestionButton").isVisible()
                    && !button(robot, "#retryQuestionButton").isDisabled()));
            if (sessionService.messages(userId, sessionId).size() == 1) return;
            if (attempt < 2) fire(robot, "#retryQuestionButton");
        }
        assertThat(sessionService.messages(userId, sessionId))
                .withFailMessage("Live provider did not generate the initial interview question after retry")
                .hasSize(1);
    }

    private void assertQuestion(String question) {
        assertThat(question).isNotBlank().hasSizeGreaterThan(8);
    }

    private void setText(FxRobot robot, String selector, String value) {
        TextInputControl control = textInput(robot, selector);
        robot.interact(() -> control.setText(value));
    }

    private void fire(FxRobot robot, String selector) {
        Button control = button(robot, selector);
        robot.interact(control::fire);
    }

    @SuppressWarnings("unchecked")
    private <T> void selectCombo(FxRobot robot, String selector, T value) {
        ComboBox<T> comboBox = (ComboBox<T>) robot.lookup(selector).queryAs(ComboBox.class);
        robot.interact(() -> comboBox.getSelectionModel().select(value));
    }

    private void selectFirst(FxRobot robot, String selector) {
        ComboBox<?> comboBox = robot.lookup(selector).queryAs(ComboBox.class);
        robot.interact(() -> comboBox.getSelectionModel().selectFirst());
    }

    private void selectListFirst(FxRobot robot, String selector) {
        ListView<?> listView = robot.lookup(selector).queryAs(ListView.class);
        robot.interact(() -> listView.getSelectionModel().selectFirst());
    }

    private DialogPane waitForDialog(FxRobot robot) throws Exception {
        waitUntil(() -> !robot.lookup(".dialog-pane").queryAll().isEmpty());
        return robot.lookup(".dialog-pane").queryAs(DialogPane.class);
    }

    private void waitForNode(FxRobot robot, String selector) throws Exception {
        waitUntil(() -> !robot.lookup(selector).queryAll().isEmpty());
    }

    private void waitUntil(java.util.concurrent.Callable<Boolean> condition) throws Exception {
        WaitForAsyncUtils.waitFor(420, TimeUnit.SECONDS, condition);
        WaitForAsyncUtils.waitForFxEvents();
    }

    private void awaitSuccessfulTask(long userId, BackgroundTaskType type) throws Exception {
        waitUntil(() -> backgroundTaskService.list(userId).stream()
                .filter(task -> task.getTaskType() == type)
                .anyMatch(task -> task.getStatus() == BackgroundTaskStatus.SUCCESS
                        || task.getStatus() == BackgroundTaskStatus.FAILED));
        var task = backgroundTaskService.list(userId).stream()
                .filter(item -> item.getTaskType() == type)
                .max(java.util.Comparator.comparingLong(item -> item.getId()))
                .orElseThrow();
        assertThat(task.getStatus())
                .withFailMessage("Live %s task failed: %s", type, task.getErrorMessage())
                .isEqualTo(BackgroundTaskStatus.SUCCESS);
    }

    private TextInputControl textInput(FxRobot robot, String selector) {
        return robot.lookup(selector).queryAs(TextInputControl.class);
    }

    private Label label(FxRobot robot, String selector) {
        return robot.lookup(selector).queryAs(Label.class);
    }

    private Button button(FxRobot robot, String selector) {
        return robot.lookup(selector).queryAs(Button.class);
    }

    private TableView<?> table(FxRobot robot, String selector) {
        return robot.lookup(selector).queryAs(TableView.class);
    }

    private static boolean truthy(String value) {
        return value != null && switch (value.strip().toLowerCase(Locale.ROOT)) {
            case "1", "true", "yes", "on" -> true;
            default -> false;
        };
    }

    private static boolean present(String name) {
        String value = System.getenv(name);
        return value != null && !value.isBlank();
    }

    private static Path fixture(String fileName) {
        try {
            var resource = LiveProviderBusinessFlowE2ETest.class.getResource("/fixtures/" + fileName);
            if (resource == null) throw new IllegalStateException("Missing live TestFX fixture: " + fileName);
            return Path.of(resource.toURI()).toAbsolutePath().normalize();
        } catch (URISyntaxException exception) {
            throw new IllegalStateException("Invalid live TestFX fixture path: " + fileName, exception);
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class LiveFileDialogConfiguration {

        @Bean
        @Primary
        FileDialogService configuredLiveFileDialogService(
                @Value("${test.resume-path}") String resumePath,
                @Value("${test.knowledge-path}") String knowledgePath
        ) {
            Path resume = readable(resumePath, "resume");
            Path knowledge = readable(knowledgePath, "knowledge document");
            return new FileDialogService() {
                @Override public Optional<Path> chooseResume(javafx.stage.Window owner) {
                    return Optional.of(resume);
                }

                @Override public Optional<Path> chooseKnowledgeDocument(javafx.stage.Window owner) {
                    return Optional.of(knowledge);
                }
            };
        }

        private Path readable(String value, String description) {
            Path path = Path.of(value).toAbsolutePath().normalize();
            if (!Files.isRegularFile(path) || !Files.isReadable(path)) {
                throw new IllegalStateException("Configured live TestFX " + description + " is not readable: " + path);
            }
            return path;
        }
    }
}
