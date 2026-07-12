package com.inin.aiinterviewer;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
@MapperScan("com.inin.aiinterviewer.infrastructure.database.mapper")
public class AiInterviewerApplication {
}

