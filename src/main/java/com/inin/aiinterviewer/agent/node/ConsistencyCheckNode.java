package com.inin.aiinterviewer.agent.node;

import com.inin.aiinterviewer.agent.model.ConsistencyCheckResult;
import com.inin.aiinterviewer.agent.prompt.AgentPrompts;
import com.inin.aiinterviewer.agent.state.InterviewGraphState;
import com.inin.aiinterviewer.agent.support.StructuredAiResponseParser;
import com.inin.aiinterviewer.domain.enums.ConsistencyIssueStatus;
import com.inin.aiinterviewer.infrastructure.ai.ChatService;
import org.bsc.langgraph4j.action.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Component
public class ConsistencyCheckNode implements NodeAction<InterviewGraphState> {

    private static final Logger log = LoggerFactory.getLogger(ConsistencyCheckNode.class);
    private static final List<String> PERSONALITY_JUDGMENTS = List.of(
            "撒谎", "说谎", "不诚实", "人格", "欺骗", "liar", "dishonest");

    private final ChatService chatService;
    private final StructuredAiResponseParser parser;

    public ConsistencyCheckNode(ChatService chatService, StructuredAiResponseParser parser) {
        this.chatService = chatService;
        this.parser = parser;
    }

    @Override
    public Map<String, Object> apply(InterviewGraphState state) {
        if (state.data().containsKey(InterviewGraphState.CONSISTENCY_CHECK_RESULT)) {
            return output(state.consistencyCheckResult());
        }
        if (!state.consistencyContext().runRequested()) {
            return output(ConsistencyCheckResult.skipped(state.consistencyContext().reason()));
        }
        String response = "";
        RuntimeException firstFailure;
        try {
            response = chatService.chat(AgentPrompts.consistencyCheck(state));
            return output(validate(parser.parse(response, ConsistencyCheckResult.class)));
        } catch (RuntimeException exception) {
            firstFailure = exception;
        }
        try {
            String repaired = chatService.chat(AgentPrompts.repairConsistencyCheck(state, response));
            return output(validate(parser.parse(repaired, ConsistencyCheckResult.class)));
        } catch (RuntimeException repairFailure) {
            log.warn("Consistency check degraded after one repair attempt: first={}, repair={}",
                    firstFailure.getClass().getSimpleName(), repairFailure.getClass().getSimpleName());
            return output(ConsistencyCheckResult.degraded("consistency_check_failed"));
        }
    }

    private ConsistencyCheckResult validate(ConsistencyCheckResult result) {
        if (result == null || result.skipped() || result.degraded()
                || result.issues().size() > 8 || result.resolutions().size() > 8) {
            throw new IllegalArgumentException("Invalid consistency result envelope");
        }
        LinkedHashMap<String, ConsistencyCheckResult.IssueCandidate> issues = new LinkedHashMap<>();
        for (var candidate : result.issues()) {
            if (candidate == null || candidate.type() == null
                    || candidate.description().isBlank() || candidate.description().length() > 2_000
                    || candidate.clarificationQuestion().isBlank()
                    || candidate.clarificationQuestion().length() > 1_000
                    || !Double.isFinite(candidate.confidence()) || candidate.confidence() < 0
                    || candidate.confidence() > 1 || candidate.relatedClaimIds().size() < 2
                    || candidate.relatedClaimIds().size() > 12
                    || containsPersonalityJudgment(candidate.description())
                    || containsPersonalityJudgment(candidate.clarificationQuestion())) {
                throw new IllegalArgumentException("Invalid consistency issue");
            }
            LinkedHashSet<String> claimIds = new LinkedHashSet<>();
            candidate.relatedClaimIds().stream()
                    .filter(id -> id != null && !id.isBlank() && id.length() <= 128)
                    .map(String::strip).sorted().forEach(claimIds::add);
            if (claimIds.size() < 2) throw new IllegalArgumentException("Issue needs two claims");
            // 生成稳定的 issueId（按 类型+关联主张 派生），让 requiresClarification() 在图内可正确路由到澄清探针；
            // 落库由 ConsistencyIssueService.apply() 重新分配 UUID，互不冲突。
            String issueId = candidate.type() + ":" + String.join(",", claimIds);
            var normalized = new ConsistencyCheckResult.IssueCandidate(
                    issueId, candidate.type(), candidate.description(), List.copyOf(claimIds),
                    candidate.clarificationQuestion(), candidate.confidence());
            issues.putIfAbsent(candidate.type() + "\u0000" + String.join("\u0000", claimIds), normalized);
        }
        LinkedHashMap<String, ConsistencyCheckResult.ResolutionCandidate> resolutions = new LinkedHashMap<>();
        for (var candidate : result.resolutions()) {
            if (candidate == null || candidate.issueId().isBlank() || candidate.issueId().length() > 128
                    || (candidate.status() != ConsistencyIssueStatus.RESOLVED
                    && candidate.status() != ConsistencyIssueStatus.CONFIRMED_CONFLICT)
                    || candidate.resolution().isBlank() || candidate.resolution().length() > 2_000
                    || !Double.isFinite(candidate.confidence()) || candidate.confidence() < 0
                    || candidate.confidence() > 1
                    || containsPersonalityJudgment(candidate.resolution())) {
                throw new IllegalArgumentException("Invalid consistency resolution");
            }
            resolutions.putIfAbsent(candidate.issueId(), candidate);
        }
        return new ConsistencyCheckResult(List.copyOf(issues.values()), List.copyOf(resolutions.values()));
    }

    private boolean containsPersonalityJudgment(String value) {
        String normalized = value == null ? "" : value.toLowerCase(java.util.Locale.ROOT);
        return PERSONALITY_JUDGMENTS.stream().anyMatch(normalized::contains);
    }

    private Map<String, Object> output(ConsistencyCheckResult result) {
        return Map.of(InterviewGraphState.CONSISTENCY_CHECK_RESULT, result);
    }
}
