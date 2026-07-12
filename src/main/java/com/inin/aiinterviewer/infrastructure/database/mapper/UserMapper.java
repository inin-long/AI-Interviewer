package com.inin.aiinterviewer.infrastructure.database.mapper;

import com.inin.aiinterviewer.domain.entity.UserEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.util.Optional;

public interface UserMapper {

    @Select("""
            SELECT id, username, password_hash, nickname, create_time, update_time, deleted
            FROM user
            WHERE username = #{username} COLLATE NOCASE AND deleted = 0
            LIMIT 1
            """)
    Optional<UserEntity> findByUsername(String username);

    @Select("""
            SELECT id, username, password_hash, nickname, create_time, update_time, deleted
            FROM user
            WHERE id = #{id} AND deleted = 0
            LIMIT 1
            """)
    Optional<UserEntity> findById(Long id);

    @Insert("""
            INSERT INTO user(username, password_hash, nickname, create_time, update_time, deleted)
            VALUES(#{username}, #{passwordHash}, #{nickname}, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserEntity entity);
}

