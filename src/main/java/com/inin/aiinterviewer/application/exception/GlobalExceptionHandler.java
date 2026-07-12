package com.inin.aiinterviewer.application.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    public String toUserMessage(Throwable throwable) {
        ApplicationException applicationException = findApplicationException(throwable);
        if (applicationException != null) {
            ErrorCode code = applicationException.getErrorCode();
            log.warn("Application error [{}]: {}", code.code(), applicationException.getMessage(), throwable);
            return code.userMessage();
        }
        log.error("Unhandled application error", throwable);
        return ErrorCode.SYSTEM_ERROR.userMessage();
    }

    private ApplicationException findApplicationException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof ApplicationException applicationException) {
                return applicationException;
            }
            current = current.getCause();
        }
        return null;
    }
}
