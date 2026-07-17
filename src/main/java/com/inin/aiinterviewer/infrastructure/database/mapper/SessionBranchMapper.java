package com.inin.aiinterviewer.infrastructure.database.mapper;

import com.inin.aiinterviewer.domain.entity.SessionBranchEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Optional;

public interface SessionBranchMapper {

    @Insert("""
            INSERT INTO session_branch(
                id, user_id, source_session_id, source_checkpoint_id, parent_branch_id,
                source_question_number, title, status, source_state_json, original_question,
                original_answer, new_answer, comparison_json, comparison_markdown, error_message,
                create_time, update_time, deleted)
            VALUES(#{id}, #{userId}, #{sourceSessionId}, #{sourceCheckpointId}, #{parentBranchId},
                   #{sourceQuestionNumber}, #{title}, #{status}, #{sourceStateJson}, #{originalQuestion},
                   #{originalAnswer}, '', '{}', '', '', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
            """)
    int insert(SessionBranchEntity entity);

    @Select("""
            SELECT id, user_id, source_session_id, source_checkpoint_id, parent_branch_id,
                   source_question_number, title, status, source_state_json, original_question,
                   original_answer, new_answer, comparison_json, comparison_markdown, error_message,
                   create_time, update_time, deleted
            FROM session_branch
            WHERE id = #{id} AND user_id = #{userId} AND deleted = 0
            LIMIT 1
            """)
    Optional<SessionBranchEntity> findById(String id, long userId);

    @Select("""
            SELECT id, user_id, source_session_id, source_checkpoint_id, parent_branch_id,
                   source_question_number, title, status, source_state_json, original_question,
                   original_answer, new_answer, comparison_json, comparison_markdown, error_message,
                   create_time, update_time, deleted
            FROM session_branch
            WHERE user_id = #{userId} AND source_session_id = #{sourceSessionId} AND deleted = 0
            ORDER BY create_time DESC, id DESC
            """)
    List<SessionBranchEntity> findAll(long userId, long sourceSessionId);

    @Update("""
            UPDATE session_branch
            SET status = 'PROCESSING', new_answer = #{newAnswer}, error_message = '',
                update_time = CURRENT_TIMESTAMP
            WHERE id = #{id} AND user_id = #{userId} AND deleted = 0
              AND status IN ('DRAFT', 'FAILED')
            """)
    int startComparison(String id, long userId, String newAnswer);

    @Update("""
            UPDATE session_branch
            SET status = 'COMPLETED', comparison_json = #{comparisonJson},
                comparison_markdown = #{comparisonMarkdown}, error_message = '',
                update_time = CURRENT_TIMESTAMP
            WHERE id = #{id} AND user_id = #{userId} AND deleted = 0 AND status = 'PROCESSING'
            """)
    int complete(String id, long userId, String comparisonJson, String comparisonMarkdown);

    @Update("""
            UPDATE session_branch
            SET status = 'FAILED', error_message = #{errorMessage}, update_time = CURRENT_TIMESTAMP
            WHERE id = #{id} AND user_id = #{userId} AND deleted = 0 AND status = 'PROCESSING'
            """)
    int fail(String id, long userId, String errorMessage);
}
