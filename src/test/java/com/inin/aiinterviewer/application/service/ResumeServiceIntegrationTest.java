package com.inin.aiinterviewer.application.service;

import com.inin.aiinterviewer.application.dto.ResumeDto;
import com.inin.aiinterviewer.application.dto.UserDto;
import com.inin.aiinterviewer.application.exception.BusinessException;
import com.inin.aiinterviewer.domain.enums.ResumeStatus;
import com.inin.aiinterviewer.infrastructure.database.mapper.ResumeMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class ResumeServiceIntegrationTest {

    @TempDir
    static Path applicationHome;

    @DynamicPropertySource
    static void applicationProperties(DynamicPropertyRegistry registry) {
        registry.add("ai.interviewer.home", () -> applicationHome.toString());
    }

    @Autowired private UserService userService;
    @Autowired private ResumeService resumeService;
    @Autowired private ResumeMapper resumeMapper;

    @Test
    void uploadsParsesListsAndDeletesResumeWithinOwningUser() throws Exception {
        UserDto owner = userService.register("resume-owner", "Owner", "safe-password");
        UserDto other = userService.register("resume-other", "Other", "safe-password");
        Path source = applicationHome.resolve("Java后端简历.md");
        Files.writeString(source, "# 张伟\n\n熟悉 Java、Spring Boot、SQLite 和 Redis。\n");

        ResumeDto uploaded = resumeService.uploadAndParse(owner.id(), source);

        assertThat(uploaded.status()).isEqualTo(ResumeStatus.COMPLETED);
        assertThat(resumeService.list(owner.id())).extracting(ResumeDto::id).containsExactly(uploaded.id());
        assertThat(resumeService.list(other.id())).isEmpty();
        assertThat(resumeMapper.findByIdAndUserId(uploaded.id(), owner.id()).orElseThrow().getParsedText())
                .contains("Spring Boot");
        assertThat(resumeMapper.findByIdAndUserId(uploaded.id(), other.id())).isEmpty();
        assertThatThrownBy(() -> resumeService.delete(other.id(), uploaded.id()))
                .isInstanceOf(BusinessException.class);

        resumeService.delete(owner.id(), uploaded.id());
        assertThat(resumeService.list(owner.id())).isEmpty();
    }
}

