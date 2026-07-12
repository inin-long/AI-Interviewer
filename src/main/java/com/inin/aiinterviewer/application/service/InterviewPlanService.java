package com.inin.aiinterviewer.application.service;

import com.inin.aiinterviewer.application.dto.InterviewPlanDto;
import com.inin.aiinterviewer.application.dto.SaveInterviewPlanCommand;
import com.inin.aiinterviewer.application.exception.BusinessException;
import com.inin.aiinterviewer.application.exception.ErrorCode;
import com.inin.aiinterviewer.application.exception.SystemException;
import com.inin.aiinterviewer.domain.entity.InterviewPlanEntity;
import com.inin.aiinterviewer.domain.enums.InterviewDifficulty;
import com.inin.aiinterviewer.infrastructure.database.mapper.InterviewPlanMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class InterviewPlanService {

    private static final List<String> DEFAULT_STAGES = List.of(
            "INTRODUCTION", "RESUME_REVIEW", "PROJECT_EXPERIENCE",
            "TECHNICAL_DEEP_DIVE", "SYSTEM_DESIGN", "SUMMARY"
    );

    private final InterviewPlanMapper planMapper;
    private final ObjectMapper objectMapper;

    public InterviewPlanService(InterviewPlanMapper planMapper, ObjectMapper objectMapper) {
        this.planMapper = planMapper;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public InterviewPlanDto create(long userId, SaveInterviewPlanCommand command) {
        InterviewPlanEntity entity = toEntity(null, userId, command);
        planMapper.insert(entity);
        return require(entity.getId(), userId);
    }

    @Transactional
    public InterviewPlanDto update(long userId, long planId, SaveInterviewPlanCommand command) {
        require(planId, userId);
        InterviewPlanEntity entity = toEntity(planId, userId, command);
        if (planMapper.update(entity) != 1) {
            throw new BusinessException(ErrorCode.PLAN_NOT_FOUND);
        }
        return require(planId, userId);
    }

    @Transactional
    public InterviewPlanDto duplicate(long userId, long planId) {
        InterviewPlanDto source = require(planId, userId);
        SaveInterviewPlanCommand copy = new SaveInterviewPlanCommand(
                source.name() + " 副本", source.jobTitle(), source.jobDescription(), source.difficulty(),
                source.durationMinutes(), source.questionCount(), source.resumeId(), source.rules(), source.stages());
        return create(userId, copy);
    }

    @Transactional
    public void delete(long userId, long planId) {
        if (planMapper.logicalDelete(planId, userId) != 1) {
            throw new BusinessException(ErrorCode.PLAN_NOT_FOUND);
        }
    }

    @Transactional(readOnly = true)
    public List<InterviewPlanDto> list(long userId) {
        return planMapper.findAllByUserId(userId).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public InterviewPlanDto require(long planId, long userId) {
        return planMapper.findByIdAndUserId(planId, userId)
                .map(this::toDto)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLAN_NOT_FOUND));
    }

    private InterviewPlanEntity toEntity(Long id, long userId, SaveInterviewPlanCommand command) {
        if (command == null || command.name() == null || command.name().isBlank()
                || command.jobTitle() == null || command.jobTitle().isBlank()
                || command.name().trim().length() > 128 || command.jobTitle().trim().length() > 128
                || command.durationMinutes() < 10 || command.durationMinutes() > 240
                || command.questionCount() < 1 || command.questionCount() > 100) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }

        InterviewPlanEntity entity = new InterviewPlanEntity();
        entity.setId(id);
        entity.setUserId(userId);
        entity.setName(command.name().trim());
        entity.setJobTitle(command.jobTitle().trim());
        entity.setJobDescription(command.jobDescription() == null ? "" : command.jobDescription().trim());
        entity.setDifficulty(command.difficulty() == null ? InterviewDifficulty.MEDIUM : command.difficulty());
        entity.setDurationMinutes(command.durationMinutes());
        entity.setQuestionCount(command.questionCount());
        entity.setResumeId(command.resumeId());
        entity.setRulesJson(writeJson(command.rules() == null ? Map.of() : command.rules()));
        entity.setStagesJson(writeJson(command.stages() == null || command.stages().isEmpty()
                ? DEFAULT_STAGES : command.stages()));
        entity.setDefaultPlan(false);
        return entity;
    }

    private InterviewPlanDto toDto(InterviewPlanEntity entity) {
        return new InterviewPlanDto(entity.getId(), entity.getName(), entity.getJobTitle(),
                entity.getJobDescription(), entity.getDifficulty(), entity.getDurationMinutes(),
                entity.getQuestionCount(), entity.getResumeId(), readMap(entity.getRulesJson()),
                readList(entity.getStagesJson()), entity.isDefaultPlan(), entity.getCreateTime(), entity.getUpdateTime());
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new SystemException(ErrorCode.SYSTEM_ERROR, exception);
        }
    }

    private Map<String, Object> readMap(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException exception) {
            throw new SystemException(ErrorCode.SYSTEM_ERROR, exception);
        }
    }

    private List<String> readList(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException exception) {
            throw new SystemException(ErrorCode.SYSTEM_ERROR, exception);
        }
    }
}
