package com.inin.aiinterviewer.ui;

import com.inin.aiinterviewer.application.dto.BackgroundTaskDto;
import com.inin.aiinterviewer.application.service.BackgroundTaskService;
import com.inin.aiinterviewer.application.service.CandidateProfileService;
import com.inin.aiinterviewer.application.service.InterviewPlanService;
import com.inin.aiinterviewer.application.service.InterviewResultService;
import com.inin.aiinterviewer.application.service.InterviewSessionService;
import com.inin.aiinterviewer.application.service.KnowledgeDocumentService;
import com.inin.aiinterviewer.application.service.ResumeService;
import com.inin.aiinterviewer.domain.enums.BackgroundTaskStatus;
import com.inin.aiinterviewer.domain.enums.BackgroundTaskType;
import com.inin.aiinterviewer.domain.enums.InterviewDifficulty;
import com.inin.aiinterviewer.domain.enums.InterviewStatus;
import com.inin.aiinterviewer.domain.enums.KnowledgeStatus;
import com.inin.aiinterviewer.domain.enums.ResumeStatus;
import com.inin.aiinterviewer.infrastructure.ai.ChatService;
import com.inin.aiinterviewer.infrastructure.ai.EmbeddingService;
import com.inin.aiinterviewer.ui.component.InterviewTranscriptView;
import com.inin.aiinterviewer.ui.component.MarkdownView;
import com.inin.aiinterviewer.ui.dialog.FileDialogService;
import com.inin.aiinterviewer.ui.navigation.JavaFxViewManager;
import com.inin.aiinterviewer.ui.navigation.Route;
import com.inin.aiinterviewer.ui.state.UserSessionState;
import javafx.css.PseudoClass;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
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
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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
    private static final String KNOWLEDGE_FILE = "outbox-and-cache-consistency.md";
    private static final List<String> QUESTIONS = List.of(
            "请简要介绍你在订单协同平台中承担的全栈职责，以及最有挑战的一项技术决策。",
            "结合知识资料说明，订单写入成功但消息发送失败时，Outbox、幂等键和补偿任务如何协作？",
            "如果 Redis 缓存删除失败且热点请求持续回源，你会怎样控制脏数据窗口和缓存重建并发？");
    private static final List<String> ANSWERS = List.of(
            "我负责订单聚合、库存预占接口和 Vue 3 管理端，最有挑战的是在不引入分布式事务的前提下保证订单与履约消息最终一致。",
            "订单与 Outbox 事件在同一数据库事务写入，requestId 唯一约束保证请求幂等。发布器至少一次投递，消费者按事件号去重；失败通过退避重试、租约恢复和补偿扫描处理。",
            "事务提交后发送失效事件，删除失败自动重试并执行延迟二次删除。热点键使用单飞合并回源、随机过期和限流，数据库始终是事实来源，并监控回源量与重建失败。"
    );
    private static final String REPORT_SUMMARY =
            "候选人能够从事务边界、业务幂等、消息重试和补偿机制解释一致性方案，技术基础扎实，表达清晰。";

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

    @DynamicPropertySource
    static void workflowProperties(DynamicPropertyRegistry registry) {
        registry.add("ai.interviewer.home", () -> applicationHome.toString());
        registry.add("task.enabled", () -> "false");
        registry.add("llm.base-url", () -> "http://127.0.0.1:1");
        registry.add("llm.api-key", () -> "testfx-local-key");
        registry.add("llm.chat-model", () -> "deterministic-test-model");
        registry.add("llm.embedding-model", () -> "deterministic-test-embedding");
        registry.add("test.resume-path", () -> resumeFixture().toString());
        registry.add("test.knowledge-path", () -> knowledgeFixture().toString());
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
    void completesLocalProfileRagInterviewAndReportWorkflow(FxRobot robot) throws Exception {
        registerAndLogin(robot);
        long userId = sessionState.requireCurrentUser().id();

        uploadAndParseResume(robot, userId);
        generateAndConfirmProfile(robot, userId);
        uploadIndexAndSearchKnowledge(robot, userId);
        createInterviewPlan(robot, userId);
        long sessionId = conductInterview(robot, userId);
        verifyReport(robot, userId, sessionId);
        deleteCompletedTask(robot, userId);
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

    private void generateAndConfirmProfile(FxRobot robot, long userId) throws Exception {
        long resumeId = resumeService.list(userId).getFirst().id();
        TableView<?> resumes = table(robot, "#resumeTable");
        robot.interact(() -> resumes.getSelectionModel().selectFirst());
        fire(robot, "#viewButton");
        waitForNode(robot, "#generateButton");
        fire(robot, "#generateButton");

        waitUntil(() -> backgroundTaskService.list(userId).stream().anyMatch(task ->
                task.getTaskType() == BackgroundTaskType.PROFILE_GENERATE
                        && task.getStatus() == BackgroundTaskStatus.PENDING));
        assertThat(backgroundTaskService.executeNext("testfx-profile-worker")).isTrue();
        waitUntil(() -> profileService.find(userId, resumeId).isPresent());

        fire(robot, "#resumesNavButton");
        waitForNode(robot, "#resumeTable");
        TableView<?> refreshedResumes = table(robot, "#resumeTable");
        robot.interact(() -> refreshedResumes.getSelectionModel().selectFirst());
        fire(robot, "#viewButton");
        waitForNode(robot, "#fullNameField");

        assertThat(textInput(robot, "#fullNameField").getText()).isEqualTo("林泽宇");
        assertThat(textInput(robot, "#targetRoleField").getText()).isEqualTo("高级全栈工程师");
        assertThat(textInput(robot, "#skillsField").getText()).contains("Java 21", "Vue 3", "Redis");
        fire(robot, "#confirmButton");
        waitUntil(() -> profileService.find(userId, resumeId).orElseThrow().confirmed());
        waitForNode(robot, "#resumeTable");
        assertThat(table(robot, "#resumeTable").getItems()).hasSize(1);
        PseudoClass selected = PseudoClass.getPseudoClass("selected");
        assertThat(button(robot, "#resumesNavButton").getPseudoClassStates()).contains(selected);
        assertThat(button(robot, "#profilesNavButton").getPseudoClassStates()).doesNotContain(selected);
    }

    private void uploadIndexAndSearchKnowledge(FxRobot robot, long userId) throws Exception {
        fire(robot, "#knowledgeNavButton");
        waitForNode(robot, "#documentTable");
        fire(robot, "#uploadButton");

        waitUntil(() -> backgroundTaskService.list(userId).stream().anyMatch(task ->
                task.getTaskType() == BackgroundTaskType.DOCUMENT_PARSE
                        && task.getStatus() == BackgroundTaskStatus.PENDING));
        assertThat(backgroundTaskService.executeNext("testfx-knowledge-worker")).isTrue();
        waitUntil(() -> !knowledgeService.list(userId).isEmpty()
                && knowledgeService.list(userId).getFirst().status() == KnowledgeStatus.READY);

        fire(robot, "#knowledgeNavButton");
        waitForNode(robot, "#documentTable");
        setText(robot, "#searchField", "Outbox 消息失败如何补偿并保证幂等");
        fire(robot, "#searchButton");
        waitUntil(() -> textInput(robot, "#searchResultArea").getText().contains("Outbox"));
        assertThat(textInput(robot, "#searchResultArea").getText())
                .contains("outbox-and-cache-consistency", "requestId", "补偿");

        TableView<?> documents = table(robot, "#documentTable");
        robot.interact(() -> documents.getSelectionModel().selectFirst());
        fire(robot, "#viewButton");
        waitForNode(robot, "#chunksArea");
        assertThat(textInput(robot, "#chunksArea").getText())
                .contains("事务消息与 Outbox", "Redis 缓存一致性", "片段");
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
        setText(robot, "#questionCountField", Integer.toString(QUESTIONS.size()));
        setText(robot, "#focusField", "全栈架构、事务消息、缓存一致性、工程质量");
        selectCombo(robot, "#difficultyBox", InterviewDifficulty.SENIOR);
        selectFirst(robot, "#resumeBox");
        selectFirst(robot, "#profileBox");
        selectListFirst(robot, "#knowledgeList");

        fire(robot, "#savePlanButton");
        waitForNode(robot, "#planTable");
        waitUntil(() -> planService.list(userId).size() == 1);

        var plan = planService.list(userId).getFirst();
        assertThat(plan.name()).isEqualTo(PLAN_NAME);
        assertThat(plan.resumeId()).isEqualTo(resumeService.list(userId).getFirst().id());
        assertThat(plan.questionCount()).isEqualTo(QUESTIONS.size());
        assertThat(plan.difficulty()).isEqualTo(InterviewDifficulty.SENIOR);
        assertThat(plan.profileId()).isNotNull();
        assertThat(plan.knowledgeDocumentIds()).containsExactly(knowledgeService.list(userId).getFirst().id());
    }

    private long conductInterview(FxRobot robot, long userId) throws Exception {
        TableView<?> plans = table(robot, "#planTable");
        robot.interact(() -> plans.getSelectionModel().selectFirst());
        fire(robot, "#startButton");
        waitForNode(robot, "#workspaceRoot");
        Stage interviewStage = (Stage) robot.lookup("#workspaceRoot").query().getScene().getWindow();
        waitUntil(interviewStage::isMaximized);
        assertThat(textInput(robot, "#answerArea").getMinHeight()).isGreaterThanOrEqualTo(132);

        waitUntil(() -> !sessionService.list(userId).isEmpty());
        long sessionId = sessionService.list(userId).getFirst().id();
        for (int index = 0; index < QUESTIONS.size(); index++) {
            int expectedMessagesBeforeAnswer = index * 2 + 1;
            waitUntil(() -> sessionService.messages(userId, sessionId).size()
                    >= expectedMessagesBeforeAnswer);
            waitUntil(() -> !textInput(robot, "#answerArea").isDisabled());
            var messages = sessionService.messages(userId, sessionId);
            assertThat(messages.get(expectedMessagesBeforeAnswer - 1).content()).isEqualTo(QUESTIONS.get(index));
            assertThat(label(robot, "#progressLabel").getText())
                    .isEqualTo("第 " + (index + 1) + " / " + QUESTIONS.size() + " 题");
            setText(robot, "#answerArea", ANSWERS.get(index));
            fire(robot, "#submitButton");
        }
        waitUntil(() -> sessionService.messages(userId, sessionId).size() == QUESTIONS.size() * 2);
        InterviewTranscriptView transcript = robot.lookup("#transcriptView").queryAs(InterviewTranscriptView.class);
        waitUntil(() -> transcript.getVvalue() >= 0.999);
        waitUntil(() -> backgroundTaskService.list(userId).stream().anyMatch(task ->
                task.getTaskType() == BackgroundTaskType.REPORT_GENERATE
                        && task.getStatus() == BackgroundTaskStatus.PENDING));

        assertThat(backgroundTaskService.executeNext("testfx-report-worker")).isTrue();
        waitUntil(() -> resultService.find(userId, sessionId).isPresent());
        waitUntil(() -> button(robot, "#reportButton").isVisible());
        assertThat(sessionService.require(userId, sessionId).status()).isEqualTo(InterviewStatus.COMPLETED);
        assertThat(sessionService.profileSnapshot(userId, sessionId)).isPresent();
        assertThat(sessionService.knowledgeSnapshot(userId, sessionId)).hasSize(1);
        assertThat(sessionService.messages(userId, sessionId).stream()
                .filter(message -> !message.citations().isEmpty()).toList()).hasSize(2)
                .allSatisfy(message -> assertThat(message.citations().getFirst().documentName())
                        .isEqualTo("outbox-and-cache-consistency"));
        return sessionId;
    }

    private void verifyReport(FxRobot robot, long userId, long sessionId) throws Exception {
        fire(robot, "#reportButton");
        waitForNode(robot, "#overallScoreLabel");

        assertThat(label(robot, "#overallScoreLabel").getText()).isEqualTo("88 / 100 · 中置信度");
        assertThat(label(robot, "#technicalScoreLabel").getText()).isEqualTo("88 分 · 中置信度");
        assertThat(label(robot, "#problemSolvingScoreLabel").getText()).isEqualTo("证据不足");
        assertThat(label(robot, "#systemDesignScoreLabel").getText()).isEqualTo("88 分 · 中置信度");

        MarkdownView reportView = robot.lookup("#reportView").queryAs(MarkdownView.class);
        assertThat(reportView.getMarkdown())
                .contains(PLAN_NAME, REPORT_SUMMARY, "综合得分：88 / 100", "问答摘要",
                        ANSWERS.getFirst(), ANSWERS.getLast(), "参考依据", "outbox-and-cache-consistency");

        var report = resultService.find(userId, sessionId).orElseThrow();
        assertThat(report.overallScore()).isEqualTo(88);
        assertThat(report.dimensions()).containsEntry("technical", 88).containsEntry("systemDesign", 88);
        assertThat(report.scoreEvidence().get("technical").evidenceIds()).hasSize(3);
        assertThat(label(robot, "#systemDesignScoreLabel").getStyleClass())
                .contains("score-evidence-link");
        assertThat(label(robot, "#problemSolvingScoreLabel").getStyleClass())
                .doesNotContain("score-evidence-link");
        Label scoreLink = label(robot, "#systemDesignScoreLabel");
        robot.interact(() -> scoreLink.getOnMouseClicked().handle(null));
        waitForNode(robot, "#messageCountLabel");
    }

    private void deleteCompletedTask(FxRobot robot, long userId) throws Exception {
        int taskCount = backgroundTaskService.list(userId).size();
        fire(robot, "#tasksNavButton");
        waitForNode(robot, "#taskTable");
        @SuppressWarnings("unchecked")
        TableView<BackgroundTaskDto> tasks = (TableView<BackgroundTaskDto>) (TableView<?>) table(robot, "#taskTable");
        waitUntil(() -> tasks.getItems().size() == taskCount);
        BackgroundTaskDto terminalTask = tasks.getItems().stream()
                .filter(task -> task.status() == BackgroundTaskStatus.SUCCESS
                        || task.status() == BackgroundTaskStatus.FAILED)
                .findFirst()
                .orElseThrow();
        robot.interact(() -> tasks.getSelectionModel().select(terminalTask));
        waitUntil(() -> !button(robot, "#deleteButton").isDisabled());
        WaitForAsyncUtils.asyncFx(() -> button(robot, "#deleteButton").fire());

        DialogPane confirmation = waitForDialog(robot);
        Button ok = (Button) confirmation.lookupButton(ButtonType.OK);
        robot.interact(ok::fire);
        waitUntil(() -> backgroundTaskService.list(userId).size() == taskCount - 1);
        assertThat(tasks.getItems()).hasSize(taskCount - 1);
    }

    private static Path resumeFixture() {
        return fixture("/fixtures/full-stack-engineer-resume.md");
    }

    private static Path knowledgeFixture() {
        return fixture("/fixtures/" + KNOWLEDGE_FILE);
    }

    private static Path fixture(String name) {
        try {
            var resource = CompleteBusinessFlowE2ETest.class.getResource(name);
            if (resource == null) throw new IllegalStateException("Missing TestFX fixture: " + name);
            return Path.of(resource.toURI()).toAbsolutePath().normalize();
        } catch (URISyntaxException exception) {
            throw new IllegalStateException("Invalid TestFX fixture path: " + name, exception);
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
        FileDialogService configuredFileDialogService(
                @Value("${test.resume-path}") String resumePath,
                @Value("${test.knowledge-path}") String knowledgePath
        ) {
            Path resume = Path.of(resumePath).toAbsolutePath().normalize();
            Path knowledge = Path.of(knowledgePath).toAbsolutePath().normalize();
            if (!Files.isRegularFile(resume) || !Files.isReadable(resume)) {
                throw new IllegalStateException("Configured TestFX resume is not readable: " + resume);
            }
            if (!Files.isRegularFile(knowledge) || !Files.isReadable(knowledge)) {
                throw new IllegalStateException("Configured TestFX knowledge file is not readable: " + knowledge);
            }
            return new FileDialogService() {
                @Override public Optional<Path> chooseResume(javafx.stage.Window owner) {
                    return Optional.of(resume);
                }

                @Override public Optional<Path> chooseKnowledgeDocument(javafx.stage.Window owner) {
                    return Optional.of(knowledge);
                }
            };
        }

        @Bean
        @Primary
        ChatService deterministicWorkflowChatService() {
            AtomicInteger questionSequence = new AtomicInteger();
            return new ChatService() {
                @Override
                public String chat(String prompt) {
                    if (prompt.contains("候选人主张提取器")) {
                        return """
                                {"claims":[{"type":"DECISION","content":"使用 Outbox 保证事件最终一致性",
                                "importance":0.95,"credibility":0.8,"missingEvidence":["故障恢复数据"]}]}
                                """;
                    }
                    if (prompt.contains("逻辑链评估器")) {
                        return """
                                {"premises":["订单和事件需要最终一致"],"problemDiagnosis":"跨服务事务不可用",
                                "alternatives":["分布式事务","Outbox"],"decision":"使用 Outbox",
                                "reasoning":"业务提交与事件记录在同一事务","actions":["写入 Outbox","异步投递"],
                                "outcome":"事件可恢复投递","validation":"通过 requestId 对账","reflection":"",
                                "gaps":[{"type":"MISSING_FAILURE_HANDLING","description":"需要补充持续投递失败的处置",
                                "severity":0.72,"relatedClaimIds":[]}]}
                                """;
                    }
                    if (prompt.contains("逐轮面试证据收集器")) {
                        return """
                                {"evidence":[{"competencyCode":"SYSTEM_DESIGN","signal":"POSITIVE",
                                "strength":0.9,"confidence":0.84,"reason":"能够比较方案并说明 Outbox 的一致性路径",
                                "relatedClaimIds":[]}]}
                                """;
                    }
                    if (prompt.contains("跨轮面试一致性检查器")) {
                        return "{\"issues\":[],\"resolutions\":[]}";
                    }
                    if (prompt.contains("技术面试证据摘要助手")) {
                        return """
                                {"overallScore":88,"technicalScore":91,"problemSolvingScore":89,
                                "projectScore":90,"systemDesignScore":90,"communicationScore":86,
                                "comprehensiveScore":88,"summary":"%s"}
                                """.formatted(REPORT_SUMMARY);
                    }
                    if (prompt.contains("技术招聘分析助手")) {
                        return """
                                {"fullName":"林泽宇","targetRole":"高级全栈工程师",
                                "yearsExperience":"7 年","education":"软件工程本科",
                                "skills":["Java 21","Spring Boot","Vue 3","TypeScript","Redis","PostgreSQL"],
                                "projects":["高并发订单协同平台","实时运营分析控制台"],
                                "experience":["7 年企业级 Web 应用研发","带领 5 人小组交付核心业务"],
                                "strengths":["全栈交付","数据一致性","可观测性"],
                                "risks":["需要继续验证超大规模系统经验"],
                                "summary":"具备端到端交付能力的高级全栈工程师"}
                                """;
                    }
                    if (prompt.contains("技术面试回答分析器")) {
                        return """
                                {"correctness":90,"depth":88,"missingPoints":[],"feedback":"回答完整"}
                                """;
                    }
                    if (prompt.contains("流程决策器")) {
                        return prompt.contains("当前阶段：INTRODUCTION")
                                ? """
                                  {"action":"NEXT_STAGE","nextStage":"RESUME_REVIEW","reason":"进入简历深挖"}
                                  """
                                : """
                                  {"action":"FOLLOW_UP","nextStage":null,"reason":"继续追问缓存恢复"}
                                  """;
                    }
                    throw new IllegalArgumentException("Unexpected deterministic chat prompt: " + prompt);
                }

                @Override
                public Flux<String> stream(String prompt) {
                    int index = questionSequence.getAndIncrement();
                    if (index >= QUESTIONS.size()) {
                        return Flux.error(new IllegalStateException("Unexpected extra interview question"));
                    }
                    if (!prompt.contains("林泽宇") || !prompt.contains("高级全栈工程师")) {
                        return Flux.error(new IllegalStateException("Confirmed profile was not added to prompt"));
                    }
                    if (index > 0 && (!prompt.contains("Outbox") || !prompt.contains("requestId"))) {
                        return Flux.error(new IllegalStateException("RAG context was not added to follow-up prompt"));
                    }
                    String question = QUESTIONS.get(index);
                    int split = question.length() / 2;
                    return Flux.just(question.substring(0, split), question.substring(split));
                }
            };
        }

        @Bean
        @Primary
        EmbeddingService deterministicWorkflowEmbeddingService() {
            return text -> new float[]{1.0f, 0.5f, 0.25f, 0.125f, 0.0625f, 0.03125f, 0.015625f, 0.0078125f};
        }
    }
}
