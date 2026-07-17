package com.inin.aiinterviewer.application.service;

import com.inin.aiinterviewer.application.dto.InterviewQuestionDto;
import com.inin.aiinterviewer.application.dto.JobPositionDto;
import com.inin.aiinterviewer.application.dto.QuestionTagDto;
import com.inin.aiinterviewer.application.dto.QuestionTagRelDto;
import com.inin.aiinterviewer.application.dto.SaveInterviewQuestionCommand;
import com.inin.aiinterviewer.application.dto.SaveJobPositionCommand;
import com.inin.aiinterviewer.application.exception.BusinessException;
import com.inin.aiinterviewer.application.exception.ErrorCode;
import com.inin.aiinterviewer.application.util.JsonUtils;
import com.inin.aiinterviewer.domain.entity.InterviewQuestionEntity;
import com.inin.aiinterviewer.domain.entity.JobPositionEntity;
import com.inin.aiinterviewer.domain.entity.QuestionTagEntity;
import com.inin.aiinterviewer.infrastructure.database.mapper.InterviewQuestionMapper;
import com.inin.aiinterviewer.infrastructure.database.mapper.JobPositionMapper;
import com.inin.aiinterviewer.infrastructure.database.mapper.QuestionTagMapper;
import com.inin.aiinterviewer.infrastructure.database.mapper.QuestionTagRelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class QuestionBankService {

    private final JobPositionMapper jobPositionMapper;
    private final InterviewQuestionMapper interviewQuestionMapper;
    private final QuestionTagMapper questionTagMapper;
    private final QuestionTagRelMapper questionTagRelMapper;

    public QuestionBankService(
            JobPositionMapper jobPositionMapper,
            InterviewQuestionMapper interviewQuestionMapper,
            QuestionTagMapper questionTagMapper,
            QuestionTagRelMapper questionTagRelMapper
    ) {
        this.jobPositionMapper = jobPositionMapper;
        this.interviewQuestionMapper = interviewQuestionMapper;
        this.questionTagMapper = questionTagMapper;
        this.questionTagRelMapper = questionTagRelMapper;
    }

    @Transactional(readOnly = true)
    public List<JobPositionDto> listJobs(long userId) {
        return jobPositionMapper.findAllByUserId(userId).stream().map(this::toJobDto).toList();
    }

    @Transactional
    public JobPositionDto createJob(long userId, SaveJobPositionCommand command) {
        JobPositionEntity entity = new JobPositionEntity();
        entity.setUserId(userId);
        entity.setTitle(requireNonBlank(command.title(), "岗位名称"));
        entity.setDepartment(command.department() == null ? null : command.department().strip());
        entity.setDescription(command.description() == null ? null : command.description().strip());
        jobPositionMapper.insert(entity);
        return toJobDto(requireJob(entity.getId(), userId));
    }

    @Transactional
    public JobPositionDto updateJob(long userId, long jobId, SaveJobPositionCommand command) {
        JobPositionEntity entity = requireJob(jobId, userId);
        entity.setTitle(requireNonBlank(command.title(), "岗位名称"));
        entity.setDepartment(command.department() == null ? null : command.department().strip());
        entity.setDescription(command.description() == null ? null : command.description().strip());
        jobPositionMapper.update(jobId, userId, entity.getTitle(), entity.getDepartment(), entity.getDescription());
        return toJobDto(requireJob(jobId, userId));
    }

    @Transactional
    public void deleteJob(long userId, long jobId) {
        if (jobPositionMapper.logicalDelete(jobId, userId) != 1) {
            throw new BusinessException(ErrorCode.QUESTION_POSITION_NOT_FOUND);
        }
    }

    @Transactional(readOnly = true)
    public List<InterviewQuestionDto> listQuestions(long userId) {
        Map<Long, String> jobTitles = new LinkedHashMap<>();
        for (JobPositionEntity job : jobPositionMapper.findAllByUserId(userId)) {
            jobTitles.put(job.getId(), job.getTitle());
        }
        Map<Long, List<String>> tagMap = loadTagMap(userId);
        return interviewQuestionMapper.findAllByUserId(userId).stream()
                .map(entity -> toQuestionDto(entity, jobTitles, tagMap))
                .toList();
    }

    @Transactional
    public InterviewQuestionDto createQuestion(long userId, SaveInterviewQuestionCommand command) {
        if (command.title() == null || command.title().isBlank()
                || command.content() == null || command.content().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        InterviewQuestionEntity entity = new InterviewQuestionEntity();
        entity.setUserId(userId);
        entity.setJobId(command.jobId());
        entity.setCategory(command.category());
        entity.setTitle(command.title().strip());
        entity.setContent(command.content().strip());
        entity.setReferenceAnswer(command.referenceAnswer() == null ? null : command.referenceAnswer().strip());
        entity.setDifficulty(command.difficulty());
        interviewQuestionMapper.insert(entity);
        applyTags(userId, entity.getId(), command.tags());
        return requireQuestionDto(entity.getId(), userId);
    }

    @Transactional
    public InterviewQuestionDto updateQuestion(long userId, long questionId, SaveInterviewQuestionCommand command) {
        InterviewQuestionEntity entity = requireQuestion(questionId, userId);
        if (command.title() == null || command.title().isBlank()
                || command.content() == null || command.content().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        entity.setJobId(command.jobId());
        entity.setCategory(command.category());
        entity.setTitle(command.title().strip());
        entity.setContent(command.content().strip());
        entity.setReferenceAnswer(command.referenceAnswer() == null ? null : command.referenceAnswer().strip());
        entity.setDifficulty(command.difficulty());
        interviewQuestionMapper.update(questionId, userId, entity.getJobId(), entity.getCategory(),
                entity.getTitle(), entity.getContent(), entity.getReferenceAnswer(), entity.getDifficulty());
        applyTags(userId, questionId, command.tags());
        return requireQuestionDto(questionId, userId);
    }

    @Transactional
    public void deleteQuestion(long userId, long questionId) {
        if (interviewQuestionMapper.logicalDelete(questionId, userId) != 1) {
            throw new BusinessException(ErrorCode.QUESTION_NOT_FOUND);
        }
        questionTagRelMapper.deleteByQuestion(questionId, userId);
    }

    @Transactional(readOnly = true)
    public List<QuestionTagDto> listTags(long userId) {
        return questionTagMapper.findAllByUserId(userId).stream()
                .map(tag -> new QuestionTagDto(tag.getId(), tag.getName()))
                .toList();
    }

    private void applyTags(long userId, long questionId, List<String> tagNames) {
        questionTagRelMapper.deleteByQuestion(questionId, userId);
        if (tagNames == null) {
            return;
        }
        for (String raw : tagNames) {
            if (raw == null) continue;
            String name = raw.strip();
            if (name.isBlank()) continue;
            long tagId = questionTagMapper.findByNameAndUserId(name, userId)
                    .map(QuestionTagEntity::getId)
                    .orElseGet(() -> {
                        QuestionTagEntity tag = new QuestionTagEntity();
                        tag.setUserId(userId);
                        tag.setName(name);
                        questionTagMapper.insert(tag);
                        return tag.getId();
                    });
            questionTagRelMapper.insert(questionId, tagId, userId);
        }
    }

    private Map<Long, List<String>> loadTagMap(long userId) {
        Map<Long, List<String>> map = new LinkedHashMap<>();
        for (QuestionTagRelDto rel : questionTagRelMapper.findAllByUserId(userId)) {
            map.computeIfAbsent(rel.getQuestionId(), key -> new ArrayList<>()).add(rel.getTagName());
        }
        return map;
    }

    private JobPositionEntity requireJob(long jobId, long userId) {
        return jobPositionMapper.findByIdAndUserId(jobId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_POSITION_NOT_FOUND));
    }

    private InterviewQuestionEntity requireQuestion(long questionId, long userId) {
        return interviewQuestionMapper.findByIdAndUserId(questionId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));
    }

    private InterviewQuestionDto requireQuestionDto(long questionId, long userId) {
        Map<Long, String> jobTitles = new LinkedHashMap<>();
        for (JobPositionEntity job : jobPositionMapper.findAllByUserId(userId)) {
            jobTitles.put(job.getId(), job.getTitle());
        }
        Map<Long, List<String>> tagMap = loadTagMap(userId);
        return toQuestionDto(requireQuestion(questionId, userId), jobTitles, tagMap);
    }

    private JobPositionDto toJobDto(JobPositionEntity entity) {
        return new JobPositionDto(entity.getId(), entity.getTitle(), entity.getDepartment(),
                entity.getDescription(), entity.getCreateTime(), entity.getUpdateTime());
    }

    private InterviewQuestionDto toQuestionDto(InterviewQuestionEntity entity,
                                             Map<Long, String> jobTitles,
                                             Map<Long, List<String>> tagMap) {
        String jobTitle = entity.getJobId() == null ? null : jobTitles.get(entity.getJobId());
        List<String> tags = tagMap.getOrDefault(entity.getId(), List.of());
        return new InterviewQuestionDto(entity.getId(), entity.getJobId(), jobTitle, entity.getCategory(),
                entity.getTitle(), entity.getContent(), entity.getReferenceAnswer(),
                entity.getDifficulty(), tags, entity.getCreateTime());
    }

    private String requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        return value.strip();
    }
}
