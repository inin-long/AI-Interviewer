package com.inin.aiinterviewer.infrastructure.document;

import com.inin.aiinterviewer.application.exception.ErrorCode;
import com.inin.aiinterviewer.application.exception.FileException;
import org.apache.tika.Tika;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
public class TikaDocumentParser implements DocumentParser {

    private static final int MAX_CHARACTERS = 2_000_000;

    private final Tika tika;

    public TikaDocumentParser() {
        tika = new Tika();
        tika.setMaxStringLength(MAX_CHARACTERS);
    }

    @Override
    public ParsedDocument parse(Path file) {
        try {
            String content = tika.parseToString(file).strip();
            if (content.isBlank()) {
                throw new FileException(ErrorCode.FILE_STORAGE_FAILED);
            }
            String detectedType = tika.detect(file);
            return new ParsedDocument(file.getFileName().toString(), content, detectedType);
        } catch (FileException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new FileException(ErrorCode.FILE_STORAGE_FAILED, exception);
        }
    }
}
