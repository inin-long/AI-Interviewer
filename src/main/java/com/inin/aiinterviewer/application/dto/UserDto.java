package com.inin.aiinterviewer.application.dto;

import java.time.LocalDateTime;

public record UserDto(Long id, String username, String nickname, LocalDateTime createTime) {
}

