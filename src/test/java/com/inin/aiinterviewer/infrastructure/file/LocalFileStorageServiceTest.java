package com.inin.aiinterviewer.infrastructure.file;

import com.inin.aiinterviewer.application.exception.FileException;
import com.inin.aiinterviewer.config.properties.AppProperties;
import com.inin.aiinterviewer.domain.enums.StorageCategory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalFileStorageServiceTest {

    @TempDir
    Path tempDirectory;

    @Test
    void storesSupportedFileWithCollisionSafeName() throws IOException {
        Path home = Files.createDirectory(tempDirectory.resolve("home"));
        Path source = tempDirectory.resolve("resume.md");
        Files.writeString(source, "# Resume");
        PathService pathService = new DefaultPathService(new AppProperties("AI", "test", home.toString()));
        LocalFileStorageService service = new LocalFileStorageService(pathService);

        StoredFile stored = service.store(1, StorageCategory.RESUMES, source);

        assertThat(stored.path()).exists();
        assertThat(stored.storageName()).endsWith("_resume.md");
        assertThat(stored.path()).startsWith(home.resolve("users").resolve("1").resolve("resumes"));
    }

    @Test
    void rejectsUnsupportedFileType() throws IOException {
        Path home = Files.createDirectory(tempDirectory.resolve("home"));
        Path source = tempDirectory.resolve("archive.exe");
        Files.write(source, new byte[]{1, 2, 3});
        PathService pathService = new DefaultPathService(new AppProperties("AI", "test", home.toString()));
        LocalFileStorageService service = new LocalFileStorageService(pathService);

        assertThatThrownBy(() -> service.store(1, StorageCategory.DOCUMENTS, source))
                .isInstanceOf(FileException.class);
    }
}
