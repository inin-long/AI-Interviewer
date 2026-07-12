package com.inin.aiinterviewer.domain.enums;

public enum StorageCategory {
    RESUMES("resumes"),
    DOCUMENTS("documents"),
    REPORTS("reports"),
    VECTOR("vector");

    private final String directoryName;

    StorageCategory(String directoryName) {
        this.directoryName = directoryName;
    }

    public String directoryName() {
        return directoryName;
    }
}

