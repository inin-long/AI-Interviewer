package com.inin.aiinterviewer.infrastructure.database.mapper;

import com.inin.aiinterviewer.domain.entity.AssessmentAnswerEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;

public interface AssessmentAnswerMapper {

    @Insert("""
            INSERT INTO assessment_answer(result_id, question_id, option_index)
            VALUES(#{resultId}, #{questionId}, #{optionIndex})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AssessmentAnswerEntity entity);
}
