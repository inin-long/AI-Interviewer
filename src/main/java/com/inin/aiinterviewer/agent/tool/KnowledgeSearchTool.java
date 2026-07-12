package com.inin.aiinterviewer.agent.tool;

import com.inin.aiinterviewer.application.service.KnowledgeDocumentService;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class KnowledgeSearchTool implements AgentTool {

    private final KnowledgeDocumentService knowledgeService;

    public KnowledgeSearchTool(KnowledgeDocumentService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    @Override
    public String name() {
        return "knowledge_search";
    }

    @Override
    public String description() {
        return "在当前用户私有知识库中检索与面试问题相关的片段";
    }

    @Override
    public ToolResult execute(ToolInput input) {
        Object queryValue = input.arguments().get("query");
        if (queryValue == null || String.valueOf(queryValue).isBlank()) {
            return ToolResult.failure("query is required");
        }
        int limit = input.arguments().get("limit") instanceof Number number ? number.intValue() : 3;
        try {
            var results = knowledgeService.search(input.userId(), String.valueOf(queryValue), limit);
            var items = results.stream().map(result -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("documentId", result.documentId());
                item.put("documentName", result.documentName());
                item.put("content", result.content());
                item.put("score", result.score());
                return Map.copyOf(item);
            }).toList();
            return ToolResult.success(Map.of("results", items));
        } catch (RuntimeException exception) {
            return ToolResult.failure("knowledge search unavailable");
        }
    }
}
