package com.inin.aiinterviewer.application.dto;

import com.inin.aiinterviewer.domain.model.Message;

import java.time.LocalDateTime;

public record InterviewMessageDto(
        int sequenceNo,
        Message.Role role,
        String content,
        LocalDateTime createTime
) {
}
