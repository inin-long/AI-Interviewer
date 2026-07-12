package com.inin.aiinterviewer.application.exception;

public class SystemException extends ApplicationException {
    public SystemException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}

