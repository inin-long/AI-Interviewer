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
            case FRIENDLY -> "语气温暖、鼓励、像愿意帮候选人发挥的搭档；多用“你接着说”“这个思路不错”这类让人放松的表达，营造安全的表达氛围";
            case SERIOUS -> "语气沉稳、客观、严谨，偏重事实与专业边界，不寒暄、不闲聊，直奔判断";
            case PRESSURE -> "语气带适度压迫感，会连续追问、挑战假设、要求当场权衡与决断，制造“被审视”的真实感，但不人身攻击、不侮辱";
            case TECHNICAL -> "以资深技术专家口吻表达，刨根问底原理、权衡与边界，关注实现细节与工程取舍";
            case MENTOR -> "以导师口吻表达，在追问的同时给方向性提示与思考框架，帮助候选人把思路理清";
            case HUMOROUS -> "语气轻松、偶有适度幽默，用生活化类比化解紧张，但专业标准不放松";
            case PROFESSIONAL_INTERVIEWER -> "以资深专业面试官口吻表达，语气稳重、结构化追问、关注岗位匹配度与能力证据，不寒暄、不跑题";
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
