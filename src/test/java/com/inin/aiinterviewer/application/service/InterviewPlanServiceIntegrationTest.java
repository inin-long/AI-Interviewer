package com.inin.aiinterviewer.application.service;

import com.inin.aiinterviewer.application.dto.InterviewPlanDto;
import com.inin.aiinterviewer.application.dto.SaveInterviewPlanCommand;
import com.inin.aiinterviewer.application.dto.UserDto;
import com.inin.aiinterviewer.application.exception.BusinessException;
import com.inin.aiinterviewer.domain.enums.InterviewDifficulty;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Path;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class InterviewPlanServiceIntegrationTest {

    @TempDir
    static Path applicationHome;

    @DynamicPropertySource
    static void applicationProperties(DynamicPropertyRegistry registry) {
        registry.add("ai.interviewer.home", () -> applicationHome.toString());
    }

    @Autowired private UserService userService;
    @Autowired private InterviewPlanService planService;
    @Autowired private ResumeService resumeService;

    @Test
    void providesUserIsolatedCrudAndCopy() {
        UserDto owner = userService.register("plan-owner", "Owner", "safe-password");
        UserDto other = userService.register("plan-other", "Other", "safe-password");
        SaveInterviewPlanCommand create = new SaveInterviewPlanCommand(
                "Java 后端高级面试", "Java 后端工程师", "负责核心服务开发",
                InterviewDifficulty.SENIOR, 45, 15, null,
                Map.of("focus", "Spring, 数据库"),
                List.of("INTRODUCTION", "PROJECT_EXPERIENCE", "TECHNICAL_DEEP_DIVE", "SUMMARY"));

        InterviewPlanDto created = planService.create(owner.id(), create);
        assertThat(created.name()).isEqualTo("Java 后端高级面试");
        assertThat(created.rules()).containsEntry("focus", "Spring, 数据库");
        assertThat(planService.list(other.id())).isEmpty();
        assertThatThrownBy(() -> planService.require(created.id(), other.id()))
                .isInstanceOf(BusinessException.class);

        SaveInterviewPlanCommand update = new SaveInterviewPlanCommand(
                "Java 后端项目深挖", "高级 Java 工程师", "重点考察项目实践",
                InterviewDifficulty.EXPERT, 60, 20, null, Map.of("focus", "项目深挖"), null);
        InterviewPlanDto updated = planService.update(owner.id(), created.id(), update);
        assertThat(updated.durationMinutes()).isEqualTo(60);
        assertThat(updated.stages()).isNotEmpty();

        InterviewPlanDto copy = planService.duplicate(owner.id(), created.id());
        assertThat(copy.name()).endsWith("副本");
        assertThat(planService.list(owner.id())).hasSize(2);

        planService.delete(owner.id(), created.id());
        assertThat(planService.list(owner.id())).extracting(InterviewPlanDto::id).containsExactly(copy.id());
    }

    @Test
    void rejectsResumeOwnedByAnotherUser() throws Exception {
        UserDto owner = userService.register("plan-resume-owner", "Owner", "safe-password");
        UserDto other = userService.register("plan-resume-other", "Other", "safe-password");
        Path source = applicationHome.resolve("other-resume.md");
        Files.writeString(source, "# Other Resume\nJava and Spring");
        var otherResume = resumeService.uploadAndParse(other.id(), source);

        SaveInterviewPlanCommand command = new SaveInterviewPlanCommand(
                "非法关联测试", "Java 工程师", "", InterviewDifficulty.MEDIUM,
                45, 10, otherResume.id(), Map.of(), null);

        assertThatThrownBy(() -> planService.create(owner.id(), command))
                .isInstanceOf(BusinessException.class);
    }
}
