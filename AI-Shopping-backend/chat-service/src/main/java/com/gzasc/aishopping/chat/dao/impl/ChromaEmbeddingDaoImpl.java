package com.gzasc.aishopping.chat.dao.impl;

import com.gzasc.aishopping.chat.dao.ChromaEmbeddingDao;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ChromaEmbeddingDaoImpl implements ChromaEmbeddingDao {

    // ==================== 配置 & 初始化 ====================

    private final RestTemplate restTemplate;

    @Value("${chroma.base-url}")
    private String chromaBaseUrl;

    @Value("${chroma.collection-name}")
    private String collectionName;

    @Value("${chroma.tenant:ai_shop}")
    private String tenant;

    @Value("${chroma.database:ai_shop}")
    private String database;

    private String collectionId;

    @PostConstruct
    public void init() {
        resolveCollectionId();
        if (collectionId == null) {
            log.error("Chroma 集合 {} 初始化失败，应用将无法启动", collectionName);
            throw new RuntimeException("Chroma 初始化失败");
        }
    }

    // Chroma tenant GET 确认存在，不存在则 POST 自动创建
    private void ensureTenant() {
        try {
            restTemplate.getForObject(chromaBaseUrl + "/api/v2/tenants/" + tenant, Map.class);
            log.info("Chroma tenant 确认存在: {}", tenant);
        } catch (Exception e) {
            log.info("Chroma tenant '{}' 不存在，正在创建", tenant);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> req = new HttpEntity<>(Map.of("name", tenant), headers);
            restTemplate.exchange(chromaBaseUrl + "/api/v2/tenants", HttpMethod.POST, req, Map.class);
            log.info("Chroma tenant 已创建: {}", tenant);
        }
    }

    // Chroma database GET 确认存在，不存在则 POST 自动创建（依赖 tenant 存在）
    private void ensureDatabase() {
        try {
            restTemplate.getForObject(chromaBaseUrl + "/api/v2/tenants/" + tenant + "/databases/" + database, Map.class);
            log.info("Chroma database 确认存在: {}/{}", tenant, database);
        } catch (Exception e) {
            log.info("Chroma database '{}/{}' 不存在，正在创建", tenant, database);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> req = new HttpEntity<>(Map.of("name", database), headers);
            restTemplate.exchange(chromaBaseUrl + "/api/v2/tenants/" + tenant + "/databases", HttpMethod.POST, req, Map.class);
            log.info("Chroma database 已创建: {}/{}", tenant, database);
        }
    }

    // 先用 v2 API（支持集合名）GET 查询已有集合；查不到则按依赖链确保 tenant → database → 创建集合
    // Chroma v0.5+ 中 delete/query/count/get/add 等 v1 API 路径必须用 UUID
    @SuppressWarnings("unchecked")
    private void resolveCollectionId() {
        String getUrl = chromaBaseUrl + "/api/v2/tenants/" + tenant + "/databases/" + database + "/collections/" + collectionName;
        try {
            Map<String, Object> result = restTemplate.getForObject(getUrl, Map.class);
            if (result != null && result.get("id") != null) {
                collectionId = (String) result.get("id");
                log.info("集合已存在: {} -> {}", collectionName, collectionId);
                return;
            }
        } catch (Exception e) {
            log.info("集合 {} 不存在，准备创建", collectionName);
        }

        // 集合不存在，逆依赖链补齐
        ensureTenant();
        ensureDatabase();
        collectionId = createCollection();
    }

    // POST 创建 Chroma 集合，使用余弦距离（hnsw:space = cosine）
    @SuppressWarnings("unchecked")
    private String createCollection() {
        String createUrl = chromaBaseUrl + "/api/v2/tenants/" + tenant + "/databases/" + database + "/collections";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> createBody = new HashMap<>();
        createBody.put("name", collectionName);
        createBody.put("metadata", Map.of("hnsw:space", "cosine"));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(createBody, headers);
        Map<String, Object> created = restTemplate.exchange(createUrl, HttpMethod.POST, request,
                new ParameterizedTypeReference<Map<String, Object>>() {}).getBody();

        if (created != null && created.get("id") != null) {
            String id = (String) created.get("id");
            log.info("集合已创建: {} -> {}", collectionName, id);
            return id;
        }
        return null;
    }

    // ==================== EmbeddingStore 接口方法 ====================

    // Chroma query 响应中 ids/distances/metadatas/documents 均为 List<List<>> 结构，
    // 外层 List 对应每个 query_embedding，取第一个（索引 0）；score 由 distance 转换：score = 1.0 - distance
    @SuppressWarnings("unchecked")
    @Override
    public EmbeddingSearchResult<TextSegment> search(EmbeddingSearchRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        try {
            String url = chromaBaseUrl + "/api/v1/collections/" + collectionId + "/query";

            // float[] 转为 List<Float> 供 JSON 序列化
            float[] vector = request.queryEmbedding().vector();
            List<Float> embeddingList = new ArrayList<>();
            for (float v : vector) embeddingList.add(v);

            Map<String, Object> body = new HashMap<>();
            body.put("query_embeddings", List.of(embeddingList));
            body.put("n_results", request.maxResults());
            body.put("include", List.of("metadatas", "documents", "distances"));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> httpRequest = new HttpEntity<>(body, headers);

            Map<String, Object> response = restTemplate.exchange(url, HttpMethod.POST, httpRequest,
                    new ParameterizedTypeReference<Map<String, Object>>() {}).getBody();

            if (response == null) return new EmbeddingSearchResult<>(List.of());

            // Chroma 返回结构：{ids: [[id1,id2,...]], distances: [[d1,d2,...]], metadatas: [[{...},...]], documents: [[str,...]]}
            List<List<String>> idsList = (List<List<String>>) response.get("ids");
            if (idsList == null || idsList.isEmpty()) return new EmbeddingSearchResult<>(List.of());

            List<String> ids = idsList.get(0);
            List<List<Double>> distancesList = (List<List<Double>>) response.get("distances");
            List<List<Map<String, Object>>> metadatasList = (List<List<Map<String, Object>>>) response.get("metadatas");
            List<List<String>> documentsList = (List<List<String>>) response.get("documents");

            List<Double> distances = distancesList != null && !distancesList.isEmpty() ? distancesList.get(0) : null;
            List<Map<String, Object>> metadatas = metadatasList != null && !metadatasList.isEmpty() ? metadatasList.get(0) : null;
            List<String> documents = documentsList != null && !documentsList.isEmpty() ? documentsList.get(0) : null;

            List<EmbeddingMatch<TextSegment>> results = new ArrayList<>();
            for (int i = 0; i < ids.size(); i++) {
                String id = ids.get(i);
                double score = distances != null && i < distances.size() ? 1.0 - distances.get(i) : 0.0;

                String text = documents != null && i < documents.size() ? documents.get(i) : "";
                TextSegment segment = TextSegment.from(text);

                if (metadatas != null && i < metadatas.size()) {
                    Map<String, Object> meta = metadatas.get(i);
                    for (Map.Entry<String, Object> entry : meta.entrySet()) {
                        if (entry.getValue() instanceof String s) {
                            segment.metadata().put(entry.getKey(), s);
                        }
                    }
                }

                // embedding vector 不回传，调用方不依赖
                results.add(new EmbeddingMatch<>(score, id, null, segment));
            }

            if (request.minScore() > 0.0) {
                double minScore = request.minScore();
                results.removeIf(m -> m.score() < minScore);
            }

            return new EmbeddingSearchResult<>(results);
        } catch (Exception e) {
            log.warn("Chroma 搜索失败", e);
            return new EmbeddingSearchResult<>(List.of());
        }
    }

    // Chroma delete 按 ID 删除
    @Override
    public void remove(String id) {
        Objects.requireNonNull(id, "id must not be null");
        try {
            String url = chromaBaseUrl + "/api/v1/collections/" + collectionId + "/delete";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> body = new HashMap<>();
            body.put("ids", List.of(id));

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            restTemplate.exchange(url, HttpMethod.POST, request,
                    new ParameterizedTypeReference<List<String>>() {}).getBody();
        } catch (Exception e) {
            log.warn("Chroma 删除失败：{}", id, e);
        }
    }

    // Chroma delete 按 ID 列表批量删除
    @Override
    public void removeAll(Collection<String> ids) {
        Objects.requireNonNull(ids, "ids must not be null");
        try {
            String url = chromaBaseUrl + "/api/v1/collections/" + collectionId + "/delete";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> body = new HashMap<>();
            body.put("ids", List.copyOf(ids));

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            restTemplate.exchange(url, HttpMethod.POST, request,
                    new ParameterizedTypeReference<List<String>>() {}).getBody();
        } catch (Exception e) {
            log.warn("Chroma 批量删除失败", e);
        }
    }

    // Chroma add 单条向量，返回自动生成的 ID
    @Override
    public String add(Embedding embedding) {
        return addAll(List.of(embedding)).get(0);
    }

    // Chroma add 带指定 ID 的单条向量
    @Override
    public void add(String id, Embedding embedding) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String url = chromaBaseUrl + "/api/v1/collections/" + collectionId + "/add";
        float[] vector = embedding.vector();
        List<Float> vecList = new ArrayList<>();
        for (float v : vector) vecList.add(v);
        Map<String, Object> body = new HashMap<>();
        body.put("embeddings", List.of(vecList));
        body.put("ids", List.of(id));
        body.put("metadatas", List.of(Map.of()));
        body.put("documents", List.of(""));
        restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);
    }

    // Chroma add 单条向量 + 文本段，返回自动生成的 ID
    @Override
    public String add(Embedding embedding, TextSegment segment) {
        return addAll(List.of(embedding), List.of(segment)).get(0);
    }

    // Chroma add 批量添加向量（无文本段）
    @Override
    public List<String> addAll(List<Embedding> embeddings) {
        Objects.requireNonNull(embeddings, "embeddings must not be null");
        List<TextSegment> emptySegments = Collections.nCopies(embeddings.size(), TextSegment.from(""));
        return addAll(embeddings, emptySegments);
    }

    // Chroma add 批量添加向量及对应文本段
    @SuppressWarnings("unchecked")
    @Override
    public List<String> addAll(List<Embedding> embeddings, List<TextSegment> segments) {
        Objects.requireNonNull(embeddings, "embeddings must not be null");
        Objects.requireNonNull(segments, "segments must not be null");
        try {
            String url = chromaBaseUrl + "/api/v1/collections/" + collectionId + "/add";

            List<String> ids = new ArrayList<>();
            List<List<Float>> embeddingList = new ArrayList<>();
            List<Map<String, Object>> metadataList = new ArrayList<>();
            List<String> documentList = new ArrayList<>();

            for (int i = 0; i < embeddings.size(); i++) {
                ids.add(UUID.randomUUID().toString());

                float[] vector = embeddings.get(i).vector();
                List<Float> vecList = new ArrayList<>();
                for (float v : vector) vecList.add(v);
                embeddingList.add(vecList);

                if (i < segments.size()) {
                    TextSegment segment = segments.get(i);
                    documentList.add(segment.text() != null ? segment.text() : "");
                    Map<String, Object> meta = new HashMap<>();
                    if (segment.metadata() != null) {
                        for (Map.Entry<String, Object> entry : segment.metadata().toMap().entrySet()) {
                            meta.put(entry.getKey(), entry.getValue());
                        }
                    }
                    metadataList.add(meta);
                } else {
                    documentList.add("");
                    metadataList.add(Map.of());
                }
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> body = new HashMap<>();
            body.put("embeddings", embeddingList);
            body.put("metadatas", metadataList);
            body.put("documents", documentList);
            body.put("ids", ids);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            restTemplate.postForObject(url, request, String.class);

            return ids;
        } catch (Exception e) {
            log.warn("Chroma 批量添加失败", e);
            throw new RuntimeException("Chroma 批量添加失败", e);
        }
    }

    // ==================== API 业务所需 ====================

    // Chroma count 返回集合中的文档总数
    @Override
    public long count() {
        try {
            String url = chromaBaseUrl + "/api/v1/collections/" + collectionId + "/count";
            Long result = restTemplate.getForObject(url, Long.class);
            return result != null ? result : 0L;
        } catch (Exception e) {
            log.warn("Chroma count 失败", e);
            return 0L;
        }
    }

    // Chroma get 拉取全部 metadatas，按 source 字段分组后返回文件名与 chunk 数
    @Override
    public List<Map<String, Object>> getDocuments() {
        try {
            String url = chromaBaseUrl + "/api/v1/collections/" + collectionId + "/get";
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

            // 按 source（文件名）分组统计 chunk 数量
            Map<String, List<Map<String, Object>>> grouped = metadatas.stream()
                    .filter(m -> m != null && m.get("source") != null)
                    .collect(Collectors.groupingBy(m -> (String) m.get("source")));

            List<Map<String, Object>> docs = new ArrayList<>();
            for (Map.Entry<String, List<Map<String, Object>>> entry : grouped.entrySet()) {
                Map<String, Object> doc = new LinkedHashMap<>();
                doc.put("fileName", entry.getKey());
                doc.put("chunkCount", entry.getValue().size());
                Map<String, Object> firstMeta = entry.getValue().get(0);
                Object importTime = firstMeta.get("importTime");
                doc.put("importTime", importTime != null ? importTime.toString() : "");
                docs.add(doc);
            }
            docs.sort(Comparator.comparing(d -> (String) d.get("fileName")));
            return docs;

        } catch (Exception e) {
            log.warn("Chroma 获取文档列表失败", e);
            return List.of();
        }
    }

    // Chroma delete 按 where 条件（source = fileName）删除
    @Override
    public int deleteBySource(String fileName) {
        try {
            String url = chromaBaseUrl + "/api/v1/collections/" + collectionId + "/delete";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> body = new HashMap<>();
            body.put("where", Map.of("source", fileName));

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            var deletedIds = restTemplate.exchange(url, HttpMethod.POST, request,
                    new ParameterizedTypeReference<List<String>>() {}).getBody();
            return deletedIds != null ? deletedIds.size() : 0;
        } catch (Exception e) {
            log.warn("Chroma 按文件名删除失败：{}", fileName, e);
            return 0;
        }
    }

    // Chroma v2 GET collection API 获取集合名称和向量维度
    @Override
    public Map<String, Object> getCollectionMetadata() {
        try {
            // 调用 Chroma v2 集合查询接口
            String url = chromaBaseUrl + "/api/v2/tenants/" + tenant + "/databases/" + database + "/collections/" + collectionName;
            Map<String, Object> result = restTemplate.getForObject(url, Map.class);
            if (result == null) return Map.of();

            Map<String, Object> meta = new LinkedHashMap<>();
            // 从响应中提取集合名称
            meta.put("collectionName", result.getOrDefault("name", ""));
            // 从 collection metadata 字典中尝试读取向量维度
            Object metadataMap = result.get("metadata");
            if (metadataMap instanceof Map) {
                Object dim = ((Map<?, ?>) metadataMap).get("dimension");
                if (dim instanceof Number) {
                    meta.put("dimension", ((Number) dim).intValue());
                }
            }
            // 未获取到 dimension 时兜底返回 0
            meta.putIfAbsent("dimension", 0);
            return meta;
        } catch (Exception e) {
            log.warn("Chroma 获取集合元信息失败", e);
            return Map.of("collectionName", collectionName, "dimension", 0);
        }
    }
}
