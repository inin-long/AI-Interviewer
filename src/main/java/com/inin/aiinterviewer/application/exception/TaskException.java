package com.inin.aiinterviewer.application.exception;

public class TaskException extends ApplicationException {
    public TaskException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}

