package com.inin.aiinterviewer.infrastructure.vector;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inin.aiinterviewer.application.exception.DataException;
import com.inin.aiinterviewer.application.exception.ErrorCode;
import com.inin.aiinterviewer.domain.enums.StorageCategory;
import com.inin.aiinterviewer.infrastructure.file.PathService;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.KnnFloatVectorField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.VectorSimilarityFunction;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.KnnFloatVectorQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Service
public class LuceneVectorStore implements VectorStorePort {

    private static final String ID = "id";
    private static final String CONTENT = "content";
    private static final String METADATA = "metadata";
    private static final String VECTOR = "vector";

    private final PathService pathService;
    private final ObjectMapper objectMapper;

    public LuceneVectorStore(PathService pathService, ObjectMapper objectMapper) {
        this.pathService = pathService;
        this.objectMapper = objectMapper;
    }

    @Override
    public synchronized void upsert(long userId, Collection<VectorDocument> documents) {
        if (documents == null || documents.isEmpty()) return;
        try (Directory directory = open(userId);
             IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig())) {
            for (VectorDocument vectorDocument : documents) {
                validate(vectorDocument);
                Document document = new Document();
                document.add(new StringField(ID, vectorDocument.id(), StringField.Store.YES));
                document.add(new StoredField(CONTENT, vectorDocument.content()));
                document.add(new StoredField(METADATA, objectMapper.writeValueAsString(vectorDocument.metadata())));
                document.add(new KnnFloatVectorField(VECTOR, vectorDocument.embedding(),
                        VectorSimilarityFunction.COSINE));
                writer.updateDocument(new Term(ID, vectorDocument.id()), document);
            }
            writer.commit();
        } catch (IOException exception) {
            throw new DataException(ErrorCode.DATA_ACCESS_FAILED, exception);
        }
    }

    @Override
    public synchronized List<VectorSearchResult> search(
            long userId, float[] queryEmbedding, int limit, double minimumScore
    ) {
        if (queryEmbedding == null || queryEmbedding.length == 0 || limit <= 0) return List.of();
        try (Directory directory = open(userId)) {
            if (!DirectoryReader.indexExists(directory)) return List.of();
            try (DirectoryReader reader = DirectoryReader.open(directory)) {
                IndexSearcher searcher = new IndexSearcher(reader);
                var topDocs = searcher.search(new KnnFloatVectorQuery(VECTOR, queryEmbedding, limit), limit);
                List<VectorSearchResult> results = new ArrayList<>();
                var storedFields = searcher.storedFields();
                for (var scoreDoc : topDocs.scoreDocs) {
                    if (scoreDoc.score < minimumScore) continue;
                    Document document = storedFields.document(scoreDoc.doc);
                    results.add(new VectorSearchResult(document.get(ID), document.get(CONTENT),
                            scoreDoc.score, readMetadata(document.get(METADATA))));
                }
                return List.copyOf(results);
            }
        } catch (IOException exception) {
            throw new DataException(ErrorCode.DATA_ACCESS_FAILED, exception);
        }
    }

    @Override
    public synchronized void delete(long userId, Collection<String> documentIds) {
        if (documentIds == null || documentIds.isEmpty()) return;
        try (Directory directory = open(userId);
             IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig())) {
            for (String id : documentIds) {
                writer.deleteDocuments(new Term(ID, id));
            }
            writer.commit();
        } catch (IOException exception) {
            throw new DataException(ErrorCode.DATA_ACCESS_FAILED, exception);
        }
    }

    private Directory open(long userId) throws IOException {
        Path indexPath = pathService.categoryRoot(userId, StorageCategory.VECTOR).resolve("knowledge-index");
        Files.createDirectories(indexPath);
        return FSDirectory.open(indexPath);
    }

    private void validate(VectorDocument document) {
        if (document == null || document.id() == null || document.id().isBlank()
                || document.content() == null || document.content().isBlank()
                || document.embedding().length == 0) {
            throw new IllegalArgumentException("Vector document is incomplete");
        }
        for (float value : document.embedding()) {
            if (!Float.isFinite(value)) throw new IllegalArgumentException("Vector contains non-finite values");
        }
    }

    private Map<String, Object> readMetadata(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException exception) {
            throw new DataException(ErrorCode.DATA_ACCESS_FAILED, exception);
        }
    }
}
