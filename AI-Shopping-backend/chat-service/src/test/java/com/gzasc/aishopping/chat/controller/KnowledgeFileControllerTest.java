package com.gzasc.aishopping.chat.controller;

import com.gzasc.aishopping.chat.service.FileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class KnowledgeFileControllerTest {

    private MockMvc mockMvc;

    @Mock
    private FileService fileService;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(new KnowledgeFileController(fileService)).build();
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
}
