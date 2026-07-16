package com.inin.aiinterviewer.infrastructure.database.mapper;

import com.inin.aiinterviewer.domain.entity.DeferredProbeEntity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Optional;

public interface DeferredProbeMapper {

    @Insert("""
            INSERT OR IGNORE INTO deferred_probe(
                id, user_id, session_id, target_claim_id, preferred_stage,
                strategy, reason, completed, create_time, update_time)
            VALUES(#{id}, #{userId}, #{sessionId}, #{targetClaimId}, #{preferredStage},
                   #{strategy}, #{reason}, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """)
    int insertIfAbsent(DeferredProbeEntity entity);

    @Select("""
            SELECT id, user_id, session_id, target_claim_id, preferred_stage,
                   strategy, reason, completed, create_time, update_time
            FROM deferred_probe
            WHERE user_id = #{userId} AND session_id = #{sessionId}
            ORDER BY completed, create_time, id
            """)
    List<DeferredProbeEntity> findAll(long userId, long sessionId);

    @Select("""
            SELECT id, user_id, session_id, target_claim_id, preferred_stage,
                   strategy, reason, completed, create_time, update_time
            FROM deferred_probe
            WHERE id = #{probeId} AND user_id = #{userId} AND session_id = #{sessionId}
            LIMIT 1
            """)
    Optional<DeferredProbeEntity> findById(String probeId, long userId, long sessionId);

    @Update("""
            UPDATE deferred_probe
            SET completed = 1, update_time = CURRENT_TIMESTAMP
            WHERE id = #{probeId} AND user_id = #{userId} AND session_id = #{sessionId}
              AND completed = 0
            """)
    int markCompleted(String probeId, long userId, long sessionId);

    @Delete("DELETE FROM deferred_probe WHERE user_id = #{userId} AND session_id = #{sessionId}")
    int deleteBySession(long userId, long sessionId);
}
