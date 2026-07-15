package com.inin.aiinterviewer.application.service;

import com.inin.aiinterviewer.application.dto.CandidateProfileDto;
import com.inin.aiinterviewer.application.dto.CandidateProfileListItemDto;
import com.inin.aiinterviewer.application.exception.BusinessException;
import com.inin.aiinterviewer.application.exception.ErrorCode;
import com.inin.aiinterviewer.application.exception.SystemException;
import com.inin.aiinterviewer.config.properties.LlmProperties;
import com.inin.aiinterviewer.domain.entity.CandidateProfileEntity;
import com.inin.aiinterviewer.domain.entity.ResumeEntity;
import com.inin.aiinterviewer.domain.enums.ProfileSource;
import com.inin.aiinterviewer.domain.enums.ProfileStatus;
import com.inin.aiinterviewer.domain.enums.ResumeStatus;
import com.inin.aiinterviewer.domain.model.CandidateProfileContent;
import com.inin.aiinterviewer.infrastructure.ai.ChatService;
import com.inin.aiinterviewer.infrastructure.database.mapper.CandidateProfileMapper;
import com.inin.aiinterviewer.infrastructure.database.mapper.ResumeMapper;
import com.inin.aiinterviewer.agent.support.StructuredAiResponseParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.List;

@Service
public class CandidateProfileService {

    private final CandidateProfileMapper profileMapper;
    private final ResumeMapper resumeMapper;
    private final LocalCandidateProfileExtractor localExtractor;
    private final ChatService chatService;
    private final LlmProperties llmProperties;
    private final ObjectMapper objectMapper;
    private final StructuredAiResponseParser responseParser;

    public CandidateProfileService(
            CandidateProfileMapper profileMapper,
            ResumeMapper resumeMapper,
            LocalCandidateProfileExtractor localExtractor,
            ChatService chatService,
            LlmProperties llmProperties,
            ObjectMapper objectMapper,
            StructuredAiResponseParser responseParser
    ) {
        this.profileMapper = profileMapper;
        this.resumeMapper = resumeMapper;
        this.localExtractor = localExtractor;
        this.chatService = chatService;
        this.llmProperties = llmProperties;
        this.objectMapper = objectMapper;
        this.responseParser = responseParser;
    }

    @Transactional(readOnly = true)
    public Optional<CandidateProfileDto> find(long userId, long resumeId) {
        return profileMapper.findByResumeIdAndUserId(resumeId, userId).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public List<CandidateProfileListItemDto> list(long userId) {
        return profileMapper.findAllByUserId(userId).stream()
                .map(this::toListItemDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CandidateProfileListItemDto> listConfirmed(long userId) {
        return list(userId).stream().filter(CandidateProfileListItemDto::confirmed).toList();
    }

    @Transactional(readOnly = true)
    public CandidateProfileDto requireConfirmed(long userId, long profileId) {
        CandidateProfileDto profile = profileMapper.findByIdAndUserId(profileId, userId)
                .map(this::toDto)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROFILE_NOT_CONFIRMED));
        if (!profile.confirmed() || profile.status() != ProfileStatus.CONFIRMED) {
            throw new BusinessException(ErrorCode.PROFILE_NOT_CONFIRMED);
        }
        return profile;
    }

    @Transactional
    public CandidateProfileDto generate(long userId, long resumeId) {
        ResumeEntity resume = requireParsedResume(userId, resumeId);
        CandidateProfileContent content;
        ProfileSource source;
        ProfileStatus status;
        if (llmProperties.isConfigured()) {
            content = extractWithAi(resume.getParsedText());
            source = ProfileSource.AI;
            status = ProfileStatus.GENERATED;
        } else {
            content = localExtractor.extract(resume.getParsedText());
            source = ProfileSource.LOCAL_DRAFT;
            status = ProfileStatus.DRAFT;
        }
        save(userId, resumeId, content, source, status, false);
        return requireProfile(userId, resumeId);
    }

    @Transactional
    public CandidateProfileDto saveManual(long userId, long resumeId, CandidateProfileContent content) {
        requireParsedResume(userId, resumeId);
        save(userId, resumeId, content, ProfileSource.MANUAL, ProfileStatus.DRAFT, false);
        return requireProfile(userId, resumeId);
    }

    @Transactional
    public CandidateProfileDto confirm(long userId, long resumeId) {
        if (profileMapper.confirm(resumeId, userId) != 1) {
            throw new BusinessException(ErrorCode.INVALID_STATE);
        }
        return requireProfile(userId, resumeId);
    }

    private ResumeEntity requireParsedResume(long userId, long resumeId) {
        ResumeEntity resume = resumeMapper.findByIdAndUserId(resumeId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));
        if (resume.getStatus() != ResumeStatus.COMPLETED
                || resume.getParsedText() == null || resume.getParsedText().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_STATE);
        }
        return resume;
    }

    private CandidateProfileContent extractWithAi(String resumeText) {
        String prompt = """
                你是技术招聘分析助手。请根据以下简历文本提取候选人画像。
                只返回一个合法 JSON 对象，不要输出 Markdown 代码围栏或解释。
                JSON 字段必须严格为：
                fullName, targetRole, yearsExperience, education, skills, projects,
                experience, strengths, risks, summary。
                skills/projects/experience/strengths/risks 必须是字符串数组；其余字段是字符串。
                不确定的信息使用空字符串或空数组，禁止臆造。

                简历文本：
                """ + resumeText.substring(0, Math.min(resumeText.length(), 30_000));
        return responseParser.parse(chatService.chatJson(prompt), CandidateProfileContent.class);
    }

    private void save(
            long userId,
            long resumeId,
            CandidateProfileContent content,
            ProfileSource source,
            ProfileStatus status,
            boolean confirmed
    ) {
        CandidateProfileEntity entity = new CandidateProfileEntity();
        entity.setUserId(userId);
        entity.setResumeId(resumeId);
        entity.setContentJson(writeJson(content));
        entity.setSource(source);
        entity.setStatus(status);
        entity.setConfirmed(confirmed);
        profileMapper.upsert(entity);
    }

    private CandidateProfileDto requireProfile(long userId, long resumeId) {
        return find(userId, resumeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_STATE));
    }

    private CandidateProfileDto toDto(CandidateProfileEntity entity) {
        try {
            CandidateProfileContent content = objectMapper.readValue(
                    entity.getContentJson(), CandidateProfileContent.class);
            return new CandidateProfileDto(entity.getId(), entity.getResumeId(), content,
                    entity.getSource(), entity.getStatus(), entity.isConfirmed(),
                    entity.getErrorMessage(), entity.getUpdateTime());
        } catch (JsonProcessingException exception) {
            throw new SystemException(ErrorCode.SYSTEM_ERROR, exception);
        }
    }

    private CandidateProfileListItemDto toListItemDto(CandidateProfileEntity entity) {
        CandidateProfileDto profile = toDto(entity);
        CandidateProfileContent content = profile.content();
        return new CandidateProfileListItemDto(profile.id(), profile.resumeId(), entity.getResumeName(),
                content.fullName(), content.targetRole(), content.skills(), profile.source(),
                profile.status(), profile.confirmed(), profile.updateTime());
    }

    private String writeJson(CandidateProfileContent content) {
        try {
            return objectMapper.writeValueAsString(content);
        } catch (JsonProcessingException exception) {
            throw new SystemException(ErrorCode.SYSTEM_ERROR, exception);
        }
    }

}
