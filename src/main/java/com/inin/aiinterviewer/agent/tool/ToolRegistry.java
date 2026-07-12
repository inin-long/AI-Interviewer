package com.inin.aiinterviewer.agent.tool;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class ToolRegistry {

    private final Map<String, AgentTool> tools;

    public ToolRegistry(List<AgentTool> availableTools) {
        Map<String, AgentTool> indexed = new LinkedHashMap<>();
        for (AgentTool tool : availableTools) {
            if (indexed.putIfAbsent(tool.name(), tool) != null) {
                throw new IllegalStateException("Duplicate agent tool name: " + tool.name());
            }
        }
        this.tools = Map.copyOf(indexed);
    }

    public Optional<AgentTool> find(String name) {
        return Optional.ofNullable(tools.get(name));
    }

    public List<AgentTool> all() {
        return List.copyOf(tools.values());
    }
}

