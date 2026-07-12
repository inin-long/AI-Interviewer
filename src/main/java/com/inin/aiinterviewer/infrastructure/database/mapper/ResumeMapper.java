package com.inin.aiinterviewer.infrastructure.database.mapper;

import com.inin.aiinterviewer.domain.entity.ResumeEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Optional;

public interface ResumeMapper {

    @Insert("""
            INSERT INTO resume(user_id, original_name, storage_name, storage_path, file_type,
                               file_size, status, create_time, update_time, deleted)
            VALUES(#{userId}, #{originalName}, #{storageName}, #{storagePath}, #{fileType},
                   #{fileSize}, #{status}, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ResumeEntity entity);

    @Select("""
            SELECT id, user_id, original_name, storage_name, storage_path, file_type, file_size,
                   status, parsed_text, error_message, create_time, update_time, deleted
            FROM resume
            WHERE id = #{id} AND user_id = #{userId} AND deleted = 0
            LIMIT 1
            """)
    Optional<ResumeEntity> findByIdAndUserId(long id, long userId);

    @Select("""
            SELECT id, user_id, original_name, storage_name, storage_path, file_type, file_size,
                   status, NULL AS parsed_text, error_message, create_time, update_time, deleted
            FROM resume
            WHERE user_id = #{userId} AND deleted = 0
            ORDER BY update_time DESC, id DESC
            """)
    List<ResumeEntity> findAllByUserId(long userId);

    @Update("""
            UPDATE resume
            SET status = 'PARSING', error_message = NULL, update_time = CURRENT_TIMESTAMP
            WHERE id = #{id} AND user_id = #{userId} AND deleted = 0
            """)
    int markParsing(long id, long userId);

    @Update("""
            UPDATE resume
            SET status = 'COMPLETED', parsed_text = #{parsedText}, error_message = NULL,
                update_time = CURRENT_TIMESTAMP
            WHERE id = #{id} AND user_id = #{userId} AND deleted = 0
            """)
    int markCompleted(long id, long userId, String parsedText);

    @Update("""
            UPDATE resume
            SET status = 'FAILED', error_message = #{errorMessage}, update_time = CURRENT_TIMESTAMP
            WHERE id = #{id} AND user_id = #{userId} AND deleted = 0
            """)
    int markFailed(long id, long userId, String errorMessage);

    @Update("""
            UPDATE resume
            SET deleted = 1, update_time = CURRENT_TIMESTAMP
            WHERE id = #{id} AND user_id = #{userId} AND deleted = 0
            """)
    int logicalDelete(long id, long userId);
}

