package com.gzasc.aishopping.chat.dao.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChromaAdminDaoImplTest {

    @Mock
    private RestTemplate restTemplate;

    private ChromaAdminDaoImpl dao;

    @BeforeEach
    void setUp() {
        dao = new ChromaAdminDaoImpl(restTemplate);
        ReflectionTestUtils.setField(dao, "chromaBaseUrl", "http://localhost:8000");
        ReflectionTestUtils.setField(dao, "collectionName", "ai_shopping");
    }

    // ---- count ----

    @Test
    @DisplayName("count - 正常返回数量")
    void count_normal() {
        when(restTemplate.getForObject("http://localhost:8000/api/v1/collections/ai_shopping/count", Long.class))
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
                eq("http://localhost:8000/api/v1/collections/ai_shopping/get"),
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
                eq("http://localhost:8000/api/v1/collections/ai_shopping/delete"),
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
