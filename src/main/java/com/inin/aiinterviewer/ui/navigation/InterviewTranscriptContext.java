package com.inin.aiinterviewer.ui.navigation;

public record InterviewTranscriptContext(long sessionId, int questionNumber) {
    public InterviewTranscriptContext {
        if (sessionId <= 0 || questionNumber <= 0) {
            throw new IllegalArgumentException("Session id and question number must be positive");
        }
    }
}
