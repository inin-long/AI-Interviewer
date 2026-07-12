package com.inin.aiinterviewer.infrastructure.file;

import com.inin.aiinterviewer.domain.enums.StorageCategory;

import java.nio.file.Path;

public interface FileStorageService {
    StoredFile store(long userId, StorageCategory category, Path source);

    void delete(long userId, StorageCategory category, String storageName);
}

