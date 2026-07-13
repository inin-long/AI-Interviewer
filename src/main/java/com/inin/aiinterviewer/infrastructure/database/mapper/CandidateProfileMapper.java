package com.inin.aiinterviewer.infrastructure.database.mapper;

import com.inin.aiinterviewer.domain.entity.CandidateProfileEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Optional;
import java.util.List;

public interface CandidateProfileMapper {

    @Select("""
            SELECT profile.id, profile.resume_id, profile.user_id, profile.content_json,
                   profile.source, profile.status, profile.error_message, profile.confirmed,
                   profile.create_time, profile.update_time, profile.deleted,
                   resume.original_name AS resume_name
            FROM candidate_profile profile
            JOIN resume ON resume.id = profile.resume_id AND resume.user_id = profile.user_id
            WHERE profile.resume_id = #{resumeId} AND profile.user_id = #{userId}
              AND profile.deleted = 0 AND resume.deleted = 0
            LIMIT 1
            """)
    Optional<CandidateProfileEntity> findByResumeIdAndUserId(long resumeId, long userId);

    @Select("""
            SELECT profile.id, profile.resume_id, profile.user_id, profile.content_json,
                   profile.source, profile.status, profile.error_message, profile.confirmed,
                   profile.create_time, profile.update_time, profile.deleted,
                   resume.original_name AS resume_name
            FROM candidate_profile profile
            JOIN resume ON resume.id = profile.resume_id AND resume.user_id = profile.user_id
            WHERE profile.id = #{id} AND profile.user_id = #{userId}
              AND profile.deleted = 0 AND resume.deleted = 0
            LIMIT 1
            """)
    Optional<CandidateProfileEntity> findByIdAndUserId(long id, long userId);

    @Select("""
            SELECT profile.id, profile.resume_id, profile.user_id, profile.content_json,
                   profile.source, profile.status, profile.error_message, profile.confirmed,
                   profile.create_time, profile.update_time, profile.deleted,
                   resume.original_name AS resume_name
            FROM candidate_profile profile
            JOIN resume ON resume.id = profile.resume_id AND resume.user_id = profile.user_id
            WHERE profile.user_id = #{userId} AND profile.deleted = 0 AND resume.deleted = 0
            ORDER BY profile.update_time DESC, profile.id DESC
            """)
    List<CandidateProfileEntity> findAllByUserId(long userId);

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
