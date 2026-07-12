package com.inin.aiinterviewer.infrastructure.file;

import java.nio.file.Path;

public record StoredFile(String originalName, String storageName, Path path, long size) {
}

