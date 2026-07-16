package com.inin.aiinterviewer.infrastructure.database.mapper;

import com.inin.aiinterviewer.domain.entity.ScenarioSessionEntity;
import com.inin.aiinterviewer.domain.enums.ScenarioStatus;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Optional;

public interface ScenarioSessionMapper {

    @Insert("""
            INSERT INTO scenario_session(
                id, user_id, interview_session_id, scenario_type, status,
                state_json, current_round, create_time, update_time)
            VALUES(#{id}, #{userId}, #{interviewSessionId}, #{scenarioType}, #{status},
                   #{stateJson}, #{currentRound}, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """)
    int insert(ScenarioSessionEntity entity);

    @Select("""
            SELECT id, user_id, interview_session_id, scenario_type, status,
                   state_json, current_round, create_time, update_time
            FROM scenario_session
            WHERE user_id = #{userId} AND interview_session_id = #{sessionId}
              AND status = 'ACTIVE'
            LIMIT 1
            """)
    Optional<ScenarioSessionEntity> findActive(long userId, long sessionId);

    @Select("""
            SELECT id, user_id, interview_session_id, scenario_type, status,
                   state_json, current_round, create_time, update_time
            FROM scenario_session
            WHERE id = #{scenarioId} AND user_id = #{userId}
              AND interview_session_id = #{sessionId}
            LIMIT 1
            """)
    Optional<ScenarioSessionEntity> findById(
            String scenarioId,
            long userId,
            long sessionId
    );

    @Update("""
            UPDATE scenario_session
            SET status = #{status}, state_json = #{stateJson},
                current_round = #{currentRound}, update_time = CURRENT_TIMESTAMP
            WHERE id = #{scenarioId} AND user_id = #{userId}
              AND interview_session_id = #{sessionId}
              AND status = 'ACTIVE' AND current_round = #{expectedRound}
            """)
    int updateState(
            String scenarioId,
            long userId,
            long sessionId,
            ScenarioStatus status,
            String stateJson,
            int currentRound,
            int expectedRound
    );

    @Delete("DELETE FROM scenario_session WHERE user_id = #{userId} AND interview_session_id = #{sessionId}")
    int deleteBySession(long userId, long sessionId);
}
