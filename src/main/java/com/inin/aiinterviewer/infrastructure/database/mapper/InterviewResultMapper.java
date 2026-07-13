package com.inin.aiinterviewer.infrastructure.database.mapper;

import com.inin.aiinterviewer.domain.entity.EvaluationEntity;
import com.inin.aiinterviewer.domain.entity.InterviewReportEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

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
                               score, status, error_message, create_time, update_time, deleted)
            VALUES(#{userId}, #{interviewId}, #{evaluationId}, #{title}, #{contentMarkdown},
                   #{score}, #{status}, #{errorMessage}, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertReport(InterviewReportEntity entity);

    @Select("""
            SELECT id, user_id, interview_id, evaluation_id, title, content_markdown,
                   score, status, error_message
            FROM report
            WHERE interview_id = #{interviewId} AND user_id = #{userId} AND deleted = 0
            LIMIT 1
            """)
    Optional<InterviewReportEntity> findReport(long userId, long interviewId);

    @Update("""
            UPDATE report
            SET status = 'GENERATING', error_message = NULL, update_time = CURRENT_TIMESTAMP
            WHERE id = #{reportId} AND user_id = #{userId} AND deleted = 0
              AND status IN ('GENERATING', 'FAILED')
            """)
    int restartReport(long reportId, long userId);

    @Update("""
            UPDATE report
            SET evaluation_id = #{evaluationId}, content_markdown = #{contentMarkdown},
                score = #{score}, status = 'COMPLETED', error_message = NULL,
                update_time = CURRENT_TIMESTAMP
            WHERE id = #{reportId} AND user_id = #{userId} AND deleted = 0
              AND status = 'GENERATING'
            """)
    int completeReport(
            long reportId,
            long userId,
            long evaluationId,
            String contentMarkdown,
            int score
    );

    @Update("""
            UPDATE report
            SET status = 'FAILED', error_message = #{errorMessage}, update_time = CURRENT_TIMESTAMP
            WHERE id = #{reportId} AND user_id = #{userId} AND deleted = 0
              AND status = 'GENERATING'
            """)
    int failReport(long reportId, long userId, String errorMessage);

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
