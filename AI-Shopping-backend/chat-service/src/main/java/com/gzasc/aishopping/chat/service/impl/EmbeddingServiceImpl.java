package com.gzasc.aishopping.chat.service.impl;

import com.gzasc.aishopping.chat.dao.ChromaEmbeddingDao;
import com.gzasc.aishopping.chat.exception.RAGException;
import com.gzasc.aishopping.chat.service.EmbeddingService;
import com.gzasc.aishopping.chat.service.FileService;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.parser.apache.poi.ApachePoiDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingServiceImpl implements EmbeddingService {

    private final EmbeddingModel embeddingModel;
    private final ChromaEmbeddingDao chromaEmbeddingDao;
    private final FileService fileService;

    @Value("${app.file.storage}")
    private String uploadDir;

    private EmbeddingStoreIngestor ingestor;

    @PostConstruct
    public void init() {
        // 初始化 Ingestor：embedding 模型、向量存储、递归切分器（每段 300 字符，重叠 30）
        this.ingestor = EmbeddingStoreIngestor.builder()
                .embeddingModel(embeddingModel)
                .embeddingStore(chromaEmbeddingDao)
                .documentSplitter(DocumentSplitters.recursive(300, 30))
                .build();
    }

    @Override
    public List<Map<String, String>> ingest(List<String> fileNames) {
        if (fileNames == null) {
            throw new NullPointerException("fileNames must not be null");
        }
        // 遍历文件列表逐个处理，失败项记录后继续下一个
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
                // 处理成功后异步移至 finish 目录
                fileService.move(fileName);
                log.info("RAG 导入成功：{}", fileName);
            } catch (Exception e) {
                log.error("RAG 导入失败：{}", fileName, e);
                failed.add(Map.of("fileName", fileName, "error", e.getMessage()));
            }
        }
        return failed;
    }

    @Override
    public float[] embed(String text) {
        // 调用 embedding 模型生成文本向量
        return embeddingModel.embed(text).content().vector();
    }

    @Override
    public Map<String, Object> getCollectionStats() {
        // 通过 HTTP 管理 DAO 获取集合元信息
        long totalChunks = chromaEmbeddingDao.count();
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalChunks", totalChunks);
        stats.put("totalDocs", chromaEmbeddingDao.getDocuments().size());
        return stats;
    }

    @Override
    public List<Map<String, Object>> getDocuments() {
        // 从 Chroma metadata 中按 source 字段聚合文档列表
        return chromaEmbeddingDao.getDocuments();
    }

    @Override
    public List<Map<String, Object>> search(String query, int topK) {
        // 先对查询文本向量化，再通过存储 DAO 执行近似搜索
        float[] queryEmbedding = embeddingModel.embed(query).content().vector();
        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(Embedding.from(queryEmbedding))
                .maxResults(topK)
                .minScore(0.0)
                .build();
        EmbeddingSearchResult<TextSegment> searchResult = chromaEmbeddingDao.search(request);
        List<EmbeddingMatch<TextSegment>> matches = searchResult.matches();
        // 将匹配结果组装为前端需要的格式
        List<Map<String, Object>> results = new ArrayList<>();
        for (EmbeddingMatch<TextSegment> match : matches) {
            TextSegment segment = match.embedded();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("chunkId", match.embeddingId());
            item.put("fileName", segment != null && segment.metadata() != null
                    ? segment.metadata().getString("source") : "");
            item.put("content", segment != null ? segment.text() : "");
            item.put("score", match.score());
            results.add(item);
        }
        return results;
    }

    @Override
    public int deleteFromVector(String fileName) {
        // 按 source 文件名删除 Chroma 中所有关联的文本段
        return chromaEmbeddingDao.deleteBySource(fileName);
    }

    private void processDocument(Path path) {
        String fileName = path.getFileName().toString();
        try {
            // 根据扩展名选择解析器（.txt 用纯文本，其余用 POI）
            Document document;
            if (fileName.toLowerCase().endsWith(".txt")) {
                document = FileSystemDocumentLoader.loadDocument(path, new TextDocumentParser());
            } else {
                document = FileSystemDocumentLoader.loadDocument(path, new ApachePoiDocumentParser());
            }
            // 注入 source / file_path 元数据后写入向量库
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
