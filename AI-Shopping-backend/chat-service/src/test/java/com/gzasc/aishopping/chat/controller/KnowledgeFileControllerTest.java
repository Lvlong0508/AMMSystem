package com.gzasc.aishopping.chat.controller;

import com.gzasc.aishopping.chat.service.EmbeddingService;
import com.gzasc.aishopping.chat.service.FileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class KnowledgeFileControllerTest {

    private MockMvc mockMvc;

    @Mock
    private FileService fileService;

    @Mock
    private EmbeddingService embeddingService;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(new KnowledgeFileController(fileService, embeddingService)).build();
    }

    @Test
    @DisplayName("上传全部成功 - 返回上传成功")
    void upload_allSuccess() throws Exception {
        when(fileService.save(anyList())).thenReturn(List.of());

        mockMvc.perform(multipart("/file/upload")
                        .file(new MockMultipartFile("files", "a.txt", "text/plain", "content".getBytes())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data").value("上传成功"));
    }

    @Test
    @DisplayName("上传部分失败 - 返回失败文件名")
    void upload_someFail() throws Exception {
        when(fileService.save(anyList())).thenReturn(List.of("b.txt"));

        mockMvc.perform(multipart("/file/upload")
                        .file(new MockMultipartFile("files", "a.txt", "text/plain", "content".getBytes()))
                        .file(new MockMultipartFile("files", "b.txt", "text/plain", "content".getBytes())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data").value("上传完成，部分文件失败：b.txt"));
    }

    @Test
    @DisplayName("无文件参数 - 返回 400")
    void upload_noFiles() throws Exception {
        mockMvc.perform(multipart("/file/upload"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("删除 upload 文件 - 成功")
    void deleteUpload_success() throws Exception {
        mockMvc.perform(post("/file/delete/upload")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[\"a.txt\", \"b.txt\"]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("删除成功"));
    }

    @Test
    @DisplayName("删除 finish 文件 - 成功")
    void deleteFinish_success() throws Exception {
        mockMvc.perform(post("/file/delete/finish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[\"a.txt\"]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("删除成功"));
    }

    @Test
    @DisplayName("删除 upload 文件 - 空列表返回错误")
    void deleteUpload_emptyList() throws Exception {
        mockMvc.perform(post("/file/delete/upload")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("请指定要删除的文件"));
    }

    @Test
    @DisplayName("导入知识库 - 全部成功")
    void ingestFiles_allSuccess() throws Exception {
        when(embeddingService.ingest(anyList())).thenReturn(List.of());

        mockMvc.perform(post("/file/ingest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[\"a.txt\"]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("导入知识库 - 部分失败")
    void ingestFiles_partialFail() throws Exception {
        when(embeddingService.ingest(anyList())).thenReturn(
                List.of(Map.of("fileName", "b.txt", "error", "文件不存在")));

        mockMvc.perform(post("/file/ingest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[\"a.txt\", \"b.txt\"]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].fileName").value("b.txt"))
                .andExpect(jsonPath("$.data[0].error").value("文件不存在"));
    }

    @Test
    @DisplayName("导入知识库 - 空列表返回错误")
    void ingestFiles_emptyList() throws Exception {
        mockMvc.perform(post("/file/ingest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    @DisplayName("获取 upload 文件列表 - 成功")
    void listUploadFiles_success() throws Exception {
        when(fileService.getFileNamesFromUpload()).thenReturn(List.of("a.txt", "b.txt"));

        mockMvc.perform(post("/file/list/upload"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0]").value("a.txt"))
                .andExpect(jsonPath("$.data[1]").value("b.txt"));
    }

    @Test
    @DisplayName("获取 finish 文件列表 - 成功")
    void listFinishFiles_success() throws Exception {
        when(fileService.getFileNamesFromFinish()).thenReturn(List.of("done.docx"));

        mockMvc.perform(post("/file/list/finish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0]").value("done.docx"));
    }
}
