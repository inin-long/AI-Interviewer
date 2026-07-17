package com.inin.aiinterviewer.infrastructure.database.mapper;

import com.inin.aiinterviewer.domain.entity.JobPositionEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Optional;

public interface JobPositionMapper {

    @Insert("""
            INSERT INTO job_position(user_id, title, department, description, create_time, update_time, deleted)
            VALUES(#{userId}, #{title}, #{department}, #{description}, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(JobPositionEntity entity);

    @Select("""
            SELECT id, user_id, title, department, description, create_time, update_time, deleted
            FROM job_position
            WHERE id = #{id} AND user_id = #{userId} AND deleted = 0
            LIMIT 1
            """)
    Optional<JobPositionEntity> findByIdAndUserId(long id, long userId);

    @Select("""
            SELECT id, user_id, title, department, description, create_time, update_time, deleted
            FROM job_position
            WHERE user_id = #{userId} AND deleted = 0
            ORDER BY update_time DESC, id DESC
            """)
    List<JobPositionEntity> findAllByUserId(long userId);

    @Update("""
            UPDATE job_position
            SET title = #{title}, department = #{department}, description = #{description},
                update_time = CURRENT_TIMESTAMP
            WHERE id = #{id} AND user_id = #{userId} AND deleted = 0
            """)
    int update(long id, long userId, String title, String department, String description);

    @Update("""
            UPDATE job_position
            SET deleted = 1, update_time = CURRENT_TIMESTAMP
            WHERE id = #{id} AND user_id = #{userId} AND deleted = 0
            """)
    int logicalDelete(long id, long userId);
}
