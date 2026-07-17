package com.inin.aiinterviewer.infrastructure.database.mapper;

import com.inin.aiinterviewer.domain.entity.AssessmentTemplateEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;

public interface AssessmentTemplateMapper {

    @Select("""
            SELECT id, code, title, description, create_time
            FROM assessment_template
            ORDER BY id ASC
            """)
    List<AssessmentTemplateEntity> findAll();

    @Select("""
            SELECT id, code, title, description, create_time
            FROM assessment_template
            WHERE code = #{code}
            LIMIT 1
            """)
    Optional<AssessmentTemplateEntity> findByCode(String code);
}
