package com.inin.aiinterviewer.infrastructure.database.mapper;

import com.inin.aiinterviewer.domain.entity.InterviewPlanEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Optional;

public interface InterviewPlanMapper {

    @Insert("""
            INSERT INTO interview_plan(user_id, name, job_title, job_description, difficulty,
                                       duration_minutes, question_count, resume_id, rules_json,
                                       profile_id, stages_json, domain_pack_id, is_default,
                                       create_time, update_time, deleted)
            VALUES(#{userId}, #{name}, #{jobTitle}, #{jobDescription}, #{difficulty},
                   #{durationMinutes}, #{questionCount}, #{resumeId}, #{rulesJson},
                   #{profileId}, #{stagesJson}, #{domainPackId}, #{defaultPlan},
                   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(InterviewPlanEntity entity);

    @Select("""
            SELECT id, user_id, name, job_title, job_description, difficulty, duration_minutes,
                   question_count, resume_id, profile_id, rules_json, stages_json, domain_pack_id, is_default,
                   create_time, update_time, deleted
            FROM interview_plan
            WHERE id = #{id} AND user_id = #{userId} AND deleted = 0
            LIMIT 1
            """)
    Optional<InterviewPlanEntity> findByIdAndUserId(long id, long userId);

    @Select("""
            SELECT id, user_id, name, job_title, job_description, difficulty, duration_minutes,
                   question_count, resume_id, profile_id, rules_json, stages_json, domain_pack_id, is_default,
                   create_time, update_time, deleted
            FROM interview_plan
            WHERE user_id = #{userId} AND deleted = 0
            ORDER BY is_default DESC, update_time DESC, id DESC
            """)
    List<InterviewPlanEntity> findAllByUserId(long userId);

    @Update("""
            UPDATE interview_plan
            SET name = #{name}, job_title = #{jobTitle}, job_description = #{jobDescription},
                difficulty = #{difficulty}, duration_minutes = #{durationMinutes},
                question_count = #{questionCount}, resume_id = #{resumeId},
                profile_id = #{profileId}, rules_json = #{rulesJson}, stages_json = #{stagesJson},
                domain_pack_id = #{domainPackId},
                update_time = CURRENT_TIMESTAMP
            WHERE id = #{id} AND user_id = #{userId} AND deleted = 0
            """)
    int update(InterviewPlanEntity entity);

    @Update("""
            UPDATE interview_plan
            SET deleted = 1, update_time = CURRENT_TIMESTAMP
            WHERE id = #{id} AND user_id = #{userId} AND deleted = 0
            """)
    int logicalDelete(long id, long userId);
}
