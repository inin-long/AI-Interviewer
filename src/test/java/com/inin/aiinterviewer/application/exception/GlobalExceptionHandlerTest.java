package com.inin.aiinterviewer.application.exception;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletionException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    @Test
    void unwrapsApplicationErrorsFromAsyncGraphFailures() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        Throwable wrapped = new CompletionException(
                new IllegalStateException(new AIException(ErrorCode.AI_RESPONSE_INVALID, null)));

        assertThat(handler.toUserMessage(wrapped)).isEqualTo("AI 返回格式无效，请重试");
    }
}
