package com.inin.aiinterviewer.infrastructure.file;

import com.inin.aiinterviewer.domain.enums.StorageCategory;

import java.nio.file.Path;

public interface PathService {
    Path applicationRoot();

    Path userRoot(long userId);

    Path categoryRoot(long userId, StorageCategory category);

    Path newStoragePath(long userId, StorageCategory category, String originalName);

    Path resolveStoredPath(long userId, StorageCategory category, String storageName);
}

