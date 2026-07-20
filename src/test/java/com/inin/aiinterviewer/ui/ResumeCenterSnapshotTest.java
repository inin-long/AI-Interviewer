package com.inin.aiinterviewer.ui;

import com.inin.aiinterviewer.application.dto.UserDto;
import com.inin.aiinterviewer.application.service.CandidateProfileService;
import com.inin.aiinterviewer.application.service.UserService;
import com.inin.aiinterviewer.domain.entity.ResumeEntity;
import com.inin.aiinterviewer.domain.enums.ResumeStatus;
import com.inin.aiinterviewer.domain.model.CandidateProfileContent;
import com.inin.aiinterviewer.infrastructure.database.mapper.ResumeMapper;
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
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

@SpringBootTest
@EnabledOnOs(OS.WINDOWS)
class ResumeCenterSnapshotTest {

    @TempDir
    static Path applicationHome;

    @Autowired private ApplicationContext applicationContext;
    @Autowired private UserService userService;
    @Autowired private UserSessionState sessionState;
    @Autowired private ResumeMapper resumeMapper;
    @Autowired private CandidateProfileService profileService;

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
    void captureResumeCenterAndDrawer() throws Exception {
        String mainOutput = System.getProperty("resume.snapshot.path");
        assumeTrue(mainOutput != null && !mainOutput.isBlank());
        String drawerOutput = System.getProperty("resume.drawer.snapshot.path");

        UserDto user = userService.register("resume-snapshot", "Mahoo", "Snapshot123!");
        sessionState.logIn(user);
        seedResumes(user.id());

        FutureTask<Parent> setup = new FutureTask<>(() -> {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main-window.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();
            Scene scene = new Scene(root, 1672, 901);
            scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
            if (root instanceof Region region) region.resize(1672, 901);
            root.applyCss();
            root.layout();
            ((Button) root.lookup("#resumesNavButton")).fire();
            root.applyCss();
            root.layout();
            writeSnapshot(root, Path.of(mainOutput), 1672, 901);
            Button portrait = (Button) root.lookup(".resume-profile-action");
            if (portrait != null && drawerOutput != null && !drawerOutput.isBlank()) portrait.fire();
            return root;
        });
        Platform.runLater(setup);
        Parent root = setup.get(30, TimeUnit.SECONDS);

        if (drawerOutput != null && !drawerOutput.isBlank()) {
            Thread.sleep(320);
            FutureTask<Void> drawerCapture = new FutureTask<>(() -> {
                root.applyCss();
                root.layout();
                writeSnapshot(root, Path.of(drawerOutput), 1672, 901);
                return null;
            });
            Platform.runLater(drawerCapture);
            drawerCapture.get(30, TimeUnit.SECONDS);
        }
    }

    private void seedResumes(long userId) {
        long product = insert(userId, "赵敏_产品经理.pdf", "pdf", 1_890_000);
        resumeMapper.markCompleted(product, userId, "赵敏，6年产品经验，负责数据平台和增长产品。");
        saveProfile(userId, product, "赵敏", "产品经理", "6 年", "本科",
                List.of("产品设计", "需求分析", "PRD", "Axure"), true);

        long ops = insert(userId, "刘洋_运维工程师.docx", "docx", 930_000);
        resumeMapper.markParsing(ops, userId);

        long test = insert(userId, "陈晨_测试开发工程师.pdf", "pdf", 1_280_000);
        resumeMapper.markFailed(test, userId, "文档内容损坏");

        long fullStack = insert(userId, "王强_全栈工程师.docx", "docx", 760_000);
        resumeMapper.markCompleted(fullStack, userId, "王强，全栈工程师，熟悉 JavaScript、Vue.js、Node.js、MySQL。");

        long java = insert(userId, "李娜_Java开发工程师.pdf", "pdf", 1_460_000);
        resumeMapper.markCompleted(java, userId, "李娜，Java 开发工程师，熟悉 Java、MySQL、Spring、Linux。");
        saveProfile(userId, java, "李娜", "Java 开发工程师", "4 年", "本科",
                List.of("Java", "MySQL", "Spring", "Linux"), false);

        long backend = insert(userId, "张伟_高级后端工程师.docx", "docx", 860_000);
        resumeMapper.markCompleted(backend, userId,
                "张伟，高级后端工程师，6年经验。负责电商交易平台和分布式订单系统。"
                        + "熟悉 Java、Spring Boot、Redis、MySQL、微服务、RabbitMQ、Docker。 ");
        saveProfile(userId, backend, "张伟", "高级后端工程师", "6 年", "计算机科学与技术 · 本科",
                List.of("Java", "Spring Boot", "Redis", "MySQL", "微服务", "RabbitMQ", "Docker", "高并发"), true);
    }

    private long insert(long userId, String name, String type, long size) {
        ResumeEntity entity = new ResumeEntity();
        entity.setUserId(userId);
        entity.setOriginalName(name);
        entity.setStorageName(name + ".snapshot");
        entity.setStoragePath(applicationHome.resolve(name).toString());
        entity.setFileType(type);
        entity.setFileSize(size);
        entity.setStatus(ResumeStatus.UPLOADED);
        resumeMapper.insert(entity);
        return entity.getId();
    }

    private void saveProfile(long userId, long resumeId, String name, String role,
                             String years, String education, List<String> skills, boolean confirmed) {
        CandidateProfileContent content = new CandidateProfileContent(
                name, role, years, education, skills,
                List.of("电商交易平台（核心开发）", "分布式订单系统（架构设计与开发）"),
                List.of("2019.06 至今 · 高级后端工程师"),
                List.of("系统设计能力", "高并发实战"), List.of(),
                "候选人具备扎实的 Java 技术栈基础，熟悉微服务与分布式架构，"
                        + "在高并发系统设计与性能优化方面经验丰富，具备良好的工程化能力与团队协作能力。");
        profileService.saveManual(userId, resumeId, content);
        if (confirmed) profileService.confirm(userId, resumeId);
    }

    private void writeSnapshot(Parent root, Path output, int width, int height) throws Exception {
        WritableImage snapshot = new WritableImage(width, height);
        root.snapshot(null, snapshot);
        int[] pixels = new int[width * height];
        snapshot.getPixelReader().getPixels(
                0, 0, width, height,
                PixelFormat.getIntArgbPreInstance(), pixels, 0, width);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB_PRE);
        image.setRGB(0, 0, width, height, pixels, 0, width);
        Files.createDirectories(output.getParent());
        ImageIO.write(image, "png", output.toFile());
    }
}
