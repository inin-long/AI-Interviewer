package com.inin.aiinterviewer.agent.tool;

public interface AgentTool {
    String name();

    String description();

    ToolResult execute(ToolInput input);
}

