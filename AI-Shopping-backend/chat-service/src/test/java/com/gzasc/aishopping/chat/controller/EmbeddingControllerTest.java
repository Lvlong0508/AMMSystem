package com.gzasc.aishopping.chat.controller;

import com.gzasc.aishopping.chat.service.EmbeddingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class EmbeddingControllerTest {

    private MockMvc mockMvc;

    @Mock
    private EmbeddingService embeddingService;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(new EmbeddingController(embeddingService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ---- /embedding/overview ----

    @Test
    @DisplayName("概览 - 成功返回统计数据与最近导入")
    void overview_success() throws Exception {
        when(embeddingService.getOverview()).thenReturn(Map.of(
                "totalChunks", 100L,
                "totalDocs", 5,
                "collectionName", "test_collection",
                "dimension", 384,
                "recentDocs", List.of(Map.of("fileName", "a.txt", "chunkCount", 3, "importTime", "2026-06-30 17:27:36"))
        ));

        mockMvc.perform(post("/embedding/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalChunks").value(100))
                .andExpect(jsonPath("$.data.totalDocs").value(5))
                .andExpect(jsonPath("$.data.collectionName").value("test_collection"))
                .andExpect(jsonPath("$.data.dimension").value(384))
                .andExpect(jsonPath("$.data.recentDocs[0].fileName").value("a.txt"));
    }

    // ---- /embedding/documents ----

    @Test
    @DisplayName("GET 文档列表 - 成功返回文档列表")
    void documents_success() throws Exception {
        when(embeddingService.getDocuments()).thenReturn(List.of(
                Map.of("fileName", "a.txt", "chunkCount", 3, "importTime", "2025-01-01T12:00:00"),
                Map.of("fileName", "b.txt", "chunkCount", 5, "importTime", "2025-01-02T12:00:00")
        ));

        mockMvc.perform(post("/embedding/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].fileName").value("a.txt"))
                .andExpect(jsonPath("$.data[0].chunkCount").value(3))
                .andExpect(jsonPath("$.data[0].importTime").value("2025-01-01T12:00:00"))
                .andExpect(jsonPath("$.data[1].fileName").value("b.txt"))
                .andExpect(jsonPath("$.data[1].chunkCount").value(5))
                .andExpect(jsonPath("$.data[1].importTime").value("2025-01-02T12:00:00"));
    }

    // ---- /embedding/search ----

    @Test
    @DisplayName("搜索 - 正常返回结果")
    void search_success() throws Exception {
        when(embeddingService.search("test query", 5)).thenReturn(List.of(
                Map.of("chunkId", "c1", "fileName", "a.txt", "content", "hello", "score", 0.95)
        ));

        mockMvc.perform(post("/embedding/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\": \"test query\", \"topK\": 5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].chunkId").value("c1"))
                .andExpect(jsonPath("$.data[0].score").value(0.95));
    }

    @Test
    @DisplayName("搜索 - 查空字符串返回错误")
    void search_blankQuery() throws Exception {
        mockMvc.perform(post("/embedding/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\": \"\", \"topK\": 5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("搜索关键词不能为空"));
    }

    @Test
    @DisplayName("搜索 - 没有 query 参数返回错误")
    void search_missingQuery() throws Exception {
        mockMvc.perform(post("/embedding/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"topK\": 5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("搜索关键词不能为空"));
    }

    // ---- /embedding/delete ----

    @Test
    @DisplayName("删除 - 正常删除返回删除数")
    void delete_success() throws Exception {
        when(embeddingService.deleteFromVector(anyList())).thenReturn(Map.of("deleted", 5));

        mockMvc.perform(post("/embedding/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[\"a.txt\", \"b.txt\"]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.deleted").value(5));
    }

    @Test
    @DisplayName("删除 - 空列表返回错误")
    void delete_emptyList() throws Exception {
        when(embeddingService.deleteFromVector(argThat(list -> ((List<?>)list).isEmpty())))
                .thenThrow(new IllegalArgumentException("请指定要删除的文件"));

        mockMvc.perform(post("/embedding/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("请指定要删除的文件"));
    }
}
