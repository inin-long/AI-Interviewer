package com.inin.aiinterviewer.application.service;

import com.inin.aiinterviewer.application.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class DomainPackServiceIntegrationTest {
    @TempDir
    static Path applicationHome;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("ai.interviewer.home", () -> applicationHome.toString());
    }

    @Autowired private DomainPackService domainPackService;

    @Test
    void loadsPersistsIndexesAndSnapshotsBuiltInDomainPacks() {
        assertThat(domainPackService.list())
                .extracting(pack -> pack.roleCode())
                .contains("java-backend", "full-stack", "product-manager");

        assertThat(domainPackService.search("Redis 缓存故障", 10))
                .extracting(pack -> pack.id())
                .contains("java-backend-1.0.0");

        var snapshot = domainPackService.snapshot("java-backend-1.0.0");
        assertThat(snapshot.id()).isEqualTo("java-backend-1.0.0");
        assertThat(snapshot.version()).isEqualTo("1.0.0");
        assertThat(snapshot.content().competencies()).isNotEmpty();
        assertThat(snapshot.content().failurePatterns()).isNotEmpty();
        assertThat(snapshot.content().probePlaybooks()).isNotEmpty();
        assertThat(snapshot.content().scenarios()).isNotEmpty();
        assertThat(snapshot.content().rubrics()).isNotEmpty();
        assertThat(Files.isDirectory(applicationHome.resolve("domain-packs").resolve("index"))).isTrue();

        assertThatThrownBy(() -> domainPackService.require("missing-pack"))
                .isInstanceOf(BusinessException.class);
    }
}
