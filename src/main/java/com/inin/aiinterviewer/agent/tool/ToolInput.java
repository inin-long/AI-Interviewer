package com.inin.aiinterviewer.agent.tool;

import java.util.Map;

public record ToolInput(long userId, long sessionId, Map<String, Object> arguments) {
    public ToolInput {
        arguments = arguments == null ? Map.of() : Map.copyOf(arguments);
    }
}

