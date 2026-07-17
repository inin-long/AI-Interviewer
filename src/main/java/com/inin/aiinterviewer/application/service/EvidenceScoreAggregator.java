package com.inin.aiinterviewer.application.service;

import com.inin.aiinterviewer.agent.model.EvaluationPayload;
import com.inin.aiinterviewer.domain.enums.ConsistencyIssueStatus;
import com.inin.aiinterviewer.domain.enums.EvidenceSignal;
import com.inin.aiinterviewer.domain.model.ClaimLedger;
import com.inin.aiinterviewer.domain.model.EvaluationEvidence;
import com.inin.aiinterviewer.domain.model.EvidenceLedger;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

@Component
public class EvidenceScoreAggregator {

    public static final String TECHNICAL = "technical";
    public static final String PROBLEM_SOLVING = "problemSolving";
    public static final String PROJECT = "project";
    public static final String SYSTEM_DESIGN = "systemDesign";
    public static final String COMMUNICATION = "communication";
    public static final String COMPREHENSIVE = "comprehensive";
    public static final String OVERALL = "overall";

    public EvaluationPayload aggregate(
            EvidenceLedger ledger,
            ClaimLedger claims,
            String narrative
    ) {
        List<EvaluationEvidence> evidence = ledger == null ? List.of() : ledger.evidence();
        ClaimLedger safeClaims = claims == null ? ClaimLedger.empty() : claims;
        LinkedHashMap<String, DimensionScore> scores = new LinkedHashMap<>();
        scores.put(TECHNICAL, dimension(evidence, safeClaims, this::technical));
        scores.put(PROBLEM_SOLVING, dimension(evidence, safeClaims, this::problemSolving));
        scores.put(PROJECT, dimension(evidence, safeClaims, this::project));
        scores.put(SYSTEM_DESIGN, dimension(evidence, safeClaims, this::systemDesign));
        scores.put(COMMUNICATION, dimension(evidence, safeClaims, this::communication));
        scores.put(COMPREHENSIVE, dimension(evidence, safeClaims, ignored -> true));

        List<DimensionScore> primary = List.of(
                scores.get(TECHNICAL), scores.get(PROBLEM_SOLVING), scores.get(PROJECT),
                scores.get(SYSTEM_DESIGN), scores.get(COMMUNICATION)).stream()
                .filter(DimensionScore::scored).toList();
        boolean overallScored = !primary.isEmpty();
        int overall = overallScored
                ? (int) Math.round(primary.stream().mapToInt(DimensionScore::score).average().orElse(50))
                : 50;
        double overallConfidence = primary.stream()
                .mapToDouble(DimensionScore::confidence).average().orElse(0);
        List<EvaluationEvidence> overallEvidence = primary.stream()
                .flatMap(value -> value.evidence().stream()).distinct().toList();

        LinkedHashMap<String, EvaluationPayload.EvidenceTrace> traces = new LinkedHashMap<>();
        scores.forEach((key, value) -> traces.put(key, trace(value)));
        traces.put(OVERALL, trace(new DimensionScore(
                overall, overallScored, overallConfidence, overallEvidence,
                overallScored ? "综合分仅由已有逐轮证据的固定能力维度等权汇总"
                        : "没有形成可用于综合评分的正向或负向证据")));
        return new EvaluationPayload(
                overall,
                scores.get(TECHNICAL).score(),
                scores.get(PROBLEM_SOLVING).score(),
                scores.get(PROJECT).score(),
                scores.get(SYSTEM_DESIGN).score(),
                scores.get(COMMUNICATION).score(),
                scores.get(COMPREHENSIVE).score(),
                narrative,
                traces,
                overallConfidence,
                overallScored);
    }

    private DimensionScore dimension(
            List<EvaluationEvidence> all,
            ClaimLedger claims,
            Predicate<String> matcher
    ) {
        List<EvaluationEvidence> relevant = all.stream()
                .filter(value -> matcher.test(value.competencyCode().toUpperCase(Locale.ROOT)))
                .toList();
        List<EvaluationEvidence> decisive = relevant.stream()
                .filter(value -> value.signal() == EvidenceSignal.POSITIVE
                        || value.signal() == EvidenceSignal.NEGATIVE)
                .toList();
        if (decisive.isEmpty()) {
            return new DimensionScore(
                    50, false, 0, relevant,
                    relevant.isEmpty() ? "未获得该维度证据，不作能力结论"
                            : "只有证据不足或中性信号，不作能力强弱结论");
        }
        double signed = decisive.stream().mapToDouble(value -> {
            double direction = value.signal() == EvidenceSignal.POSITIVE ? 1 : -1;
            return direction * value.strength() * value.confidence();
        }).average().orElse(0);
        int confirmedConflicts = (int) claims.issues().stream()
                .filter(issue -> issue.status() == ConsistencyIssueStatus.CONFIRMED_CONFLICT)
                .filter(issue -> decisive.stream().anyMatch(value -> value.relatedClaimIds().stream()
                        .anyMatch(issue.relatedClaimIds()::contains)))
                .count();
        int score = boundedScore((int) Math.round(50 + signed * 50) - Math.min(15, confirmedConflicts * 5));
        double averageConfidence = decisive.stream()
                .mapToDouble(EvaluationEvidence::confidence).average().orElse(0);
        double breadth = 1.0 - Math.exp(-decisive.size() / 2.0);
        double insufficiencyRatio = relevant.isEmpty() ? 0
                : relevant.stream().filter(value -> value.signal() == EvidenceSignal.INSUFFICIENT).count()
                / (double) relevant.size();
        double confidence = averageConfidence * breadth * (1 - insufficiencyRatio * 0.5);
        String rationale = "由 " + decisive.size() + " 条正向/负向逐轮证据汇总"
                + (confirmedConflicts == 0 ? "" : "，并扣除已确认矛盾影响");
        return new DimensionScore(score, true, confidence, relevant, rationale);
    }

    private EvaluationPayload.EvidenceTrace trace(DimensionScore value) {
        LinkedHashSet<String> claimIds = new LinkedHashSet<>();
        value.evidence().forEach(item -> claimIds.addAll(item.relatedClaimIds()));
        return new EvaluationPayload.EvidenceTrace(
                value.scored(), value.confidence(),
                value.evidence().stream().map(EvaluationEvidence::id).distinct().toList(),
                value.evidence().stream().map(EvaluationEvidence::messageId).distinct().toList(),
                List.copyOf(claimIds), value.rationale());
    }

    private boolean technical(String code) {
        return contains(code, "JAVA", "DATA", "DISTRIBUTED", "SYSTEM", "FRONTEND",
                "BACKEND", "DELIVERY", "TECHNICAL", "ENGINEERING", "JVM", "DATABASE");
    }

    private boolean problemSolving(String code) {
        return contains(code, "PROBLEM", "METRIC", "USER_INSIGHT", "PRIORITIZATION",
                "DECISION", "DIAGNOSIS", "FAILURE", "TROUBLESHOOT");
    }

    private boolean project(String code) {
        return contains(code, "PROJECT", "DELIVERY", "OWNERSHIP", "EXPERIENCE", "EXECUTION");
    }

    private boolean systemDesign(String code) {
        return contains(code, "SYSTEM", "ARCHITECT", "DISTRIBUTED");
    }

    private boolean communication(String code) {
        return contains(code, "COMMUNICATION", "COLLABORATION", "STAKEHOLDER", "TEAMWORK");
    }

    private boolean contains(String code, String... fragments) {
        for (String fragment : fragments) if (code.contains(fragment)) return true;
        return false;
    }

    private int boundedScore(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private record DimensionScore(
            int score,
            boolean scored,
            double confidence,
            List<EvaluationEvidence> evidence,
            String rationale
    ) {
        private DimensionScore {
            evidence = evidence == null ? List.of() : List.copyOf(evidence);
        }
    }
}
