package com.inin.aiinterviewer.application.service;

import com.inin.aiinterviewer.application.exception.BusinessException;
import com.inin.aiinterviewer.application.exception.ErrorCode;
import com.inin.aiinterviewer.domain.enums.StorageCategory;
import com.inin.aiinterviewer.infrastructure.file.FileStorageService;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class InterviewPlanAssetService {

    public static final String ICON_PATH_RULE = "planIconPath";
    private static final long MAX_ICON_BYTES = 5L * 1024 * 1024;

    private final FileStorageService storageService;

    public InterviewPlanAssetService(FileStorageService storageService) {
        this.storageService = storageService;
    }

    public String storeIcon(long userId, Path source) {
        validate(source);
        return storageService.store(userId, StorageCategory.PLAN_ASSETS, source).path().toString();
    }

    private void validate(Path source) {
        try {
            if (source == null || !Files.isRegularFile(source) || Files.size(source) > MAX_ICON_BYTES
                    || ImageIO.read(source.toFile()) == null) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED);
            }
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
    }
}
