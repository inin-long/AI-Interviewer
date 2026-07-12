package com.inin.aiinterviewer.application.exception;

public abstract class ApplicationException extends RuntimeException {

    private final ErrorCode errorCode;

    protected ApplicationException(ErrorCode errorCode) {
        super(errorCode.userMessage());
        this.errorCode = errorCode;
    }

    protected ApplicationException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.userMessage(), cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}

