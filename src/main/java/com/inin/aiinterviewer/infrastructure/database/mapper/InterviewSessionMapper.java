package com.inin.aiinterviewer.infrastructure.database.mapper;

import com.inin.aiinterviewer.domain.entity.InterviewSessionEntity;
import com.inin.aiinterviewer.domain.enums.InterviewStage;
import com.inin.aiinterviewer.domain.enums.InterviewStatus;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Optional;
import java.util.List;

public interface InterviewSessionMapper {

    @Select("""
            SELECT id, user_id, plan_id, resume_id, profile_id, title, job_title, plan_snapshot_json,
                   profile_snapshot_json, knowledge_snapshot_json,
                   stage, status, prompt_version, started_time, completed_time,
                   create_time, update_time, deleted
            FROM interview_session
            WHERE user_id = #{userId} AND deleted = 0
            ORDER BY update_time DESC, id DESC
            """)
    List<InterviewSessionEntity> findAllByUserId(long userId);

    @Insert("""
            INSERT INTO interview_session(user_id, plan_id, resume_id, profile_id, title, job_title,
                                          plan_snapshot_json, profile_snapshot_json, knowledge_snapshot_json,
                                          stage, status, prompt_version,
                                          started_time, create_time, update_time, deleted)
            VALUES(#{userId}, #{planId}, #{resumeId}, #{profileId}, #{title}, #{jobTitle},
                   #{planSnapshotJson}, #{profileSnapshotJson}, #{knowledgeSnapshotJson},
                   #{stage}, #{status}, #{promptVersion},
                   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(InterviewSessionEntity entity);

    @Select("""
            SELECT id, user_id, plan_id, resume_id, profile_id, title, job_title, plan_snapshot_json,
                   profile_snapshot_json, knowledge_snapshot_json,
                   stage, status, prompt_version, started_time, completed_time,
                   create_time, update_time, deleted
            FROM interview_session
            WHERE id = #{id} AND user_id = #{userId} AND deleted = 0
            LIMIT 1
            """)
    Optional<InterviewSessionEntity> findByIdAndUserId(long id, long userId);

    @Select("""
            SELECT id, user_id, plan_id, resume_id, profile_id, title, job_title, plan_snapshot_json,
                   profile_snapshot_json, knowledge_snapshot_json,
                   stage, status, prompt_version, started_time, completed_time,
                   create_time, update_time, deleted
            FROM interview_session
            WHERE user_id = #{userId} AND plan_id = #{planId} AND deleted = 0
              AND status IN ('CREATED', 'RUNNING', 'PAUSED')
            ORDER BY id DESC
            LIMIT 1
            """)
    Optional<InterviewSessionEntity> findResumableByPlan(long userId, long planId);

    @Update("""
            UPDATE interview_session
            SET status = #{status}, update_time = CURRENT_TIMESTAMP
            WHERE id = #{id} AND user_id = #{userId} AND deleted = 0
            """)
    int updateStatus(long id, long userId, InterviewStatus status);

    @Update("""
            UPDATE interview_session
            SET stage = #{stage}, update_time = CURRENT_TIMESTAMP
            WHERE id = #{id} AND user_id = #{userId} AND deleted = 0
            """)
    int updateStage(long id, long userId, InterviewStage stage);

    @Update("""
            UPDATE interview_session
            SET stage = 'COMPLETED', status = 'COMPLETED', completed_time = CURRENT_TIMESTAMP,
                update_time = CURRENT_TIMESTAMP
            WHERE id = #{id} AND user_id = #{userId} AND deleted = 0
              AND status IN ('RUNNING', 'PAUSED')
            """)
    int complete(long id, long userId);

    @Update("""
            UPDATE interview_session
            SET deleted = 1, update_time = CURRENT_TIMESTAMP
            WHERE id = #{id} AND user_id = #{userId} AND deleted = 0
            """)
    int delete(long id, long userId);
}
