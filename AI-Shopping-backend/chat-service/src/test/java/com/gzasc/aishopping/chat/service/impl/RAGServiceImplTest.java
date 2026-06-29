package com.gzasc.aishopping.chat.service.impl;

import com.gzasc.aishopping.chat.service.FileService;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RAGServiceImplTest {

    @Mock
    private EmbeddingModel embeddingModel;

    @Mock
    private EmbeddingStore<TextSegment> embeddingStore;

    @Mock
    private FileService fileService;

    @Captor
    private ArgumentCaptor<List<TextSegment>> segmentsCaptor;

    private RAGServiceImpl ragService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        ragService = new RAGServiceImpl(embeddingModel, embeddingStore, fileService);
        ReflectionTestUtils.setField(ragService, "uploadDir", tempDir.toString());
        ragService.init();
    }

    private void createTxtFile(String name, String content) throws IOException {
        Files.writeString(tempDir.resolve(name), content);
    }

    private void createDocxFile(String name) throws IOException {
        Path file = tempDir.resolve(name);
        try (OutputStream os = Files.newOutputStream(file);
             ZipOutputStream zos = new ZipOutputStream(os)) {
            zos.putNextEntry(new ZipEntry("[Content_Types].xml"));
            zos.write("<?xml version=\"1.0\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"xml\" ContentType=\"application/xml\"/></Types>".getBytes());
            zos.closeEntry();
            zos.putNextEntry(new ZipEntry("word/document.xml"));
            zos.write("<?xml version=\"1.0\"?><w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\"><w:body><w:p><w:r><w:t>Hello</w:t></w:r></w:p></w:body></w:document>".getBytes());
            zos.closeEntry();
        }
    }

    private void mockEmbedding() {
        when(embeddingModel.embedAll(anyList())).thenAnswer(invocation -> {
            List<TextSegment> segments = invocation.getArgument(0);
            List<Embedding> embeddings = segments.stream()
                    .map(s -> Embedding.from(new float[]{0.1f, 0.2f, 0.3f}))
                    .toList();
            return Response.from(embeddings);
        });
        when(embeddingStore.addAll(anyList(), anyList())).thenReturn(List.of("id1"));
    }

    @Test
    @DisplayName("单文件导入成功 - 调用 fileService.move()")
    void input_singleFile_success() throws IOException {
        createTxtFile("test.txt", "Hello. This is a test document for RAG ingestion.");
        mockEmbedding();

        List<Map<String, String>> result = ragService.input(List.of("test.txt"));

        assertTrue(result.isEmpty());
        verify(fileService).move("test.txt");
        verify(embeddingModel).embedAll(anyList());
        verify(embeddingStore).addAll(anyList(), anyList());
    }

    @Test
    @DisplayName("多文件全部成功 - 返回空列表")
    void input_multipleFiles_success() throws IOException {
        createTxtFile("a.txt", "Content A");
        createTxtFile("b.txt", "Content B");
        createTxtFile("c.txt", "Content C");
        mockEmbedding();

        List<Map<String, String>> result = ragService.input(List.of("a.txt", "b.txt", "c.txt"));

        assertTrue(result.isEmpty());
        verify(embeddingModel, times(3)).embedAll(anyList());
        verify(embeddingStore, times(3)).addAll(anyList(), anyList());
    }

    @Test
    @DisplayName("文件不存在 - 不调用 fileService.move()")
    void input_fileNotFound() {
        List<Map<String, String>> result = ragService.input(List.of("not-exist.txt"));

        assertEquals(1, result.size());
        assertEquals("not-exist.txt", result.get(0).get("fileName"));
        assertEquals("文件不存在", result.get(0).get("error"));
        verify(fileService, never()).move(anyString());
    }

    @Test
    @DisplayName("部分成功部分失败 - 返回失败列表")
    void input_partialSuccess() throws IOException {
        createTxtFile("good.txt", "Good content.");
        mockEmbedding();

        List<Map<String, String>> result = ragService.input(List.of("good.txt", "bad.txt"));

        assertEquals(1, result.size());
        assertEquals("bad.txt", result.get(0).get("fileName"));
        assertEquals("文件不存在", result.get(0).get("error"));
        verify(embeddingModel, times(1)).embedAll(anyList());
    }

    @Test
    @DisplayName("空列表 - 返回空列表")
    void input_emptyList() {
        List<Map<String, String>> result = ragService.input(List.of());

        assertTrue(result.isEmpty());
        verify(embeddingModel, never()).embedAll(anyList());
        verify(embeddingStore, never()).addAll(anyList(), anyList());
    }

    @Test
    @DisplayName("null 列表 - 抛 NullPointerException")
    void input_null() {
        assertThrows(NullPointerException.class, () -> ragService.input(null));
    }

    @Test
    @DisplayName("Metadata 注入 source 和 file_path")
    void input_metadata() throws IOException {
        createTxtFile("mydoc.txt", "Metadata test content.");
        mockEmbedding();

        ragService.input(List.of("mydoc.txt"));

        verify(embeddingStore).addAll(anyList(), segmentsCaptor.capture());
        List<TextSegment> segments = segmentsCaptor.getValue();
        assertFalse(segments.isEmpty());
        for (TextSegment segment : segments) {
            assertEquals("mydoc.txt", segment.metadata().getString("source"));
            assertEquals(tempDir.resolve("mydoc.txt").toAbsolutePath().toString(),
                    segment.metadata().getString("file_path"));
        }
    }

    @Test
    @DisplayName(".docx 文件走 ApachePoiParser 分支")
    void input_docxFile() throws IOException {
        createDocxFile("report.docx");

        List<Map<String, String>> result = ragService.input(List.of("report.docx"));

        assertEquals(1, result.size());
        assertEquals("report.docx", result.get(0).get("fileName"));
        assertTrue(result.get(0).get("error").contains("Failed to load document"),
                "应触发 ApachePoiParser 解析失败，实际: " + result.get(0).get("error"));
    }

    @Test
    @DisplayName("文件不存在 - 错误信息为'文件不存在'")
    void input_notExistMessage() {
        List<Map<String, String>> result = ragService.input(List.of("unknown.txt"));

        assertEquals("unknown.txt", result.get(0).get("fileName"));
        assertEquals("文件不存在", result.get(0).get("error"));
    }
}
