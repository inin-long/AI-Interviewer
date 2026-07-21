package com.inin.aiinterviewer.infrastructure.database;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class FlywayMigrationIntegrationTest {

    private static final Pattern VERSIONED_MIGRATION = Pattern.compile("^V(\\d+)__.+\\.sql$");

    @TempDir
    Path tempDirectory;

    @Test
    void migrationVersionsAreUnique() throws Exception {
        Path migrationDirectory = Path.of("src", "main", "resources", "db", "migration");
        List<String> fileNames;
        try (var files = Files.list(migrationDirectory)) {
            fileNames = files.filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .sorted().toList();
        }

        Map<String, List<String>> byVersion = fileNames.stream().collect(Collectors.groupingBy(file -> {
            var matcher = VERSIONED_MIGRATION.matcher(file);
            assertThat(matcher.matches())
                    .as("Flyway migration name %s", file).isTrue();
            return matcher.group(1);
        }));

        assertThat(byVersion).allSatisfy((version, files) ->
                assertThat(files).as("Flyway version V%s", version).hasSize(1));
    }

    @Test
    void upgradesAnS1DatabaseFromVersion21ToLatest() throws Exception {
        Path database = tempDirectory.resolve("upgrade.db");
        String url = "jdbc:sqlite:" + database.toAbsolutePath();
        Flyway.configure()
                .dataSource(url, null, null)
                .locations("classpath:db/migration")
                .target(MigrationVersion.fromVersion("21"))
                .load().migrate();

        Flyway latest = Flyway.configure()
                .dataSource(url, null, null)
                .locations("classpath:db/migration")
                .load();
        latest.migrate();

        assertThat(latest.info().current().getVersion().getVersion()).isEqualTo("39");
        try (var connection = DriverManager.getConnection(url);
             var statement = connection.createStatement();
             var result = statement.executeQuery("""
                     SELECT COUNT(*)
                     FROM sqlite_master
                     WHERE type = 'table'
                       AND name IN ('domain_pack', 'session_branch', 'interview_question',
                                    'assessment_result', 'skill_article', 'career_plan',
                                    'knowledge_category', 'interview_plan_category')
                     """)) {
            assertThat(result.next()).isTrue();
            assertThat(result.getInt(1)).isEqualTo(8);
        }
    }
}
