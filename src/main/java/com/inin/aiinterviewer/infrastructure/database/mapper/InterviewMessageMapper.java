package com.inin.aiinterviewer.infrastructure.database.mapper;

import com.inin.aiinterviewer.domain.entity.InterviewMessageEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface InterviewMessageMapper {

    @Select("""
            SELECT COALESCE(MAX(sequence_no), 0) + 1
            FROM message
            WHERE user_id = #{userId} AND session_id = #{sessionId} AND deleted = 0
            """)
    int nextSequence(long userId, long sessionId);

    @Insert("""
            INSERT INTO message(user_id, session_id, sequence_no, role, content,
                                metadata_json, create_time, deleted)
            VALUES(#{userId}, #{sessionId}, #{sequenceNo}, #{role}, #{content},
                   #{metadataJson}, CURRENT_TIMESTAMP, 0)
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(InterviewMessageEntity entity);

    @Select("""
            SELECT id, user_id, session_id, sequence_no, role, content,
                   metadata_json, create_time, deleted
            FROM message
            WHERE user_id = #{userId} AND session_id = #{sessionId} AND deleted = 0
            ORDER BY sequence_no
            """)
    List<InterviewMessageEntity> findAll(long userId, long sessionId);

    @Update("""
            UPDATE message
            SET deleted = 1
            WHERE user_id = #{userId} AND session_id = #{sessionId} AND deleted = 0
            """)
    int deleteBySession(long userId, long sessionId);
}
