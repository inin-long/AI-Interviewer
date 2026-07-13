package com.inin.aiinterviewer.agent.tool;

import com.inin.aiinterviewer.application.service.InterviewSessionService;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class CandidateProfileTool implements AgentTool {

    private final InterviewSessionService sessionService;

    public CandidateProfileTool(InterviewSessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Override
    public String name() {
        return "candidate_profile_get";
    }

    @Override
    public String description() {
        return "读取当前面试会话创建时冻结的已确认候选人画像快照";
    }

    @Override
    public ToolResult execute(ToolInput input) {
        try {
            return sessionService.profileSnapshot(input.userId(), input.sessionId())
                    .map(profile -> {
                        var content = profile.content();
                        Map<String, Object> data = new LinkedHashMap<>();
                        data.put("profileId", profile.id());
                        data.put("resumeId", profile.resumeId());
                        data.put("fullName", content.fullName());
                        data.put("targetRole", content.targetRole());
                        data.put("yearsExperience", content.yearsExperience());
                        data.put("education", content.education());
                        data.put("skills", content.skills());
                        data.put("projects", content.projects());
                        data.put("experience", content.experience());
                        data.put("strengths", content.strengths());
                        data.put("risks", content.risks());
                        data.put("summary", content.summary());
                        return ToolResult.success(data);
                    })
                    .orElseGet(() -> ToolResult.failure("candidate profile is not associated"));
        } catch (RuntimeException exception) {
            return ToolResult.failure("candidate profile unavailable");
        }
    }
}
