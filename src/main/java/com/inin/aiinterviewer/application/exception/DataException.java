package com.inin.aiinterviewer.application.exception;

public class DataException extends ApplicationException {
    public DataException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}

