package com.inin.aiinterviewer.infrastructure.database.mapper;

import com.inin.aiinterviewer.domain.entity.DocumentChunkEntity;
import com.inin.aiinterviewer.domain.entity.KnowledgeDocumentEntity;
import com.inin.aiinterviewer.domain.enums.KnowledgeStatus;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Delete;

import java.util.List;
import java.util.Optional;

public interface KnowledgeDocumentMapper {

    @Insert("""
            INSERT INTO document(user_id, name, original_name, storage_name, storage_path,
                                 file_type, file_size, category, status, create_time, update_time, deleted)
            VALUES(#{userId}, #{name}, #{originalName}, #{storageName}, #{storagePath},
                   #{fileType}, #{fileSize}, #{category}, #{status}, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(KnowledgeDocumentEntity entity);

    @Select("""
            SELECT id, user_id, name, original_name, storage_name, storage_path, file_type,
                   file_size, category, status, error_message, create_time, update_time
            FROM document WHERE id = #{id} AND user_id = #{userId} AND deleted = 0 LIMIT 1
            """)
    Optional<KnowledgeDocumentEntity> findById(long id, long userId);

    @Select("""
            SELECT id, user_id, name, original_name, storage_name, storage_path, file_type,
                   file_size, category, status, error_message, create_time, update_time
            FROM document WHERE user_id = #{userId} AND deleted = 0
            ORDER BY update_time DESC, id DESC
            """)
    List<KnowledgeDocumentEntity> findAll(long userId);

    @Update("""
            UPDATE document SET status = #{status}, error_message = #{errorMessage},
                   update_time = CURRENT_TIMESTAMP
            WHERE id = #{id} AND user_id = #{userId} AND deleted = 0
            """)
    int updateStatus(long id, long userId, KnowledgeStatus status, String errorMessage);

    @Insert("""
            INSERT INTO document_chunk(user_id, document_id, chunk_index, content, token_count,
                                       vector_id, metadata_json, create_time, deleted)
            VALUES(#{userId}, #{documentId}, #{chunkIndex}, #{content}, #{tokenCount},
                   #{vectorId}, #{metadataJson}, CURRENT_TIMESTAMP, 0)
            ON CONFLICT(document_id, chunk_index) DO UPDATE SET
                user_id = excluded.user_id,
                content = excluded.content,
                token_count = excluded.token_count,
                vector_id = excluded.vector_id,
                metadata_json = excluded.metadata_json,
                create_time = CURRENT_TIMESTAMP,
                deleted = 0
            """)
    int insertChunk(DocumentChunkEntity entity);

    @Select("""
            SELECT id, user_id, document_id, chunk_index, content, token_count, vector_id, metadata_json
            FROM document_chunk
            WHERE document_id = #{documentId} AND user_id = #{userId} AND deleted = 0
            ORDER BY chunk_index
            """)
    List<DocumentChunkEntity> findChunks(long documentId, long userId);

    @Update("""
            UPDATE document_chunk SET deleted = 1
            WHERE document_id = #{documentId} AND user_id = #{userId} AND deleted = 0
            """)
    int deleteChunks(long documentId, long userId);

    @Update("""
            UPDATE document SET deleted = 1, update_time = CURRENT_TIMESTAMP
            WHERE id = #{id} AND user_id = #{userId} AND deleted = 0
            """)
    int logicalDelete(long id, long userId);

    @Delete("""
            DELETE FROM interview_plan_document
            WHERE document_id = #{documentId} AND user_id = #{userId}
            """)
    int deletePlanLinks(long documentId, long userId);

    @Update("""
            UPDATE document SET name = #{name}, category = #{category},
                   update_time = CURRENT_TIMESTAMP
            WHERE id = #{id} AND user_id = #{userId} AND deleted = 0
            """)
    int updateDocumentMeta(Long id, Long userId, String name, String category);

    @Update("""
            UPDATE document_chunk SET content = #{content}, token_count = #{tokenCount}
            WHERE id = #{chunkId} AND user_id = #{userId} AND deleted = 0
            """)
    int updateChunkContent(Long chunkId, Long userId, String content, int tokenCount);

    @Select("""
            SELECT id, user_id, document_id, chunk_index, content, token_count, vector_id, metadata_json
            FROM document_chunk WHERE id = #{chunkId} AND user_id = #{userId} AND deleted = 0 LIMIT 1
            """)
    List<DocumentChunkEntity> findChunksByChunkId(Long chunkId, long userId);
}
