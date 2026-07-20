package com.inin.aiinterviewer.ui;

import com.inin.aiinterviewer.application.dto.InterviewMessageDto;
import com.inin.aiinterviewer.application.dto.KnowledgeCitationDto;
import com.inin.aiinterviewer.application.dto.UserDto;
import com.inin.aiinterviewer.application.event.BackgroundTaskCompletedEvent;
import com.inin.aiinterviewer.application.exception.GlobalExceptionHandler;
import com.inin.aiinterviewer.domain.enums.BackgroundTaskType;
import com.inin.aiinterviewer.domain.model.Message;
import com.inin.aiinterviewer.ui.animation.AuthIllustrationMotion;
import com.inin.aiinterviewer.ui.component.AppDialog;
import com.inin.aiinterviewer.ui.component.AppSelect;
import com.inin.aiinterviewer.ui.component.InterviewTranscriptView;
import com.inin.aiinterviewer.ui.component.DrawerPane;
import com.inin.aiinterviewer.ui.component.ResumeProfileDrawerView;
import com.inin.aiinterviewer.ui.navigation.InterviewTranscriptContext;
import com.inin.aiinterviewer.ui.navigation.JavaFxViewManager;
import com.inin.aiinterviewer.ui.navigation.Route;
import com.inin.aiinterviewer.ui.state.UserSessionState;
import com.inin.aiinterviewer.ui.state.TaskNotificationCenter;
import javafx.application.Platform;
import javafx.animation.Animation;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ListView;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.css.PseudoClass;
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

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@EnabledOnOs(OS.WINDOWS)
class JavaFxFxmlLoadTest {

    @TempDir
    static Path applicationHome;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private UserSessionState sessionState;

    @Autowired
    private TaskNotificationCenter taskNotificationCenter;

    @Autowired
    private GlobalExceptionHandler exceptionHandler;

    @DynamicPropertySource
    static void applicationProperties(DynamicPropertyRegistry registry) {
        registry.add("ai.interviewer.home", () -> applicationHome.toString());
    }

    @BeforeAll
    static void startJavaFxToolkit() throws InterruptedException {
        CountDownLatch started = new CountDownLatch(1);
        try {
            Platform.startup(started::countDown);
        } catch (IllegalStateException alreadyStarted) {
            started.countDown();
        }
        assertThat(started.await(10, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void loadsPublicAuthenticationViewsWithSpringControllers() throws Exception {
        assertThat(loadOnFxThread("/fxml/login.fxml")).isNotNull();
        assertThat(loadOnFxThread("/fxml/register.fxml")).isNotNull();
    }

    @Test
    void authenticationScreensUseReferenceLayoutAndInteractivePasswordControls() throws Exception {
        FutureTask<boolean[]> task = new FutureTask<>(() -> {
            Parent login = load("/fxml/login.fxml");
            Scene loginScene = new Scene(login, 1440, 820);
            loginScene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
            login.applyCss();
            login.layout();

            VBox loginCard = (VBox) login.lookup(".login-form-card");
            ImageView illustration = (ImageView) login.lookup("#authIllustration");
            PasswordField password = (PasswordField) login.lookup("#passwordField");
            TextField visiblePassword = (TextField) login.lookup("#visiblePasswordField");
            Button visibilityButton = (Button) login.lookup("#passwordVisibilityButton");
            password.setText("local-secret");
            visibilityButton.fire();

            Parent register = load("/fxml/register.fxml");
            Scene registerScene = new Scene(register, 1440, 820);
            registerScene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
            register.applyCss();
            register.layout();
            VBox registerCard = (VBox) register.lookup(".register-form-card");
            CheckBox agreement = (CheckBox) register.lookup("#agreementCheckBox");
            HBox registerMain = (HBox) register.lookup(".auth-main");
            AnchorPane registerShowcase = (AnchorPane) register.lookup(".auth-showcase");
            VBox featureList = (VBox) register.lookup("#authFeatureList");
            HBox securityNote = (HBox) register.lookup("#authSecurityNote");
            double securityGap = securityNote.getLayoutY()
                    - featureList.getLayoutY() - featureList.getHeight();

            return new boolean[]{
                    Math.abs(loginCard.getWidth() - 480) < 0.5,
                    Math.abs(loginCard.getHeight() - 590) < 0.5,
                    Math.abs(registerCard.getWidth() - 480) < 0.5,
                    Math.abs(registerCard.getHeight() - 620) < 0.5,
                    registerShowcase.getLayoutX() >= 71.5,
                    registerMain.getWidth() - registerCard.getLayoutX() - registerCard.getWidth() >= 71.5,
                    registerCard.getLayoutX() - registerShowcase.getLayoutX()
                            - registerShowcase.getWidth() >= 35.5,
                    securityGap >= 13.5 && securityGap <= 14.5,
                    illustration != null && illustration.getImage() != null,
                    !password.isVisible() && visiblePassword.isVisible(),
                    "local-secret".equals(visiblePassword.getText()),
                    agreement.isSelected(),
                    login.lookup(".window-header") == null,
                    register.lookup(".window-header") == null
            };
        });
        Platform.runLater(task);
        assertThat(task.get(15, TimeUnit.SECONDS)).containsOnly(true);
    }

    @Test
    void applicationStartsWithCompactDesktopWindowDimensions() throws Exception {
        FutureTask<double[]> task = new FutureTask<>(() -> {
            JavaFxViewManager compactViewManager = new JavaFxViewManager(
                    applicationContext, sessionState, exceptionHandler);
            Stage stage = new Stage();
            compactViewManager.attachStage(stage);
            compactViewManager.switchView(Route.LOGIN);
            double[] dimensions = {stage.getScene().getWidth(), stage.getScene().getHeight()};
            stage.close();
            return dimensions;
        });
        Platform.runLater(task);

        assertThat(task.get(15, TimeUnit.SECONDS)).containsExactly(1440.0, 820.0);
    }

    @Test
    void authenticationIllustrationStartsItsNativeMotionLoop() throws Exception {
        FutureTask<Boolean> task = new FutureTask<>(() -> {
            ImageView illustration = new ImageView();
            illustration.setId("authIllustration");
            AnchorPane showcase = new AnchorPane(illustration);
            Animation motion = AuthIllustrationMotion.start(showcase);
            boolean running = motion.getStatus() == Animation.Status.RUNNING
                    && motion.getCycleCount() == 1;
            motion.stop();
            return running;
        });
        Platform.runLater(task);
        assertThat(task.get(15, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void loadsMainWindowForAuthenticatedUser() throws Exception {
        sessionState.logIn(new UserDto(1L, "test-user", "测试用户", LocalDateTime.now()));
        assertThat(loadOnFxThread("/fxml/main-window.fxml")).isNotNull();
        assertThat(loadOnFxThread("/fxml/resume-view.fxml")).isNotNull();
        assertThat(loadOnFxThread("/fxml/resume-detail-view.fxml")).isNotNull();
        assertThat(loadOnFxThread("/fxml/profile-view.fxml")).isNotNull();
        assertThat(loadOnFxThread("/fxml/task-view.fxml")).isNotNull();
        assertThat(loadOnFxThread("/fxml/task-detail-view.fxml")).isNotNull();
        assertThat(loadOnFxThread("/fxml/plan-view.fxml")).isNotNull();
        assertThat(loadOnFxThread("/fxml/plan-editor-view.fxml")).isNotNull();
        assertThat(loadOnFxThread("/fxml/interview-workspace-view.fxml")).isNotNull();
        assertThat(loadOnFxThread("/fxml/report-detail-view.fxml")).isNotNull();
        assertThat(loadOnFxThread("/fxml/session-branch-view.fxml")).isNotNull();
        assertThat(loadOnFxThread("/fxml/knowledge-view.fxml")).isNotNull();
        assertThat(loadOnFxThread("/fxml/knowledge-detail-view.fxml")).isNotNull();
        assertThat(loadOnFxThread("/fxml/history-view.fxml")).isNotNull();
        assertThat(loadOnFxThread("/fxml/interview-history-detail-view.fxml")).isNotNull();
        assertThat(loadOnFxThread("/fxml/settings-view.fxml")).isNotNull();
    }

    @Test
    void dashboardStartActionContinuesIntoPlanCreationWhenNoPlanExists() throws Exception {
        sessionState.logIn(new UserDto(1L, "dashboard-flow-user", "Mahoo", LocalDateTime.now()));
        FutureTask<Boolean> task = new FutureTask<>(() -> {
            Parent root = load("/fxml/main-window.fxml");
            Scene scene = new Scene(root, 1672, 901);
            scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
            root.applyCss();
            root.layout();
            Button start = (Button) root.lookup("#dashboardStartButton");
            start.fire();
            root.applyCss();
            root.layout();
            return root.lookup("#nameField") != null
                    && root.lookup("#savePlanButton") != null;
        });
        Platform.runLater(task);
        assertThat(task.get(15, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void dashboardUsesTheSimplifiedTopbarAndFullHeightRecentPlanPanel() throws Exception {
        sessionState.logIn(new UserDto(1L, "dashboard-layout-user", "inin", LocalDateTime.now()));
        FutureTask<boolean[]> task = new FutureTask<>(() -> {
            Parent root = load("/fxml/main-window.fxml");
            Scene scene = new Scene(root, 1672, 901);
            scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
            root.applyCss();
            root.layout();

            HBox topbar = (HBox) root.lookup(".topbar");
            VBox recentInterviewPanel = (VBox) root.lookup("#recentInterviewList").getParent();
            VBox recentPlanPanel = (VBox) root.lookup("#recentPlanList").getParent();
            long topbarButtonCount = topbar.getChildren().stream()
                    .filter(Button.class::isInstance)
                    .count();

            return new boolean[]{
                    root.lookup("#taskStatusButton") == null,
                    topbarButtonCount == 1 && root.lookup("#userMenuButton") != null,
                    root.lookup("#uploadResumeButton") == null,
                    root.lookup("#aiSettingsButton") == null,
                    Math.abs(recentInterviewPanel.getBoundsInParent().getHeight()
                            - recentPlanPanel.getBoundsInParent().getHeight()) < 0.5
            };
        });
        Platform.runLater(task);
        assertThat(task.get(15, TimeUnit.SECONDS)).containsOnly(true);
    }

    @Test
    void selectAndTextInputHaveMatchingRenderedHeight() throws Exception {
        if (sessionState.currentUser().isEmpty()) {
            sessionState.logIn(new UserDto(1L, "style-test-user", "样式测试用户", LocalDateTime.now()));
        }
        FutureTask<double[]> task = new FutureTask<>(() -> {
            Parent root = load("/fxml/plan-editor-view.fxml");
            Scene scene = new Scene(root, 1200, 800);
            scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
            root.applyCss();
            root.layout();
            ComboBox<?> difficulty = (ComboBox<?>) root.lookup("#difficultyBox");
            TextField name = (TextField) root.lookup("#nameField");
            return new double[]{difficulty.getHeight(), name.getHeight()};
        });
        Platform.runLater(task);
        double[] heights = task.get(15, TimeUnit.SECONDS);
        assertThat(heights[0]).isEqualTo(52.0);
        assertThat(heights[0]).isEqualTo(heights[1]);
    }

    @Test
    void planEditorUsesAlignedSelectorsAndMultiSelectKnowledgeList() throws Exception {
        if (sessionState.currentUser().isEmpty()) {
            sessionState.logIn(new UserDto(1L, "plan-form-user", "方案表单用户", LocalDateTime.now()));
        }
        FutureTask<boolean[]> task = new FutureTask<>(() -> {
            Parent root = load("/fxml/plan-editor-view.fxml");
            Scene scene = new Scene(root, 1200, 900);
            scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
            root.applyCss();
            root.layout();
            TextField name = (TextField) root.lookup("#nameField");
            ComboBox<?> resume = (ComboBox<?>) root.lookup("#resumeBox");
            ComboBox<?> profile = (ComboBox<?>) root.lookup("#profileBox");
            ListView<?> knowledge = (ListView<?>) root.lookup("#knowledgeList");
            return new boolean[]{
                    name.getHeight() == resume.getHeight(),
                    name.getHeight() == profile.getHeight(),
                    knowledge.getSelectionModel().getSelectionMode() == SelectionMode.MULTIPLE
            };
        });
        Platform.runLater(task);
        assertThat(task.get(15, TimeUnit.SECONDS)).containsOnly(true);
    }

    @Test
    void knowledgeLibraryUsesCategoryNavigationDocumentRowsAndReusableDrawer() throws Exception {
        if (sessionState.currentUser().isEmpty()) {
            sessionState.logIn(new UserDto(1L, "knowledge-layout-user", "知识库用户", LocalDateTime.now()));
        }
        FutureTask<boolean[]> task = new FutureTask<>(() -> {
            Parent root = load("/fxml/knowledge-view.fxml");
            Scene scene = new Scene(root, 1200, 760);
            scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
            root.applyCss();
            root.layout();
            DrawerPane drawer = (DrawerPane) root.lookup("#documentDrawer");
            drawer.open("通用抽屉", new Label("可复用内容"));
            return new boolean[]{
                    root.lookup("#categoryList") instanceof ListView,
                    root.lookup("#documentList") instanceof ListView,
                    root.lookup("#sortBox") instanceof AppSelect,
                    root.lookup("#documentTable") == null,
                    root.lookup("#searchResultArea") == null,
                    drawer.isOpen(),
                    Math.abs(drawer.getDrawerWidth() - 442) < 0.5,
                    root.lookupAll(".knowledge-stat-card").size() == 1,
                    root.lookupAll(".knowledge-stat-segment").size() == 4
            };
        });
        Platform.runLater(task);
        assertThat(task.get(15, TimeUnit.SECONDS)).containsOnly(true);
    }

    @Test
    void resumeCenterUsesCardListProcessRailGeneratedAssetsAndPortraitDrawer() throws Exception {
        if (sessionState.currentUser().isEmpty()) {
            sessionState.logIn(new UserDto(1L, "resume-layout-user", "简历中心用户", LocalDateTime.now()));
        }
        FutureTask<boolean[]> task = new FutureTask<>(() -> {
            Parent root = load("/fxml/resume-view.fxml");
            Scene scene = new Scene(root, 1440, 839);
            scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
            root.applyCss();
            root.layout();
            DrawerPane drawer = (DrawerPane) root.lookup("#profileDrawer");
            ResumeProfileDrawerView portrait = new ResumeProfileDrawerView();
            drawer.open("候选人画像预览工作区", portrait);

            Parent shell = load("/fxml/main-window.fxml");
            Scene shellScene = new Scene(shell, 1440, 820);
            shellScene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
            shell.applyCss();
            shell.layout();
            ((Button) shell.lookup("#resumesNavButton")).fire();
            shell.applyCss();
            shell.layout();
            Region artwork = (Region) shell.lookup("#resumeSidebarArtwork");

            return new boolean[]{
                    root.lookup("#resumeList") instanceof ListView,
                    root.lookup("#sortSelect") instanceof AppSelect,
                    root.lookupAll(".resume-stat-card").size() == 4,
                    root.lookup(".resume-process-card") != null,
                    drawer.isOpen(),
                    Math.abs(drawer.getDrawerWidth() - 470) < 0.5,
                    getClass().getResource("/images/resume/candidate-avatar.png") != null,
                    getClass().getResource("/images/resume/resume-sidebar-illustration.png") != null,
                    artwork.isVisible() && artwork.isManaged()
            };
        });
        Platform.runLater(task);
        assertThat(task.get(15, TimeUnit.SECONDS)).containsOnly(true);
    }

    @Test
    void sharedDialogAndSelectOwnProductChromeAndInteractionStates() throws Exception {
        FutureTask<boolean[]> task = new FutureTask<>(() -> {
            AppSelect<String> select = new AppSelect<>();
            select.getItems().setAll("按更新时间", "按名称", "按大小");
            select.setValue("按更新时间");

            AppDialog<String> dialog = new AppDialog<>(
                    null,
                    "组件验证",
                    "通用弹窗",
                    "用于验证品牌化外壳和操作区。",
                    AppDialog.Tone.INFORMATION);
            dialog.setBody(new Label("弹窗内容"));
            Button cancel = dialog.addCancelAction("取消");
            Button confirm = dialog.addAction("确定", () -> "ok", AppDialog.ActionStyle.PRIMARY);

            StackPane root = new StackPane(select, dialog.getDialogPane());
            Scene scene = new Scene(root, 720, 480);
            scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
            root.applyCss();
            root.layout();

            AppDialog<Boolean> cancelDialog = new AppDialog<>(null, "关闭验证", "取消操作");
            Button productCancel = cancelDialog.addCancelAction("取消");
            cancelDialog.show();
            boolean cancelDialogOpened = cancelDialog.isShowing();
            productCancel.fire();
            boolean cancelActionClosed = !cancelDialog.isShowing();

            AppDialog<Boolean> chromeDialog = new AppDialog<>(null, "关闭验证", "标题栏关闭");
            chromeDialog.addCancelAction("取消");
            chromeDialog.show();
            Button chromeClose = (Button) chromeDialog.getDialogPane().lookup(".app-dialog-close-button");
            boolean chromeDialogOpened = chromeDialog.isShowing() && chromeClose != null;
            chromeClose.fire();
            boolean chromeActionClosed = !chromeDialog.isShowing();

            AppDialog<Boolean> escapeDialog = new AppDialog<>(null, "关闭验证", "Esc 关闭");
            escapeDialog.addCancelAction("取消");
            escapeDialog.show();
            boolean escapeDialogOpened = escapeDialog.isShowing();
            escapeDialog.getDialogPane().fireEvent(new KeyEvent(
                    KeyEvent.KEY_PRESSED, "", "", KeyCode.ESCAPE,
                    false, false, false, false));
            boolean escapeActionClosed = !escapeDialog.isShowing();

            return new boolean[]{
                    select.getStyleClass().contains("app-select"),
                    select.getCellFactory() != null,
                    select.getButtonCell().getStyleClass().contains("app-select-button-cell"),
                    dialog.getDialogPane().lookup(".app-dialog-shell") != null,
                    dialog.getDialogPane().lookup(".app-dialog-brand") != null,
                    dialog.getDialogPane().lookup(".app-dialog-actions") != null,
                    cancel.isCancelButton(),
                    confirm.isDefaultButton(),
                    confirm.getStyleClass().contains("app-dialog-primary-button"),
                    cancelDialogOpened,
                    cancelActionClosed,
                    chromeDialogOpened,
                    chromeActionClosed,
                    escapeDialogOpened,
                    escapeActionClosed
            };
        });
        Platform.runLater(task);
        assertThat(task.get(15, TimeUnit.SECONDS)).containsOnly(true);
    }

    @Test
    void globalScrollbarsUseSlimProductSkinWithoutArrowButtons() throws Exception {
        FutureTask<boolean[]> task = new FutureTask<>(() -> {
            ListView<String> list = new ListView<>();
            for (int index = 1; index <= 40; index++) {
                list.getItems().add("第 " + index + " 条较长的滚动内容，用于验证横向与纵向滚动轴");
            }
            list.setPrefSize(250, 150);

            TextArea textArea = new TextArea("滚动内容\n".repeat(30)
                    + "这是一行用于触发横向滚动轴的较长文本内容".repeat(8));
            textArea.setWrapText(false);
            textArea.setPrefSize(250, 150);

            ScrollPane scrollPane = new ScrollPane(new VBox(12,
                    new Label("滚动区域"), new VBox(320), new Label("底部内容")));
            scrollPane.setPrefSize(210, 150);
            HBox root = new HBox(18, list, textArea, scrollPane);
            Scene scene = new Scene(root, 760, 190);
            scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
            root.applyCss();
            root.layout();

            List<ScrollBar> bars = root.lookupAll(".scroll-bar").stream()
                    .filter(ScrollBar.class::isInstance)
                    .map(ScrollBar.class::cast)
                    .toList();
            List<ScrollBar> visible = bars.stream().filter(ScrollBar::isVisible).toList();
            boolean slim = visible.stream().allMatch(bar ->
                    bar.getOrientation() == javafx.geometry.Orientation.VERTICAL
                            ? bar.prefWidth(-1) <= 10.5
                            : bar.prefHeight(-1) <= 10.5);
            boolean arrowsCollapsed = visible.stream().allMatch(bar -> {
                Node increment = bar.lookup(".increment-button");
                Node decrement = bar.lookup(".decrement-button");
                if (!(increment instanceof Region incrementRegion)
                        || !(decrement instanceof Region decrementRegion)) return false;
                return bar.getOrientation() == javafx.geometry.Orientation.VERTICAL
                        ? incrementRegion.prefHeight(-1) <= 0.5 && decrementRegion.prefHeight(-1) <= 0.5
                        : incrementRegion.prefWidth(-1) <= 0.5 && decrementRegion.prefWidth(-1) <= 0.5;
            });
            boolean styledThumbs = visible.stream().allMatch(bar -> {
                Node thumb = bar.lookup(".thumb");
                return thumb instanceof Region region && !region.getBackground().getFills().isEmpty();
            });
            return new boolean[]{bars.size() >= 5, visible.size() >= 3, slim, arrowsCollapsed, styledThumbs};
        });
        Platform.runLater(task);
        assertThat(task.get(15, TimeUnit.SECONDS)).containsOnly(true);
    }

    @Test
    void interviewWorkspaceUsesEvidenceRailAndReportLinksToSources() throws Exception {
        if (sessionState.currentUser().isEmpty()) {
            sessionState.logIn(new UserDto(1L, "citation-layout-user", "引用布局用户", LocalDateTime.now()));
        }
        FutureTask<boolean[]> task = new FutureTask<>(() -> {
            Parent workspace = load("/fxml/interview-workspace-view.fxml");
            Scene scene = new Scene(workspace, 1280, 860);
            scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
            workspace.applyCss();
            workspace.layout();
            VBox rail = (VBox) workspace.lookup("#citationRail");
            VBox citations = (VBox) workspace.lookup("#citationContainer");
            Button retryReport = (Button) workspace.lookup("#retryReportButton");
            BorderPane workspaceRoot = (BorderPane) workspace.lookup("#workspaceRoot");

            Parent report = load("/fxml/report-detail-view.fxml");
            Button sourceDirectory = (Button) report.lookup("#sourceDirectoryButton");
            Button trainingPlan = (Button) report.lookup("#trainingPlanButton");
            VBox trainingRecommendations = (VBox) report.lookup("#trainingRecommendationContainer");
            return new boolean[]{
                    rail != null && rail.getStyleClass().contains("citation-rail"),
                    rail != null && rail.getWidth() >= 270 && rail.getWidth() <= 330,
                    citations != null,
                    workspaceRoot == workspace,
                    retryReport != null && "重新生成报告".equals(retryReport.getText()),
                    sourceDirectory != null && "参考依据".equals(sourceDirectory.getText()),
                    trainingPlan != null && "创建专项训练方案".equals(trainingPlan.getText()),
                    trainingRecommendations != null
            };
        });
        Platform.runLater(task);
        assertThat(task.get(15, TimeUnit.SECONDS)).containsOnly(true);
    }

    @Test
    void transcriptRendersQuestionCardsCitationsAndStreamingOutput() throws Exception {
        FutureTask<boolean[]> task = new FutureTask<>(() -> {
            Parent workspace = load("/fxml/interview-workspace-view.fxml");
            Scene scene = new Scene(workspace, 1280, 860);
            scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
            InterviewTranscriptView transcript = (InterviewTranscriptView) workspace.lookup("#transcriptView");
            transcript.setCitationHandler(documentId -> { });
            transcript.setMessages(List.of(
                    new InterviewMessageDto(
                            1, Message.Role.ASSISTANT, "请介绍你的项目。", LocalDateTime.now(), false, List.of()),
                    new InterviewMessageDto(
                            2, Message.Role.USER, "我负责订单系统。", LocalDateTime.now(), false, List.of()),
                    new InterviewMessageDto(
                            3, Message.Role.ASSISTANT, "缓存如何保证一致性？", LocalDateTime.now(), false,
                            List.of(new KnowledgeCitationDto(
                                    8L, "缓存设计.md", 1, "使用失效通知与补偿任务。", 0.9)))));
            transcript.appendPendingUserAnswer("我会先保存回答，再等待下一题。");
            transcript.beginAssistantStream(3);
            transcript.appendAssistantChunk("请继续说明故障恢复方案。");
            workspace.applyCss();
            workspace.layout();

            InterviewTranscriptView emptyTranscript = new InterviewTranscriptView();
            emptyTranscript.appendPendingUserAnswer("先保存这条回答。");
            emptyTranscript.beginAssistantStream(1);

            Parent history = load("/fxml/interview-history-detail-view.fxml");
            Parent report = load("/fxml/report-detail-view.fxml");
            Scene reportScene = new Scene(report, 1200, 820);
            reportScene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
            report.applyCss();
            report.layout();
            return new boolean[]{
                    transcript.getQuestionCount() == 3,
                    transcript.lookupAll(".transcript-message-card").size() == 5,
                    transcript.lookupAll(".transcript-citation-chip").size() == 1,
                    ((VBox) emptyTranscript.getContent()).getChildren().size() == 2,
                    history.lookup("#transcriptView") instanceof InterviewTranscriptView,
                    report.lookup("#citationNavigationContainer") instanceof VBox
            };
        });
        Platform.runLater(task);
        assertThat(task.get(15, TimeUnit.SECONDS)).containsOnly(true);
    }

    @Test
    void transcriptNavigationContextRejectsInvalidTargets() {
        var context = new InterviewTranscriptContext(12L, 3);
        assertThat(context.sessionId()).isEqualTo(12L);
        assertThat(context.questionNumber()).isEqualTo(3);
        assertThatThrownBy(() -> new InterviewTranscriptContext(0, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new InterviewTranscriptContext(1, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void sidebarSpansWindowAndNavigationTracksActiveRoute() throws Exception {
        if (sessionState.currentUser().isEmpty()) {
            sessionState.logIn(new UserDto(1L, "navigation-test-user", "导航测试用户", LocalDateTime.now()));
        }
        FutureTask<boolean[]> task = new FutureTask<>(() -> {
            BorderPane root = (BorderPane) load("/fxml/main-window.fxml");
            Scene scene = new Scene(root, 1200, 800);
            scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
            root.applyCss();
            root.layout();
            Button dashboard = (Button) root.lookup("#dashboardNavButton");
            Button plans = (Button) root.lookup("#plansNavButton");
            Button settings = (Button) root.lookup("#settingsNavButton");
            HBox activityReceipt = (HBox) root.lookup("#activityReceipt");
            VBox sidebar = (VBox) root.lookup("#sidebar");
            var navigationOrder = sidebar.getChildren().stream()
                    .filter(Button.class::isInstance)
                    .map(Button.class::cast)
                    .map(Button::getText)
                    .toList();
            PseudoClass selected = PseudoClass.getPseudoClass("selected");
            boolean initialSelection = dashboard.getPseudoClassStates().contains(selected);
            plans.fire();
            boolean switchedSelection = plans.getPseudoClassStates().contains(selected)
                    && !dashboard.getPseudoClassStates().contains(selected);
            boolean fullHeight = root.getTop() == null
                    && Math.abs(root.getLeft().getBoundsInParent().getHeight() - root.getHeight()) < 0.5;
            boolean topbarInsideContent = root.getCenter() instanceof BorderPane content
                    && content.getTop() != null;
            settings.fire();
            boolean settingsLoaded = settings.getPseudoClassStates().contains(selected)
                    && !plans.getPseudoClassStates().contains(selected)
                    && root.lookup("#generalNavButton") != null;
            boolean taskFeedbackReady = activityReceipt != null
                    && !activityReceipt.isVisible()
                    && !activityReceipt.isManaged();
            boolean expectedNavigationOrder = navigationOrder.equals(List.of(
                    "首页", "面试方案", "面试记录", "简历", "知识库", "设置"));
            return new boolean[]{initialSelection, switchedSelection, fullHeight,
                    topbarInsideContent, settingsLoaded,
                    taskFeedbackReady, expectedNavigationOrder};
        });
        Platform.runLater(task);
        assertThat(task.get(15, TimeUnit.SECONDS)).containsOnly(true);
    }

    @Test
    void mainWindowShowsANonModalReceiptForCompletedBackgroundTasks() throws Exception {
        if (sessionState.currentUser().isEmpty()) {
            sessionState.logIn(new UserDto(1L, "notification-test-user", "通知测试用户", LocalDateTime.now()));
        }
        long userId = sessionState.requireCurrentUser().id();
        AtomicReference<BorderPane> rootReference = new AtomicReference<>();
        FutureTask<Void> setup = new FutureTask<>(() -> {
            BorderPane root = (BorderPane) load("/fxml/main-window.fxml");
            Scene scene = new Scene(root, 1200, 800);
            scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
            root.applyCss();
            root.layout();
            rootReference.set(root);
            return null;
        });
        Platform.runLater(setup);
        setup.get(15, TimeUnit.SECONDS);

        taskNotificationCenter.taskCompleted(new BackgroundTaskCompletedEvent(
                99L, userId, BackgroundTaskType.REPORT_GENERATE));

        FutureTask<boolean[]> assertion = new FutureTask<>(() -> {
            BorderPane root = rootReference.get();
            HBox receipt = (HBox) root.lookup("#activityReceipt");
            Label title = (Label) root.lookup("#activityTitleLabel");
            Label detail = (Label) root.lookup("#activityDetailLabel");
            Button close = (Button) root.lookup(".activity-receipt-close");
            boolean shown = receipt.isVisible() && receipt.isManaged()
                    && "面试报告生成已完成".equals(title.getText())
                    && detail.getText().contains("面试记录");
            close.fire();
            boolean dismissed = !receipt.isVisible() && !receipt.isManaged();
            return new boolean[]{shown, dismissed};
        });
        Platform.runLater(assertion);
        assertThat(assertion.get(15, TimeUnit.SECONDS)).containsOnly(true);
    }

    @Test
    void settingsMasksSecretAndSwitchesCategoryWithActiveState() throws Exception {
        if (sessionState.currentUser().isEmpty()) {
            sessionState.logIn(new UserDto(1L, "settings-test-user", "设置测试用户", LocalDateTime.now()));
        }
        FutureTask<boolean[]> task = new FutureTask<>(() -> {
            Parent root = load("/fxml/settings-view.fxml");
            Scene scene = new Scene(root, 1100, 760);
            scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
            root.applyCss();
            root.layout();
            Button general = (Button) root.lookup("#generalNavButton");
            Button ai = (Button) root.lookup("#aiNavButton");
            VBox generalPane = (VBox) root.lookup("#generalPane");
            VBox aiPane = (VBox) root.lookup("#aiPane");
            PasswordField key = (PasswordField) root.lookup("#apiKeyField");
            PseudoClass selected = PseudoClass.getPseudoClass("selected");
            boolean initial = general.getPseudoClassStates().contains(selected)
                    && generalPane.isVisible() && !aiPane.isVisible();
            ai.fire();
            boolean switched = ai.getPseudoClassStates().contains(selected)
                    && !general.getPseudoClassStates().contains(selected)
                    && aiPane.isVisible() && !generalPane.isVisible();
            boolean masked = key.getText() == null || !key.getText().startsWith("sk-");
            return new boolean[]{initial, switched, masked};
        });
        Platform.runLater(task);
        assertThat(task.get(15, TimeUnit.SECONDS)).containsOnly(true);
    }

    private Parent loadOnFxThread(String resource) throws Exception {
        FutureTask<Parent> task = new FutureTask<>(() -> {
            return load(resource);
        });
        Platform.runLater(task);
        return task.get(15, TimeUnit.SECONDS);
    }

    private Parent load(String resource) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(resource));
        loader.setControllerFactory(applicationContext::getBean);
        return loader.load();
    }
}
