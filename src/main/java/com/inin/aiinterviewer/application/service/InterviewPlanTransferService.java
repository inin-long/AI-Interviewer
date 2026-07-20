package com.inin.aiinterviewer.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inin.aiinterviewer.application.dto.InterviewPlanDto;
import com.inin.aiinterviewer.application.dto.SaveInterviewPlanCommand;
import com.inin.aiinterviewer.application.exception.BusinessException;
import com.inin.aiinterviewer.application.exception.ErrorCode;
import com.inin.aiinterviewer.domain.enums.InterviewDifficulty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class InterviewPlanTransferService {

    private final ObjectMapper objectMapper;
    private final InterviewPlanService planService;

    public InterviewPlanTransferService(ObjectMapper objectMapper, InterviewPlanService planService) {
        this.objectMapper = objectMapper;
        this.planService = planService;
    }

    public void exportPlan(InterviewPlanDto plan, Path target) {
        if (plan == null || target == null) throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        LinkedHashMap<String, Object> rules = new LinkedHashMap<>(plan.rules());
        rules.remove(InterviewPlanAssetService.ICON_PATH_RULE);
        PlanTransferDocument document = new PlanTransferDocument(
                1, plan.name(), plan.jobTitle(), plan.jobDescription(), plan.difficulty(),
                plan.durationMinutes(), plan.questionCount(), rules, plan.stages(),
                plan.domainPackId(), plan.knowledgeCategories());
        try {
            Path parent = target.toAbsolutePath().normalize().getParent();
            if (parent != null) Files.createDirectories(parent);
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(target.toFile(), document);
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.FILE_STORAGE_FAILED);
        }
    }

    public InterviewPlanDto importPlan(long userId, Path source) {
        if (source == null || !Files.isRegularFile(source)) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        }
        try {
            PlanTransferDocument document = objectMapper.readValue(source.toFile(), PlanTransferDocument.class);
            if (document.version() != 1) throw new BusinessException(ErrorCode.VALIDATION_FAILED);
            SaveInterviewPlanCommand command = new SaveInterviewPlanCommand(
                    document.name() + "（导入）", document.jobTitle(), document.jobDescription(),
                    document.difficulty(), document.durationMinutes(), document.questionCount(),
                    null, null, List.of(), document.rules(), document.stages(),
                    document.domainPackId(), document.knowledgeCategories());
            return planService.create(userId, command);
        } catch (IOException | IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
    }

    public record PlanTransferDocument(
            int version,
            String name,
            String jobTitle,
            String jobDescription,
            InterviewDifficulty difficulty,
            int durationMinutes,
            int questionCount,
            Map<String, Object> rules,
            List<String> stages,
            String domainPackId,
            List<String> knowledgeCategories
    ) {
        public PlanTransferDocument {
            rules = rules == null ? Map.of() : Map.copyOf(rules);
            stages = stages == null ? List.of() : List.copyOf(stages);
            knowledgeCategories = knowledgeCategories == null ? List.of() : List.copyOf(knowledgeCategories);
        }
    }
}
