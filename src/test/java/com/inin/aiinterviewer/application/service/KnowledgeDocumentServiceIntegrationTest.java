package com.inin.aiinterviewer.application.service;

import com.inin.aiinterviewer.domain.enums.KnowledgeStatus;
import com.inin.aiinterviewer.infrastructure.ai.EmbeddingService;
import com.inin.aiinterviewer.infrastructure.ai.ChatService;
import com.inin.aiinterviewer.agent.tool.ToolInput;
import com.inin.aiinterviewer.agent.tool.ToolRegistry;
import com.inin.aiinterviewer.application.dto.KnowledgeCitationDto;
import com.inin.aiinterviewer.application.dto.SaveInterviewPlanCommand;
import com.inin.aiinterviewer.domain.enums.InterviewDifficulty;
import com.inin.aiinterviewer.application.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.List;
import java.util.ArrayDeque;
import java.util.Queue;

import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(KnowledgeDocumentServiceIntegrationTest.FakeEmbeddingConfiguration.class)
class KnowledgeDocumentServiceIntegrationTest {

    @TempDir static Path applicationHome;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("ai.interviewer.home", () -> applicationHome.toString());
        registry.add("rag.chunk-size", () -> 120);
        registry.add("rag.overlap", () -> 20);
        registry.add("task.enabled", () -> false);
    }

    @Autowired private UserService userService;
    @Autowired private KnowledgeDocumentService knowledgeService;
    @Autowired private KnowledgeDocumentTaskService knowledgeTaskService;
    @Autowired private BackgroundTaskService backgroundTaskService;
    @Autowired private ToolRegistry toolRegistry;
    @Autowired private InterviewPlanService planService;
    @Autowired private InterviewSessionService sessionService;
    @Autowired private InterviewAgentService agentService;
    @Autowired private FakeChatService chatService;

    @BeforeEach
    void resetFakeChatService() {
        chatService.clear();
    }

    @Test
    void uploadsChunksIndexesSearchesAndIsolatesUsers() throws Exception {
        var owner = userService.register("knowledge-owner", "Owner", "safe-password");
        var other = userService.register("knowledge-other", "Other", "safe-password");
        Path source = applicationHome.resolve("redis-notes.md");
        Files.writeString(source, ("Redis 使用缓存穿透保护、互斥锁和过期策略。\n\n"
                + "数据库索引需要关注选择性与回表成本。\n").repeat(6));

        var document = knowledgeService.uploadAndIndex(owner.id(), source, "项目文档");

        assertThat(document.status()).isEqualTo(KnowledgeStatus.READY);
        assertThat(knowledgeService.detail(owner.id(), document.id()).chunks()).hasSizeGreaterThan(1);
        assertThat(knowledgeService.list(other.id())).isEmpty();
        assertThat(knowledgeService.search(owner.id(), "Redis 缓存", 3))
                .isNotEmpty().allSatisfy(result -> assertThat(result.documentId()).isEqualTo(document.id()));
        assertThat(knowledgeService.search(other.id(), "Redis 缓存", 3)).isEmpty();
        assertThatThrownBy(() -> planService.create(other.id(), new SaveInterviewPlanCommand(
                "跨用户知识文档", "Java 工程师", "", InterviewDifficulty.MEDIUM,
                30, 5, null, null, List.of(document.id()), Map.of(), null)))
                .isInstanceOf(BusinessException.class);

        Path unselectedSource = applicationHome.resolve("mysql-notes.md");
        Files.writeString(unselectedSource, "MySQL 联合索引需要关注最左匹配原则。\n".repeat(8));
        var unselected = knowledgeService.uploadAndIndex(owner.id(), unselectedSource, "数据库资料");
        var plan = planService.create(owner.id(), new SaveInterviewPlanCommand(
                "知识范围面试", "Java 工程师", "", InterviewDifficulty.MEDIUM,
                30, 5, null, null, List.of(document.id()), Map.of(), null));
        var session = sessionService.create(owner.id(), plan.id());
        assertThat(session.knowledgeSnapshot()).extracting(snapshot -> snapshot.id())
                .containsExactly(document.id());

        var toolResult = toolRegistry.find("knowledge_search").orElseThrow()
                .execute(new ToolInput(owner.id(), session.id(), Map.of("query", "Redis 缓存", "limit", 5)));
        assertThat(toolResult.success()).isTrue();
        @SuppressWarnings("unchecked")
        var scopedResults = (java.util.List<Map<String, Object>>) toolResult.data().get("results");
        assertThat(scopedResults).isNotEmpty()
                .allSatisfy(item -> assertThat(item.get("documentId")).isEqualTo(document.id()));
        assertThat(scopedResults).allSatisfy(item -> assertThat(item.get("chunkIndex")).isInstanceOf(Number.class));

        chatService.enqueueStream(Flux.just("Redis 缓存如何避免穿透？"));
        agentService.generateInitialQuestion(owner.id(), session.id()).collectList().block();
        chatService.enqueueChat("""
                {"correctness":80,"depth":72,"missingPoints":["故障恢复"],"feedback":"方向正确"}
                """);
        chatService.enqueueChat("""
                {"action":"FOLLOW_UP","nextStage":null,"reason":"继续验证缓存设计"}
                """);
        chatService.enqueueStream(Flux.just("请结合知识资料说明缓存穿透保护的边界条件。"));
        agentService.answer(owner.id(), session.id(), "Redis 可以使用空值缓存和布隆过滤器。")
                .collectList().block();
        var citedQuestion = sessionService.messages(owner.id(), session.id()).getLast();
        assertThat(citedQuestion.citations()).isNotEmpty().allSatisfy(citation -> {
            assertThat(citation.documentId()).isEqualTo(document.id());
            assertThat(citation.documentName()).isEqualTo(document.name());
            assertThat(citation.excerpt()).contains("Redis");
        });
        assertThatThrownBy(() -> sessionService.saveAssistantOutput(
                owner.id(), session.id(), "非法来源问题", null, false,
                List.of(new KnowledgeCitationDto(
                        unselected.id(), unselected.name(), 0, "不属于会话范围", 0.8))))
                .isInstanceOf(BusinessException.class);

        planService.update(owner.id(), plan.id(), new SaveInterviewPlanCommand(
                "知识范围已修改", "Java 工程师", "", InterviewDifficulty.MEDIUM,
                30, 5, null, null, List.of(), Map.of(), null));
        assertThat(sessionService.require(owner.id(), session.id()).knowledgeSnapshot())
                .extracting(snapshot -> snapshot.id()).containsExactly(document.id());
        assertThat(toolRegistry.find("knowledge_search").orElseThrow()
                .execute(new ToolInput(other.id(), session.id(), Map.of("query", "Redis"))).success())
                .isFalse();

        var noKnowledgePlan = planService.create(owner.id(), new SaveInterviewPlanCommand(
                "无知识范围", "Java 工程师", "", InterviewDifficulty.MEDIUM,
                30, 5, null, Map.of(), null));
        var noKnowledgeSession = sessionService.create(owner.id(), noKnowledgePlan.id());
        assertThat(toolRegistry.find("knowledge_search").orElseThrow()
                .execute(new ToolInput(owner.id(), noKnowledgeSession.id(), Map.of("query", "Redis"))).success())
                .isFalse();

        var deletionPlan = planService.create(owner.id(), new SaveInterviewPlanCommand(
                "删除解绑验证", "Java 工程师", "", InterviewDifficulty.MEDIUM,
                30, 5, null, null, List.of(document.id()), Map.of(), null));
        knowledgeService.delete(owner.id(), document.id());
        assertThat(planService.require(deletionPlan.id(), owner.id()).knowledgeDocumentIds()).isEmpty();
        assertThat(knowledgeService.list(owner.id())).extracting(item -> item.id())
                .containsExactly(unselected.id());
        assertThat(knowledgeService.search(owner.id(), "Redis 缓存", 3))
                .extracting(result -> result.documentId()).doesNotContain(document.id());
        assertThat(sessionService.messages(owner.id(), session.id()).getLast().citations())
                .isNotEmpty().allSatisfy(citation -> assertThat(citation.documentId()).isEqualTo(document.id()));
    }

    @Test
    void queuesDocumentProcessingAndCompletesItThroughPersistentWorkerContract() throws Exception {
        var owner = userService.register("queued-knowledge-owner", "Queue Owner", "safe-password");
        Path source = applicationHome.resolve("queued-notes.txt");
        Files.writeString(source, "Java 虚拟线程适合高并发阻塞式 I/O 场景。\n".repeat(12));

        var queued = knowledgeTaskService.uploadAndEnqueue(owner.id(), source, "并发资料");
        assertThat(queued.document().status()).isEqualTo(KnowledgeStatus.UPLOADED);
        assertThat(backgroundTaskService.executeNext("knowledge-test-worker")).isTrue();

        assertThat(backgroundTaskService.require(owner.id(), queued.taskId()).getStatus().name())
                .isEqualTo("SUCCESS");
        assertThat(knowledgeService.detail(owner.id(), queued.document().id()).document().status())
                .isEqualTo(KnowledgeStatus.READY);
        assertThat(knowledgeService.search(owner.id(), "虚拟线程", 3)).isNotEmpty();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FakeEmbeddingConfiguration {
        @Bean
        @Primary
        EmbeddingService fakeEmbeddingService() {
            return text -> text.toLowerCase().contains("redis")
                    ? new float[]{1, 0, 0}
                    : new float[]{0, 1, 0};
        }

        @Bean
        @Primary
        FakeChatService fakeChatService() {
            return new FakeChatService();
        }
    }

    static class FakeChatService implements ChatService {
        private final Queue<String> chats = new ArrayDeque<>();
        private final Queue<Flux<String>> streams = new ArrayDeque<>();

        synchronized void enqueueChat(String response) { chats.add(response); }
        synchronized void enqueueStream(Flux<String> response) { streams.add(response); }

        @Override
        public synchronized String chat(String prompt) {
            if (prompt.contains("候选人主张提取器")) {
                return """
                        {"claims":[{"type":"DECISION","content":"使用空值缓存和布隆过滤器防止缓存穿透",
                        "importance":0.9,"credibility":0.75,"missingEvidence":["边界条件"]}]}
                        """;
            }
            return chats.remove();
        }

        @Override
        public synchronized Flux<String> stream(String prompt) { return streams.remove(); }

        synchronized void clear() {
            chats.clear();
            streams.clear();
        }
    }
}
