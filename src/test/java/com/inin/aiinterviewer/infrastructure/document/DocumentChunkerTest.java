package com.inin.aiinterviewer.infrastructure.document;

import com.inin.aiinterviewer.config.properties.RagProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentChunkerTest {

    @Test
    void createsStableOverlappingChunks() {
        DocumentChunker chunker = new DocumentChunker(new RagProperties(100, 20));
        String text = "0123456789".repeat(23);

        var chunks = chunker.chunk(text);

        assertThat(chunks).hasSize(3);
        assertThat(chunks.get(0).content()).hasSize(100);
        assertThat(chunks.get(1).content())
                .startsWith(chunks.get(0).content().substring(80));
        assertThat(chunks).extracting(TextChunk::index).containsExactly(0, 1, 2);
    }
}
