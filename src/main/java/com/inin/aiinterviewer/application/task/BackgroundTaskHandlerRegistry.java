package com.inin.aiinterviewer.application.task;

import com.inin.aiinterviewer.domain.enums.BackgroundTaskType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class BackgroundTaskHandlerRegistry {
    private final Map<BackgroundTaskType, BackgroundTaskHandler> handlers;

    public BackgroundTaskHandlerRegistry(List<BackgroundTaskHandler> handlers) {
        Map<BackgroundTaskType, BackgroundTaskHandler> indexed = new EnumMap<>(BackgroundTaskType.class);
        for (BackgroundTaskHandler handler : handlers) {
            BackgroundTaskHandler previous = indexed.put(handler.taskType(), handler);
            if (previous != null) {
                throw new IllegalStateException("Duplicate background task handler: " + handler.taskType());
            }
        }
        this.handlers = Map.copyOf(indexed);
    }

    public BackgroundTaskHandler require(BackgroundTaskType type) {
        BackgroundTaskHandler handler = handlers.get(type);
        if (handler == null) throw new IllegalStateException("No background task handler for " + type);
        return handler;
    }
}
