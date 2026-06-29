package com.gzasc.aishopping.chat.service.impl;

import com.gzasc.aishopping.chat.exception.RAGException;
import com.gzasc.aishopping.chat.service.RAGService;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.parser.apache.poi.ApachePoiDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RAGServiceImpl implements RAGService {
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;

    private EmbeddingStoreIngestor ingestor;

    @PostConstruct
    public void init() {
        this.ingestor = EmbeddingStoreIngestor.builder()
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .documentSplitter(DocumentSplitters.recursive(300, 30))
                .build();
    }

    @Override
    public Map<String, String> input(List<String> filePaths) {
        Map<String, String> failed = new LinkedHashMap<>();

        for (String filePath : filePaths) {
            try {
                processDocument(filePath);
                log.info("RAG 导入成功：{}", filePath);
            } catch (Exception e) {
                log.error("RAG 导入失败：{}", filePath, e);
                failed.put(Path.of(filePath).getFileName().toString(), e.getMessage());
            }
        }

        return failed;
    }

    private void processDocument(String filePath) {
        Path path = Path.of(filePath);
        String fileName = path.getFileName().toString();

        try {
            Document document;
            if (fileName.toLowerCase().endsWith(".txt")) {
                document = FileSystemDocumentLoader.loadDocument(path, new TextDocumentParser());
            } else {
                document = FileSystemDocumentLoader.loadDocument(path, new ApachePoiDocumentParser());
            }

            Metadata metadata = document.metadata().copy();
            metadata.put("source", fileName);
            metadata.put("file_path", path.toAbsolutePath().toString());
            Document enrichedDocument = Document.from(document.text(), metadata);

            ingestor.ingest(enrichedDocument);
        } catch (Exception e) {
            throw new RAGException("文档处理失败 [" + fileName + "]：" + e.getMessage(), e);
        }
    }
}
