package com.inin.aiinterviewer.domain.model;

import java.util.List;

public record CandidateProfileContent(
        String fullName,
        String targetRole,
        String yearsExperience,
        String education,
        List<String> skills,
        List<String> projects,
        List<String> experience,
        List<String> strengths,
        List<String> risks,
        String summary
) {
    public CandidateProfileContent {
        fullName = value(fullName);
        targetRole = value(targetRole);
        yearsExperience = value(yearsExperience);
        education = value(education);
        skills = copy(skills);
        projects = copy(projects);
        experience = copy(experience);
        strengths = copy(strengths);
        risks = copy(risks);
        summary = value(summary);
    }

    private static String value(String value) {
        return value == null ? "" : value.strip();
    }

    private static List<String> copy(List<String> values) {
        return values == null ? List.of() : values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::strip)
                .toList();
    }
}

