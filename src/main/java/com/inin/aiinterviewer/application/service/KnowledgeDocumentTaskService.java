package com.inin.aiinterviewer.application.service;

import com.inin.aiinterviewer.application.dto.KnowledgeDocumentDto;
import com.inin.aiinterviewer.domain.enums.BackgroundTaskType;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

@Service
public class KnowledgeDocumentTaskService {
    private final KnowledgeDocumentService knowledgeService;
    private final BackgroundTaskService taskService;

    public KnowledgeDocumentTaskService(
            KnowledgeDocumentService knowledgeService,
            BackgroundTaskService taskService
    ) {
        this.knowledgeService = knowledgeService;
        this.taskService = taskService;
    }

    public QueuedKnowledgeDocument uploadAndEnqueue(long userId, Path source, String category) {
        KnowledgeDocumentDto document = knowledgeService.upload(userId, source, category);
        try {
            long taskId = taskService.enqueue(userId, BackgroundTaskType.DOCUMENT_PARSE,
                    new KnowledgeDocumentTaskPayload(document.id()));
            return new QueuedKnowledgeDocument(document, taskId);
        } catch (RuntimeException exception) {
            knowledgeService.delete(userId, document.id());
            throw exception;
        }
    }

    public record QueuedKnowledgeDocument(KnowledgeDocumentDto document, long taskId) {
    }

    public record KnowledgeDocumentTaskPayload(long documentId) {
    }
}
