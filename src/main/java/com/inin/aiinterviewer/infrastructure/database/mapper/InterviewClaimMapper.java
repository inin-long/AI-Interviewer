package com.inin.aiinterviewer.infrastructure.database.mapper;

import com.inin.aiinterviewer.domain.entity.InterviewClaimEntity;
import com.inin.aiinterviewer.domain.enums.ClaimStatus;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface InterviewClaimMapper {

    @Insert("""
            INSERT INTO interview_claim(
                id, user_id, session_id, source_message_id, claim_type, content,
                importance, credibility, status, missing_evidence_json,
                supporting_evidence_ids_json, conflicting_evidence_ids_json,
                create_time, update_time)
            VALUES(#{id}, #{userId}, #{sessionId}, #{sourceMessageId}, #{claimType}, #{content},
                   #{importance}, #{credibility}, #{status}, #{missingEvidenceJson},
                   #{supportingEvidenceIdsJson}, #{conflictingEvidenceIdsJson},
                   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """)
    int insert(InterviewClaimEntity entity);

    @Select("""
            SELECT id, user_id, session_id, source_message_id, claim_type, content,
                   importance, credibility, status, missing_evidence_json,
                   supporting_evidence_ids_json, conflicting_evidence_ids_json,
                   create_time, update_time
            FROM interview_claim
            WHERE user_id = #{userId} AND session_id = #{sessionId}
            ORDER BY importance DESC, create_time, id
            """)
    List<InterviewClaimEntity> findAll(long userId, long sessionId);

    @Delete("""
            DELETE FROM interview_claim
            WHERE user_id = #{userId} AND session_id = #{sessionId}
              AND source_message_id = #{sourceMessageId}
            """)
    int deleteBySourceMessage(long userId, long sessionId, long sourceMessageId);

    @Update("""
            UPDATE interview_claim
            SET status = #{status}, credibility = #{credibility},
                supporting_evidence_ids_json = #{supportingEvidenceIdsJson},
                conflicting_evidence_ids_json = #{conflictingEvidenceIdsJson},
                update_time = CURRENT_TIMESTAMP
            WHERE id = #{id} AND user_id = #{userId} AND session_id = #{sessionId}
            """)
    int updateAssessment(
            String id,
            long userId,
            long sessionId,
            ClaimStatus status,
            double credibility,
            String supportingEvidenceIdsJson,
            String conflictingEvidenceIdsJson
    );

    @Delete("DELETE FROM interview_claim WHERE user_id = #{userId} AND session_id = #{sessionId}")
    int deleteBySession(long userId, long sessionId);
}
