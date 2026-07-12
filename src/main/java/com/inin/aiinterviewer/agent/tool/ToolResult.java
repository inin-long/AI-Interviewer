package com.inin.aiinterviewer.agent.tool;

import java.util.Map;

public record ToolResult(boolean success, Map<String, Object> data, String error) {
    public ToolResult {
        data = data == null ? Map.of() : Map.copyOf(data);
    }

    public static ToolResult success(Map<String, Object> data) {
        return new ToolResult(true, data, null);
    }

    public static ToolResult failure(String error) {
        return new ToolResult(false, Map.of(), error);
    }
}

