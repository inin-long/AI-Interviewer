package com.inin.aiinterviewer.infrastructure.ai;

import reactor.core.publisher.Flux;

public interface ChatService {
    String chat(String prompt);

    /**
     * Requests one JSON object. Implementations may use provider-specific JSON
     * mode and streaming transport; local test doubles can reuse {@link #chat(String)}.
     */
    default String chatJson(String prompt) {
        return chat(prompt);
    }

    Flux<String> stream(String prompt);
}
