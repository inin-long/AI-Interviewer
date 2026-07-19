package com.inin.aiinterviewer.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inin.aiinterviewer.application.dto.DomainPackDto;
import com.inin.aiinterviewer.application.exception.AIException;
import com.inin.aiinterviewer.application.exception.BusinessException;
import com.inin.aiinterviewer.application.exception.ErrorCode;
import com.inin.aiinterviewer.application.exception.SystemException;
import com.inin.aiinterviewer.agent.prompt.AgentPrompts;
import com.inin.aiinterviewer.domain.entity.DomainPackEntity;
import com.inin.aiinterviewer.domain.model.DomainPack;
import com.inin.aiinterviewer.domain.model.DomainPackSnapshot;
import com.inin.aiinterviewer.infrastructure.ai.OpenAiChatService;
import com.inin.aiinterviewer.infrastructure.database.mapper.DomainPackMapper;
import com.inin.aiinterviewer.infrastructure.domain.DomainPackIndex;
import com.inin.aiinterviewer.infrastructure.domain.DomainPackLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class DomainPackService implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DomainPackService.class);

    private final DomainPackMapper mapper;
    private final DomainPackLoader loader;
    private final DomainPackIndex index;
    private final ObjectMapper objectMapper;
    private final OpenAiChatService openAiChatService;

    public DomainPackService(
            DomainPackMapper mapper,
            DomainPackLoader loader,
            DomainPackIndex index,
            ObjectMapper objectMapper,
            OpenAiChatService openAiChatService
    ) {
        this.mapper = mapper;
        this.loader = loader;
        this.index = index;
        this.objectMapper = objectMapper;
        this.openAiChatService = openAiChatService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<DomainPack> builtIns = loader.loadBuiltIns();
        mapper.disableBuiltIns();
        builtIns.stream().map(pack -> toEntity(pack, DomainPackEntity.SOURCE_BUILTIN)).forEach(mapper::upsert);
        rebuildIndex();
    }

    @Transactional(readOnly = true)
    public List<DomainPackDto> list() {
        return mapper.findAllEnabled().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<DomainPackDto> search(String query, int limit) {
        if (limit <= 0 || limit > 100) throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        LinkedHashMap<String, DomainPackDto> enabled = new LinkedHashMap<>();
        mapper.findAllEnabled().forEach(entity -> enabled.put(entity.getId(), toDto(entity)));
        return index.search(query, limit).stream().map(enabled::get).filter(java.util.Objects::nonNull).toList();
    }

    @Transactional(readOnly = true)
    public DomainPack require(String id) {
        if (id == null || id.isBlank()) throw new BusinessException(ErrorCode.DOMAIN_PACK_NOT_FOUND);
        return mapper.findEnabledById(id.strip()).map(this::read)
                .orElseThrow(() -> new BusinessException(ErrorCode.DOMAIN_PACK_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public DomainPackSnapshot snapshot(String id) {
        return DomainPackSnapshot.from(require(id));
    }

    @Transactional(readOnly = true)
    public boolean exists(String id) {
        if (id == null || id.isBlank()) return false;
        return mapper.findEnabledById(id.strip()).isPresent();
    }

    @Transactional(readOnly = true)
    public String resolveId(String requestedId, String jobTitle) {
        if (requestedId != null && !requestedId.isBlank()) {
            String normalized = requestedId.strip();
            // 显式选择「无知识包」模式
            if (DomainPackSnapshot.NONE_PACK_ID.equals(normalized)) return DomainPackSnapshot.NONE_PACK_ID;
            // 显式选择了某个包：若仍然存在则使用，否则安全回退到「无知识包」
            return exists(normalized) ? normalized : DomainPackSnapshot.NONE_PACK_ID;
        }
        // 未显式选择时，默认兜底为「无知识包」模式
        return DomainPackSnapshot.NONE_PACK_ID;
    }

    /**
     * 保存用户自建知识包。包内容由调用方构建（含 id / displayName / competencies 等），
     * 这里只做基础校验与持久化，并将来源标记为 USER，避免被启动时的 disableBuiltIns 清除。
     */
    @Transactional
    public DomainPack saveUserPack(DomainPack pack) {
        if (pack == null || pack.displayName() == null || pack.displayName().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        if (pack.competencies().isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        String id = (pack.id() == null || pack.id().isBlank())
                ? "user" + UUID.randomUUID().toString().replace("-", "")
                : pack.id().strip();
        DomainPack toSave = new DomainPack(id, pack.roleCode(), pack.industryCode(),
                pack.version() == null || pack.version().isBlank() ? "1.0.0" : pack.version(),
                pack.displayName(), pack.competencies(), pack.metrics(), pack.failurePatterns(),
                pack.probePlaybooks(), pack.scenarios(), pack.rubrics());
        mapper.upsert(toEntity(toSave, DomainPackEntity.SOURCE_USER));
        rebuildIndex();
        return toSave;
    }

    /** 仅允许删除用户自建的知识包，内置包不可被删除。 */
    @Transactional
    public void deleteUserPack(String id) {
        if (id == null || id.isBlank()) throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        mapper.deleteUserPack(id.strip());
        rebuildIndex();
    }

    /**
     * 根据岗位 JD 调用大模型生成知识包，并作为用户自建包保存。
     * 解析失败时抛出清晰的业务异常，不会污染已有数据。
     */
    @Transactional
    public DomainPack generateFromJobDescription(String jobDescription, String name) {
        if (jobDescription == null || jobDescription.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        String displayName = (name == null || name.isBlank())
                ? "AI 生成：" + jobDescription.trim().split("\\s+")[0]
                : name.trim();
        try {
            String json = openAiChatService.chatJson(AgentPrompts.generateDomainPack(jobDescription));
            DomainPack pack = parseAiPack(json);
            return saveUserPack(new DomainPack(
                    null, deriveRoleCode(displayName), deriveIndustryCode(jobDescription),
                    "1.0.0", displayName, pack.competencies(), pack.metrics(),
                    pack.failurePatterns(), pack.probePlaybooks(), List.of(), pack.rubrics()));
        } catch (AIException exception) {
            throw new BusinessException(ErrorCode.AI_NOT_CONFIGURED);
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new BusinessException(ErrorCode.AI_CALL_FAILED);
        }
    }

    private String deriveRoleCode(String displayName) {
        String cleaned = displayName.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
        return cleaned.isEmpty() ? "userpack" : cleaned.substring(0, Math.min(cleaned.length(), 32));
    }

    private String deriveIndustryCode(String jd) {
        return "user";
    }

    @SuppressWarnings("unchecked")
    private DomainPack parseAiPack(String json) {
        Map<String, Object> root;
        try {
            root = objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.AI_CALL_FAILED);
        }
        if (root == null) throw new BusinessException(ErrorCode.AI_CALL_FAILED);

        List<DomainPack.CompetencyDefinition> competencies = readCompetencies(root.get("competencies"));
        if (competencies.isEmpty()) {
            throw new BusinessException(ErrorCode.AI_CALL_FAILED);
        }
        List<DomainPack.MetricDefinition> metrics = readMetrics(root.get("metrics"));
        List<DomainPack.FailurePattern> failurePatterns = readFailurePatterns(root.get("failurePatterns"));
        List<DomainPack.ProbePlaybook> probePlaybooks = readProbePlaybooks(root.get("probePlaybooks"));
        List<DomainPack.EvaluationRubric> rubrics = readRubrics(root.get("rubrics"));
        return new DomainPack(null, "user", "user", "1.0.0", "AI 生成知识包",
                competencies, metrics, failurePatterns, probePlaybooks, List.of(), rubrics);
    }

    private List<Map<String, Object>> asMapList(Object value) {
        if (!(value instanceof List<?> list)) return List.of();
        return list.stream()
                .filter(item -> item instanceof Map)
                .map(item -> (Map<String, Object>) item)
                .collect(Collectors.toList());
    }

    private List<String> asStringList(Object value) {
        if (!(value instanceof List<?> list)) return List.of();
        return list.stream().filter(o -> o != null).map(Object::toString).toList();
    }

    private String asText(Object value, String fallback) {
        return value == null ? fallback : value.toString();
    }

    private double asDouble(Object value, double fallback) {
        if (value instanceof Number number) return number.doubleValue();
        if (value instanceof String str && !str.isBlank()) {
            try {
                return Double.parseDouble(str);
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private List<DomainPack.CompetencyDefinition> readCompetencies(Object value) {
        List<Map<String, Object>> items = asMapList(value);
        List<DomainPack.CompetencyDefinition> result = new ArrayList<>();
        int index = 0;
        for (Map<String, Object> item : items) {
            index++;
            String code = asText(item.get("code"), "CUST_" + index);
            String name = asText(item.get("name"), "能力" + index);
            String description = asText(item.get("description"), "");
            double importance = Math.max(0.0, Math.min(1.0, asDouble(item.get("importance"), 0.8)));
            result.add(new DomainPack.CompetencyDefinition(
                    code, name, description, importance, asStringList(item.get("indicators"))));
        }
        return result;
    }

    private List<DomainPack.MetricDefinition> readMetrics(Object value) {
        return asMapList(value).stream()
                .map(item -> new DomainPack.MetricDefinition(
                        asText(item.get("code"), "METRIC"),
                        asText(item.get("name"), "指标"),
                        asText(item.get("description"), "")))
                .toList();
    }

    private List<DomainPack.FailurePattern> readFailurePatterns(Object value) {
        return asMapList(value).stream()
                .map(item -> new DomainPack.FailurePattern(
                        asText(item.get("code"), "FAILURE"),
                        asText(item.get("name"), "失效模式"),
                        asText(item.get("description"), ""),
                        asStringList(item.get("symptoms")),
                        asStringList(item.get("probes"))))
                .toList();
    }

    private List<DomainPack.ProbePlaybook> readProbePlaybooks(Object value) {
        return asMapList(value).stream()
                .map(item -> new DomainPack.ProbePlaybook(
                        asText(item.get("code"), "PROBE"),
                        asText(item.get("objective"), ""),
                        asStringList(item.get("expectedEvidence")),
                        asStringList(item.get("templates"))))
                .toList();
    }

    private List<DomainPack.EvaluationRubric> readRubrics(Object value) {
        return asMapList(value).stream()
                .map(item -> new DomainPack.EvaluationRubric(
                        asText(item.get("competencyCode"), ""),
                        asStringList(item.get("positiveSignals")),
                        asStringList(item.get("negativeSignals")),
                        asStringList(item.get("insufficientEvidenceSignals"))))
                .toList();
    }

    private void rebuildIndex() {
        List<DomainPack> all = new ArrayList<>();
        for (DomainPackEntity entity : mapper.findAllEnabled()) {
            try {
                all.add(read(entity));
            } catch (RuntimeException exception) {
                log.warn("跳过无法解析的知识包，id={}", entity.getId(), exception);
            }
        }
        index.rebuild(all);
    }

    private DomainPackEntity toEntity(DomainPack pack, String source) {
        DomainPackEntity entity = new DomainPackEntity();
        entity.setId(pack.id());
        entity.setRoleCode(pack.roleCode());
        entity.setIndustryCode(pack.industryCode());
        entity.setDisplayName(pack.displayName());
        entity.setVersion(pack.version());
        entity.setContentJson(write(pack));
        entity.setEnabled(true);
        entity.setSource(source);
        return entity;
    }

    private DomainPackDto toDto(DomainPackEntity entity) {
        return new DomainPackDto(entity.getId(), entity.getRoleCode(), entity.getIndustryCode(),
                entity.getVersion(), entity.getDisplayName(), entity.getSource());
    }

    private String write(DomainPack pack) {
        try {
            return objectMapper.writeValueAsString(pack);
        } catch (JsonProcessingException exception) {
            throw new SystemException(ErrorCode.SYSTEM_ERROR, exception);
        }
    }

    private DomainPack read(DomainPackEntity entity) {
        try {
            return objectMapper.readValue(entity.getContentJson(), DomainPack.class);
        } catch (JsonProcessingException exception) {
            throw new SystemException(ErrorCode.DATA_ACCESS_FAILED, exception);
        }
    }
}
