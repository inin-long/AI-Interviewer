package com.inin.aiinterviewer.domain.model;

import java.time.LocalDateTime;

public record Message(Role role, String content, LocalDateTime createTime) {
    public enum Role {
        USER,
        ASSISTANT,
        SYSTEM
    }
}

