package com.inin.aiinterviewer.infrastructure.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inin.aiinterviewer.domain.model.DomainPack;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class DomainPackLoader {
    private static final String RESOURCE_PATTERN = "classpath*:domain-packs/*.json";
    private static final Pattern SAFE_CODE = Pattern.compile("[a-z0-9][a-z0-9._-]{1,63}");
    private static final Pattern COMPETENCY_CODE = Pattern.compile("[A-Z][A-Z0-9_]{1,63}");

    private final ResourcePatternResolver resources;
    private final ObjectMapper objectMapper;

    public DomainPackLoader(ResourcePatternResolver resources, ObjectMapper objectMapper) {
        this.resources = resources;
        this.objectMapper = objectMapper;
    }

    public List<DomainPack> loadBuiltIns() {
        try {
            Resource[] matches = resources.getResources(RESOURCE_PATTERN);
            List<DomainPack> packs = new ArrayList<>(matches.length);
            Set<String> ids = new HashSet<>();
            for (Resource resource : matches) {
                try (InputStream input = resource.getInputStream()) {
                    DomainPack pack = objectMapper.readValue(input, DomainPack.class);
                    validate(pack, resource.getDescription());
                    if (!ids.add(pack.id())) {
                        throw new IllegalStateException("Duplicate DomainPack id: " + pack.id());
                    }
                    packs.add(pack);
                }
            }
            if (packs.isEmpty()) throw new IllegalStateException("No built-in DomainPack resources were found");
            return packs.stream().sorted(java.util.Comparator.comparing(DomainPack::id)).toList();
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot load built-in DomainPack resources", exception);
        }
    }

    private void validate(DomainPack pack, String source) {
        if (pack == null || !code(pack.id()) || !code(pack.roleCode())
                || pack.version() == null || pack.version().isBlank()
                || pack.displayName() == null || pack.displayName().isBlank()
                || pack.competencies().isEmpty() || pack.probePlaybooks().isEmpty()
                || pack.scenarios().isEmpty() || pack.rubrics().isEmpty()) {
            throw new IllegalStateException("Incomplete DomainPack: " + source);
        }
        Set<String> competencyCodes = new HashSet<>();
        for (DomainPack.CompetencyDefinition competency : pack.competencies()) {
            if (competency == null || competency.code() == null
                    || !COMPETENCY_CODE.matcher(competency.code()).matches() || competency.name() == null
                    || competency.name().isBlank() || !Double.isFinite(competency.importance())
                    || competency.importance() < 0 || competency.importance() > 1
                    || !competencyCodes.add(competency.code())) {
                throw new IllegalStateException("Invalid DomainPack competency in " + source);
            }
        }
        for (DomainPack.EvaluationRubric rubric : pack.rubrics()) {
            if (rubric == null || !competencyCodes.contains(rubric.competencyCode())) {
                throw new IllegalStateException("DomainPack rubric references an unknown competency in " + source);
            }
        }
    }

    private boolean code(String value) {
        return value != null && SAFE_CODE.matcher(value).matches();
    }
}
