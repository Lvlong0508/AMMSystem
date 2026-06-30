package com.gzasc.aishopping.chat.service.impl;

import com.gzasc.aishopping.chat.dao.ChromaEmbeddingDao;
import com.gzasc.aishopping.chat.service.FileService;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmbeddingServiceImplTest {

    @Mock
    private EmbeddingModel embeddingModel;

    @Mock
    private ChromaEmbeddingDao chromaEmbeddingDao;

    @Mock
    private FileService fileService;

    @Captor
    private ArgumentCaptor<List<TextSegment>> segmentsCaptor;

    private EmbeddingServiceImpl embeddingService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        embeddingService = new EmbeddingServiceImpl(embeddingModel, chromaEmbeddingDao, fileService);
        ReflectionTestUtils.setField(embeddingService, "uploadDir", tempDir.toString());
        embeddingService.init();
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
        when(chromaEmbeddingDao.addAll(anyList(), anyList())).thenReturn(List.of("id1"));
    }

    // ==================== ingest ====================

    @Test
    @DisplayName("单文件导入成功 - 调用 fileService.move()")
    void ingest_singleFile_success() throws IOException {
        createTxtFile("test.txt", "Hello. This is a test document for RAG ingestion.");
        mockEmbedding();

        List<Map<String, String>> result = embeddingService.ingest(List.of("test.txt"));

        assertTrue(result.isEmpty());
        verify(fileService).move("test.txt");
        verify(embeddingModel).embedAll(anyList());
        verify(chromaEmbeddingDao).addAll(anyList(), anyList());
    }

    @Test
    @DisplayName("多文件全部成功 - 返回空列表")
    void ingest_multipleFiles_success() throws IOException {
        createTxtFile("a.txt", "Content A");
        createTxtFile("b.txt", "Content B");
        createTxtFile("c.txt", "Content C");
        mockEmbedding();

        List<Map<String, String>> result = embeddingService.ingest(List.of("a.txt", "b.txt", "c.txt"));

        assertTrue(result.isEmpty());
        verify(embeddingModel, times(3)).embedAll(anyList());
        verify(chromaEmbeddingDao, times(3)).addAll(anyList(), anyList());
    }

    @Test
    @DisplayName("文件不存在 - 不调用 fileService.move()")
    void ingest_fileNotFound() {
        List<Map<String, String>> result = embeddingService.ingest(List.of("not-exist.txt"));

        assertEquals(1, result.size());
        assertEquals("not-exist.txt", result.get(0).get("fileName"));
        assertEquals("文件不存在", result.get(0).get("error"));
        verify(fileService, never()).move(anyString());
    }

    @Test
    @DisplayName("部分成功部分失败 - 返回失败列表")
    void ingest_partialSuccess() throws IOException {
        createTxtFile("good.txt", "Good content.");
        mockEmbedding();

        List<Map<String, String>> result = embeddingService.ingest(List.of("good.txt", "bad.txt"));

        assertEquals(1, result.size());
        assertEquals("bad.txt", result.get(0).get("fileName"));
        assertEquals("文件不存在", result.get(0).get("error"));
        verify(embeddingModel, times(1)).embedAll(anyList());
    }

    @Test
    @DisplayName("空列表 - 返回空列表")
    void ingest_emptyList() {
        List<Map<String, String>> result = embeddingService.ingest(List.of());

        assertTrue(result.isEmpty());
        verify(embeddingModel, never()).embedAll(anyList());
        verify(chromaEmbeddingDao, never()).addAll(anyList(), anyList());
    }

    @Test
    @DisplayName("null 列表 - 抛 NullPointerException")
    void ingest_null() {
        assertThrows(NullPointerException.class, () -> embeddingService.ingest(null));
    }

    @Test
    @DisplayName("Metadata 注入 source 和 file_path")
    void ingest_metadata() throws IOException {
        createTxtFile("mydoc.txt", "Metadata test content.");
        mockEmbedding();

        embeddingService.ingest(List.of("mydoc.txt"));

        verify(chromaEmbeddingDao).addAll(anyList(), segmentsCaptor.capture());
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
    void ingest_docxFile() throws IOException {
        createDocxFile("report.docx");

        List<Map<String, String>> result = embeddingService.ingest(List.of("report.docx"));

        assertEquals(1, result.size());
        assertEquals("report.docx", result.get(0).get("fileName"));
        assertTrue(result.get(0).get("error").contains("Failed to load document"),
                "应触发 ApachePoiParser 解析失败，实际: " + result.get(0).get("error"));
    }

    // ==================== embed ====================

    @Test
    @DisplayName("embed - 返回向量数组")
    void embed_returnsVector() {
        when(embeddingModel.embed("hello")).thenReturn(
                Response.from(Embedding.from(new float[]{0.1f, 0.2f, 0.3f})));

        float[] vector = embeddingService.embed("hello");

        assertArrayEquals(new float[]{0.1f, 0.2f, 0.3f}, vector);
    }

    // ==================== getCollectionStats ====================

    @Test
    @DisplayName("getCollectionStats - 返回统计信息")
    void getCollectionStats_returnsStats() {
        when(chromaEmbeddingDao.count()).thenReturn(100L);
        when(chromaEmbeddingDao.getDocuments()).thenReturn(
                List.of(Map.of("fileName", "a.txt"), Map.of("fileName", "b.txt")));

        Map<String, Object> stats = embeddingService.getCollectionStats();

        assertEquals(100L, stats.get("totalChunks"));
        assertEquals(2, stats.get("totalDocs"));
    }

    // ==================== getDocuments ====================

    @Test
    @DisplayName("getDocuments - 委托到 chromaEmbeddingDao")
    void getDocuments_delegates() {
        List<Map<String, Object>> expected = List.of(Map.of("fileName", "a.txt", "chunkCount", 5));
        when(chromaEmbeddingDao.getDocuments()).thenReturn(expected);

        List<Map<String, Object>> result = embeddingService.getDocuments();

        assertSame(expected, result);
    }

    // ==================== search ====================

    @Test
    @DisplayName("search - 向量搜索后组装结果")
    void search_returnsResults() {
        when(embeddingModel.embed("test query")).thenReturn(
                Response.from(Embedding.from(new float[]{0.1f, 0.2f, 0.3f})));
        TextSegment segment = TextSegment.from("matched content");
        segment.metadata().put("source", "file.txt");
        List<EmbeddingMatch<TextSegment>> matches = List.of(
                new EmbeddingMatch<>(0.95, "chunk1", Embedding.from(new float[]{0.1f, 0.2f, 0.3f}), segment));
        when(chromaEmbeddingDao.search(any(EmbeddingSearchRequest.class))).thenReturn(new EmbeddingSearchResult<>(matches));

        List<Map<String, Object>> results = embeddingService.search("test query", 5);

        assertEquals(1, results.size());
        assertEquals("chunk1", results.get(0).get("chunkId"));
        assertEquals("file.txt", results.get(0).get("fileName"));
        assertEquals("matched content", results.get(0).get("content"));
        assertEquals(0.95, results.get(0).get("score"));
    }

    @Test
    @DisplayName("search - 空结果返回空列表")
    void search_emptyResult() {
        when(embeddingModel.embed(anyString())).thenReturn(
                Response.from(Embedding.from(new float[]{0.1f, 0.2f})));
        when(chromaEmbeddingDao.search(any(EmbeddingSearchRequest.class))).thenReturn(new EmbeddingSearchResult<>(List.of()));

        List<Map<String, Object>> results = embeddingService.search("empty", 5);

        assertTrue(results.isEmpty());
    }

    // ==================== deleteFromVector ====================

    @Test
    @DisplayName("deleteFromVector - 委托到 chromaEmbeddingDao 并返回删除数")
    void deleteFromVector_delegates() {
        when(chromaEmbeddingDao.deleteBySource("bad.txt")).thenReturn(3);

        int deleted = embeddingService.deleteFromVector("bad.txt");

        assertEquals(3, deleted);
        verify(chromaEmbeddingDao).deleteBySource("bad.txt");
    }

    @Test
    @DisplayName("deleteFromVector - 无匹配返回 0")
    void deleteFromVector_noMatch() {
        when(chromaEmbeddingDao.deleteBySource("nonexistent.txt")).thenReturn(0);

        int deleted = embeddingService.deleteFromVector("nonexistent.txt");

        assertEquals(0, deleted);
    }
}
