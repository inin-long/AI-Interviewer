package com.inin.aiinterviewer.application.exception;

public class FileException extends ApplicationException {
    public FileException(ErrorCode errorCode) {
        super(errorCode);
    }

    public FileException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}

