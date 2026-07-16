package com.inin.aiinterviewer.application.service;

import com.inin.aiinterviewer.agent.model.EvidenceCollectionResult;
import com.inin.aiinterviewer.domain.enums.EvidenceSignal;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class CollaborationEvidenceCollector {

    public static final String COMPETENCY_CODE = "COLLABORATION_ADAPTABILITY";

    public EvidenceCollectionResult enrich(String answer, EvidenceCollectionResult source) {
        List<EvidenceCollectionResult.EvidenceCandidate> evidence = new ArrayList<>();
        if (source != null) evidence.addAll(source.evidence());
        String normalized = answer == null ? "" : answer.toLowerCase(Locale.ROOT);
        addIfMatched(evidence, normalized, "ACTIVE_CLARIFICATION", EvidenceSignal.POSITIVE, 0.64,
                List.of("我想确认", "请问这里", "你的意思是", "是否可以理解为", "这里指的是"),
                "主动澄清问题边界");
        addIfMatched(evidence, normalized, "ACKNOWLEDGES_UNCERTAINTY", EvidenceSignal.POSITIVE, 0.72,
                List.of("我不确定", "信息不足", "暂时无法判断", "还需要更多信息", "目前不能确认"),
                "明确承认信息不足并保留判断");
        addIfMatched(evidence, normalized, "REVISES_VIEW", EvidenceSignal.POSITIVE, 0.78,
                List.of("我会修正", "调整我的观点", "刚才的说法不准确", "重新考虑后", "我收回刚才"),
                "基于新信息修正原有观点");
        addIfMatched(evidence, normalized, "INTEGRATES_OPPOSITION", EvidenceSignal.POSITIVE, 0.70,
                List.of("考虑这个反对意见", "吸收这个意见", "结合你的反馈", "这个质疑有道理", "接受这个建议"),
                "能够吸收反对意见并调整方案");
        addIfMatched(evidence, normalized, "UNCRITICAL_AGREEMENT", EvidenceSignal.NEGATIVE, 0.70,
                List.of("你说得都对", "我完全同意你说的一切", "无条件同意", "你说什么都对"),
                "在缺少依据时表现出迎合倾向");
        if (containsAny(normalized, List.of("我不同意", "我仍然认为", "我会坚持", "我不赞同"))
                && containsAny(normalized, List.of("因为", "依据", "数据", "证据", "约束"))) {
            add(evidence, "EVIDENCE_BASED_DISSENT", EvidenceSignal.POSITIVE, 0.76,
                    "能基于依据表达不同意见");
        }
        addIfMatched(evidence, normalized, "JOINT_PROBLEM_SOLVING", EvidenceSignal.POSITIVE, 0.66,
                List.of("我们可以一起", "先对齐", "共同推进", "和相关团队一起", "拉齐相关方"),
                "主动提出与他人共同推进问题");
        return source != null && source.degraded() && evidence.isEmpty()
                ? source : new EvidenceCollectionResult(List.copyOf(evidence));
    }

    private void addIfMatched(
            List<EvidenceCollectionResult.EvidenceCandidate> evidence,
            String answer,
            String observation,
            EvidenceSignal signal,
            double confidence,
            List<String> patterns,
            String reason
    ) {
        if (containsAny(answer, patterns)) add(evidence, observation, signal, confidence, reason);
    }

    private void add(
            List<EvidenceCollectionResult.EvidenceCandidate> evidence,
            String observation,
            EvidenceSignal signal,
            double confidence,
            String reason
    ) {
        evidence.add(new EvidenceCollectionResult.EvidenceCandidate(
                COMPETENCY_CODE, signal, signal == EvidenceSignal.NEGATIVE ? 0.62 : 0.68,
                confidence, "[" + observation + "] " + reason, List.of()));
    }

    private boolean containsAny(String value, List<String> patterns) {
        return patterns.stream().anyMatch(value::contains);
    }
}
