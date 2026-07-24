package com.inin.aiinterviewer.ui;

import com.inin.aiinterviewer.application.dto.InterviewMessageDto;
import com.inin.aiinterviewer.application.dto.KnowledgeCitationDto;
import com.inin.aiinterviewer.application.dto.UserDto;
import com.inin.aiinterviewer.domain.model.Message;
import com.inin.aiinterviewer.ui.component.InterviewTranscriptView;
import com.inin.aiinterviewer.ui.state.UserSessionState;
import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

@SpringBootTest
@EnabledOnOs(OS.WINDOWS)
class InterviewWorkspaceSnapshotTest {

    @TempDir
    static Path applicationHome;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private UserSessionState sessionState;

    @DynamicPropertySource
    static void applicationProperties(DynamicPropertyRegistry registry) {
        registry.add("ai.interviewer.home", () -> applicationHome.toString());
    }

    @BeforeAll
    static void startJavaFx() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException alreadyStarted) {
            latch.countDown();
        }
        latch.await(10, TimeUnit.SECONDS);
    }

    @Test
    void captureInterviewWorkspace() throws Exception {
        String outputPath = System.getProperty("interview.snapshot.path");
        assumeTrue(outputPath != null && !outputPath.isBlank());
        sessionState.logIn(new UserDto(1L, "snapshot-user", "张三", LocalDateTime.now()));
        FutureTask<Void> task = new FutureTask<>(() -> {
            BorderPane shell = (BorderPane) load("/fxml/main-window.fxml");
            Parent workspace = load("/fxml/interview-workspace-view.fxml");
            Scene scene = new Scene(shell, 1672, 901);
            scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
            shell.applyCss();
            shell.layout();
            shell.pseudoClassStateChanged(PseudoClass.getPseudoClass("interview-mode"), true);
            prepareShell(shell);
            ((StackPane) shell.lookup("#contentHost")).getChildren().setAll(workspace);
            shell.resize(1672, 901);
            shell.applyCss();
            shell.layout();
            prepareWorkspace(workspace);
            shell.applyCss();
            shell.layout();

            WritableImage snapshot = new WritableImage(1672, 901);
            shell.snapshot(null, snapshot);
            int[] pixels = new int[1672 * 901];
            snapshot.getPixelReader().getPixels(
                    0, 0, 1672, 901,
                    PixelFormat.getIntArgbPreInstance(), pixels, 0, 1672);
            BufferedImage image = new BufferedImage(1672, 901, BufferedImage.TYPE_INT_ARGB_PRE);
            image.setRGB(0, 0, 1672, 901, pixels, 0, 1672);
            Path output = Path.of(outputPath);
            Files.createDirectories(output.getParent());
            ImageIO.write(image, "png", output.toFile());
            return null;
        });
        Platform.runLater(task);
        task.get(30, TimeUnit.SECONDS);
    }

    private void prepareShell(BorderPane shell) {
        text(shell, "#contentTitleLabel", "Java 后端高级面试");
        text(shell, "#contentSubtitleLabel", "方案：Java 后端高级工程师 · 正式模拟  |  岗位：高级 Java 后端工程师");
        text(shell, "#sessionClockLabel", "00:16:28");
        text(shell, "#usernameLabel", "张三");
        text(shell, "#aiStatusLabel", "本地数据已同步");
        show(shell, "#contentSubtitleLabel", true);
        show(shell, "#sessionClockLabel", true);
        show(shell, "#interviewStorageNote", true);
        show(shell, "#interviewBrandImage", true);
        show(shell, "#defaultBrandImage", false);
        show(shell, "#interviewUserAvatar", true);
        show(shell, "#avatarLabel", false);
        show(shell, "#topbarMenuIcon", false);
        show(shell, "#interviewNavButton", true);
        show(shell, "#profilesNavButton", false);
        show(shell, "#questionBankNavButton", false);
        show(shell, "#careerAssessmentNavButton", false);
        show(shell, "#skillsLibraryNavButton", false);
        show(shell, "#careerPlanningNavButton", false);
        show(shell, "#tasksNavButton", false);
        Node interviewNav = shell.lookup("#interviewNavButton");
        interviewNav.pseudoClassStateChanged(PseudoClass.getPseudoClass("selected"), true);
    }

    private void prepareWorkspace(Parent workspace) {
        text(workspace, "#stageLabel", "技术深挖");
        text(workspace, "#stageRailLabel", "技术深挖");
        text(workspace, "#progressLabel", "第 7 / 15 题");
        text(workspace, "#remainingTimeLabel", "23:18");
        text(workspace, "#overallProgressLabel", "46%");
        ((ProgressBar) workspace.lookup("#overallProgressBar")).setProgress(0.46);
        text(workspace, "#infoJobLabel", "Java 后端高级工程师");
        text(workspace, "#planNameLabel", "Java 后端高级工程师 · v2.1");
        text(workspace, "#resumeNameLabel", "张三_Java高级开发工程师.pdf");
        text(workspace, "#durationLabel", "60 分钟");
        text(workspace, "#scoreStateLabel", "总分 100");
        List<Double> scores = List.of(0.68, 0.60, 0.70, 0.55, 0.0);
        int index = 0;
        for (Node node : workspace.lookupAll(".rail-score-progress")) {
            ((ProgressBar) node).setProgress(scores.get(index++));
        }
        List<String> values = List.of("68/100", "60/100", "70/100", "55/100", "--/100");
        index = 0;
        for (Node node : workspace.lookupAll(".rail-score-value")) {
            ((Label) node).setText(values.get(index++));
        }

        InterviewTranscriptView transcript = (InterviewTranscriptView) workspace.lookup("#transcriptView");
        transcript.setMessages(List.of(
                new InterviewMessageDto(1, Message.Role.ASSISTANT,
                        "请结合你在项目中的实际经验，说明 Spring 事务的传播行为（如 REQUIRED、REQUIRES_NEW 等）在什么场景下使用比较合适？\n如果在一个复杂业务中，涉及多服务调用和数据库操作，如何保证数据的一致性？请举例说明。",
                        utcTime(2, 12, 45), false, List.of()),
                new InterviewMessageDto(2, Message.Role.USER,
                        "在我们电商系统的订单服务中，使用了 Spring 事务来保证订单创建的原子性。\n- 对于 REQUIRED：在同一事务中共享连接，适用于大部分默认场景。\n- 对于 REQUIRES_NEW：常用于日志记录、发送消息等独立事务的场景，避免主事务回滚影响日志落库。",
                        utcTime(2, 15, 32), false, List.of()),
                new InterviewMessageDto(3, Message.Role.ASSISTANT,
                        "很好。那在你刚才提到的订单创建流程中，引入 Redis 缓存来提升性能的话，你会如何设计缓存策略？\n如何保证缓存和数据库的一致性？如果遇到缓存穿透、击穿、雪崩问题，你是如何应对的？",
                        utcTime(2, 17, 1), false,
                        List.of(new KnowledgeCitationDto(1L, "Spring 事务与源码解析.md", 0, "事务传播行为", 0.92))),
                new InterviewMessageDto(4, Message.Role.USER,
                        "// 缓存策略：读多写少，使用 Cache-Aside 模式\n// 1. 先读缓存，未命中再查数据库\n// 2. 更新数据库后，删除缓存（延迟双删）\n// 3. 使用合理的 TTL 和随机过期时间，防止雪崩\n// 4. 布隆过滤器 + 空值缓存，防止缓存穿透",
                        utcTime(2, 19, 48), false, List.of())));
        transcript.beginAssistantStream(3);

        VBox documents = (VBox) workspace.lookup("#citationContainer");
        documents.getChildren().setAll(
                documentRow("▣", "Spring 事务与源码解析.md", "核心"),
                documentRow("▣", "Redis 缓存设计与最佳实践.md", "核心"),
                documentRow("▣", "分布式系统一致性方案汇总.md", "推荐"));
        text(workspace, "#citationCountLabel", "查看全部（5）");
        text(workspace, "#currentCitationLabel", "Spring 事务与 Redis 缓存设计");
    }

    private HBox documentRow(String icon, String title, String tag) {
        Label iconLabel = new Label(icon);
        iconLabel.getStyleClass().add("rail-document-icon");
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("rail-document-link");
        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        Label tagLabel = new Label(tag);
        tagLabel.getStyleClass().add("推荐".equals(tag)
                ? "rail-document-tag-recommended" : "rail-document-tag");
        HBox row = new HBox(7, iconLabel, titleLabel, spacer, tagLabel);
        row.getStyleClass().add("rail-document-row");
        return row;
    }

    private LocalDateTime utcTime(int hour, int minute, int second) {
        return LocalDateTime.of(2026, 7, 23, hour, minute, second);
    }

    private Parent load(String path) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
        loader.setControllerFactory(applicationContext::getBean);
        return loader.load();
    }

    private void text(Parent root, String selector, String value) {
        ((Label) root.lookup(selector)).setText(value);
    }

    private void show(Parent root, String selector, boolean visible) {
        Node node = root.lookup(selector);
        node.setVisible(visible);
        node.setManaged(visible);
    }
}
