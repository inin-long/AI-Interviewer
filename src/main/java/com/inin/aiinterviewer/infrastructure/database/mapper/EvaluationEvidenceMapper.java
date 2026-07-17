package com.inin.aiinterviewer.infrastructure.database.mapper;

import com.inin.aiinterviewer.domain.entity.EvaluationEvidenceEntity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface EvaluationEvidenceMapper {

    @Insert("""
            INSERT INTO evaluation_evidence(
                id, user_id, session_id, message_id, competency_code, signal,
                strength, confidence, reason, related_claim_ids_json, create_time)
            VALUES(#{id}, #{userId}, #{sessionId}, #{messageId}, #{competencyCode}, #{signal},
                   #{strength}, #{confidence}, #{reason}, #{relatedClaimIdsJson}, CURRENT_TIMESTAMP)
            """)
    int insert(EvaluationEvidenceEntity entity);

    @Select("""
            SELECT id, user_id, session_id, message_id, competency_code, signal,
                   strength, confidence, reason, related_claim_ids_json, create_time
            FROM evaluation_evidence
            WHERE user_id = #{userId} AND session_id = #{sessionId}
            ORDER BY create_time, id
            """)
    List<EvaluationEvidenceEntity> findAll(long userId, long sessionId);

    @Delete("""
            DELETE FROM evaluation_evidence
            WHERE user_id = #{userId} AND session_id = #{sessionId} AND message_id = #{messageId}
            """)
    int deleteByMessage(long userId, long sessionId, long messageId);

    @Delete("DELETE FROM evaluation_evidence WHERE user_id = #{userId} AND session_id = #{sessionId}")
    int deleteBySession(long userId, long sessionId);
}
