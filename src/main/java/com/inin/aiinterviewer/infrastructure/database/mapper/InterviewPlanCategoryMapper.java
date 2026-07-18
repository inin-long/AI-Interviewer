package com.inin.aiinterviewer.infrastructure.database.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface InterviewPlanCategoryMapper {

    @Insert("""
            INSERT INTO interview_plan_category(plan_id, category, user_id, create_time)
            VALUES(#{planId}, #{category}, #{userId}, CURRENT_TIMESTAMP)
            """)
    int insert(long planId, String category, long userId);

    @Delete("""
            DELETE FROM interview_plan_category
            WHERE plan_id = #{planId} AND user_id = #{userId}
            """)
    int deleteByPlan(long planId, long userId);

    @Select("""
            SELECT category
            FROM interview_plan_category
            WHERE plan_id = #{planId} AND user_id = #{userId}
            ORDER BY create_time, category
            """)
    List<String> findCategories(long planId, long userId);
}
