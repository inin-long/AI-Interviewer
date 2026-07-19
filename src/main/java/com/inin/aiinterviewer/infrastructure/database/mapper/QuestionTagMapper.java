package com.inin.aiinterviewer.infrastructure.database.mapper;

import com.inin.aiinterviewer.domain.entity.QuestionTagEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Optional;

public interface QuestionTagMapper {

    @Insert("""
            INSERT INTO question_tag(user_id, name, create_time, deleted)
            VALUES(#{userId}, #{name}, CURRENT_TIMESTAMP, 0)
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(QuestionTagEntity entity);

    @Select("""
            SELECT id, user_id, name, create_time, deleted
            FROM question_tag
            WHERE id = #{id} AND user_id = #{userId} AND deleted = 0
            LIMIT 1
            """)
    Optional<QuestionTagEntity> findByIdAndUserId(long id, long userId);

    @Select("""
            SELECT id, user_id, name, create_time, deleted
            FROM question_tag
            WHERE (user_id = #{userId} OR user_id = 1) AND deleted = 0
            ORDER BY name ASC, id ASC
            """)
    List<QuestionTagEntity> findAllByUserId(long userId);

    @Select("""
            SELECT id, user_id, name, create_time, deleted
            FROM question_tag
            WHERE user_id = #{userId} AND name = #{name} AND deleted = 0
            LIMIT 1
            """)
    Optional<QuestionTagEntity> findByNameAndUserId(String name, long userId);

    @Update("""
            UPDATE question_tag
            SET deleted = 1
            WHERE id = #{id} AND user_id = #{userId} AND deleted = 0
            """)
    int logicalDelete(long id, long userId);
}
