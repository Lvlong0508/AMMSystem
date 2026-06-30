package com.gzasc.aishopping.chat.service;

import com.gzasc.aishopping.chat.dao.ChromaEmbeddingStorageDao;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VectorAdminService {

    private final RestTemplate restTemplate;
    private final ChromaEmbeddingStorageDao chromaDao;

    @Value("${chroma.base-url}")
    private String chromaBaseUrl;

    @Value("${chroma.collection-name}")
    private String collectionName;

    /**
     * 获取集合概览：通过 Chroma REST API 获取 count
     */
    public Map<String, Object> getCollectionStats() {
        String url = chromaBaseUrl + "/api/v1/collections/" + collectionName + "/count";
        Long count;
        try {
            count = restTemplate.getForObject(url, Long.class);
        } catch (Exception e) {
            log.warn("调用 Chroma count API 失败, 返回 fallback: 0", e);
            count = 0L;
        }
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("collectionName", collectionName);
        stats.put("totalChunks", count != null ? count : 0L);
        stats.put("totalDocs", 0);
        stats.put("dimension", 1536);
        return stats;
    }

    /**
     * 获取所有已导入文档列表（按 source 文件名分组）
     */
    public List<Map<String, Object>> getDocuments() {
        try {
            String url = chromaBaseUrl + "/api/v1/collections/" + collectionName + "/get";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> body = new HashMap<>();
            body.put("limit", 10000);
            body.put("include", Collections.singletonList("metadatas"));

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            var response = restTemplate.exchange(url, HttpMethod.POST, request,
                    new ParameterizedTypeReference<Map<String, Object>>() {}).getBody();

            if (response == null) return List.of();

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> metadatas = (List<Map<String, Object>>) response.get("metadatas");
            if (metadatas == null || metadatas.isEmpty()) return List.of();

            Map<String, List<Map<String, Object>>> grouped = metadatas.stream()
                    .filter(m -> m != null && m.get("source") != null)
                    .collect(Collectors.groupingBy(m -> (String) m.get("source")));

            List<Map<String, Object>> docs = new ArrayList<>();
            for (Map.Entry<String, List<Map<String, Object>>> entry : grouped.entrySet()) {
                Map<String, Object> doc = new LinkedHashMap<>();
                doc.put("fileName", entry.getKey());
                doc.put("chunkCount", entry.getValue().size());
                doc.put("importTime", "");
                docs.add(doc);
            }

            docs.sort(Comparator.comparing(d -> (String) d.get("fileName")));
            return docs;

        } catch (Exception e) {
            log.warn("获取 Chroma 文档列表失败", e);
            return List.of();
        }
    }

    /**
     * 向量搜索
     */
    public List<Map<String, Object>> search(float[] queryEmbedding, int topK) {
        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(Embedding.from(queryEmbedding))
                .maxResults(topK)
                .minScore(0.0)
                .build();

        List<EmbeddingMatch<TextSegment>> matches = chromaDao.search(request);

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

    /**
     * 根据 source 文件名删除向量库中所有相关记录
     */
    public int deleteBySource(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            log.warn("deleteBySource: fileName is null or blank, skipping");
            return 0;
        }
        try {
            String url = chromaBaseUrl + "/api/v1/collections/" + collectionName + "/delete";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> body = new HashMap<>();
            body.put("where", Map.of("source", fileName));
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            var deletedIds = restTemplate.exchange(url, HttpMethod.POST, request,
                    new ParameterizedTypeReference<List<String>>() {}).getBody();
            return deletedIds != null ? deletedIds.size() : 0;
        } catch (Exception e) {
            log.warn("Chroma 按文件名删除失败: {}", fileName, e);
            return 0;
        }
    }
}
