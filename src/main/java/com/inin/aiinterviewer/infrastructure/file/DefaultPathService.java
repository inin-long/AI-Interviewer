package com.inin.aiinterviewer.infrastructure.file;

import com.inin.aiinterviewer.config.properties.AppProperties;
import com.inin.aiinterviewer.domain.enums.StorageCategory;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.UUID;

@Service
public class DefaultPathService implements PathService {

    private final Path applicationRoot;

    public DefaultPathService(AppProperties appProperties) {
        this.applicationRoot = Path.of(appProperties.dataRoot()).toAbsolutePath().normalize();
    }

    @Override
    public Path applicationRoot() {
        return applicationRoot;
    }

    @Override
    public Path userRoot(long userId) {
        requireValidUserId(userId);
        return applicationRoot.resolve("users").resolve(Long.toString(userId)).normalize();
    }

    @Override
    public Path categoryRoot(long userId, StorageCategory category) {
        return userRoot(userId).resolve(category.directoryName()).normalize();
    }

    @Override
    public Path newStoragePath(long userId, StorageCategory category, String originalName) {
        String safeName = sanitizeFileName(originalName);
        String prefix = UUID.randomUUID().toString().substring(0, 8);
        return resolveStoredPath(userId, category, prefix + "_" + safeName);
    }

    @Override
    public Path resolveStoredPath(long userId, StorageCategory category, String storageName) {
        Path root = categoryRoot(userId, category);
        Path resolved = root.resolve(sanitizeFileName(storageName)).normalize();
        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException("Resolved path escaped user storage root");
        }
        return resolved;
    }

    private String sanitizeFileName(String originalName) {
        if (originalName == null || originalName.isBlank()) {
            throw new IllegalArgumentException("File name must not be blank");
        }
        String slashNormalized = originalName.replace('\\', '/');
        int lastSeparator = slashNormalized.lastIndexOf('/');
        String fileNameOnly = lastSeparator >= 0
                ? slashNormalized.substring(lastSeparator + 1)
                : slashNormalized;
        String safeName = fileNameOnly
                .replaceAll("[\\p{Cntrl}<>:\"/\\\\|?*]", "_")
                .trim();
        if (safeName.isBlank() || safeName.equals(".") || safeName.equals("..")) {
            throw new IllegalArgumentException("File name is invalid");
        }
        return safeName.length() > 180 ? safeName.substring(safeName.length() - 180) : safeName;
    }

    private void requireValidUserId(long userId) {
        if (userId <= 0) {
            throw new IllegalArgumentException("User id must be positive");
        }
    }
}
