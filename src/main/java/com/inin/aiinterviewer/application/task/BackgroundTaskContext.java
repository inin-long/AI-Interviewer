package com.inin.aiinterviewer.application.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inin.aiinterviewer.domain.entity.BackgroundTaskEntity;

public record BackgroundTaskContext(
        BackgroundTaskEntity task,
        ObjectMapper objectMapper
) {
    public long taskId() { return task.getId(); }
    public long userId() { return task.getUserId(); }
    public int attempt() { return task.getAttemptCount(); }

    public <T> T payload(Class<T> type) {
        try {
            return objectMapper.readValue(task.getPayloadJson(), type);
        } catch (Exception exception) {
            throw new IllegalArgumentException("后台任务参数格式无效", exception);
        }
    }
}
