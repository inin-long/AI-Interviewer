package com.inin.aiinterviewer.ui;

import com.inin.aiinterviewer.application.service.BackgroundTaskService;
import com.inin.aiinterviewer.application.service.InterviewPlanService;
import com.inin.aiinterviewer.application.service.InterviewResultService;
import com.inin.aiinterviewer.application.service.InterviewSessionService;
import com.inin.aiinterviewer.application.service.ResumeService;
import com.inin.aiinterviewer.domain.enums.BackgroundTaskStatus;
import com.inin.aiinterviewer.domain.enums.BackgroundTaskType;
import com.inin.aiinterviewer.domain.enums.InterviewDifficulty;
import com.inin.aiinterviewer.domain.enums.InterviewStatus;
import com.inin.aiinterviewer.domain.enums.ResumeStatus;
import com.inin.aiinterviewer.infrastructure.ai.ChatService;
import com.inin.aiinterviewer.ui.component.MarkdownView;
import com.inin.aiinterviewer.ui.dialog.FileDialogService;
import com.inin.aiinterviewer.ui.navigation.JavaFxViewManager;
import com.inin.aiinterviewer.ui.navigation.Route;
import com.inin.aiinterviewer.ui.state.UserSessionState;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputControl;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
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
import reactor.core.publisher.Flux;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(CompleteBusinessFlowE2ETest.WorkflowTestConfiguration.class)
@ExtendWith(ApplicationExtension.class)
@EnabledOnOs(OS.WINDOWS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CompleteBusinessFlowE2ETest {

    private static final String USERNAME = "fullstack_e2e";
    private static final String PASSWORD = "FullStack-2026";
    private static final String PLAN_NAME = "高级全栈工程师完整流程面试";
    private static final String RESUME_FILE = "full-stack-engineer-resume.md";
    private static final String FIRST_QUESTION =
            "请结合订单协同平台的经历，说明你如何用 Outbox 模式和幂等键保证订单与消息的一致性？";
    private static final String ANSWER = "订单写入和 Outbox 事件写入同一个数据库事务，并以业务请求号建立唯一约束。"
            + "事务提交后由发布器投递事件，消费者同样按事件编号幂等处理。发送失败会重试并告警，"
            + "对长时间未投递的记录由补偿任务扫描，因此既避免双写不一致，也能处理消息重复。";
    private static final String REPORT_SUMMARY =
            "候选人能够从事务边界、业务幂等、消息重试和补偿机制解释一致性方案，技术基础扎实，表达清晰。";

    @TempDir
    static Path applicationHome;

    @Autowired private JavaFxViewManager viewManager;
    @Autowired private UserSessionState sessionState;
    @Autowired private BackgroundTaskService backgroundTaskService;
    @Autowired private ResumeService resumeService;
    @Autowired private InterviewPlanService planService;
    @Autowired private InterviewSessionService sessionService;
    @Autowired private InterviewResultService resultService;

    @DynamicPropertySource
    static void workflowProperties(DynamicPropertyRegistry registry) {
        registry.add("ai.interviewer.home", () -> applicationHome.toString());
        registry.add("task.enabled", () -> "false");
        registry.add("llm.base-url", () -> "http://127.0.0.1:1");
        registry.add("llm.api-key", () -> "testfx-local-key");
        registry.add("llm.chat-model", () -> "deterministic-test-model");
        registry.add("llm.embedding-model", () -> "");
        registry.add("test.resume-path", () -> resumeFixture().toString());
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
    void completesRegistrationResumeInterviewAndReportWorkflow(FxRobot robot) throws Exception {
        registerAndLogin(robot);
        long userId = sessionState.requireCurrentUser().id();

        uploadAndParseResume(robot, userId);
        createInterviewPlan(robot, userId);
        long sessionId = conductInterview(robot, userId);
        verifyReport(robot, userId, sessionId);
    }

    private void registerAndLogin(FxRobot robot) throws Exception {
        fire(robot, "#showRegisterButton");
        waitForNode(robot, "#registerButton");

        setText(robot, "#usernameField", USERNAME);
        setText(robot, "#nicknameField", "林泽宇");
        setText(robot, "#passwordField", PASSWORD);
        setText(robot, "#confirmPasswordField", PASSWORD);
        robot.interact(() -> robot.lookup("#agreementCheckBox")
                .queryAs(javafx.scene.control.CheckBox.class).setSelected(true));
        WaitForAsyncUtils.asyncFx(() -> button(robot, "#registerButton").fire());

        DialogPane dialog = waitForDialog(robot);
        Button okButton = (Button) dialog.lookupButton(ButtonType.OK);
        robot.interact(okButton::fire);
        waitForNode(robot, "#loginButton");

        setText(robot, "#usernameField", USERNAME);
        setText(robot, "#passwordField", PASSWORD);
        fire(robot, "#loginButton");
        waitForNode(robot, "#mainRoot");

        assertThat(label(robot, "#usernameLabel").getText()).isEqualTo("林泽宇");
        assertThat(sessionState.requireCurrentUser().username()).isEqualTo(USERNAME);
    }

    private void uploadAndParseResume(FxRobot robot, long userId) throws Exception {
        fire(robot, "#resumesNavButton");
        waitForNode(robot, "#resumeTable");
        fire(robot, "#uploadButton");

        waitUntil(() -> backgroundTaskService.list(userId).stream()
                .anyMatch(task -> task.getTaskType() == BackgroundTaskType.RESUME_PARSE));
        waitUntil(() -> resumeService.list(userId).size() == 1);
        assertThat(backgroundTaskService.executeNext("testfx-resume-worker")).isTrue();
        waitUntil(() -> resumeService.list(userId).getFirst().status() == ResumeStatus.COMPLETED);

        fire(robot, "#resumesNavButton");
        TableView<?> table = table(robot, "#resumeTable");
        waitUntil(() -> table.getItems().size() == 1);
        assertThat(table.getItems().getFirst().toString()).contains(RESUME_FILE);
        assertThat(resumeService.getDetail(userId, resumeService.list(userId).getFirst().id()).parsedText())
                .contains("高级全栈工程师", "Outbox 模式", "Vue 3");
    }

    private void createInterviewPlan(FxRobot robot, long userId) throws Exception {
        fire(robot, "#plansNavButton");
        waitForNode(robot, "#planTable");
        fire(robot, "#createPlanButton");
        waitForNode(robot, "#savePlanButton");

        setText(robot, "#nameField", PLAN_NAME);
        setText(robot, "#jobTitleField", "高级全栈工程师");
        setText(robot, "#jobDescriptionArea",
                "负责 Java/Spring Boot 服务、Vue 3 前端、数据一致性、可观测性及自动化交付。");
        setText(robot, "#durationField", "30");
        setText(robot, "#questionCountField", "1");
        setText(robot, "#focusField", "全栈架构、事务消息、缓存一致性、工程质量");
        selectCombo(robot, "#difficultyBox", InterviewDifficulty.SENIOR);
        selectFirst(robot, "#resumeBox");

        fire(robot, "#savePlanButton");
        waitForNode(robot, "#planTable");
        waitUntil(() -> planService.list(userId).size() == 1);

        var plan = planService.list(userId).getFirst();
        assertThat(plan.name()).isEqualTo(PLAN_NAME);
        assertThat(plan.resumeId()).isEqualTo(resumeService.list(userId).getFirst().id());
        assertThat(plan.questionCount()).isEqualTo(1);
        assertThat(plan.difficulty()).isEqualTo(InterviewDifficulty.SENIOR);
    }

    private long conductInterview(FxRobot robot, long userId) throws Exception {
        TableView<?> plans = table(robot, "#planTable");
        robot.interact(() -> plans.getSelectionModel().selectFirst());
        fire(robot, "#startButton");
        waitForNode(robot, "#workspaceRoot");

        waitUntil(() -> !sessionService.list(userId).isEmpty());
        long sessionId = sessionService.list(userId).getFirst().id();
        waitUntil(() -> sessionService.messages(userId, sessionId).size() == 1);
        waitUntil(() -> !textInput(robot, "#answerArea").isDisabled());

        assertThat(sessionService.messages(userId, sessionId).getFirst().content()).isEqualTo(FIRST_QUESTION);
        assertThat(label(robot, "#progressLabel").getText()).isEqualTo("第 1 / 1 题");

        setText(robot, "#answerArea", ANSWER);
        fire(robot, "#submitButton");
        waitUntil(() -> sessionService.messages(userId, sessionId).size() == 2);
        waitUntil(() -> backgroundTaskService.list(userId).stream().anyMatch(task ->
                task.getTaskType() == BackgroundTaskType.REPORT_GENERATE
                        && task.getStatus() == BackgroundTaskStatus.PENDING));

        assertThat(backgroundTaskService.executeNext("testfx-report-worker")).isTrue();
        waitUntil(() -> resultService.find(userId, sessionId).isPresent());
        waitUntil(() -> button(robot, "#reportButton").isVisible());
        assertThat(sessionService.require(userId, sessionId).status()).isEqualTo(InterviewStatus.COMPLETED);
        return sessionId;
    }

    private void verifyReport(FxRobot robot, long userId, long sessionId) throws Exception {
        fire(robot, "#reportButton");
        waitForNode(robot, "#overallScoreLabel");

        assertThat(label(robot, "#overallScoreLabel").getText()).isEqualTo("88 / 100");
        assertThat(label(robot, "#technicalScoreLabel").getText()).isEqualTo("91 分");
        assertThat(label(robot, "#systemDesignScoreLabel").getText()).isEqualTo("90 分");

        MarkdownView reportView = robot.lookup("#reportView").queryAs(MarkdownView.class);
        assertThat(reportView.getMarkdown())
                .contains(PLAN_NAME, REPORT_SUMMARY, "综合得分：88 / 100", "问答摘要", ANSWER);

        var report = resultService.find(userId, sessionId).orElseThrow();
        assertThat(report.overallScore()).isEqualTo(88);
        assertThat(report.dimensions()).containsEntry("technical", 91).containsEntry("systemDesign", 90);
    }

    private static Path resumeFixture() {
        try {
            var resource = CompleteBusinessFlowE2ETest.class
                    .getResource("/fixtures/full-stack-engineer-resume.md");
            if (resource == null) throw new IllegalStateException("Missing full-stack resume fixture");
            return Path.of(resource.toURI()).toAbsolutePath().normalize();
        } catch (URISyntaxException exception) {
            throw new IllegalStateException("Invalid full-stack resume fixture path", exception);
        }
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

    private DialogPane waitForDialog(FxRobot robot) throws Exception {
        waitUntil(() -> !robot.lookup(".dialog-pane").queryAll().isEmpty());
        return robot.lookup(".dialog-pane").queryAs(DialogPane.class);
    }

    private void waitForNode(FxRobot robot, String selector) throws Exception {
        waitUntil(() -> !robot.lookup(selector).queryAll().isEmpty());
        WaitForAsyncUtils.waitForFxEvents();
    }

    private void waitUntil(java.util.concurrent.Callable<Boolean> condition) throws Exception {
        WaitForAsyncUtils.waitFor(15, TimeUnit.SECONDS, condition);
        WaitForAsyncUtils.waitForFxEvents();
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

    @TestConfiguration(proxyBeanMethods = false)
    static class WorkflowTestConfiguration {

        @Bean
        @Primary
        FileDialogService configuredFileDialogService(@Value("${test.resume-path}") String configuredPath) {
            Path resume = Path.of(configuredPath).toAbsolutePath().normalize();
            if (!Files.isRegularFile(resume) || !Files.isReadable(resume)) {
                throw new IllegalStateException("Configured TestFX resume is not readable: " + resume);
            }
            return owner -> Optional.of(resume);
        }

        @Bean
        @Primary
        ChatService deterministicWorkflowChatService() {
            return new ChatService() {
                @Override
                public String chat(String prompt) {
                    if (prompt.contains("技术面试评分器")) {
                        return """
                                {"overallScore":88,"technicalScore":91,"problemSolvingScore":89,
                                "projectScore":90,"systemDesignScore":90,"communicationScore":86,
                                "comprehensiveScore":88,"summary":"%s"}
                                """.formatted(REPORT_SUMMARY);
                    }
                    if (prompt.contains("技术面试回答分析器")) {
                        return """
                                {"correctness":90,"depth":88,"missingPoints":[],"feedback":"回答完整"}
                                """;
                    }
                    if (prompt.contains("流程决策器")) {
                        return """
                                {"action":"FOLLOW_UP","nextStage":null,"reason":"继续追问"}
                                """;
                    }
                    throw new IllegalArgumentException("Unexpected deterministic chat prompt: " + prompt);
                }

                @Override
                public Flux<String> stream(String prompt) {
                    return Flux.just(FIRST_QUESTION.substring(0, 34), FIRST_QUESTION.substring(34));
                }
            };
        }
    }
}
