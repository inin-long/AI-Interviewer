package com.inin.aiinterviewer.infrastructure.database.mapper;

import com.inin.aiinterviewer.domain.entity.ResumeOptimizationEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Optional;

public interface ResumeOptimizationMapper {

    @Insert("""
            INSERT INTO resume_optimization(user_id, original_text, optimized_text, highlights_json,
                                          create_time, deleted)
            VALUES(#{userId}, #{originalText}, #{optimizedText}, #{highlightsJson},
                    CURRENT_TIMESTAMP, 0)
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ResumeOptimizationEntity entity);

    @Select("""
            SELECT id, user_id, original_text, optimized_text, highlights_json, create_time, deleted
            FROM resume_optimization
            WHERE id = #{id} AND user_id = #{userId} AND deleted = 0
            LIMIT 1
            """)
    Optional<ResumeOptimizationEntity> findByIdAndUserId(long id, long userId);

    @Select("""
            SELECT id, user_id, original_text, optimized_text, highlights_json, create_time, deleted
            FROM resume_optimization
            WHERE user_id = #{userId} AND deleted = 0
            ORDER BY create_time DESC, id DESC
            """)
    List<ResumeOptimizationEntity> findAllByUserId(long userId);

    @Update("""
            UPDATE resume_optimization
            SET deleted = 1
            WHERE id = #{id} AND user_id = #{userId} AND deleted = 0
            """)
    int logicalDelete(long id, long userId);
}
