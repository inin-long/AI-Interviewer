package com.inin.aiinterviewer.config;

import com.inin.aiinterviewer.config.properties.AppProperties;
import com.inin.aiinterviewer.config.properties.LlmProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class ConfigValidator implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ConfigValidator.class);

    private final AppProperties appProperties;
    private final LlmProperties llmProperties;

    public ConfigValidator(AppProperties appProperties, LlmProperties llmProperties) {
        this.appProperties = appProperties;
        this.llmProperties = llmProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        Path dataRoot = Path.of(appProperties.dataRoot()).toAbsolutePath().normalize();
        initializeRuntimeDirectories(dataRoot);
        if (!Files.isDirectory(dataRoot) || !Files.isWritable(dataRoot)) {
            throw new IllegalStateException("Application data directory is not writable: " + dataRoot);
        }
        if (llmProperties.isConfigured()) {
            log.info("AI provider configuration detected for chat model: {}", llmProperties.chatModel());
        } else {
            log.info("AI provider is not configured; local account and data features remain available");
        }
    }

    private void initializeRuntimeDirectories(Path dataRoot) {
        try {
            Files.createDirectories(dataRoot.resolve("database"));
            Files.createDirectories(dataRoot.resolve("users"));
            Files.createDirectories(dataRoot.resolve("logs"));
            Files.createDirectories(dataRoot.resolve("temp"));
            Path configDirectory = Files.createDirectories(dataRoot.resolve("config"));
            Path example = configDirectory.resolve("application-local.example.yml");
            if (Files.notExists(example)) {
                ClassPathResource resource = new ClassPathResource("application-local.example.yml");
                try (var input = resource.getInputStream()) {
                    Files.copy(input, example);
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot initialize application runtime directories: " + dataRoot, exception);
        }
    }
}
