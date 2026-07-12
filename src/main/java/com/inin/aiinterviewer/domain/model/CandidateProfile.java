package com.inin.aiinterviewer.domain.model;

import java.util.List;
import java.util.Map;

public record CandidateProfile(
        List<String> skills,
        List<Map<String, Object>> projects,
        List<Map<String, Object>> experience,
        Map<String, Object> education,
        String summary
) {
    public CandidateProfile {
        skills = skills == null ? List.of() : List.copyOf(skills);
        projects = projects == null ? List.of() : List.copyOf(projects);
        experience = experience == null ? List.of() : List.copyOf(experience);
        education = education == null ? Map.of() : Map.copyOf(education);
    }
}

