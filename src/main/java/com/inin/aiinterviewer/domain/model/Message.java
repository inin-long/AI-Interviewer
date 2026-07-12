package com.inin.aiinterviewer.domain.model;

import java.time.LocalDateTime;
import java.io.Serializable;

public record Message(Role role, String content, LocalDateTime createTime) implements Serializable {
    public enum Role {
        USER,
        ASSISTANT,
        SYSTEM
    }
}
