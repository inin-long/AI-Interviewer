package com.inin.aiinterviewer.infrastructure.database.mapper;

import com.inin.aiinterviewer.domain.entity.ConsistencyIssueEntity;
import com.inin.aiinterviewer.domain.enums.ConsistencyIssueStatus;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Optional;

public interface ConsistencyIssueMapper {

    @Insert("""
            INSERT INTO consistency_issue(
                id, user_id, session_id, issue_type, status, description,
                related_claim_ids_json, clarification_question, resolution,
                create_time, update_time)
            VALUES(#{id}, #{userId}, #{sessionId}, #{issueType}, #{status}, #{description},
                   #{relatedClaimIdsJson}, #{clarificationQuestion}, #{resolution},
                   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            ON CONFLICT(session_id, issue_type, related_claim_ids_json) DO UPDATE SET
                description = excluded.description,
                clarification_question = excluded.clarification_question,
                update_time = CURRENT_TIMESTAMP
            WHERE consistency_issue.status IN ('POTENTIAL', 'CLARIFIED')
            """)
    int insertOrRefresh(ConsistencyIssueEntity entity);

    @Select("""
            SELECT id, user_id, session_id, issue_type, status, description,
                   related_claim_ids_json, clarification_message_id,
                   clarification_question, resolution, create_time, update_time
            FROM consistency_issue
            WHERE user_id = #{userId} AND session_id = #{sessionId}
            ORDER BY create_time, id
            """)
    List<ConsistencyIssueEntity> findAll(long userId, long sessionId);

    @Select("""
            SELECT id, user_id, session_id, issue_type, status, description,
                   related_claim_ids_json, clarification_message_id,
                   clarification_question, resolution, create_time, update_time
            FROM consistency_issue
            WHERE id = #{issueId} AND user_id = #{userId} AND session_id = #{sessionId}
            LIMIT 1
            """)
    Optional<ConsistencyIssueEntity> findById(String issueId, long userId, long sessionId);

    @Update("""
            UPDATE consistency_issue
            SET status = 'CLARIFIED', clarification_message_id = #{messageId},
                update_time = CURRENT_TIMESTAMP
            WHERE id = #{issueId} AND user_id = #{userId} AND session_id = #{sessionId}
              AND status = 'POTENTIAL'
            """)
    int markClarified(String issueId, long userId, long sessionId, long messageId);

    @Update("""
            UPDATE consistency_issue
            SET status = #{status}, resolution = #{resolution}, update_time = CURRENT_TIMESTAMP
            WHERE id = #{issueId} AND user_id = #{userId} AND session_id = #{sessionId}
              AND status = 'CLARIFIED'
            """)
    int resolve(
            String issueId,
            long userId,
            long sessionId,
            ConsistencyIssueStatus status,
            String resolution
    );

    @Delete("DELETE FROM consistency_issue WHERE user_id = #{userId} AND session_id = #{sessionId}")
    int deleteBySession(long userId, long sessionId);
}
