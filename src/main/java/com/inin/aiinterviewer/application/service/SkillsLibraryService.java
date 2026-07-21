package com.inin.aiinterviewer.application.service;

import com.inin.aiinterviewer.application.dto.SaveSkillArticleCommand;
import com.inin.aiinterviewer.application.dto.SkillArticleDto;
import com.inin.aiinterviewer.application.exception.BusinessException;
import com.inin.aiinterviewer.application.exception.ErrorCode;
import com.inin.aiinterviewer.application.util.JsonUtils;
import com.inin.aiinterviewer.domain.entity.SkillArticleEntity;
import com.inin.aiinterviewer.infrastructure.database.mapper.SkillArticleMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class SkillsLibraryService {

    private final SkillArticleMapper skillArticleMapper;

    public SkillsLibraryService(SkillArticleMapper skillArticleMapper) {
        this.skillArticleMapper = skillArticleMapper;
    }

    @Transactional(readOnly = true)
    public List<SkillArticleDto> listArticles(long userId) {
        return skillArticleMapper.findAllVisible(userId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SkillArticleDto> listByCategory(long userId, String category) {
        if (category == null || category.isBlank() || "ALL".equalsIgnoreCase(category)) {
            return listArticles(userId);
        }
        return listArticles(userId).stream()
                .filter(article -> category.equalsIgnoreCase(article.category()))
                .toList();
    }

    @Transactional(readOnly = true)
    public SkillArticleDto getArticle(long id) {
        return skillArticleMapper.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new BusinessException(ErrorCode.SKILL_ARTICLE_NOT_FOUND));
    }

    @Transactional
    public SkillArticleDto createArticle(long userId, SaveSkillArticleCommand command) {
        if (command.title() == null || command.title().isBlank()
                || command.contentMarkdown() == null || command.contentMarkdown().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        SkillArticleEntity entity = new SkillArticleEntity();
        entity.setUserId(userId);
        entity.setCategory(normalizeCategory(command.category()));
        entity.setTitle(command.title().strip());
        entity.setSummary(command.summary() == null ? null : command.summary().strip());
        entity.setContentMarkdown(command.contentMarkdown().strip());
        entity.setTagsJson(JsonUtils.toJson(command.tags()));
        skillArticleMapper.insert(entity);
        return toDto(requireOwner(entity.getId(), userId));
    }

    @Transactional
    public SkillArticleDto updateArticle(long userId, long id, SaveSkillArticleCommand command) {
        SkillArticleEntity entity = requireOwner(id, userId);
        if (command.title() == null || command.title().isBlank()
                || command.contentMarkdown() == null || command.contentMarkdown().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        entity.setCategory(normalizeCategory(command.category()));
        entity.setTitle(command.title().strip());
        entity.setSummary(command.summary() == null ? null : command.summary().strip());
        entity.setContentMarkdown(command.contentMarkdown().strip());
        entity.setTagsJson(JsonUtils.toJson(command.tags()));
        skillArticleMapper.updateOwner(id, userId, entity.getCategory(), entity.getTitle(),
                entity.getSummary(), entity.getContentMarkdown(), entity.getTagsJson());
        return toDto(requireOwner(id, userId));
    }

    @Transactional
    public void deleteArticle(long userId, long id) {
        requireOwner(id, userId);
        if (skillArticleMapper.logicalDelete(id, userId) != 1) {
            throw new BusinessException(ErrorCode.SKILL_ARTICLE_NOT_FOUND);
        }
    }

    private SkillArticleEntity requireOwner(long id, long userId) {
        SkillArticleEntity entity = skillArticleMapper.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.SKILL_ARTICLE_NOT_FOUND));
        if (entity.getUserId() == null || entity.getUserId() != userId) {
            throw new BusinessException(ErrorCode.SKILL_ARTICLE_NOT_OWNER);
        }
        return entity;
    }

    private String normalizeCategory(String category) {
        if (category == null) return "GENERAL";
        return switch (category.strip().toUpperCase(java.util.Locale.ROOT)) {
            case "STAR", "STAR 法则", "STAR法则" -> "STAR";
            case "ETIQUETTE", "礼仪指南" -> "ETIQUETTE";
            case "BEHAVIOR", "行为面试" -> "BEHAVIOR";
            case "GENERAL", "通用技巧", "其他" -> "GENERAL";
            default -> "GENERAL";
        };
    }

    private SkillArticleDto toDto(SkillArticleEntity entity) {
        return new SkillArticleDto(entity.getId(), entity.getUserId(), entity.getCategory(),
                entity.getTitle(), entity.getSummary(), entity.getContentMarkdown(),
                JsonUtils.readStringList(entity.getTagsJson()), entity.getCreateTime());
    }
}
