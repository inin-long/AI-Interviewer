package com.inin.aiinterviewer.application.dto;

import com.inin.aiinterviewer.domain.model.Message;

import java.time.LocalDateTime;
import java.util.List;

public record InterviewMessageDto(
        int sequenceNo,
        Message.Role role,
        String content,
        LocalDateTime createTime,
        boolean partial,
        List<KnowledgeCitationDto> citations
) {
    public InterviewMessageDto {
        citations = citations == null ? List.of() : List.copyOf(citations);
    }

    public InterviewMessageDto(
            int sequenceNo,
            Message.Role role,
            String content,
            LocalDateTime createTime
    ) {
        this(sequenceNo, role, content, createTime, false, List.of());
    }
}
