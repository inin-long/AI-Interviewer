package com.inin.aiinterviewer.infrastructure.database.mapper;

import com.inin.aiinterviewer.domain.entity.EvaluationEntity;
import com.inin.aiinterviewer.domain.entity.InterviewReportEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.util.Optional;

public interface InterviewResultMapper {

    @Insert("""
            INSERT INTO evaluation(user_id, interview_id, overall_score, technical_score,
                                   problem_solving_score, project_score, system_design_score,
                                   communication_score, comprehensive_score, content_json,
                                   create_time, update_time, deleted)
            VALUES(#{userId}, #{interviewId}, #{overallScore}, #{technicalScore},
                   #{problemSolvingScore}, #{projectScore}, #{systemDesignScore},
                   #{communicationScore}, #{comprehensiveScore}, #{contentJson},
                   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertEvaluation(EvaluationEntity entity);

    @Insert("""
            INSERT INTO report(user_id, interview_id, evaluation_id, title, content_markdown,
                               score, status, create_time, update_time, deleted)
            VALUES(#{userId}, #{interviewId}, #{evaluationId}, #{title}, #{contentMarkdown},
                   #{score}, #{status}, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertReport(InterviewReportEntity entity);

    @Select("""
            SELECT id, user_id, interview_id, evaluation_id, title, content_markdown, score, status
            FROM report
            WHERE interview_id = #{interviewId} AND user_id = #{userId} AND deleted = 0
            LIMIT 1
            """)
    Optional<InterviewReportEntity> findReport(long userId, long interviewId);

    @Select("""
            SELECT id, user_id, interview_id, overall_score, technical_score,
                   problem_solving_score, project_score, system_design_score,
                   communication_score, comprehensive_score, content_json
            FROM evaluation
            WHERE interview_id = #{interviewId} AND user_id = #{userId} AND deleted = 0
            LIMIT 1
            """)
    Optional<EvaluationEntity> findEvaluation(long userId, long interviewId);
}
