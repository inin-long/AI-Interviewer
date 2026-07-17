package com.inin.aiinterviewer.agent.prompt;

import com.inin.aiinterviewer.domain.enums.InterviewMode;
import com.inin.aiinterviewer.domain.enums.InterviewerPersona;
import com.inin.aiinterviewer.domain.model.InterviewPlanSettings;

import java.util.Map;

public final class PersonaRenderer {

    private PersonaRenderer() {
    }

    public static String instructions(Map<String, Object> rules) {
        InterviewPlanSettings settings = InterviewPlanSettings.fromRules(rules);
        return instructions(settings.persona(), settings.mode());
    }

    static String instructions(InterviewerPersona persona, InterviewMode mode) {
        String voice = switch (persona) {
            case PROFESSIONAL_INTERVIEWER -> "专业、客观、简洁，使用中性的面试措辞";
            case FUTURE_PEER -> "以未来同事的合作视角自然交流，关注方案如何被团队复用";
            case TECH_LEAD -> "以技术负责人视角表达，关注落地、风险、收益依据和责任边界";
            case ARCHITECT -> "以架构师视角表达，关注系统边界、约束、演进与技术取舍";
            case INCIDENT_COMMANDER -> "以故障指挥者视角表达，冷静、明确，关注止损顺序和恢复依据";
            case PRODUCT_LEADER -> "以产品负责人视角表达，关注用户价值、指标、优先级和协作推进";
        };
        String modeInstruction = switch (mode) {
            case FORMAL_SIMULATION -> "保持正式模拟，不提供答案、遗漏提示或即时评分";
            case COACHING -> "保持教练式友好语气，但本题仍只提问，不直接给出答案";
            case SCENARIO_SIMULATION -> "保持情境沙盘的在场感，但不得创造导演未声明的事件";
        };
        return "当前 Persona 只控制表达方式，不得改变追问目标、事实、难度、评分标准或预期证据。"
                + "表达风格：" + voice + "。" + modeInstruction + "。";
    }
}
