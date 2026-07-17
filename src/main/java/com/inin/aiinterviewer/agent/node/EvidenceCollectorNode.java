package com.inin.aiinterviewer.agent.node;

import com.inin.aiinterviewer.agent.model.EvidenceCollectionResult;
import com.inin.aiinterviewer.agent.prompt.AgentPrompts;
import com.inin.aiinterviewer.agent.state.InterviewGraphState;
import com.inin.aiinterviewer.agent.support.StructuredAiResponseParser;
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
public class EvidenceCollectorNode implements NodeAction<InterviewGraphState> {

    private static final Logger log = LoggerFactory.getLogger(EvidenceCollectorNode.class);

    private final ChatService chatService;
    private final StructuredAiResponseParser parser;

    public EvidenceCollectorNode(ChatService chatService, StructuredAiResponseParser parser) {
        this.chatService = chatService;
        this.parser = parser;
    }

    @Override
    public Map<String, Object> apply(InterviewGraphState state) {
        if (state.data().containsKey(InterviewGraphState.EVIDENCE_COLLECTION_RESULT)) {
            return output(state.evidenceCollectionResult());
        }
        String response = "";
        RuntimeException firstFailure;
        try {
            response = chatService.chat(AgentPrompts.evidenceCollection(state));
            return output(validate(parser.parse(response, EvidenceCollectionResult.class)));
        } catch (RuntimeException exception) {
            firstFailure = exception;
        }
        try {
            String repaired = chatService.chat(AgentPrompts.repairEvidenceCollection(state, response));
            return output(validate(parser.parse(repaired, EvidenceCollectionResult.class)));
        } catch (RuntimeException repairFailure) {
            log.warn("Evidence collection degraded after one repair attempt: first={}, repair={}",
                    firstFailure.getClass().getSimpleName(), repairFailure.getClass().getSimpleName());
            return output(EvidenceCollectionResult.degraded("evidence_collection_failed"));
        }
    }

    private EvidenceCollectionResult validate(EvidenceCollectionResult result) {
        if (result == null || result.degraded() || result.evidence().isEmpty()
                || result.evidence().size() > 12) {
            throw new IllegalArgumentException("Invalid evidence collection envelope");
        }
        LinkedHashMap<String, EvidenceCollectionResult.EvidenceCandidate> unique = new LinkedHashMap<>();
        for (EvidenceCollectionResult.EvidenceCandidate candidate : result.evidence()) {
            if (candidate == null || candidate.signal() == null
                    || !candidate.competencyCode().matches("[A-Z][A-Z0-9_]{1,63}")
                    || !Double.isFinite(candidate.strength()) || candidate.strength() < 0
                    || candidate.strength() > 1 || !Double.isFinite(candidate.confidence())
                    || candidate.confidence() < 0 || candidate.confidence() > 1
                    || candidate.reason().isBlank() || candidate.reason().length() > 1_000
                    || candidate.relatedClaimIds().size() > 12) {
                throw new IllegalArgumentException("Invalid evaluation evidence");
            }
            LinkedHashSet<String> claimIds = new LinkedHashSet<>();
            for (String claimId : candidate.relatedClaimIds()) {
                if (claimId != null && !claimId.isBlank() && claimId.length() <= 128) {
                    claimIds.add(claimId.strip());
                }
            }
            var normalized = new EvidenceCollectionResult.EvidenceCandidate(
                    candidate.competencyCode(), candidate.signal(), candidate.strength(),
                    candidate.confidence(), candidate.reason(), List.copyOf(claimIds));
            unique.putIfAbsent(normalized.competencyCode() + "\u0000" + normalized.reason(), normalized);
        }
        if (unique.isEmpty()) throw new IllegalArgumentException("No valid evaluation evidence");
        return new EvidenceCollectionResult(List.copyOf(unique.values()));
    }

    private Map<String, Object> output(EvidenceCollectionResult result) {
        return Map.of(InterviewGraphState.EVIDENCE_COLLECTION_RESULT, result);
    }
}
