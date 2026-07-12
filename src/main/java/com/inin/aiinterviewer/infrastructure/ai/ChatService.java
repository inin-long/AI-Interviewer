package com.inin.aiinterviewer.infrastructure.ai;

import reactor.core.publisher.Flux;

public interface ChatService {
    String chat(String prompt);

    Flux<String> stream(String prompt);
}

