package com.inin.aiinterviewer.infrastructure.database.mapper;

import com.inin.aiinterviewer.domain.entity.AgentCheckpointEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface AgentCheckpointMapper {

    @Insert("""
            INSERT INTO agent_checkpoint(user_id, session_id, node_name, state_json,
                                         state_version, create_time, update_time, deleted)
            VALUES(#{userId}, #{sessionId}, #{nodeName}, #{stateJson},
                   #{stateVersion}, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AgentCheckpointEntity entity);

    @Select("""
            SELECT id, user_id, session_id, node_name, state_json, state_version,
                   create_time, update_time, deleted
            FROM agent_checkpoint
            WHERE user_id = #{userId} AND session_id = #{sessionId} AND deleted = 0
            ORDER BY id DESC
            """)
    List<AgentCheckpointEntity> findLatestFirst(long userId, long sessionId);
}
