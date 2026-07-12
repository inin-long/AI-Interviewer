package com.inin.aiinterviewer.application.exception;

public class BusinessException extends ApplicationException {
    public BusinessException(ErrorCode errorCode) {
        super(errorCode);
    }
}

