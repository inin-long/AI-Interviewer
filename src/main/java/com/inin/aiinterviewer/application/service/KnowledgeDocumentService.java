package com.inin.aiinterviewer.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inin.aiinterviewer.application.dto.KnowledgeDetailDto;
import com.inin.aiinterviewer.application.dto.KnowledgeDocumentDto;
import com.inin.aiinterviewer.application.dto.KnowledgeSearchResultDto;
import com.inin.aiinterviewer.application.exception.BusinessException;
import com.inin.aiinterviewer.application.exception.ErrorCode;
import com.inin.aiinterviewer.application.exception.SystemException;
import com.inin.aiinterviewer.domain.entity.DocumentChunkEntity;
import com.inin.aiinterviewer.domain.entity.KnowledgeDocumentEntity;
import com.inin.aiinterviewer.domain.enums.KnowledgeStatus;
import com.inin.aiinterviewer.domain.enums.StorageCategory;
import com.inin.aiinterviewer.infrastructure.ai.EmbeddingService;
import com.inin.aiinterviewer.infrastructure.database.mapper.KnowledgeDocumentMapper;
import com.inin.aiinterviewer.infrastructure.document.DocumentChunker;
import com.inin.aiinterviewer.infrastructure.document.DocumentParser;
import com.inin.aiinterviewer.infrastructure.file.FileStorageService;
import com.inin.aiinterviewer.infrastructure.file.PathService;
import com.inin.aiinterviewer.infrastructure.file.StoredFile;
import com.inin.aiinterviewer.infrastructure.vector.VectorDocument;
import com.inin.aiinterviewer.infrastructure.vector.VectorStorePort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.Collection;

@Service
public class KnowledgeDocumentService {

    private static final long MAX_FILE_SIZE = 30L * 1024 * 1024;

    private final KnowledgeDocumentMapper mapper;
    private final FileStorageService fileStorageService;
    private final PathService pathService;
    private final DocumentParser documentParser;
    private final DocumentChunker chunker;
    private final EmbeddingService embeddingService;
    private final VectorStorePort vectorStore;
    private final ObjectMapper objectMapper;

    public KnowledgeDocumentService(
            KnowledgeDocumentMapper mapper,
            FileStorageService fileStorageService,
            PathService pathService,
            DocumentParser documentParser,
            DocumentChunker chunker,
            EmbeddingService embeddingService,
            VectorStorePort vectorStore,
            ObjectMapper objectMapper
    ) {
        this.mapper = mapper;
        this.fileStorageService = fileStorageService;
        this.pathService = pathService;
        this.documentParser = documentParser;
        this.chunker = chunker;
        this.embeddingService = embeddingService;
        this.vectorStore = vectorStore;
        this.objectMapper = objectMapper;
    }

    public KnowledgeDocumentDto uploadAndIndex(long userId, Path source, String category) {
        KnowledgeDocumentDto uploaded = upload(userId, source, category);
        try {
            processDocument(userId, uploaded.id());
        } catch (RuntimeException ignored) {
            // Synchronous compatibility API returns the persisted FAILED state.
        }
        return require(userId, uploaded.id());
    }

    public KnowledgeDocumentDto upload(long userId, Path source, String category) {
        StoredFile stored = fileStorageService.store(userId, StorageCategory.DOCUMENTS, source);
        if (stored.size() > MAX_FILE_SIZE) {
            fileStorageService.delete(userId, StorageCategory.DOCUMENTS, stored.storageName());
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        KnowledgeDocumentEntity entity = new KnowledgeDocumentEntity();
        entity.setUserId(userId);
        entity.setName(baseName(stored.originalName()));
        entity.setOriginalName(stored.originalName());
        entity.setStorageName(stored.storageName());
        entity.setStoragePath(stored.path().toString());
        entity.setFileType(extension(stored.originalName()));
        entity.setFileSize(stored.size());
        entity.setCategory(category == null || category.isBlank() ? "技术资料" : category.strip());
        entity.setStatus(KnowledgeStatus.UPLOADED);
        try {
            mapper.insert(entity);
        } catch (RuntimeException exception) {
            fileStorageService.delete(userId, StorageCategory.DOCUMENTS, stored.storageName());
            throw exception;
        }

        return require(userId, entity.getId());
    }

    public void processDocument(long userId, long documentId) {
        KnowledgeDocumentEntity entity = mapper.findById(documentId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));
        List<String> previousVectorIds = mapper.findChunks(documentId, userId).stream()
                .map(DocumentChunkEntity::getVectorId).filter(java.util.Objects::nonNull).toList();
        if (!previousVectorIds.isEmpty()) vectorStore.delete(userId, previousVectorIds);
        mapper.deleteChunks(documentId, userId);

        List<String> vectorIds = new ArrayList<>();
        try {
            mapper.updateStatus(entity.getId(), userId, KnowledgeStatus.PARSING, null);
            Path storedPath = pathService.resolveStoredPath(
                    userId, StorageCategory.DOCUMENTS, entity.getStorageName());
            var parsed = documentParser.parse(storedPath);
            var chunks = chunker.chunk(parsed.content());
            if (chunks.isEmpty()) throw new BusinessException(ErrorCode.VALIDATION_FAILED);
            mapper.updateStatus(entity.getId(), userId, KnowledgeStatus.INDEXING, null);

            List<VectorDocument> vectors = new ArrayList<>();
            List<DocumentChunkEntity> records = new ArrayList<>();
            for (var chunk : chunks) {
                String vectorId = entity.getId() + ":" + chunk.index();
                Map<String, Object> metadata = Map.of(
                        "documentId", entity.getId(), "chunkIndex", chunk.index(),
                        "documentName", entity.getName(), "category", entity.getCategory());
                vectors.add(new VectorDocument(vectorId, chunk.content(),
                        embeddingService.embed(chunk.content()), metadata));
                vectorIds.add(vectorId);

                DocumentChunkEntity record = new DocumentChunkEntity();
                record.setUserId(userId);
                record.setDocumentId(entity.getId());
                record.setChunkIndex(chunk.index());
                record.setContent(chunk.content());
                record.setTokenCount(chunk.estimatedTokenCount());
                record.setVectorId(vectorId);
                record.setMetadataJson(writeJson(metadata));
                records.add(record);
            }
            vectorStore.upsert(userId, vectors);
            for (DocumentChunkEntity record : records) mapper.insertChunk(record);
            mapper.updateStatus(entity.getId(), userId, KnowledgeStatus.READY, null);
        } catch (RuntimeException exception) {
            if (!vectorIds.isEmpty()) vectorStore.delete(userId, vectorIds);
            mapper.deleteChunks(entity.getId(), userId);
            mapper.updateStatus(entity.getId(), userId, KnowledgeStatus.FAILED, safeError(exception));
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public List<KnowledgeDocumentDto> list(long userId) {
        return mapper.findAll(userId).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<KnowledgeDocumentDto> listReady(long userId) {
        return list(userId).stream().filter(document -> document.status() == KnowledgeStatus.READY).toList();
    }

    @Transactional(readOnly = true)
    public KnowledgeDocumentDto requireReady(long userId, long documentId) {
        KnowledgeDocumentDto document = require(userId, documentId);
        if (document.status() != KnowledgeStatus.READY) {
            throw new BusinessException(ErrorCode.INVALID_STATE);
        }
        return document;
    }

    @Transactional(readOnly = true)
    public List<KnowledgeDocumentDto> requireReadyAll(long userId, List<Long> documentIds) {
        if (documentIds == null || documentIds.isEmpty()) return List.of();
        return new LinkedHashSet<>(documentIds).stream()
                .map(documentId -> requireReady(userId, documentId))
                .toList();
    }

    @Transactional(readOnly = true)
    public KnowledgeDetailDto detail(long userId, long documentId) {
        KnowledgeDocumentDto document = require(userId, documentId);
        return new KnowledgeDetailDto(document,
                mapper.findChunks(documentId, userId).stream().map(DocumentChunkEntity::getContent).toList());
    }

    @Transactional(readOnly = true)
    public List<Long> findChunkIds(long documentId, long userId) {
        return mapper.findChunks(documentId, userId).stream()
                .map(DocumentChunkEntity::getId).toList();
    }

    public List<KnowledgeSearchResultDto> search(long userId, String query, int limit) {
        return searchInternal(userId, query, limit, null);
    }

    public List<KnowledgeSearchResultDto> search(
            long userId, String query, int limit, Collection<Long> allowedDocumentIds
    ) {
        if (allowedDocumentIds == null || allowedDocumentIds.isEmpty()) return List.of();
        return searchInternal(userId, query, limit, allowedDocumentIds);
    }

    private List<KnowledgeSearchResultDto> searchInternal(
            long userId, String query, int limit, Collection<Long> allowedDocumentIds
    ) {
        if (query == null || query.isBlank()) throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        float[] embedding = embeddingService.embed(query.strip());
        int boundedLimit = Math.max(1, Math.min(limit, 20));
        var results = allowedDocumentIds == null
                ? vectorStore.search(userId, embedding, boundedLimit, 0.0)
                : vectorStore.search(userId, embedding, boundedLimit, 0.0, allowedDocumentIds);
        return results.stream()
                .map(result -> new KnowledgeSearchResultDto(
                        longMetadata(result.metadata(), "documentId"),
                        intMetadata(result.metadata(), "chunkIndex"),
                        String.valueOf(result.metadata().getOrDefault("documentName", "知识文档")),
                        result.content(), result.score()))
                .toList();
    }

    @Transactional
    public void updateMetadata(long userId, long documentId, String newName, String newCategory) {
        require(userId, documentId);
        if (newName == null || newName.isBlank()) throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        mapper.updateDocumentMeta(documentId, userId,
                newName.strip(),
                (newCategory == null || newCategory.isBlank()) ? "技术资料" : newCategory.strip());
    }

    @Transactional
    public void updateChunk(long userId, long chunkId, String newContent) {
        var chunks = mapper.findChunksByChunkId(chunkId, userId);
        if (chunks.isEmpty()) throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        DocumentChunkEntity entity = chunks.get(0);
        int estimatedTokens = Math.max(1, newContent.length() / 3);
        mapper.updateChunkContent(chunkId, userId, newContent.strip(), estimatedTokens);
        try {
            float[] embedding = embeddingService.embed(newContent.strip());
            List<VectorDocument> vectors = List.of(new VectorDocument(
                    entity.getVectorId(), newContent.strip(), embedding,
                    readJson(entity.getMetadataJson())));
            vectorStore.upsert(userId, vectors);
        } catch (RuntimeException ignored) {
            // Re-embedding failure should not block content save.
        }
    }

    @Transactional
    public void delete(long userId, long documentId) {
        KnowledgeDocumentEntity document = mapper.findById(documentId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));
        List<String> vectorIds = mapper.findChunks(documentId, userId).stream()
                .map(DocumentChunkEntity::getVectorId).toList();
        vectorStore.delete(userId, vectorIds);
        mapper.deleteChunks(documentId, userId);
        mapper.deletePlanLinks(documentId, userId);
        if (mapper.logicalDelete(documentId, userId) != 1) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        }
        fileStorageService.delete(userId, StorageCategory.DOCUMENTS, document.getStorageName());
    }

    private KnowledgeDocumentDto require(long userId, long documentId) {
        return mapper.findById(documentId, userId).map(this::toDto)
                .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));
    }

    private KnowledgeDocumentDto toDto(KnowledgeDocumentEntity entity) {
        return new KnowledgeDocumentDto(entity.getId(), entity.getName(), entity.getOriginalName(),
                entity.getFileType(), entity.getFileSize(), entity.getCategory(), entity.getStatus(),
                entity.getErrorMessage(), entity.getCreateTime(), entity.getUpdateTime());
    }

    private String extension(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private String baseName(String name) {
        int dot = name.lastIndexOf('.');
        return dot <= 0 ? name : name.substring(0, dot);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new SystemException(ErrorCode.SYSTEM_ERROR, exception);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readJson(String json) {
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            return Map.of();
        }
    }

    private long longMetadata(Map<String, Object> metadata, String key) {
        Object value = metadata.get(key);
        return value instanceof Number number ? number.longValue() : Long.parseLong(String.valueOf(value));
    }

    private int intMetadata(Map<String, Object> metadata, String key) {
        Object value = metadata.get(key);
        return value instanceof Number number ? number.intValue() : Integer.parseInt(String.valueOf(value));
    }

    private String safeError(RuntimeException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? "知识文档处理失败"
                : message.substring(0, Math.min(500, message.length()));
    }
}
