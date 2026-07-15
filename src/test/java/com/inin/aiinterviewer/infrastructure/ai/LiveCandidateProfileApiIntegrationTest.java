package com.inin.aiinterviewer.infrastructure.ai;

import com.inin.aiinterviewer.application.service.BackgroundTaskService;
import com.inin.aiinterviewer.application.service.CandidateProfileService;
import com.inin.aiinterviewer.application.service.CandidateProfileTaskService;
import com.inin.aiinterviewer.application.service.ResumeService;
import com.inin.aiinterviewer.application.service.UserService;
import com.inin.aiinterviewer.domain.enums.BackgroundTaskStatus;
import com.inin.aiinterviewer.domain.enums.ProfileSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Focused, opt-in diagnostic for the exact production resume -> background task
 * -> structured provider response -> candidate profile path.
 */
@SpringBootTest
@EnabledIf("liveProviderConfigured")
class LiveCandidateProfileApiIntegrationTest {

    @TempDir
    static Path applicationHome;

    @Autowired private UserService userService;
    @Autowired private ResumeService resumeService;
    @Autowired private CandidateProfileTaskService profileTaskService;
    @Autowired private BackgroundTaskService backgroundTaskService;
    @Autowired private CandidateProfileService profileService;

    static boolean liveProviderConfigured() {
        return truthy(System.getenv("AI_LLM_LIVE_TEST"))
                && present("AI_LLM_API_KEY")
                && present("AI_LLM_BASE_URL")
                && present("AI_LLM_CHAT_MODEL");
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("ai.interviewer.home", () -> applicationHome.toString());
        registry.add("task.enabled", () -> "false");
        registry.add("task.retry-count", () -> "1");
        registry.add("llm.timeout", () -> "300s");
        registry.add("llm.max-retries", () -> "0");
        registry.add("llm.max-tokens", () -> "2048");
    }

    @Test
    void generatesAProfileFromTheFullStackResumeWithOneObservableProviderAttempt() throws Exception {
        var user = userService.register("live-profile-focused", "Live Profile", "safe-password");
        var resume = resumeService.uploadAndParse(user.id(), fixture("full-stack-engineer-resume.md"));
        long taskId = profileTaskService.enqueue(user.id(), resume.id());
        Instant started = Instant.now();

        assertThat(backgroundTaskService.executeNext("live-profile-focused-worker")).isTrue();

        var task = backgroundTaskService.require(user.id(), taskId);
        assertThat(task.getStatus())
                .withFailMessage("Real candidate-profile request failed after %s: %s",
                        Duration.between(started, Instant.now()), task.getErrorMessage())
                .isEqualTo(BackgroundTaskStatus.SUCCESS);
        assertThat(task.getAttemptCount()).isEqualTo(1);
        assertThat(profileService.find(user.id(), resume.id()))
                .get()
                .satisfies(profile -> {
                    assertThat(profile.source()).isEqualTo(ProfileSource.AI);
                    assertThat(profile.content().fullName()).isNotBlank();
                    assertThat(profile.content().targetRole()).containsAnyOf("全栈", "工程师");
                    assertThat(profile.content().skills()).isNotEmpty();
                    assertThat(profile.content().summary()).isNotBlank();
                });
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
            var resource = LiveCandidateProfileApiIntegrationTest.class.getResource("/fixtures/" + fileName);
            if (resource == null) throw new IllegalStateException("Missing live profile fixture: " + fileName);
            return Path.of(resource.toURI()).toAbsolutePath().normalize();
        } catch (URISyntaxException exception) {
            throw new IllegalStateException("Invalid live profile fixture path: " + fileName, exception);
        }
    }
}
