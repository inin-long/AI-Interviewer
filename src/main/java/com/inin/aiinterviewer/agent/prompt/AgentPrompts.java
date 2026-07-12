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
                ? "结合上一轮分析继续追问或进入新阶段后提出首题。上一轮分析："
                    + json(objectMapper, state.analysis())
                : "这是本场面试的第一题。";
        return """
                你是一名专业、克制的中文技术面试官。一次只提出一个清晰问题，不给答案，不输出 JSON。
                %s

                当前阶段：%s
                目标岗位：%s
                岗位描述：%s
                难度：%s
                重点规则：%s
                较早对话摘要：%s
                可参考的用户私有知识片段：%s
                最近对话：%s
                """.formatted(intent, state.stage(), state.plan().jobTitle(), state.plan().jobDescription(),
                state.plan().difficulty(), json(objectMapper, state.plan().rules()), state.summary(),
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
