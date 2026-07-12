package com.inin.aiinterviewer.infrastructure.document;

import java.nio.file.Path;

public interface DocumentParser {
    ParsedDocument parse(Path file);
}

