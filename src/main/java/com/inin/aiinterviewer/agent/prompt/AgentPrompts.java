package com.inin.aiinterviewer.agent.prompt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inin.aiinterviewer.agent.state.InterviewGraphState;
import com.inin.aiinterviewer.application.exception.ErrorCode;
import com.inin.aiinterviewer.application.exception.SystemException;
import com.inin.aiinterviewer.domain.model.Message;

import java.util.List;

public final class AgentPrompts {

    private AgentPrompts() {
    }

    public static String claimExtraction(InterviewGraphState state) {
        return """
                你是技术面试中的“候选人主张提取器”。仅从候选人本轮回答中提取可在后续追问中验证的原子主张，
                不得补充候选人没有表达的事实。每条主张只能表达一个事实、指标、责任、因果、决策、结果、约束、失败或观点。
                必须只返回一个 JSON 对象，不要 Markdown 代码块，不要额外说明：
                {"claims":[{"type":"FACT|METRIC|OWNERSHIP|CAUSALITY|DECISION|RESULT|CONSTRAINT|FAILURE|OPINION",
                "content":"可独立验证的简洁主张","importance":0.0到1.0,"credibility":0.0到1.0,
                "missingEvidence":["仍需确认的证据"]}]}
                最多返回 12 条；没有可验证主张时返回 {"claims":[]}。
                importance 表示对岗位判断的重要程度，credibility 仅表示当前回答自身的可置信程度，不等于最终评分。

                当前阶段：%s
                当前问题：%s
                候选人回答：%s
                已冻结领域包：%s
                当前待验证主张账本：%s
                """.formatted(state.stage(), state.currentQuestion(), state.answer(),
                state.domainPackContext(), state.claimLedgerContext());
    }

    public static String repairClaimExtraction(InterviewGraphState state, String invalidResponse) {
        String response = invalidResponse == null ? "" : invalidResponse;
        if (response.length() > 8_000) response = response.substring(0, 8_000);
        return """
                你是 JSON 格式修复器。下面是一次无效的“候选人主张提取”结果。
                请根据候选人原回答重新生成，必须只返回严格 JSON 对象，不要解释，不要 Markdown：
                {"claims":[{"type":"FACT|METRIC|OWNERSHIP|CAUSALITY|DECISION|RESULT|CONSTRAINT|FAILURE|OPINION",
                "content":"非空字符串","importance":0.0到1.0,"credibility":0.0到1.0,
                "missingEvidence":["字符串"]}]}
                最多 12 条，没有主张则返回 {"claims":[]}。

                候选人原回答：%s
                无效结果：%s
                """.formatted(state.answer(), response);
    }

    public static String logicChainEvaluation(InterviewGraphState state) {
        return """
                你是技术面试回答的逻辑链评估器。只分析候选人本轮明确表达的内容，不得补写事实。
                将重要回答拆成前提、问题判断、备选方案、决策、选择依据、执行动作、作用机制、结果、验证和反思。
                必须只返回严格 JSON，不要 Markdown 或解释：
                {"premises":["前提"],"problemDiagnosis":"问题判断或空字符串","alternatives":["备选方案"],
                "decision":"最终决策或空字符串","reasoning":"选择依据和作用机制或空字符串",
                "actions":["执行动作"],"outcome":"结果或空字符串","validation":"验证方式或空字符串",
                "reflection":"反思改进或空字符串","gaps":[{"type":"MISSING_BASELINE|MISSING_MECHANISM|
                MISSING_EXECUTION_PATH|MISSING_ALTERNATIVES|MISSING_TRADE_OFF|MISSING_VALIDATION|
                MISSING_PERSONAL_CONTRIBUTION|MISSING_FAILURE_HANDLING|CAUSALITY_JUMP|RESULT_WITHOUT_EVIDENCE",
                "description":"具体缺口","severity":0.0到1.0,"relatedClaimIds":["相关主张ID"]}]}
                relatedClaimIds 只能使用下方账本中已存在的 ID；无法关联时返回空数组。最多 12 个缺口。

                当前问题：%s
                候选人回答：%s
                本轮主张：%s
                主张账本：%s
                已冻结领域包：%s
                """.formatted(state.currentQuestion(), state.answer(), state.claimExtraction(),
                state.claimLedgerContext(), state.domainPackContext());
    }

    public static String repairLogicChainEvaluation(InterviewGraphState state, String invalidResponse) {
        String response = invalidResponse == null ? "" : invalidResponse;
        if (response.length() > 8_000) response = response.substring(0, 8_000);
        return """
                你是 JSON 格式修复器。重新分析回答并只输出严格 JSON；所有列表不可为 null，文本缺失时使用空字符串，
                severity 必须在 0.0 到 1.0，gap type 必须来自指定枚举，禁止额外字段说明。
                必需字段：premises, problemDiagnosis, alternatives, decision, reasoning, actions, outcome,
                validation, reflection, gaps；每个 gap 必需 type, description, severity, relatedClaimIds。

                候选人回答：%s
                无效结果：%s
                """.formatted(state.answer(), response);
    }

    public static String analysis(InterviewGraphState state) {
        return """
                你是严谨的技术面试回答分析器。只基于问题和回答评分，不补充候选人没有表达的事实。
                必须只返回一个 JSON 对象，不要 Markdown 代码块：
                {"correctness":0到100整数,"depth":0到100整数,"missingPoints":["缺失点"],"feedback":"简洁反馈"}

                当前阶段：%s
                面试问题：%s
                候选人回答：%s
                """.formatted(state.stage(), state.currentQuestion(), state.answer());
    }

    public static String decision(InterviewGraphState state, ObjectMapper objectMapper) {
        return """
                你是受规则约束的技术面试流程决策器。根据回答分析选择追问或请求进入下一阶段。
                必须只返回一个 JSON 对象，不要 Markdown 代码块：
                {"action":"FOLLOW_UP或NEXT_STAGE","nextStage":"阶段枚举或null","reason":"简短原因"}
                可用阶段枚举：INTRODUCTION, RESUME_REVIEW, PROJECT_EXPERIENCE, TECHNICAL_DEEP_DIVE,
                SYSTEM_DESIGN, CODING, BEHAVIORAL, SUMMARY, COMPLETED。
                你不能创建阶段；阶段请求仍会由程序规则二次校验。

                当前阶段：%s
                回答分析：%s
                方案阶段：%s
                题目上限：%d
                """.formatted(state.stage(), json(objectMapper, state.analysis()),
                state.plan().stages(), state.plan().questionCount());
    }

    public static String question(InterviewGraphState state, ObjectMapper objectMapper) {
        List<Message> recent = state.messages().size() <= 8
                ? state.messages()
                : state.messages().subList(state.messages().size() - 8, state.messages().size());
        String intent = state.data().containsKey(InterviewGraphState.ANALYSIS)
                ? "严格按照结构化追问计划渲染下一题。上一轮分析："
                    + json(objectMapper, state.analysis())
                : "这是本场面试的第一题。";
        return """
                你是问题语言渲染器，不负责改变面试策略。一次只提出一个清晰的中文问题，不给答案，不输出 JSON。
                当追问计划含 targetClaimId 或 targetLogicGap 时，问题必须直接围绕该目标及 expectedEvidence，禁止改成通用知识题；
                当 targetClaimId 为空时，围绕计划中的阶段目标提出该阶段首题。不要暴露内部 ID、评分、可信度或策略枚举。
                %s

                结构化追问计划：%s
                当前阶段：%s
                目标岗位：%s
                岗位描述：%s
                难度：%s
                重点规则：%s
                已确认候选人画像快照：%s
                已冻结领域包：%s
                本轮提取的候选人主张：%s
                当前待验证主张账本：%s
                较早对话摘要：%s
                可参考的用户私有知识片段：%s
                最近对话：%s
                """.formatted(intent, json(objectMapper, state.probePlan()),
                state.stage(), state.plan().jobTitle(), state.plan().jobDescription(),
                state.plan().difficulty(), json(objectMapper, state.plan().rules()),
                state.candidateProfileContext(), state.domainPackContext(),
                json(objectMapper, state.claimExtraction()), state.claimLedgerContext(), state.summary(),
                state.retrievedContext(), json(objectMapper, recent));
    }

    private static String json(ObjectMapper objectMapper, Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new SystemException(ErrorCode.SYSTEM_ERROR, exception);
        }
    }
}
