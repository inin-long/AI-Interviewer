package com.inin.aiinterviewer.infrastructure.document;

import com.inin.aiinterviewer.config.properties.RagProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DocumentChunker {

    private final RagProperties properties;

    public DocumentChunker(RagProperties properties) {
        this.properties = properties;
    }

    public List<TextChunk> chunk(String text) {
        if (text == null || text.isBlank()) return List.of();
        String normalized = text.replace("\r\n", "\n").replace('\r', '\n').strip();
        List<TextChunk> chunks = new ArrayList<>();
        int start = 0;
        while (start < normalized.length()) {
            int maximumEnd = Math.min(start + properties.chunkSize(), normalized.length());
            int end = boundary(normalized, start, maximumEnd);
            String content = normalized.substring(start, end).strip();
            if (!content.isBlank()) {
                chunks.add(new TextChunk(chunks.size(), content, Math.max(1, (content.length() + 3) / 4)));
            }
            if (end >= normalized.length()) break;
            int next = Math.max(start + 1, end - properties.overlap());
            start = next;
        }
        return List.copyOf(chunks);
    }

    private int boundary(String text, int start, int maximumEnd) {
        if (maximumEnd >= text.length()) return text.length();
        int minimum = start + properties.chunkSize() / 2;
        int paragraph = text.lastIndexOf("\n\n", maximumEnd);
        if (paragraph >= minimum) return paragraph + 2;
        int line = text.lastIndexOf('\n', maximumEnd);
        return line >= minimum ? line + 1 : maximumEnd;
    }
}
