package com.inin.aiinterviewer.infrastructure.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inin.aiinterviewer.application.exception.DataException;
import com.inin.aiinterviewer.application.exception.ErrorCode;
import com.inin.aiinterviewer.domain.model.DomainPack;
import com.inin.aiinterviewer.infrastructure.file.PathService;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

@Component
public class DomainPackIndex {
    private static final String ID = "id";
    private static final String SEARCH_TEXT = "searchText";

    private final Path indexPath;
    private final ObjectMapper objectMapper;

    public DomainPackIndex(PathService pathService, ObjectMapper objectMapper) {
        this.indexPath = pathService.applicationRoot().resolve("domain-packs").resolve("index");
        this.objectMapper = objectMapper;
    }

    public synchronized void rebuild(Collection<DomainPack> packs) {
        try {
            Files.createDirectories(indexPath);
            try (StandardAnalyzer analyzer = new StandardAnalyzer();
                 Directory directory = FSDirectory.open(indexPath);
                 IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(analyzer))) {
                writer.deleteAll();
                for (DomainPack pack : packs) {
                    Document document = new Document();
                    document.add(new StringField(ID, pack.id(), StringField.Store.YES));
                    document.add(new TextField(SEARCH_TEXT, searchableText(pack), TextField.Store.NO));
                    writer.addDocument(document);
                }
                writer.commit();
            }
        } catch (IOException exception) {
            throw new DataException(ErrorCode.DATA_ACCESS_FAILED, exception);
        }
    }

    public synchronized List<String> search(String text, int limit) {
        if (limit <= 0) return List.of();
        try {
            Files.createDirectories(indexPath);
            try (StandardAnalyzer analyzer = new StandardAnalyzer();
                 Directory directory = FSDirectory.open(indexPath)) {
                if (!DirectoryReader.indexExists(directory)) return List.of();
                Query query = query(analyzer, text);
                try (DirectoryReader reader = DirectoryReader.open(directory)) {
                    IndexSearcher searcher = new IndexSearcher(reader);
                    var hits = searcher.search(query, limit);
                    List<String> ids = new ArrayList<>(hits.scoreDocs.length);
                    var fields = searcher.storedFields();
                    for (var hit : hits.scoreDocs) ids.add(fields.document(hit.doc).get(ID));
                    return List.copyOf(ids);
                }
            }
        } catch (IOException exception) {
            throw new DataException(ErrorCode.DATA_ACCESS_FAILED, exception);
        }
    }

    private Query query(StandardAnalyzer analyzer, String text) throws IOException {
        if (text == null || text.isBlank()) return new MatchAllDocsQuery();
        LinkedHashSet<String> terms = new LinkedHashSet<>();
        try (TokenStream stream = analyzer.tokenStream(SEARCH_TEXT, text)) {
            CharTermAttribute term = stream.addAttribute(CharTermAttribute.class);
            stream.reset();
            while (stream.incrementToken()) terms.add(term.toString());
            stream.end();
        }
        if (terms.isEmpty()) return new MatchAllDocsQuery();
        BooleanQuery.Builder query = new BooleanQuery.Builder();
        terms.forEach(value -> query.add(new TermQuery(new Term(SEARCH_TEXT, value)), BooleanClause.Occur.SHOULD));
        query.setMinimumNumberShouldMatch(1);
        return query.build();
    }

    private String searchableText(DomainPack pack) {
        try {
            return pack.displayName() + " " + pack.roleCode() + " "
                    + (pack.industryCode() == null ? "" : pack.industryCode()) + " "
                    + objectMapper.writeValueAsString(pack);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot serialize DomainPack for indexing", exception);
        }
    }
}
