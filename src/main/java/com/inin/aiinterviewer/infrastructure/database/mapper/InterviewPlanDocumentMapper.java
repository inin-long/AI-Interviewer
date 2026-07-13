package com.inin.aiinterviewer.infrastructure.database.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface InterviewPlanDocumentMapper {

    @Insert("""
            INSERT INTO interview_plan_document(plan_id, document_id, user_id, create_time)
            VALUES(#{planId}, #{documentId}, #{userId}, CURRENT_TIMESTAMP)
            """)
    int insert(long planId, long documentId, long userId);

    @Delete("""
            DELETE FROM interview_plan_document
            WHERE plan_id = #{planId} AND user_id = #{userId}
            """)
    int deleteByPlan(long planId, long userId);

    @Select("""
            SELECT document_id
            FROM interview_plan_document
            WHERE plan_id = #{planId} AND user_id = #{userId}
            ORDER BY create_time, document_id
            """)
    List<Long> findDocumentIds(long planId, long userId);
}
