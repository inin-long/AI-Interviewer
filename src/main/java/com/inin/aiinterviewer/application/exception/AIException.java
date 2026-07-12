package com.inin.aiinterviewer.application.exception;

public class AIException extends ApplicationException {
    public AIException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}

