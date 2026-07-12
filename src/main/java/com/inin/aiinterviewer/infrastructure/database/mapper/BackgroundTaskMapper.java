package com.inin.aiinterviewer.infrastructure.database.mapper;

import com.inin.aiinterviewer.domain.entity.BackgroundTaskEntity;
import com.inin.aiinterviewer.domain.enums.BackgroundTaskStatus;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Optional;

public interface BackgroundTaskMapper {

    @Insert("""
            INSERT INTO task(user_id, task_type, status, progress, attempt_count, payload_json,
                             available_time, create_time, update_time, deleted)
            VALUES(#{userId}, #{taskType}, 'PENDING', 0, 0, #{payloadJson},
                   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(BackgroundTaskEntity task);

    @Select("""
            UPDATE task
               SET status = 'RUNNING', progress = 1, attempt_count = attempt_count + 1,
                   worker_id = #{workerId}, started_time = CURRENT_TIMESTAMP,
                   finished_time = NULL, error_message = NULL, update_time = CURRENT_TIMESTAMP
             WHERE id = (
                    SELECT id FROM task
                     WHERE status = 'PENDING' AND deleted = 0
                       AND available_time <= CURRENT_TIMESTAMP
                     ORDER BY available_time, create_time, id
                     LIMIT 1
             ) AND status = 'PENDING' AND deleted = 0
            RETURNING id, user_id, task_type, status, progress, attempt_count, payload_json,
                      error_message, worker_id, available_time, started_time, finished_time,
                      create_time, update_time
            """)
    Optional<BackgroundTaskEntity> claimNext(String workerId);

    @Update("""
            UPDATE task SET status = 'SUCCESS', progress = 100, worker_id = NULL,
                   error_message = NULL, finished_time = CURRENT_TIMESTAMP,
                   update_time = CURRENT_TIMESTAMP
             WHERE id = #{id} AND status = 'RUNNING' AND worker_id = #{workerId} AND deleted = 0
            """)
    int markSuccess(long id, String workerId);

    @Update("""
            UPDATE task SET status = 'PENDING', progress = 0, worker_id = NULL,
                   error_message = #{errorMessage}, started_time = NULL, finished_time = NULL,
                   available_time = datetime(CURRENT_TIMESTAMP, '+' || #{delaySeconds} || ' seconds'),
                   update_time = CURRENT_TIMESTAMP
             WHERE id = #{id} AND status = 'RUNNING' AND worker_id = #{workerId} AND deleted = 0
            """)
    int scheduleRetry(long id, String workerId, String errorMessage, long delaySeconds);

    @Update("""
            UPDATE task SET status = 'FAILED', progress = 0, worker_id = NULL,
                   error_message = #{errorMessage}, finished_time = CURRENT_TIMESTAMP,
                   update_time = CURRENT_TIMESTAMP
             WHERE id = #{id} AND status = 'RUNNING' AND worker_id = #{workerId} AND deleted = 0
            """)
    int markFailed(long id, String workerId, String errorMessage);

    @Update("""
            UPDATE task SET status = 'PENDING', progress = 0, worker_id = NULL,
                   error_message = CASE
                       WHEN error_message IS NULL OR error_message = '' THEN '应用重启后自动恢复'
                       ELSE error_message
                   END,
                   started_time = NULL, finished_time = NULL,
                   available_time = CURRENT_TIMESTAMP, update_time = CURRENT_TIMESTAMP
             WHERE status = 'RUNNING' AND deleted = 0
            """)
    int recoverInterrupted();

    @Select("""
            SELECT id, user_id, task_type, status, progress, attempt_count, payload_json,
                   error_message, worker_id, available_time, started_time, finished_time,
                   create_time, update_time
              FROM task WHERE id = #{id} AND user_id = #{userId} AND deleted = 0
            """)
    Optional<BackgroundTaskEntity> findById(long id, long userId);

    @Select("""
            SELECT id, user_id, task_type, status, progress, attempt_count, payload_json,
                   error_message, worker_id, available_time, started_time, finished_time,
                   create_time, update_time
              FROM task WHERE user_id = #{userId} AND deleted = 0
             ORDER BY create_time DESC, id DESC
            """)
    List<BackgroundTaskEntity> findAll(long userId);

    @Select("SELECT COUNT(*) FROM task WHERE status = #{status} AND deleted = 0")
    int countByStatus(BackgroundTaskStatus status);
}
