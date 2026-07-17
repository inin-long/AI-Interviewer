package com.inin.aiinterviewer.infrastructure.database.mapper;

import com.inin.aiinterviewer.domain.entity.AssessmentQuestionEntity;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;

public interface AssessmentQuestionMapper {

    @Select("""
            SELECT id, template_id, dimension, content, options_json, sort_order
            FROM assessment_question
            WHERE template_id = #{templateId}
            ORDER BY sort_order ASC, id ASC
            """)
    List<AssessmentQuestionEntity> findByTemplateId(long templateId);

    @Select("""
            SELECT id, template_id, dimension, content, options_json, sort_order
            FROM assessment_question
            WHERE id = #{id}
            LIMIT 1
            """)
    Optional<AssessmentQuestionEntity> findById(long id);
}
