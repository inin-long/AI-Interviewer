package com.inin.aiinterviewer.application.service;

import com.inin.aiinterviewer.domain.model.CandidateProfileContent;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class LocalCandidateProfileExtractor {

    private static final List<String> KNOWN_SKILLS = List.of(
            "Java", "Spring Boot", "Spring Cloud", "Redis", "MySQL", "PostgreSQL",
            "SQLite", "MongoDB", "Kafka", "RabbitMQ", "Docker", "Kubernetes",
            "Linux", "Nginx", "Git", "Maven", "Gradle", "Vue", "React",
            "Python", "Go", "C++", "微服务", "分布式", "JVM", "SQL"
    );
    private static final Pattern YEARS = Pattern.compile("(\\d{1,2})\\s*年(?:工作|开发|项目)?经验");

    public CandidateProfileContent extract(String resumeText) {
        String lower = resumeText.toLowerCase(Locale.ROOT);
        List<String> skills = KNOWN_SKILLS.stream()
                .filter(skill -> lower.contains(skill.toLowerCase(Locale.ROOT)))
                .toList();
        Matcher matcher = YEARS.matcher(resumeText);
        String years = matcher.find() ? matcher.group(1) + " 年" : "";
        List<String> risks = new ArrayList<>();
        if (skills.isEmpty()) risks.add("尚未从简历文本识别出明确技术关键词");
        if (years.isBlank()) risks.add("工作年限需要人工补充确认");
        String compact = resumeText.replaceAll("\\s+", " ").strip();
        String summary = compact.substring(0, Math.min(compact.length(), 300));
        return new CandidateProfileContent("", "", years, "", skills,
                List.of(), List.of(), skills.isEmpty() ? List.of() : List.of("技术栈信息较明确"),
                risks, summary);
    }
}

