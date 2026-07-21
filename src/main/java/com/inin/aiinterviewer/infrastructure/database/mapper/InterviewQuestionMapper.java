package com.inin.aiinterviewer.infrastructure.database.mapper;

import com.inin.aiinterviewer.domain.entity.InterviewQuestionEntity;
import com.inin.aiinterviewer.domain.enums.InterviewDifficulty;
import com.inin.aiinterviewer.domain.enums.QuestionCategory;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Optional;

public interface InterviewQuestionMapper {

    @Insert("""
            INSERT INTO interview_question(user_id, job_id, category, title, content, reference_answer,
                                          difficulty, create_time, update_time, deleted)
            VALUES(#{userId}, #{jobId}, #{category}, #{title}, #{content}, #{referenceAnswer},
                    #{difficulty}, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(InterviewQuestionEntity entity);

    @Select("""
            SELECT id, user_id, job_id, category, title, content, reference_answer, difficulty,
                   create_time, update_time, deleted
            FROM interview_question
            WHERE id = #{id} AND user_id = #{userId} AND deleted = 0
            LIMIT 1
            """)
    Optional<InterviewQuestionEntity> findByIdAndUserId(long id, long userId);

    @Select("""
            SELECT id, user_id, job_id, category, title, content, reference_answer, difficulty,
                   create_time, update_time, deleted
            FROM interview_question
            WHERE (user_id = #{userId} OR user_id = 1) AND deleted = 0
            ORDER BY update_time DESC, id DESC
            """)
    List<InterviewQuestionEntity> findAllByUserId(long userId);

    @Update("""
            UPDATE interview_question
            SET job_id = #{jobId}, category = #{category}, title = #{title}, content = #{content},
                reference_answer = #{referenceAnswer}, difficulty = #{difficulty},
                update_time = CURRENT_TIMESTAMP
            WHERE id = #{id} AND user_id = #{userId} AND deleted = 0
            """)
    int update(long id, long userId, Long jobId, QuestionCategory category, String title,
               String content, String referenceAnswer, InterviewDifficulty difficulty);

    @Update("""
            UPDATE interview_question
            SET deleted = 1, update_time = CURRENT_TIMESTAMP
            WHERE id = #{id} AND user_id = #{userId} AND deleted = 0
            """)
    int logicalDelete(long id, long userId);
}
