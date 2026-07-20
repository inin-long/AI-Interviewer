package com.inin.aiinterviewer.ui;

import com.inin.aiinterviewer.application.dto.SaveInterviewPlanCommand;
import com.inin.aiinterviewer.application.dto.UserDto;
import com.inin.aiinterviewer.application.service.InterviewPlanAssetService;
import com.inin.aiinterviewer.application.service.InterviewPlanService;
import com.inin.aiinterviewer.application.service.InterviewSessionService;
import com.inin.aiinterviewer.application.service.KnowledgeDocumentService;
import com.inin.aiinterviewer.application.service.UserService;
import com.inin.aiinterviewer.domain.entity.ResumeEntity;
import com.inin.aiinterviewer.domain.enums.InterviewDifficulty;
import com.inin.aiinterviewer.domain.enums.ResumeStatus;
import com.inin.aiinterviewer.infrastructure.database.mapper.ResumeMapper;
import com.inin.aiinterviewer.ui.navigation.ContentNavigator;
import com.inin.aiinterviewer.ui.state.UserSessionState;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Region;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

@SpringBootTest
@EnabledOnOs(OS.WINDOWS)
class InterviewPlanSnapshotTest {

    @TempDir static Path applicationHome;

    @Autowired private ApplicationContext applicationContext;
    @Autowired private UserService userService;
    @Autowired private UserSessionState sessionState;
    @Autowired private ResumeMapper resumeMapper;
    @Autowired private KnowledgeDocumentService knowledgeService;
    @Autowired private InterviewPlanService planService;
    @Autowired private InterviewSessionService interviewSessionService;
    @Autowired private ContentNavigator contentNavigator;

    @DynamicPropertySource
    static void applicationProperties(DynamicPropertyRegistry registry) {
        registry.add("ai.interviewer.home", () -> applicationHome.toString());
    }

    @BeforeAll
    static void startJavaFx() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        try { Platform.startup(latch::countDown); } catch (IllegalStateException alreadyStarted) { latch.countDown(); }
        latch.await(10, TimeUnit.SECONDS);
    }

    @Test
    void capturePlanWorkspaceEditorAndDetail() throws Exception {
        String listOutput = System.getProperty("plan.snapshot.path");
        assumeTrue(listOutput != null && !listOutput.isBlank());
        String editorOutput = System.getProperty("plan.editor.snapshot.path");
        String detailOutput = System.getProperty("plan.detail.snapshot.path");

        UserDto user = userService.register("plan-snapshot", "Mahoo", "Snapshot123!");
        sessionState.logIn(user);
        long resumeId = seedResume(user.id());
        knowledgeService.createCategory(user.id(), "Java 后端工程知识库");
        knowledgeService.createCategory(user.id(), "Spring 框架知识库");
        knowledgeService.createCategory(user.id(), "分布式系统知识库");
        List<Long> ids = seedPlans(user.id(), resumeId);
        interviewSessionService.startOrResume(user.id(), ids.getFirst());

        FutureTask<Parent> setup = new FutureTask<>(() -> {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main-window.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();
            Scene scene = new Scene(root, 1672, 901);
            scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
            if (root instanceof Region region) region.resize(1672, 901);
            root.applyCss();
            root.layout();
            ((Button) root.lookup("#plansNavButton")).fire();
            root.applyCss();
            root.layout();
            writeSnapshot(root, Path.of(listOutput), 1672, 901);

            if (detailOutput != null && !detailOutput.isBlank()) {
                contentNavigator.showSubPage("/fxml/plan-detail-view.fxml", "面试方案详情", ids.getFirst());
                root.applyCss();
                root.layout();
                writeSnapshot(root, Path.of(detailOutput), 1672, 901);
            }
            if (editorOutput != null && !editorOutput.isBlank()) {
                contentNavigator.showSubPage("/fxml/plan-editor-view.fxml", "编辑面试方案", ids.getFirst());
                root.applyCss();
                root.layout();
                writeSnapshot(root, Path.of(editorOutput), 1672, 901);
            }
            return root;
        });
        Platform.runLater(setup);
        setup.get(45, TimeUnit.SECONDS);
    }

    private long seedResume(long userId) {
        ResumeEntity entity = new ResumeEntity();
        entity.setUserId(userId);
        entity.setOriginalName("张三_Java后端工程师.pdf");
        entity.setStorageName("snapshot-resume.pdf");
        entity.setStoragePath(applicationHome.resolve("snapshot-resume.pdf").toString());
        entity.setFileType("pdf");
        entity.setFileSize(1_280_000);
        entity.setStatus(ResumeStatus.COMPLETED);
        resumeMapper.insert(entity);
        return entity.getId();
    }

    private List<Long> seedPlans(long userId, long resumeId) throws Exception {
        String javaIcon = Path.of(getClass().getResource("/plan-icons/java-upload.png").toURI()).toString();
        String springIcon = Path.of(getClass().getResource("/plan-icons/spring-upload.png").toURI()).toString();
        long campus = create(userId, "校招通用面试", "后端开发工程师", InterviewDifficulty.JUNIOR, 30, 10,
                null, null, "计算机基础、数据结构、算法、项目基础", List.of());
        long redis = create(userId, "Redis 中间件强化", "Java 后端工程师", InterviewDifficulty.MEDIUM, 35, 10,
                null, null, "Redis 数据结构、缓存策略、持久化、集群", List.of("Java 后端工程知识库"));
        long system = create(userId, "系统设计专项面试", "后端开发工程师", InterviewDifficulty.SENIOR, 60, 16,
                null, null, "高并发、分布式、系统设计、可扩展性", List.of("分布式系统知识库"));
        long spring = create(userId, "Spring 项目深挖", "Java 后端工程师", InterviewDifficulty.MEDIUM, 40, 12,
                resumeId, springIcon, "Spring 原理、事务、AOP、项目实战", List.of("Spring 框架知识库"));
        long java = create(userId, "Java 后端高级面试", "Java 后端工程师", InterviewDifficulty.SENIOR, 45, 15,
                resumeId, javaIcon, "Spring、MySQL、Redis、项目深挖",
                List.of("Java 后端工程知识库", "Spring 框架知识库"));
        return List.of(java, spring, system, redis, campus);
    }

    private long create(long userId, String name, String job, InterviewDifficulty difficulty,
                        int duration, int questions, Long resumeId, String icon, String focus,
                        List<String> categories) {
        LinkedHashMap<String, Object> rules = new LinkedHashMap<>();
        rules.put("focus", focus);
        rules.put("adaptiveFollowup", true);
        rules.put("generateReport", true);
        rules.put("stageBlueprint", List.of(
                Map.of("stage", "INTRODUCTION", "enabled", true, "questions", 1, "minutes", 3, "weight", 5),
                Map.of("stage", "RESUME_REVIEW", "enabled", true, "questions", 2, "minutes", 5, "weight", 10),
                Map.of("stage", "PROJECT_EXPERIENCE", "enabled", true, "questions", 4, "minutes", 13, "weight", 25),
                Map.of("stage", "TECHNICAL_DEEP_DIVE", "enabled", true, "questions", 4, "minutes", 12, "weight", 25),
                Map.of("stage", "SYSTEM_DESIGN", "enabled", true, "questions", 2, "minutes", 8, "weight", 20),
                Map.of("stage", "SUMMARY", "enabled", true, "questions", 2, "minutes", 4, "weight", 15)));
        if (icon != null) rules.put(InterviewPlanAssetService.ICON_PATH_RULE, icon);
        SaveInterviewPlanCommand command = new SaveInterviewPlanCommand(
                name, job, "负责核心业务系统设计与开发，关注工程质量、性能和团队协作。",
                difficulty, duration, questions, resumeId, null, List.of(), rules,
                List.of("INTRODUCTION", "RESUME_REVIEW", "PROJECT_EXPERIENCE",
                        "TECHNICAL_DEEP_DIVE", "SYSTEM_DESIGN", "SUMMARY"), null, categories);
        return planService.create(userId, command).id();
    }

    private void writeSnapshot(Parent root, Path output, int width, int height) throws Exception {
        WritableImage snapshot = new WritableImage(width, height);
        root.snapshot(null, snapshot);
        int[] pixels = new int[width * height];
        snapshot.getPixelReader().getPixels(0, 0, width, height,
                PixelFormat.getIntArgbPreInstance(), pixels, 0, width);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB_PRE);
        image.setRGB(0, 0, width, height, pixels, 0, width);
        Files.createDirectories(output.getParent());
        ImageIO.write(image, "png", output.toFile());
    }
}
