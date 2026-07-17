package com.inin.aiinterviewer.infrastructure.database.mapper;

import com.inin.aiinterviewer.application.dto.QuestionTagRelDto;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface QuestionTagRelMapper {

    @Insert("""
            INSERT INTO question_tag_rel(question_id, tag_id, user_id, create_time)
            VALUES(#{questionId}, #{tagId}, #{userId}, CURRENT_TIMESTAMP)
            """)
    int insert(long questionId, long tagId, long userId);

    @Update("""
            UPDATE question_tag_rel
            SET question_id = question_id
            WHERE question_id = #{questionId} AND user_id = #{userId}
            """)
    int touch(long questionId, long userId);

    @Select("""
            SELECT r.question_id AS questionId, t.name AS tagName
            FROM question_tag_rel r
            JOIN question_tag t ON t.id = r.tag_id
            WHERE r.user_id = #{userId} AND t.deleted = 0
            """)
    List<QuestionTagRelDto> findAllByUserId(long userId);

    @Update("""
            DELETE FROM question_tag_rel
            WHERE question_id = #{questionId} AND user_id = #{userId}
            """)
    int deleteByQuestion(long questionId, long userId);
}
