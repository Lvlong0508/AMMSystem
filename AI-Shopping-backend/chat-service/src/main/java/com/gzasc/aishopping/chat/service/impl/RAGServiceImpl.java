package com.gzasc.aishopping.chat.service.impl;

import com.gzasc.aishopping.chat.exception.RAGException;
import com.gzasc.aishopping.chat.service.FileService;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RAGServiceImpl implements RAGService {

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final FileService fileService;

    @Value("${app.file.storage}")
    private String uploadDir;

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
    public List<Map<String, String>> input(List<String> fileNames) {
        if (fileNames == null) {
            throw new NullPointerException("fileNames must not be null");
        }

        List<Map<String, String>> failed = new ArrayList<>();

        for (String fileName : fileNames) {
            Path filePath = Path.of(uploadDir, fileName);
            if (!Files.exists(filePath)) {
                log.warn("文件不存在，跳过 RAG：{}", fileName);
                failed.add(Map.of("fileName", fileName, "error", "文件不存在"));
                continue;
            }
            try {
                processDocument(filePath);
                fileService.move(fileName);
                log.info("RAG 导入成功：{}", fileName);
            } catch (Exception e) {
                log.error("RAG 导入失败：{}", fileName, e);
                failed.add(Map.of("fileName", fileName, "error", e.getMessage()));
            }
        }
        return failed;
    }

    private void processDocument(Path path) {
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
