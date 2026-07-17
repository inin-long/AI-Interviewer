package com.inin.aiinterviewer.infrastructure.database.mapper;

import com.inin.aiinterviewer.domain.entity.AssessmentResultEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Optional;

public interface AssessmentResultMapper {

    @Insert("""
            INSERT INTO assessment_result(user_id, template_code, result_code, scores_json,
                                        report_markdown, create_time, deleted)
            VALUES(#{userId}, #{templateCode}, #{resultCode}, #{scoresJson},
                    #{reportMarkdown}, CURRENT_TIMESTAMP, 0)
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AssessmentResultEntity entity);

    @Select("""
            SELECT id, user_id, template_code, result_code, scores_json, report_markdown,
                   create_time, deleted
            FROM assessment_result
            WHERE id = #{id} AND user_id = #{userId} AND deleted = 0
            LIMIT 1
            """)
    Optional<AssessmentResultEntity> findByIdAndUserId(long id, long userId);

    @Select("""
            SELECT id, user_id, template_code, result_code, scores_json, report_markdown,
                   create_time, deleted
            FROM assessment_result
            WHERE user_id = #{userId} AND deleted = 0
            ORDER BY create_time DESC, id DESC
            """)
    List<AssessmentResultEntity> findAllByUserId(long userId);

    @Update("""
            UPDATE assessment_result
            SET deleted = 1
            WHERE id = #{id} AND user_id = #{userId} AND deleted = 0
            """)
    int logicalDelete(long id, long userId);
}
