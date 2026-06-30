package com.gzasc.aishopping.chat.dao.impl;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChromaEmbeddingDaoImplTest {

    @Mock
    private RestTemplate restTemplate;

    private ChromaEmbeddingDaoImpl dao;

    private static final String COLLECTION_UUID = "550e8400-e29b-41d4-a716-446655440000";

    private static final String QUERY_URL = "http://localhost:8000/api/v1/collections/" + COLLECTION_UUID + "/query";
    private static final String DELETE_URL = "http://localhost:8000/api/v1/collections/" + COLLECTION_UUID + "/delete";

    @BeforeEach
    void setUp() {
        dao = new ChromaEmbeddingDaoImpl(restTemplate);
        ReflectionTestUtils.setField(dao, "chromaBaseUrl", "http://localhost:8000");
        ReflectionTestUtils.setField(dao, "collectionName", "ai_shopping");
        ReflectionTestUtils.setField(dao, "collectionId", COLLECTION_UUID);
        ReflectionTestUtils.setField(dao, "tenant", "default_tenant");
        ReflectionTestUtils.setField(dao, "database", "default_database");
    }

    // ---- search ----

    @Test
    @DisplayName("search - 正常检索返回匹配列表")
    void search_normal_returnsMatches() {
        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(Embedding.from(new float[]{0.1f, 0.2f}))
                .maxResults(3)
                .build();

        Map<String, Object> chromaResponse = Map.of(
                "ids", List.of(List.of("id1", "id2")),
                "distances", List.of(List.of(0.05, 0.15)),
                "metadatas", List.of(List.of(Map.of(), Map.of())),
                "documents", List.of(List.of("result1", "result2"))
        );
        when(restTemplate.exchange(
                eq(QUERY_URL), eq(HttpMethod.POST), any(HttpEntity.class), any(ParameterizedTypeReference.class)
        )).thenReturn(ResponseEntity.ok(chromaResponse));

        EmbeddingSearchResult<TextSegment> result = dao.search(request);

        assertNotNull(result);
        List<EmbeddingMatch<TextSegment>> matches = result.matches();
        assertEquals(2, matches.size());
        assertEquals("result1", matches.get(0).embedded().text());
        assertEquals(0.95, matches.get(0).score(), 1e-9);
    }

    @Test
    @DisplayName("search - 请求参数正确拼装")
    void search_passesRequestThrough() {
        Embedding embedding = Embedding.from(new float[]{0.5f, 0.6f, 0.7f});
        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(embedding)
                .maxResults(5)
                .minScore(0.7)
                .build();

        Map<String, Object> chromaResponse = Map.of(
                "ids", List.of(List.of()),
                "distances", List.of(List.of()),
                "metadatas", List.of(List.of()),
                "documents", List.of(List.of())
        );
        when(restTemplate.exchange(
                anyString(), any(HttpMethod.class), any(HttpEntity.class), any(ParameterizedTypeReference.class)
        )).thenReturn(ResponseEntity.ok(chromaResponse));

        dao.search(request);

        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(
                eq(QUERY_URL), eq(HttpMethod.POST), captor.capture(), any(ParameterizedTypeReference.class)
        );
        Map<String, Object> body = (Map<String, Object>) captor.getValue().getBody();
        List<List<Float>> embeddings = (List<List<Float>>) body.get("query_embeddings");
        assertArrayEquals(new double[]{0.5, 0.6, 0.7},
                embeddings.get(0).stream().mapToDouble(Float::doubleValue).toArray(), 1e-6);
        assertEquals(5, body.get("n_results"));
    }

    @Test
    @DisplayName("search - 空结果返回空列表")
    void search_emptyResult_returnsEmptyList() {
        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(Embedding.from(new float[]{0.1f, 0.2f}))
                .maxResults(10)
                .build();

        Map<String, Object> chromaResponse = Map.of(
                "ids", List.of(List.of()),
                "distances", List.of(List.of()),
                "metadatas", List.of(List.of()),
                "documents", List.of(List.of())
        );
        when(restTemplate.exchange(
                anyString(), any(HttpMethod.class), any(HttpEntity.class), any(ParameterizedTypeReference.class)
        )).thenReturn(ResponseEntity.ok(chromaResponse));

        EmbeddingSearchResult<TextSegment> result = dao.search(request);

        assertTrue(result.matches().isEmpty());
    }

    @Test
    @DisplayName("search - null 请求抛异常")
    void search_nullRequest_throwsException() {
        assertThrows(NullPointerException.class, () -> dao.search(null));
    }

    // ---- remove ----

    @Test
    @DisplayName("remove - 调用 HTTP delete")
    void remove_delegatesToStore() {
        when(restTemplate.exchange(
                eq(DELETE_URL), eq(HttpMethod.POST), any(HttpEntity.class), any(ParameterizedTypeReference.class)
        )).thenReturn(ResponseEntity.ok(List.of("abc-123")));

        dao.remove("abc-123");

        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(
                eq(DELETE_URL), eq(HttpMethod.POST), captor.capture(), any(ParameterizedTypeReference.class)
        );
        Map<String, Object> body = (Map<String, Object>) captor.getValue().getBody();
        assertEquals(List.of("abc-123"), body.get("ids"));
    }

    @Test
    @DisplayName("remove - null id 抛异常")
    void remove_nullId_throwsException() {
        assertThrows(NullPointerException.class, () -> dao.remove(null));
    }

    // ---- removeAll ----

    @Test
    @DisplayName("removeAll - 调用 HTTP batch delete")
    void removeAll_delegatesToStore() {
        List<String> ids = List.of("id1", "id2", "id3");
        when(restTemplate.exchange(
                eq(DELETE_URL), eq(HttpMethod.POST), any(HttpEntity.class), any(ParameterizedTypeReference.class)
        )).thenReturn(ResponseEntity.ok(List.of("id1", "id2", "id3")));

        dao.removeAll(ids);

        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(
                eq(DELETE_URL), eq(HttpMethod.POST), captor.capture(), any(ParameterizedTypeReference.class)
        );
        Map<String, Object> body = (Map<String, Object>) captor.getValue().getBody();
        assertEquals(ids, body.get("ids"));
    }

    @Test
    @DisplayName("removeAll - 空集合调用 HTTP")
    void removeAll_emptyCollection_passesThrough() {
        Collection<String> empty = Collections.emptyList();
        when(restTemplate.exchange(
                anyString(), any(HttpMethod.class), any(HttpEntity.class), any(ParameterizedTypeReference.class)
        )).thenReturn(ResponseEntity.ok(List.of()));

        dao.removeAll(empty);

        verify(restTemplate).exchange(
                eq(DELETE_URL), eq(HttpMethod.POST), any(HttpEntity.class), any(ParameterizedTypeReference.class)
        );
    }

    @Test
    @DisplayName("removeAll - null 抛异常")
    void removeAll_null_throwsException() {
        assertThrows(NullPointerException.class, () -> dao.removeAll((Collection<String>) null));
    }

    // ---- count ----

    @Test
    @DisplayName("count - 正常返回数量")
    void count_normal() {
        when(restTemplate.getForObject(
                "http://localhost:8000/api/v1/collections/" + COLLECTION_UUID + "/count", Long.class))
                .thenReturn(42L);

        long result = dao.count();

        assertEquals(42L, result);
    }

    @Test
    @DisplayName("count - 接口异常返回 0")
    void count_exception_returnsZero() {
        when(restTemplate.getForObject(anyString(), eq(Long.class)))
                .thenThrow(new RuntimeException("connection refused"));

        long result = dao.count();

        assertEquals(0L, result);
    }

    // ---- 集合自动创建 ----

    @Test
    @DisplayName("resolveCollectionId - 不存在时自动创建")
    void resolveCollectionId_autoCreate() {
        ReflectionTestUtils.setField(dao, "collectionId", null);
        String getUrl = "http://localhost:8000/api/v2/tenants/default_tenant/databases/default_database/collections/ai_shopping";
        String createUrl = "http://localhost:8000/api/v2/tenants/default_tenant/databases/default_database/collections";

        when(restTemplate.getForObject(eq(getUrl), eq(Map.class)))
                .thenThrow(new RuntimeException("404 Not Found"));
        when(restTemplate.getForObject(
                "http://localhost:8000/api/v2/tenants/default_tenant", Map.class))
                .thenReturn(Map.of("name", "default_tenant"));
        when(restTemplate.getForObject(
                "http://localhost:8000/api/v2/tenants/default_tenant/databases/default_database", Map.class))
                .thenReturn(Map.of("name", "default_database"));
        when(restTemplate.exchange(
                eq(createUrl), eq(HttpMethod.POST), any(HttpEntity.class), any(ParameterizedTypeReference.class)
        )).thenReturn(ResponseEntity.ok(Map.of("id", COLLECTION_UUID, "name", "ai_shopping")));

        ReflectionTestUtils.invokeMethod(dao, "resolveCollectionId");

        String resultId = (String) ReflectionTestUtils.getField(dao, "collectionId");
        assertEquals(COLLECTION_UUID, resultId);
        verify(restTemplate).getForObject(eq(getUrl), eq(Map.class));
        verify(restTemplate).exchange(
                eq(createUrl), eq(HttpMethod.POST), any(HttpEntity.class), any(ParameterizedTypeReference.class)
        );
    }

    // ---- getDocuments ----

    @Test
    @DisplayName("getDocuments - 正常返回按 source 分组的文档列表")
    void getDocuments_normal() {
        Map<String, Object> chromaResponse = Map.of(
                "metadatas", List.of(
                        Map.of("source", "a.txt"),
                        Map.of("source", "a.txt"),
                        Map.of("source", "b.txt")
                )
        );
        when(restTemplate.exchange(
                eq("http://localhost:8000/api/v1/collections/" + COLLECTION_UUID + "/get"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(ResponseEntity.ok(chromaResponse));

        List<Map<String, Object>> docs = dao.getDocuments();

        assertEquals(2, docs.size());
        assertEquals("a.txt", docs.get(0).get("fileName"));
        assertEquals(2, docs.get(0).get("chunkCount"));
        assertEquals("b.txt", docs.get(1).get("fileName"));
        assertEquals(1, docs.get(1).get("chunkCount"));
    }

    @Test
    @DisplayName("getDocuments - 元数据为空返回空列表")
    void getDocuments_emptyMetadatas() {
        Map<String, Object> chromaResponse = Map.of("metadatas", List.of());
        when(restTemplate.exchange(
                anyString(), any(HttpMethod.class), any(), any(ParameterizedTypeReference.class)
        )).thenReturn(ResponseEntity.ok(chromaResponse));

        List<Map<String, Object>> docs = dao.getDocuments();

        assertTrue(docs.isEmpty());
    }

    @Test
    @DisplayName("getDocuments - 响应为 null 返回空列表")
    void getDocuments_nullResponse() {
        when(restTemplate.exchange(
                anyString(), any(HttpMethod.class), any(), any(ParameterizedTypeReference.class)
        )).thenReturn(ResponseEntity.ok(null));

        List<Map<String, Object>> docs = dao.getDocuments();

        assertTrue(docs.isEmpty());
    }

    @Test
    @DisplayName("getDocuments - 接口异常返回空列表")
    void getDocuments_exception() {
        when(restTemplate.exchange(
                anyString(), any(HttpMethod.class), any(), any(ParameterizedTypeReference.class)
        )).thenThrow(new RuntimeException("timeout"));

        List<Map<String, Object>> docs = dao.getDocuments();

        assertTrue(docs.isEmpty());
    }

    // ---- deleteBySource ----

    @Test
    @DisplayName("deleteBySource - 正常删除返回删除数")
    void deleteBySource_normal() {
        when(restTemplate.exchange(
                eq("http://localhost:8000/api/v1/collections/" + COLLECTION_UUID + "/delete"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(ResponseEntity.ok(List.of("id1", "id2", "id3")));

        int deleted = dao.deleteBySource("a.txt");

        assertEquals(3, deleted);
    }

    @Test
    @DisplayName("deleteBySource - 无匹配返回 0")
    void deleteBySource_emptyResult() {
        when(restTemplate.exchange(
                anyString(), any(HttpMethod.class), any(), any(ParameterizedTypeReference.class)
        )).thenReturn(ResponseEntity.ok(List.of()));

        int deleted = dao.deleteBySource("nonexistent.txt");

        assertEquals(0, deleted);
    }

    @Test
    @DisplayName("deleteBySource - 接口异常返回 0")
    void deleteBySource_exception() {
        when(restTemplate.exchange(
                anyString(), any(HttpMethod.class), any(), any(ParameterizedTypeReference.class)
        )).thenThrow(new RuntimeException("delete failed"));

        int deleted = dao.deleteBySource("a.txt");

        assertEquals(0, deleted);
    }
}
