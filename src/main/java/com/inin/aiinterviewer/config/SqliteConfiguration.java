package com.inin.aiinterviewer.config;

import com.inin.aiinterviewer.config.properties.AppProperties;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
public class SqliteConfiguration {

    @Bean
    DataSource dataSource(AppProperties appProperties) {
        Path database = Path.of(appProperties.dataRoot(), "database", "app.db")
                .toAbsolutePath()
                .normalize();
        try {
            Files.createDirectories(database.getParent());
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot create SQLite database directory: " + database.getParent(), exception);
        }

        SQLiteConfig config = new SQLiteConfig();
        config.enforceForeignKeys(true);
        config.setBusyTimeout(5_000);
        config.setJournalMode(SQLiteConfig.JournalMode.WAL);
        config.setSynchronous(SQLiteConfig.SynchronousMode.NORMAL);

        SQLiteDataSource dataSource = new SQLiteDataSource(config);
        dataSource.setUrl("jdbc:sqlite:" + database);
        return dataSource;
    }
}
