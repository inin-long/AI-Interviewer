package com.inin.aiinterviewer.infrastructure.file;

import com.inin.aiinterviewer.application.exception.ErrorCode;
import com.inin.aiinterviewer.application.exception.FileException;
import com.inin.aiinterviewer.domain.enums.StorageCategory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;

@Service
public class LocalFileStorageService implements FileStorageService {

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
            "pdf", "docx", "md", "txt", "png", "jpg", "jpeg");

    private final PathService pathService;

    public LocalFileStorageService(PathService pathService) {
        this.pathService = pathService;
    }

    @Override
    public StoredFile store(long userId, StorageCategory category, Path source) {
        Path normalizedSource = source == null ? null : source.toAbsolutePath().normalize();
        if (normalizedSource == null || !Files.isRegularFile(normalizedSource)) {
            throw new FileException(ErrorCode.FILE_NOT_FOUND);
        }
        String originalName = normalizedSource.getFileName().toString();
        validateExtension(originalName);

        Path target = pathService.newStoragePath(userId, category, originalName);
        try {
            Files.createDirectories(target.getParent());
            Files.copy(normalizedSource, target, StandardCopyOption.COPY_ATTRIBUTES);
            return new StoredFile(originalName, target.getFileName().toString(), target, Files.size(target));
        } catch (IOException exception) {
            throw new FileException(ErrorCode.FILE_STORAGE_FAILED, exception);
        }
    }

    @Override
    public void delete(long userId, StorageCategory category, String storageName) {
        Path target = pathService.resolveStoredPath(userId, category, storageName);
        try {
            Files.deleteIfExists(target);
        } catch (IOException exception) {
            throw new FileException(ErrorCode.FILE_STORAGE_FAILED, exception);
        }
    }

    private void validateExtension(String fileName) {
        int separator = fileName.lastIndexOf('.');
        String extension = separator < 0 ? "" : fileName.substring(separator + 1).toLowerCase(Locale.ROOT);
        if (!SUPPORTED_EXTENSIONS.contains(extension)) {
            throw new FileException(ErrorCode.FILE_TYPE_NOT_SUPPORTED);
        }
    }
}
