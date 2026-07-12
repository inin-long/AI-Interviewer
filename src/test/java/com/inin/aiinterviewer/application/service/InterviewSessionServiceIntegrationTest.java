package com.inin.aiinterviewer.application.service;

import com.inin.aiinterviewer.agent.state.InterviewState;
import com.inin.aiinterviewer.application.dto.InterviewSessionDto;
import com.inin.aiinterviewer.application.dto.SaveInterviewPlanCommand;
import com.inin.aiinterviewer.application.exception.BusinessException;
import com.inin.aiinterviewer.domain.entity.AgentCheckpointEntity;
import com.inin.aiinterviewer.domain.enums.InterviewDifficulty;
import com.inin.aiinterviewer.domain.enums.InterviewStage;
import com.inin.aiinterviewer.domain.enums.InterviewStatus;
import com.inin.aiinterviewer.infrastructure.database.mapper.AgentCheckpointMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class InterviewSessionServiceIntegrationTest {

    @TempDir
    static Path applicationHome;

    @DynamicPropertySource
    static void applicationProperties(DynamicPropertyRegistry registry) {
        registry.add("ai.interviewer.home", () -> applicationHome.toString());
    }

    @Autowired private UserService userService;
    @Autowired private InterviewPlanService planService;
    @Autowired private InterviewSessionService sessionService;
    @Autowired private InterviewHistoryService historyService;
    @Autowired private AgentCheckpointMapper checkpointMapper;

    @Test
    void snapshotsPlanPersistsProgressAndRestoresLatestValidCheckpoint() {
        var owner = userService.register("session-owner", "Owner", "safe-password");
        var other = userService.register("session-other", "Other", "safe-password");
        var plan = planService.create(owner.id(), command("Java 基础面试", "Spring"));

        InterviewSessionDto session = sessionService.create(owner.id(), plan.id());
        assertThat(session.status()).isEqualTo(InterviewStatus.RUNNING);
        assertThat(session.planSnapshot().name()).isEqualTo("Java 基础面试");
        assertThat(sessionService.loadLatestState(owner.id(), session.id()))
                .get().extracting(InterviewState::stage).isEqualTo(InterviewStage.INTRODUCTION);

        planService.update(owner.id(), plan.id(), command("已修改的方案", "数据库"));
        assertThat(sessionService.require(owner.id(), session.id()).planSnapshot().name())
                .isEqualTo("Java 基础面试");

        InterviewState answered = sessionService.appendUserAnswer(owner.id(), session.id(), "我会从 JVM 内存模型开始回答。");
        assertThat(answered.latestAnswer()).contains("JVM");
        assertThat(sessionService.messages(owner.id(), session.id()))
                .singleElement().satisfies(message -> assertThat(message.content()).contains("JVM"));

        InterviewState transitioned = sessionService.transitionStage(
                owner.id(), session.id(), InterviewStage.RESUME_REVIEW);
        assertThat(transitioned.stage()).isEqualTo(InterviewStage.RESUME_REVIEW);

        AgentCheckpointEntity corrupt = new AgentCheckpointEntity();
        corrupt.setUserId(owner.id());
        corrupt.setSessionId(session.id());
        corrupt.setNodeName("corrupt_test_checkpoint");
        corrupt.setStateJson("{\"stateVersion\":\"999\"}");
        corrupt.setStateVersion("999");
        checkpointMapper.insert(corrupt);
        assertThat(sessionService.loadLatestState(owner.id(), session.id()))
                .get().extracting(InterviewState::stage).isEqualTo(InterviewStage.RESUME_REVIEW);

        assertThat(sessionService.pause(owner.id(), session.id()).status()).isEqualTo(InterviewStatus.PAUSED);
        assertThat(sessionService.startOrResume(owner.id(), plan.id()).id()).isEqualTo(session.id());
        assertThat(sessionService.require(owner.id(), session.id()).status()).isEqualTo(InterviewStatus.RUNNING);

        assertThat(historyService.list(owner.id(), "Java", null))
                .singleElement().satisfies(item -> {
                    assertThat(item.sessionId()).isEqualTo(session.id());
                    assertThat(item.messageCount()).isEqualTo(1);
                    assertThat(item.reportAvailable()).isFalse();
                });
        assertThat(historyService.list(owner.id(), "不存在的岗位", null)).isEmpty();
        assertThat(historyService.list(owner.id(), "", InterviewStatus.RUNNING)).hasSize(1);
        assertThat(historyService.list(other.id(), "", null)).isEmpty();
        assertThat(historyService.detail(owner.id(), session.id()).messages()).hasSize(1);

        assertThatThrownBy(() -> sessionService.require(other.id(), session.id()))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> historyService.detail(other.id(), session.id()))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> sessionService.loadLatestState(other.id(), session.id()))
                .isInstanceOf(BusinessException.class);
    }

    private SaveInterviewPlanCommand command(String name, String focus) {
        return new SaveInterviewPlanCommand(
                name, "Java 工程师", "负责核心服务开发", InterviewDifficulty.MEDIUM,
                45, 10, null, Map.of("focus", focus),
                List.of("INTRODUCTION", "RESUME_REVIEW", "TECHNICAL_DEEP_DIVE", "SUMMARY"));
    }
}
