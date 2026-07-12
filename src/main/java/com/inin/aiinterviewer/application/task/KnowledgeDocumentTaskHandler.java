package com.inin.aiinterviewer.application.task;

import com.inin.aiinterviewer.application.service.KnowledgeDocumentService;
import com.inin.aiinterviewer.application.service.KnowledgeDocumentTaskService.KnowledgeDocumentTaskPayload;
import com.inin.aiinterviewer.domain.enums.BackgroundTaskType;
import org.springframework.stereotype.Component;

@Component
public class KnowledgeDocumentTaskHandler implements BackgroundTaskHandler {
    private final KnowledgeDocumentService knowledgeService;

    public KnowledgeDocumentTaskHandler(KnowledgeDocumentService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    @Override
    public BackgroundTaskType taskType() {
        return BackgroundTaskType.DOCUMENT_PARSE;
    }

    @Override
    public void handle(BackgroundTaskContext context) {
        KnowledgeDocumentTaskPayload payload = context.payload(KnowledgeDocumentTaskPayload.class);
        knowledgeService.processDocument(context.userId(), payload.documentId());
    }
}
