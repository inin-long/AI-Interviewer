package com.inin.aiinterviewer.agent.prompt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inin.aiinterviewer.agent.state.InterviewGraphState;
import com.inin.aiinterviewer.application.exception.ErrorCode;
import com.inin.aiinterviewer.application.exception.SystemException;
import com.inin.aiinterviewer.domain.model.Message;

import java.util.List;
import java.util.Map;

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

    public static String evidenceCollection(InterviewGraphState state) {
        return """
                你是逐轮面试证据收集器。只基于候选人本轮回答、已提取主张和逻辑链，生成能力证据。
                证据不足必须标为 INSUFFICIENT，不能直接标为 NEGATIVE；strength 表示证据方向强度，
                confidence 表示该证据判断的可靠程度，两者必须分开。每轮至少返回一条证据。
                面试官 Persona 只影响问题表达，不得影响证据提取、能力映射、信号方向、强度或置信度。
                competencyCode 优先使用已冻结领域包中的能力 code；若无法匹配，使用 COMMUNICATION 或 PROBLEM_SOLVING。
                必须只返回严格 JSON，不要 Markdown 或解释：
                {"evidence":[{"competencyCode":"大写下划线代码","signal":"POSITIVE|NEGATIVE|NEUTRAL|INSUFFICIENT",
                "strength":0.0到1.0,"confidence":0.0到1.0,"reason":"本轮回答中的具体依据",
                "relatedClaimIds":["账本中真实存在的主张ID"]}]}
                最多 12 条。relatedClaimIds 不得编造；无法关联时返回空数组。

                当前阶段：%s
                当前问题：%s
                候选人回答：%s
                本轮主张：%s
                逻辑链：%s
                已冻结领域包：%s
                当前证据账本摘要：%s
                """.formatted(state.stage(), state.currentQuestion(), state.answer(),
                state.claimExtraction(), state.logicChainResult(), state.domainPackContext(),
                state.evidenceLedgerContext());
    }

    public static String repairEvidenceCollection(InterviewGraphState state, String invalidResponse) {
        String response = invalidResponse == null ? "" : invalidResponse;
        if (response.length() > 8_000) response = response.substring(0, 8_000);
        return """
                你是 JSON 格式修复器。根据候选人回答重新生成至少一条能力证据，只输出严格 JSON：
                {"evidence":[{"competencyCode":"UPPER_SNAKE_CASE","signal":"POSITIVE|NEGATIVE|NEUTRAL|INSUFFICIENT",
                "strength":0.0到1.0,"confidence":0.0到1.0,"reason":"非空具体依据","relatedClaimIds":[]}]}
                没有充分证据时使用 INSUFFICIENT，禁止用 NEGATIVE 代替证据不足。

                候选人回答：%s
                无效结果：%s
                """.formatted(state.answer(), response);
    }

    public static String consistencyCheck(InterviewGraphState state) {
        return """
                你是跨轮面试一致性检查器。只比较本轮主张、相关历史主张与待澄清问题。
                语义差异只能标记为潜在矛盾，不能认定候选人撒谎、欺骗、不诚实或作任何人格判断。
                新矛盾必须生成中性、具体的澄清问题；只有已经标为 CLARIFIED 的问题，才可根据本轮解释
                输出 RESOLVED 或 CONFIRMED_CONFLICT。无法确定时不要输出负面结论。
                必须只返回严格 JSON，不要 Markdown 或解释：
                {"issues":[{"issueId":"","type":"FACT_CONFLICT|TIMELINE_CONFLICT|OWNERSHIP_CONFLICT|TECHNOLOGY_CONFLICT|METRIC_CONFLICT|DECISION_PRINCIPLE_CONFLICT|VALUE_CONFLICT",
                "description":"两项陈述之间的客观差异","relatedClaimIds":["至少两个真实主张ID"],
                "clarificationQuestion":"引用两项说法并请候选人解释范围或条件","confidence":0.0到1.0}],
                "resolutions":[{"issueId":"已存在且处于CLARIFIED的矛盾ID","status":"RESOLVED|CONFIRMED_CONFLICT",
                "resolution":"候选人解释及客观判断","confidence":0.0到1.0}]}
                没有潜在矛盾或可处理澄清时返回空数组。不得编造主张 ID 或矛盾 ID。

                当前阶段：%s
                本轮回答：%s
                调度原因：%s
                本轮主张：%s
                相关历史主张：%s
                待澄清问题：%s
                """.formatted(state.stage(), state.answer(), state.consistencyContext().reason(),
                state.consistencyContext().currentClaims(),
                state.consistencyContext().historicalClaims(),
                state.consistencyContext().openIssues());
    }

    public static String repairConsistencyCheck(InterviewGraphState state, String invalidResponse) {
        String response = invalidResponse == null ? "" : invalidResponse;
        if (response.length() > 8_000) response = response.substring(0, 8_000);
        return """
                你是 JSON 格式修复器。重新检查跨轮陈述，只输出严格 JSON：
                {"issues":[],"resolutions":[]}
                如有潜在矛盾，issues 中必须含 type、description、至少两个真实 relatedClaimIds、
                中性 clarificationQuestion 和 0 到 1 的 confidence；issueId 留空。
                只有已处于 CLARIFIED 的问题才能输出 RESOLVED 或 CONFIRMED_CONFLICT。
                禁止人格判断，无法确定时返回空数组。

                调度上下文：%s
                本轮回答：%s
                无效结果：%s
                """.formatted(state.consistencyContext(), state.answer(), response);
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

    public static String scenarioDirection(InterviewGraphState state) {
        return """
                你是纯文本技术情境沙盘的 Scenario Director。候选人刚做出一项决策；你必须让该决策产生可解释的后果，
                再注入一个与目标能力直接相关的事件。不得随机刁难，不得改写已知事实，不得创建未声明的变量，
                不得在下一问题中泄露隐藏信息。压力只能来自真实资源、依赖、需求或协作约束，禁止侮辱、嘲讽或人格判断。
                必须只返回一个 JSON 对象，不要 Markdown 代码块：
                {"decisionAction":"对候选人实际行动的原子摘要",
                "decisionRationale":"该行动的决策依据",
                "eventType":"CONSTRAINT_CHANGE|RESOURCE_SHOCK|DEPENDENCY_FAILURE|TRAFFIC_SPIKE|REQUIREMENT_CHANGE|STAKEHOLDER_ESCALATION|RECOVERY_SIGNAL",
                "eventDescription":"由本轮决策触发或暴露的后果",
                "changes":{"仅使用当前 variables 已存在的键":"更新值"},
                "nextQuestion":"要求候选人处理后果的单个中性中文问题",
                "completeAfterEvent":false}

                场景完整内部状态（hiddenInformation 仅供导演推演，严禁向候选人泄露）：%s
                候选人本轮原始回答：%s
                当前结构化追问目标：%s
                当前压力状态：%s
                """.formatted(state.activeScenario(), state.answer(), state.probePlan(), state.pressureState());
    }

    public static String repairScenarioDirection(InterviewGraphState state, String response) {
        return """
                修复下面的 Scenario Director 输出。只返回一个符合指定字段的 JSON 对象。
                changes 必须非空且只能使用当前 variables 中已有键；事件必须由候选人本轮决策引起，
                nextQuestion 必须是一个中性、可回答且不泄露 hiddenInformation 的中文问题。

                当前场景：%s
                候选人回答：%s
                待修复输出：%s
                """.formatted(state.activeScenario(), state.answer(), response);
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
                当追问计划含 targetConsistencyIssueId 时，必须忠实使用 objective 中的中性澄清问题；
                当含 targetDeferredProbeId 时，必须围绕延迟验证的 targetClaimId 和 expectedEvidence 提问，
                当 shouldInjectScenario 为 true 时，必须忠实表达 objective 中的场景后果问题，不得另造事件或改变变量；
                压力只能来自证据要求、假设挑战、资源约束或故障事件；无论压力等级如何，都禁止侮辱、嘲讽、人身攻击、敌意否定或故意制造不可回答的问题。
                不得指控候选人撒谎或进行人格判断；当含 targetClaimId 或 targetLogicGap 时，问题必须直接围绕该目标及 expectedEvidence，禁止改成通用知识题；
                当 targetClaimId 为空时，围绕计划中的阶段目标提出该阶段首题。不要暴露内部 ID、评分、可信度或策略枚举。
                %s
                %s

                结构化追问计划：%s
                压力控制状态：%s
                当前场景公开状态：%s
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
                """.formatted(PersonaRenderer.instructions(state.plan().rules()), intent,
                json(objectMapper, state.probePlan()),
                json(objectMapper, state.pressureState()),
                json(objectMapper, publicScenario(state)),
                state.stage(), state.plan().jobTitle(), state.plan().jobDescription(),
                state.plan().difficulty(), json(objectMapper, state.plan().rules()),
                state.candidateProfileContext(), state.domainPackContext(),
                json(objectMapper, state.claimExtraction()), state.claimLedgerContext(), state.summary(),
                state.retrievedContext(), json(objectMapper, recent));
    }

    public static String regenerateQuestion(
            String originalPrompt,
            String rejectedQuestion,
            List<?> issues
    ) {
        return """
                上一次问题草稿未通过质量审查。只重新生成一次，仍然只能输出一个清晰的中文问题，
                必须保持原结构化追问目标、Persona 表达风格、当前阶段、岗位难度和场景事实不变；
                不得增加候选人未声明的信息、参考答案或无意义压力，也不得重复历史问题。

                质量问题：%s
                被拒绝的草稿：%s

                原问题渲染指令：
                %s
                """.formatted(issues, rejectedQuestion, originalPrompt);
    }

    private static Object publicScenario(InterviewGraphState state) {
        if (state.activeScenario() == null) return Map.of();
        var scenario = state.activeScenario();
        return Map.of(
                "id", scenario.id(),
                "type", scenario.type(),
                "objective", scenario.objective(),
                "candidateRole", scenario.candidateRole(),
                "knownFacts", scenario.knownFacts(),
                "assumptions", scenario.assumptions(),
                "variables", scenario.variables(),
                "constraints", scenario.constraints(),
                "currentRound", scenario.currentRound(),
                "maxRounds", scenario.maxRounds());
    }

    private static String json(ObjectMapper objectMapper, Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new SystemException(ErrorCode.SYSTEM_ERROR, exception);
        }
    }
}
