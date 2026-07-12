package com.inin.aiinterviewer.application.service;

import com.inin.aiinterviewer.application.dto.UserDto;
import com.inin.aiinterviewer.application.exception.BusinessException;
import com.inin.aiinterviewer.domain.enums.ProfileSource;
import com.inin.aiinterviewer.domain.enums.ProfileStatus;
import com.inin.aiinterviewer.domain.model.CandidateProfileContent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class CandidateProfileServiceIntegrationTest {

    @TempDir
    static Path applicationHome;

    @DynamicPropertySource
    static void applicationProperties(DynamicPropertyRegistry registry) {
        registry.add("ai.interviewer.home", () -> applicationHome.toString());
    }

    @Autowired private UserService userService;
    @Autowired private ResumeService resumeService;
    @Autowired private CandidateProfileService profileService;

    @Test
    void createsClearlyMarkedLocalDraftThenAllowsManualConfirmation() throws Exception {
        UserDto owner = userService.register("profile-owner", "Owner", "safe-password");
        UserDto other = userService.register("profile-other", "Other", "safe-password");
        Path source = applicationHome.resolve("profile-resume.md");
        Files.writeString(source, """
                # 张三
                5 年开发经验，目标岗位 Java 后端工程师。
                熟悉 Java、Spring Boot、Redis、MySQL、Docker 和微服务架构。
                """);
        var resume = resumeService.uploadAndParse(owner.id(), source);

        var draft = profileService.generate(owner.id(), resume.id());

        assertThat(draft.source()).isEqualTo(ProfileSource.LOCAL_DRAFT);
        assertThat(draft.status()).isEqualTo(ProfileStatus.DRAFT);
        assertThat(draft.confirmed()).isFalse();
        assertThat(draft.content().skills()).contains("Java", "Spring Boot", "Redis", "MySQL");
        assertThat(profileService.find(other.id(), resume.id())).isEmpty();
        assertThatThrownBy(() -> profileService.generate(other.id(), resume.id()))
                .isInstanceOf(BusinessException.class);

        CandidateProfileContent manual = new CandidateProfileContent(
                "张三", "Java 后端工程师", "5 年", "本科",
                List.of("Java", "Spring Boot", "Redis"),
                List.of("订单系统核心开发"), List.of("后端开发 5 年"),
                List.of("项目经验完整"), List.of("系统设计深度待验证"),
                "具备较完整的 Java 后端项目经验。");
        var saved = profileService.saveManual(owner.id(), resume.id(), manual);
        assertThat(saved.source()).isEqualTo(ProfileSource.MANUAL);

        var confirmed = profileService.confirm(owner.id(), resume.id());
        assertThat(confirmed.confirmed()).isTrue();
        assertThat(confirmed.status()).isEqualTo(ProfileStatus.CONFIRMED);
    }
}

