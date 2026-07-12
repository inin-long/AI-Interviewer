package com.inin.aiinterviewer.application.service;

import com.inin.aiinterviewer.application.dto.ResumeDto;
import com.inin.aiinterviewer.application.exception.BusinessException;
import com.inin.aiinterviewer.application.exception.ErrorCode;
import com.inin.aiinterviewer.domain.entity.ResumeEntity;
import com.inin.aiinterviewer.domain.enums.ResumeStatus;
import com.inin.aiinterviewer.domain.enums.StorageCategory;
import com.inin.aiinterviewer.infrastructure.database.mapper.ResumeMapper;
import com.inin.aiinterviewer.infrastructure.document.DocumentParser;
import com.inin.aiinterviewer.infrastructure.document.ParsedDocument;
import com.inin.aiinterviewer.infrastructure.file.FileStorageService;
import com.inin.aiinterviewer.infrastructure.file.StoredFile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

@Service
public class ResumeService {

    private static final long MAX_FILE_SIZE = 20L * 1024 * 1024;

    private final ResumeMapper resumeMapper;
    private final FileStorageService fileStorageService;
    private final DocumentParser documentParser;

    public ResumeService(
            ResumeMapper resumeMapper,
            FileStorageService fileStorageService,
            DocumentParser documentParser
    ) {
        this.resumeMapper = resumeMapper;
        this.fileStorageService = fileStorageService;
        this.documentParser = documentParser;
    }

    public ResumeDto uploadAndParse(long userId, Path source) {
        StoredFile stored = fileStorageService.store(userId, StorageCategory.RESUMES, source);
        if (stored.size() > MAX_FILE_SIZE) {
            fileStorageService.delete(userId, StorageCategory.RESUMES, stored.storageName());
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }

        ResumeEntity entity = new ResumeEntity();
        entity.setUserId(userId);
        entity.setOriginalName(stored.originalName());
        entity.setStorageName(stored.storageName());
        entity.setStoragePath(stored.path().toString());
        entity.setFileType(extension(stored.originalName()));
        entity.setFileSize(stored.size());
        entity.setStatus(ResumeStatus.UPLOADED);
        insert(entity);

        try {
            resumeMapper.markParsing(entity.getId(), userId);
            ParsedDocument parsed = documentParser.parse(stored.path());
            resumeMapper.markCompleted(entity.getId(), userId, parsed.content());
        } catch (RuntimeException exception) {
            resumeMapper.markFailed(entity.getId(), userId, safeError(exception));
        }
        return requireResume(entity.getId(), userId);
    }

    @Transactional(readOnly = true)
    public List<ResumeDto> list(long userId) {
        return resumeMapper.findAllByUserId(userId).stream().map(this::toDto).toList();
    }

    @Transactional
    public void delete(long userId, long resumeId) {
        ResumeEntity entity = resumeMapper.findByIdAndUserId(resumeId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));
        if (resumeMapper.logicalDelete(resumeId, userId) != 1) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        }
        fileStorageService.delete(userId, StorageCategory.RESUMES, entity.getStorageName());
    }

    @Transactional
    protected void insert(ResumeEntity entity) {
        resumeMapper.insert(entity);
    }

    @Transactional(readOnly = true)
    protected ResumeDto requireResume(long id, long userId) {
        return resumeMapper.findByIdAndUserId(id, userId)
                .map(this::toDto)
                .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));
    }

    private ResumeDto toDto(ResumeEntity entity) {
        return new ResumeDto(entity.getId(), entity.getOriginalName(), entity.getFileType(),
                entity.getFileSize(), entity.getStatus(), entity.getErrorMessage(),
                entity.getCreateTime(), entity.getUpdateTime());
    }

    private String extension(String name) {
        int separator = name.lastIndexOf('.');
        return separator < 0 ? "" : name.substring(separator + 1).toLowerCase(Locale.ROOT);
    }

    private String safeError(RuntimeException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? "简历解析失败" : message.substring(0, Math.min(500, message.length()));
    }
}

