package com.gzasc.aishopping.chat.service.impl;

import com.gzasc.aishopping.chat.service.RAGService;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RAGServiceImplTest {

    @Mock
    private EmbeddingModel embeddingModel;

    @Mock
    private EmbeddingStore<TextSegment> embeddingStore;

    @Captor
    private ArgumentCaptor<List<TextSegment>> segmentsCaptor;

    private RAGService ragService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        var impl = new RAGServiceImpl(embeddingModel, embeddingStore);
        impl.init();
        ragService = impl;
    }

    private Path createTxtFile(String name, String content) throws IOException {
        Path file = tempDir.resolve(name);
        Files.writeString(file, content);
        return file;
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

    private Path createDocxFile(String name) throws IOException {
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
        return file;
    }

    @Test
    @DisplayName("单文件导入成功 - 返回空Map")
    void input_singleFile_success_returnsEmptyMap() throws IOException {
        Path file = createTxtFile("test.txt", "Hello. This is a test document for RAG ingestion.");
        mockEmbedding();

        Map<String, String> result = ragService.input(List.of(file.toString()));

        assertTrue(result.isEmpty());
        verify(embeddingModel).embedAll(anyList());
        verify(embeddingStore).addAll(anyList(), anyList());
    }

    @Test
    @DisplayName("多文件全部成功 - 返回空Map")
    void input_multipleFilesAllSuccess_returnsEmptyMap() throws IOException {
        createTxtFile("a.txt", "Content A");
        createTxtFile("b.txt", "Content B");
        createTxtFile("c.txt", "Content C");
        mockEmbedding();

        Map<String, String> result = ragService.input(List.of(
                tempDir.resolve("a.txt").toString(),
                tempDir.resolve("b.txt").toString(),
                tempDir.resolve("c.txt").toString()
        ));

        assertTrue(result.isEmpty());
        verify(embeddingModel, times(3)).embedAll(anyList());
        verify(embeddingStore, times(3)).addAll(anyList(), anyList());
    }

    @Test
    @DisplayName("文件不存在 - 返回失败Map")
    void input_fileNotFound_returnsFailedMap() {
        String nonExistent = tempDir.resolve("not-exist.txt").toString();

        Map<String, String> result = ragService.input(List.of(nonExistent));

        assertEquals(1, result.size());
        assertTrue(result.containsKey("not-exist.txt"));
        assertNotNull(result.get("not-exist.txt"));
    }

    @Test
    @DisplayName("部分成功部分失败 - 返回失败Map")
    void input_partialSuccess_returnsFailedMap() throws IOException {
        Path goodFile = createTxtFile("good.txt", "Good content.");
        String badFile = tempDir.resolve("bad.txt").toString();
        mockEmbedding();

        Map<String, String> result = ragService.input(List.of(goodFile.toString(), badFile));

        assertEquals(1, result.size());
        assertTrue(result.containsKey("bad.txt"));
        assertFalse(result.containsKey("good.txt"));
        verify(embeddingModel, times(1)).embedAll(anyList());
    }

    @Test
    @DisplayName("空文件列表 - 返回空Map")
    void input_emptyList_returnsEmptyMap() {
        Map<String, String> result = ragService.input(List.of());

        assertTrue(result.isEmpty());
        verify(embeddingModel, never()).embedAll(anyList());
        verify(embeddingStore, never()).addAll(anyList(), anyList());
    }

    @Test
    @DisplayName("null 文件列表 - 抛 NullPointerException")
    void input_null_throwsNullPointer() {
        assertThrows(NullPointerException.class, () -> ragService.input(null));
    }

    @Test
    @DisplayName("导入时注入 Metadata - 包含 source 和 file_path")
    void input_metadata_containsSourceAndFilePath() throws IOException {
        Path file = createTxtFile("mydoc.txt", "Metadata test content.");
        mockEmbedding();

        ragService.input(List.of(file.toString()));

        verify(embeddingStore).addAll(anyList(), segmentsCaptor.capture());
        List<TextSegment> segments = segmentsCaptor.getValue();
        assertFalse(segments.isEmpty());
        for (TextSegment segment : segments) {
            assertEquals("mydoc.txt", segment.metadata().getString("source"));
            assertEquals(file.toAbsolutePath().toString(), segment.metadata().getString("file_path"));
        }
    }

    @Test
    @DisplayName(".docx 文件走 ApachePoiParser 分支")
    void input_docxFile_triggersApachePoiParser() throws IOException {
        Path file = createDocxFile("report.docx");

        Map<String, String> result = ragService.input(List.of(file.toString()));

        String msg = result.get("report.docx");
        assertNotNull(msg);
        assertTrue(msg.contains("Failed to load document"),
                "应触发 ApachePoiParser 解析失败，实际: " + msg);
    }

    @Test
    @DisplayName("导入异常 - 错误消息包含文件名")
    void input_failure_messageContainsFileName() {
        Map<String, String> result = ragService.input(List.of(tempDir.resolve("unknown.txt").toString()));

        String msg = result.get("unknown.txt");
        assertNotNull(msg);
        assertTrue(msg.contains("unknown.txt"),
                "错误消息应包含文件名，实际: " + msg);
    }
}
