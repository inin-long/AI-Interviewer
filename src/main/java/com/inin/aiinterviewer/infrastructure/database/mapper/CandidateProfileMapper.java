package com.inin.aiinterviewer.infrastructure.database.mapper;

import com.inin.aiinterviewer.domain.entity.CandidateProfileEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Optional;

public interface CandidateProfileMapper {

    @Select("""
            SELECT id, resume_id, user_id, content_json, source, status, error_message,
                   confirmed, create_time, update_time, deleted
            FROM candidate_profile
            WHERE resume_id = #{resumeId} AND user_id = #{userId} AND deleted = 0
            LIMIT 1
            """)
    Optional<CandidateProfileEntity> findByResumeIdAndUserId(long resumeId, long userId);

    @Insert("""
            INSERT INTO candidate_profile(resume_id, user_id, content_json, source, status,
                                          error_message, confirmed, create_time, update_time, deleted)
            VALUES(#{resumeId}, #{userId}, #{contentJson}, #{source}, #{status},
                   #{errorMessage}, #{confirmed}, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
            ON CONFLICT(resume_id) DO UPDATE SET
                content_json = excluded.content_json,
                source = excluded.source,
                status = excluded.status,
                error_message = excluded.error_message,
                confirmed = excluded.confirmed,
                update_time = CURRENT_TIMESTAMP,
                deleted = 0
            """)
    int upsert(CandidateProfileEntity entity);

    @Update("""
            UPDATE candidate_profile
            SET confirmed = 1, status = 'CONFIRMED', update_time = CURRENT_TIMESTAMP
            WHERE resume_id = #{resumeId} AND user_id = #{userId} AND deleted = 0
            """)
    int confirm(long resumeId, long userId);
}

