package com.gzasc.aishopping.chat.dao.impl;

import com.gzasc.aishopping.chat.dao.ChromaAdminDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ChromaAdminDaoImpl implements ChromaAdminDao {

    private final RestTemplate restTemplate;

    @Value("${chroma.base-url}")
    private String chromaBaseUrl;

    @Value("${chroma.collection-name}")
    private String collectionName;

    @Override
    public long count() {
        try {
            String url = chromaBaseUrl + "/api/v1/collections/" + collectionName + "/count";
            Long result = restTemplate.getForObject(url, Long.class);
            return result != null ? result : 0L;
        } catch (Exception e) {
            log.warn("Chroma count 失败", e);
            return 0L;
        }
    }

    @Override
    public List<Map<String, Object>> getDocuments() {
        try {
            String url = chromaBaseUrl + "/api/v1/collections/" + collectionName + "/get";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> body = new HashMap<>();
            body.put("limit", 10000);
            body.put("include", Collections.singletonList("metadatas"));

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            var response = restTemplate.exchange(url, org.springframework.http.HttpMethod.POST, request,
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
                docs.add(doc);
            }
            docs.sort(Comparator.comparing(d -> (String) d.get("fileName")));
            return docs;

        } catch (Exception e) {
            log.warn("Chroma 获取文档列表失败", e);
            return List.of();
        }
    }

    @Override
    public int deleteBySource(String fileName) {
        try {
            String url = chromaBaseUrl + "/api/v1/collections/" + collectionName + "/delete";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> body = new HashMap<>();
            body.put("where", Map.of("source", fileName));

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            var deletedIds = restTemplate.exchange(url, org.springframework.http.HttpMethod.POST, request,
                    new ParameterizedTypeReference<List<String>>() {}).getBody();
            return deletedIds != null ? deletedIds.size() : 0;
        } catch (Exception e) {
            log.warn("Chroma 按文件名删除失败：{}", fileName, e);
            return 0;
        }
    }
}
