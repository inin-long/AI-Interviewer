package com.inin.aiinterviewer.ui.controller;

import com.inin.aiinterviewer.application.exception.GlobalExceptionHandler;
import com.inin.aiinterviewer.config.properties.AppProperties;
import com.inin.aiinterviewer.config.properties.LlmProperties;
import com.inin.aiinterviewer.config.properties.TaskProperties;
import com.inin.aiinterviewer.infrastructure.ai.ChatService;
import com.inin.aiinterviewer.infrastructure.ai.EmbeddingService;
import com.inin.aiinterviewer.infrastructure.file.PathService;
import com.inin.aiinterviewer.ui.navigation.JavaFxViewManager;
import javafx.css.PseudoClass;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.VBox;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.awt.Desktop;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@Scope("prototype")
public class SettingsController {
    private static final PseudoClass SELECTED = PseudoClass.getPseudoClass("selected");

    private final AppProperties appProperties;
    private final LlmProperties llmProperties;
    private final TaskProperties taskProperties;
    private final PathService pathService;
    private final ChatService chatService;
    private final EmbeddingService embeddingService;
    private final GlobalExceptionHandler exceptionHandler;
    private final JavaFxViewManager viewManager;

    @FXML private Button generalNavButton;
    @FXML private Button aiNavButton;
    @FXML private Button dataNavButton;
    @FXML private VBox generalPane;
    @FXML private VBox aiPane;
    @FXML private VBox dataPane;

    @FXML private Label appNameLabel;
    @FXML private Label appVersionLabel;
    @FXML private Label javaVersionLabel;
    @FXML private Label javafxStatusLabel;

    @FXML private TextField baseUrlField;
    @FXML private PasswordField apiKeyField;
    @FXML private TextField chatModelField;
    @FXML private TextField embeddingModelField;
    @FXML private Label timeoutLabel;
    @FXML private Label configurationSourceLabel;
    @FXML private Label connectionStatusLabel;
    @FXML private Label connectionDetailLabel;
    @FXML private Button testConnectionButton;
    @FXML private Button saveConfigButton;
    @FXML private Label saveStatusLabel;

    @FXML private TextField dataRootField;
    @FXML private Label databasePathLabel;
    @FXML private Label userFilesPathLabel;
    @FXML private Label logPathLabel;
    @FXML private Label configPathLabel;
    @FXML private Label workerStatusLabel;
    @FXML private Label retryPolicyLabel;
    @FXML private Label dataActionLabel;

    public SettingsController(
            AppProperties appProperties,
            LlmProperties llmProperties,
            TaskProperties taskProperties,
            PathService pathService,
            ChatService chatService,
            EmbeddingService embeddingService,
            GlobalExceptionHandler exceptionHandler,
            JavaFxViewManager viewManager
    ) {
        this.appProperties = appProperties;
        this.llmProperties = llmProperties;
        this.taskProperties = taskProperties;
        this.pathService = pathService;
        this.chatService = chatService;
        this.embeddingService = embeddingService;
        this.exceptionHandler = exceptionHandler;
        this.viewManager = viewManager;
    }

    @FXML
    private void initialize() {
        appNameLabel.setText(appProperties.name());
        appVersionLabel.setText(appProperties.version());
        javaVersionLabel.setText(System.getProperty("java.version", "未知"));
        javafxStatusLabel.setText(System.getProperty("javafx.version", "21"));

        baseUrlField.setText(valueOrEmpty(llmProperties.baseUrl()));
        chatModelField.setText(valueOrEmpty(llmProperties.chatModel()));
        embeddingModelField.setText(valueOrEmpty(llmProperties.embeddingModel()));
        apiKeyField.setText(llmProperties.apiKey() == null || llmProperties.apiKey().isBlank()
                ? "" : "configured-secret");
        apiKeyField.setPromptText(llmProperties.isConfigured() ? "已配置（不会显示明文）" : "未配置");
        timeoutLabel.setText(llmProperties.timeout() == null ? "—" : llmProperties.timeout().toSeconds() + " 秒");
        configurationSourceLabel.setText(configurationSource());
        setConnectionState("未检测", "点击“测试连接”验证对话和 Embedding 能力。", "status-neutral");
        testConnectionButton.setDisable(!llmProperties.isConfigured());

        Path root = pathService.applicationRoot();
        dataRootField.setText(root.toString());
        databasePathLabel.setText(root.resolve("database/app.db").toString());
        userFilesPathLabel.setText(root.resolve("users").toString());
        logPathLabel.setText(root.resolve("logs").toString());
        configPathLabel.setText(root.resolve("config").toString());
        workerStatusLabel.setText(taskProperties.enabled()
                ? "已启用 · " + taskProperties.workerCount() + " 个 Worker"
                : "已停用");
        retryPolicyLabel.setText("最多 " + taskProperties.retryCount() + " 次 · 间隔 "
                + readable(taskProperties.retryDelay()));
        showGeneral();
    }

    @FXML private void showGeneral() { showPane(generalPane, generalNavButton); }
    @FXML private void showAi() { showPane(aiPane, aiNavButton); }
    @FXML private void showData() { showPane(dataPane, dataNavButton); }

    @FXML
    private void testConnection() {
        if (!llmProperties.isConfigured()) {
            setConnectionState("未配置", "请先通过环境变量或外部 application-local.yml 配置 AI。", "status-failed");
            return;
        }
        testConnectionButton.setDisable(true);
        setConnectionState("检测中", "正在验证同步对话和 Embedding 接口…", "status-neutral");
        Task<ConnectionTestResult> task = new Task<>() {
            @Override
            protected ConnectionTestResult call() {
                long chatStarted = System.nanoTime();
                String response = chatService.chat("只回复两个字：正常");
                long chatMillis = Duration.ofNanos(System.nanoTime() - chatStarted).toMillis();
                if (response == null || response.isBlank()) throw new IllegalStateException("对话模型返回空内容");
                if (!llmProperties.isEmbeddingConfigured()) {
                    return new ConnectionTestResult(chatMillis, 0, 0, false);
                }
                long embeddingStarted = System.nanoTime();
                float[] embedding = embeddingService.embed("AI Interviewer 设置页连接测试");
                long embeddingMillis = Duration.ofNanos(System.nanoTime() - embeddingStarted).toMillis();
                return new ConnectionTestResult(chatMillis, embeddingMillis, embedding.length, true);
            }
        };
        task.setOnSucceeded(event -> {
            testConnectionButton.setDisable(false);
            ConnectionTestResult result = task.getValue();
            String embedding = result.embeddingTested()
                    ? "Embedding " + result.embeddingMillis() + " ms · " + result.dimensions() + " 维"
                    : "Embedding 未配置";
            setConnectionState("已连接", "Chat " + result.chatMillis() + " ms · " + embedding, "status-success");
        });
        task.setOnFailed(event -> {
            testConnectionButton.setDisable(false);
            setConnectionState("连接失败", exceptionHandler.toUserMessage(task.getException()), "status-failed");
        });
        Thread worker = new Thread(task, "settings-ai-connection-test");
        worker.setDaemon(true);
        worker.start();
    }

    @FXML
    private void copyDataRoot() {
        copy(dataRootField.getText());
        setDataAction("数据根目录已复制到剪贴板。", false);
    }

    @FXML private void openDataRoot() { openDirectory(pathService.applicationRoot(), "数据目录"); }
    @FXML
    private void openConfigDirectory() {
        showData();
        openDirectory(pathService.applicationRoot().resolve("config"), "配置目录");
    }

    private void showPane(VBox pane, Button navButton) {
        generalPane.setVisible(pane == generalPane);
        generalPane.setManaged(pane == generalPane);
        aiPane.setVisible(pane == aiPane);
        aiPane.setManaged(pane == aiPane);
        dataPane.setVisible(pane == dataPane);
        dataPane.setManaged(pane == dataPane);
        generalNavButton.pseudoClassStateChanged(SELECTED, navButton == generalNavButton);
        aiNavButton.pseudoClassStateChanged(SELECTED, navButton == aiNavButton);
        dataNavButton.pseudoClassStateChanged(SELECTED, navButton == dataNavButton);
    }

    private void setConnectionState(String state, String detail, String styleClass) {
        connectionStatusLabel.setText("● " + state);
        connectionStatusLabel.getStyleClass().removeAll("status-success", "status-failed", "status-neutral");
        connectionStatusLabel.getStyleClass().add(styleClass);
        connectionDetailLabel.setText(detail);
    }

    private String configurationSource() {
        String envKey = System.getenv("AI_LLM_API_KEY");
        if (envKey != null && !envKey.isBlank()) return "Windows 用户/进程环境变量";
        if (llmProperties.isConfigured()) return "外部 application-local.yml 或启动参数";
        return "未检测到完整配置";
    }

    @FXML
    private void saveConfig() {
        String baseUrlRaw = baseUrlField.getText();
        String apiKeyRaw = apiKeyField.getText();
        String chatModelRaw = chatModelField.getText();
        String embeddingModelRaw = embeddingModelField.getText();

        String apiKey;
        if ("configured-secret".equals(apiKeyRaw)) {
            apiKey = llmProperties.apiKey() == null ? "" : llmProperties.apiKey();
        } else {
            apiKey = apiKeyRaw == null ? "" : apiKeyRaw.strip();
        }
        String chatModel = chatModelRaw == null ? "" : chatModelRaw.strip();

        if (apiKey.isBlank()) {
            setSaveStatus("API Key 不能为空", true);
            return;
        }
        if (chatModel.isBlank()) {
            setSaveStatus("Chat Model 不能为空", true);
            return;
        }

        try {
            Path configDir = pathService.applicationRoot().resolve("config");
            Files.createDirectories(configDir);
            Path configFile = configDir.resolve("application-local.yml");

            // 保留文件中其它顶层配置（避免覆盖），仅替换 llm 段
            Map<String, Object> root = loadOrEmpty(configFile);
            Map<String, Object> llm = new LinkedHashMap<>();
            llm.put("base-url", blankToNull(baseUrlRaw) == null ? "https://api.openai.com" : blankToNull(baseUrlRaw));
            llm.put("api-key", apiKey);
            llm.put("chat-model", chatModel);
            llm.put("embedding-model", blankToNull(embeddingModelRaw) == null ? "" : blankToNull(embeddingModelRaw));
            // 关键：沿用 master 已有的 300s / max-retries / max-tokens 配置，绝不硬编码 60s（develop 的雷）
            llm.put("timeout", llmProperties.timeout() == null ? "300s" : llmProperties.timeout().toSeconds() + "s");
            llm.put("max-retries", llmProperties.maxRetries() == null ? 0 : llmProperties.maxRetries());
            llm.put("max-tokens", llmProperties.maxTokens() == null ? 2048 : llmProperties.maxTokens());
            if (llmProperties.thinkingEnabled() != null) {
                llm.put("thinking-enabled", llmProperties.thinkingEnabled());
            }
            root.put("llm", llm);

            try (Writer writer = Files.newBufferedWriter(configFile, StandardCharsets.UTF_8)) {
                new Yaml().dump(root, writer);
            }

            configurationSourceLabel.setText(configurationSource());
            apiKeyField.setText("configured-secret");
            apiKeyField.setPromptText("已配置（不会显示明文）");
            testConnectionButton.setDisable(!llmProperties.isConfigured());

            setSaveStatus("已保存到 " + configFile + "，重启应用后配置生效。", false);
        } catch (Exception exception) {
            setSaveStatus("保存失败：" + exception.getMessage(), true);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> loadOrEmpty(Path file) throws IOException {
        if (!Files.exists(file)) return new LinkedHashMap<>();
        try (InputStream in = Files.newInputStream(file)) {
            Object loaded = new Yaml().load(in);
            if (loaded instanceof Map) return (Map<String, Object>) loaded;
        }
        return new LinkedHashMap<>();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private void setSaveStatus(String text, boolean failed) {
        saveStatusLabel.setText(text);
        saveStatusLabel.getStyleClass().removeAll("secondary-text", "danger-text", "success-text");
        saveStatusLabel.getStyleClass().add(failed ? "danger-text" : "success-text");
    }

    private void copy(String value) {
        ClipboardContent content = new ClipboardContent();
        content.putString(value);
        Clipboard.getSystemClipboard().setContent(content);
    }

    private void openDirectory(Path directory, String name) {
        try {
            Files.createDirectories(directory);
            if (!Desktop.isDesktopSupported()) throw new IOException("当前系统不支持打开目录");
            Desktop.getDesktop().open(directory.toFile());
            setDataAction(name + "已打开。", false);
        } catch (IOException | UnsupportedOperationException exception) {
            setDataAction(name + "打开失败：" + exception.getMessage(), true);
        }
    }

    private void setDataAction(String text, boolean failed) {
        dataActionLabel.setText(text);
        dataActionLabel.getStyleClass().removeAll("secondary-text", "danger-text");
        dataActionLabel.getStyleClass().add(failed ? "danger-text" : "secondary-text");
    }

    private String readable(Duration duration) {
        if (duration == null) return "—";
        return duration.toMillis() < 1000 ? duration.toMillis() + " ms" : duration.toSeconds() + " 秒";
    }

    private String valueOrEmpty(String value) { return value == null ? "" : value; }

    private record ConnectionTestResult(
            long chatMillis,
            long embeddingMillis,
            int dimensions,
            boolean embeddingTested
    ) {
    }
}
