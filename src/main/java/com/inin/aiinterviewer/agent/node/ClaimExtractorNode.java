package com.inin.aiinterviewer.agent.node;

import com.inin.aiinterviewer.agent.model.ClaimExtractionResult;
import com.inin.aiinterviewer.agent.prompt.AgentPrompts;
import com.inin.aiinterviewer.agent.state.InterviewGraphState;
import com.inin.aiinterviewer.agent.support.StructuredAiResponseParser;
import com.inin.aiinterviewer.infrastructure.ai.ChatService;
import org.bsc.langgraph4j.action.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ClaimExtractorNode implements NodeAction<InterviewGraphState> {

    private static final Logger log = LoggerFactory.getLogger(ClaimExtractorNode.class);
    private static final int MAX_CLAIMS = 12;
    private static final int MAX_CONTENT_LENGTH = 1_000;
    private static final int MAX_MISSING_EVIDENCE = 10;

    private final ChatService chatService;
    private final StructuredAiResponseParser parser;

    public ClaimExtractorNode(ChatService chatService, StructuredAiResponseParser parser) {
        this.chatService = chatService;
        this.parser = parser;
    }

    @Override
    public Map<String, Object> apply(InterviewGraphState state) {
        if (state.data().containsKey(InterviewGraphState.CLAIM_EXTRACTION)) {
            return output(state.claimExtraction());
        }
        String response = "";
        RuntimeException firstFailure;
        try {
            response = chatService.chat(AgentPrompts.claimExtraction(state));
            return output(validate(parser.parse(response, ClaimExtractionResult.class)));
        } catch (RuntimeException exception) {
            firstFailure = exception;
        }

        try {
            String repaired = chatService.chat(AgentPrompts.repairClaimExtraction(state, response));
            return output(validate(parser.parse(repaired, ClaimExtractionResult.class)));
        } catch (RuntimeException repairFailure) {
            log.warn("Claim extraction degraded after one repair attempt: first={}, repair={}",
                    firstFailure.getClass().getSimpleName(), repairFailure.getClass().getSimpleName());
            return output(ClaimExtractionResult.degraded("claim_extraction_failed"));
        }
    }

    private Map<String, Object> output(ClaimExtractionResult result) {
        return Map.of(InterviewGraphState.CLAIM_EXTRACTION, result);
    }

    private ClaimExtractionResult validate(ClaimExtractionResult result) {
        if (result == null || result.degraded() || result.claims().size() > MAX_CLAIMS) {
            throw new IllegalArgumentException("Invalid claim extraction envelope");
        }
        LinkedHashMap<String, ClaimExtractionResult.ClaimCandidate> unique = new LinkedHashMap<>();
        for (ClaimExtractionResult.ClaimCandidate candidate : result.claims()) {
            if (candidate == null || candidate.type() == null || candidate.content() == null
                    || candidate.content().isBlank() || candidate.content().length() > MAX_CONTENT_LENGTH
                    || !Double.isFinite(candidate.importance()) || candidate.importance() < 0
                    || candidate.importance() > 1 || !Double.isFinite(candidate.credibility())
                    || candidate.credibility() < 0 || candidate.credibility() > 1
                    || candidate.missingEvidence().size() > MAX_MISSING_EVIDENCE) {
                throw new IllegalArgumentException("Invalid extracted claim");
            }
            List<String> missingEvidence = candidate.missingEvidence().stream()
                    .filter(value -> value != null && !value.isBlank())
                    .map(String::strip)
                    .distinct()
                    .toList();
            ClaimExtractionResult.ClaimCandidate normalized = new ClaimExtractionResult.ClaimCandidate(
                    candidate.type(), candidate.content().strip(), candidate.importance(),
                    candidate.credibility(), missingEvidence);
            unique.putIfAbsent(normalized.content(), normalized);
        }
        return new ClaimExtractionResult(List.copyOf(unique.values()));
    }
}
