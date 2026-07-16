package com.inin.aiinterviewer.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inin.aiinterviewer.application.dto.DomainPackDto;
import com.inin.aiinterviewer.application.exception.BusinessException;
import com.inin.aiinterviewer.application.exception.ErrorCode;
import com.inin.aiinterviewer.application.exception.SystemException;
import com.inin.aiinterviewer.domain.entity.DomainPackEntity;
import com.inin.aiinterviewer.domain.model.DomainPack;
import com.inin.aiinterviewer.domain.model.DomainPackSnapshot;
import com.inin.aiinterviewer.infrastructure.database.mapper.DomainPackMapper;
import com.inin.aiinterviewer.infrastructure.domain.DomainPackIndex;
import com.inin.aiinterviewer.infrastructure.domain.DomainPackLoader;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;

@Service
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class DomainPackService implements ApplicationRunner {
    public static final String DEFAULT_PACK_ID = "java-backend-1.0.0";
    private final DomainPackMapper mapper;
    private final DomainPackLoader loader;
    private final DomainPackIndex index;
    private final ObjectMapper objectMapper;

    public DomainPackService(
            DomainPackMapper mapper,
            DomainPackLoader loader,
            DomainPackIndex index,
            ObjectMapper objectMapper
    ) {
        this.mapper = mapper;
        this.loader = loader;
        this.index = index;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<DomainPack> builtIns = loader.loadBuiltIns();
        mapper.disableAll();
        builtIns.stream().map(this::toEntity).forEach(mapper::upsert);
        index.rebuild(builtIns);
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
    public String resolveId(String requestedId, String jobTitle) {
        if (requestedId != null && !requestedId.isBlank()) {
            return require(requestedId).id();
        }
        if (jobTitle == null || jobTitle.isBlank()) return require(DEFAULT_PACK_ID).id();
        List<DomainPackDto> matches = search(jobTitle, 10);
        if (!matches.isEmpty()) return matches.getFirst().id();
        return require(DEFAULT_PACK_ID).id();
    }

    private DomainPackEntity toEntity(DomainPack pack) {
        DomainPackEntity entity = new DomainPackEntity();
        entity.setId(pack.id());
        entity.setRoleCode(pack.roleCode());
        entity.setIndustryCode(pack.industryCode());
        entity.setDisplayName(pack.displayName());
        entity.setVersion(pack.version());
        entity.setContentJson(write(pack));
        entity.setEnabled(true);
        return entity;
    }

    private DomainPackDto toDto(DomainPackEntity entity) {
        return new DomainPackDto(entity.getId(), entity.getRoleCode(), entity.getIndustryCode(),
                entity.getVersion(), entity.getDisplayName());
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
