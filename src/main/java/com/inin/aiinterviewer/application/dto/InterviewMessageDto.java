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
        List<KnowledgeCitationDto> citations,
        long id
) {
    public InterviewMessageDto {
        citations = citations == null ? List.of() : List.copyOf(citations);
    }

    public InterviewMessageDto(
            int sequenceNo,
            Message.Role role,
            String content,
            LocalDateTime createTime,
            boolean partial,
            List<KnowledgeCitationDto> citations
    ) {
        this(sequenceNo, role, content, createTime, partial, citations, 0);
    }

    public InterviewMessageDto(
            int sequenceNo,
            Message.Role role,
            String content,
            LocalDateTime createTime
    ) {
        this(sequenceNo, role, content, createTime, false, List.of(), 0);
    }
}
