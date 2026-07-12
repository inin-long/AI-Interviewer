package com.inin.aiinterviewer.application.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    public String toUserMessage(Throwable throwable) {
        if (throwable instanceof ApplicationException applicationException) {
            ErrorCode code = applicationException.getErrorCode();
            log.warn("Application error [{}]: {}", code.code(), throwable.getMessage(), throwable);
            return code.userMessage();
        }
        log.error("Unhandled application error", throwable);
        return ErrorCode.SYSTEM_ERROR.userMessage();
    }
}

