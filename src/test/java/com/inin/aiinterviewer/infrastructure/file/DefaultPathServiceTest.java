package com.inin.aiinterviewer.infrastructure.file;

import com.inin.aiinterviewer.config.properties.AppProperties;
import com.inin.aiinterviewer.domain.enums.StorageCategory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultPathServiceTest {

    @TempDir
    Path applicationHome;

    @Test
    void alwaysResolvesGeneratedFilesInsideTheUsersCategory() {
        DefaultPathService service = new DefaultPathService(
                new AppProperties("AI Interviewer", "test", applicationHome.toString()));

        Path categoryRoot = service.categoryRoot(42, StorageCategory.RESUMES);
        Path generated = service.newStoragePath(42, StorageCategory.RESUMES, "../my:resume.pdf");

        assertThat(generated.normalize().startsWith(categoryRoot.normalize())).isTrue();
        assertThat(generated.getFileName().toString()).endsWith("my_resume.pdf");
    }
}
