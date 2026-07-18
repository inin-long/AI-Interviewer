package com.inin.aiinterviewer.domain.entity;

public class KnowledgeCategoryEntity {
    private String name;
    private long documentCount;
    private long readyCount;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public long getDocumentCount() { return documentCount; }
    public void setDocumentCount(long documentCount) { this.documentCount = documentCount; }
    public long getReadyCount() { return readyCount; }
    public void setReadyCount(long readyCount) { this.readyCount = readyCount; }
}
