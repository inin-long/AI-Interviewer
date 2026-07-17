package com.inin.aiinterviewer.infrastructure.database.mapper;

import com.inin.aiinterviewer.domain.entity.CareerPlanEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Optional;

public interface CareerPlanMapper {

    @Insert("""
            INSERT INTO career_plan(user_id, current_role, target_role, industry, experience_years,
                                   plan_markdown, create_time, deleted)
            VALUES(#{userId}, #{currentRole}, #{targetRole}, #{industry}, #{experienceYears},
                    #{planMarkdown}, CURRENT_TIMESTAMP, 0)
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CareerPlanEntity entity);

    @Select("""
            SELECT id, user_id, current_role, target_role, industry, experience_years,
                   plan_markdown, create_time, deleted
            FROM career_plan
            WHERE id = #{id} AND user_id = #{userId} AND deleted = 0
            LIMIT 1
            """)
    Optional<CareerPlanEntity> findByIdAndUserId(long id, long userId);

    @Select("""
            SELECT id, user_id, current_role, target_role, industry, experience_years,
                   plan_markdown, create_time, deleted
            FROM career_plan
            WHERE user_id = #{userId} AND deleted = 0
            ORDER BY create_time DESC, id DESC
            """)
    List<CareerPlanEntity> findAllByUserId(long userId);

    @Update("""
            UPDATE career_plan
            SET deleted = 1
            WHERE id = #{id} AND user_id = #{userId} AND deleted = 0
            """)
    int logicalDelete(long id, long userId);
}
