package com.gzasc.aishopping.chat.dao.impl;

import com.gzasc.aishopping.chat.exception.FileException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class FileStorageDaoImplTest {

    private FileStorageDaoImpl dao;
    private Path uploadDir;
    private Path finishDir;

    @BeforeEach
    void setUp() throws IOException {
        uploadDir = Files.createTempDirectory("test-upload-");
        finishDir = Files.createTempDirectory("test-finish-");

        dao = new FileStorageDaoImpl();
        ReflectionTestUtils.setField(dao, "storagePath", uploadDir.toString());
        ReflectionTestUtils.setField(dao, "finishPath", finishDir.toString());
        dao.init();
    }

    @AfterEach
    void tearDown() throws IOException {
        try (var files = Files.walk(uploadDir)) {
            files.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try { Files.deleteIfExists(path); } catch (IOException ignored) {}
                    });
        }
        try (var files = Files.walk(finishDir)) {
            files.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try { Files.deleteIfExists(path); } catch (IOException ignored) {}
                    });
        }
    }

    private MultipartFile file(String name, String content) {
        return new MockMultipartFile("file", name, "text/plain", content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("存储文件 - 写入文件并记录索引")
    void storeFile_createsFileAndIndex() {
        dao.storeFile(file("report.txt", "hello"), 1L);

        assertTrue(Files.exists(uploadDir.resolve("report.txt")));
        assertTrue(Files.exists(uploadDir.resolve("index.txt")));

        List<String> names = dao.getFileNameFromUpload();
        assertEquals(List.of("report.txt"), names);
    }

    @Test
    @DisplayName("存储文件 - 同名文件自动追加 (1) (2) 冲突后缀")
    void storeFile_conflictAppendsNumber() {
        dao.storeFile(file("report.txt", "first"), 1L);
        dao.storeFile(file("report.txt", "second"), 1L);
        dao.storeFile(file("report.txt", "third"), 1L);

        assertTrue(Files.exists(uploadDir.resolve("report.txt")));
        assertTrue(Files.exists(uploadDir.resolve("report (1).txt")));
        assertTrue(Files.exists(uploadDir.resolve("report (2).txt")));
        assertEquals(3, dao.getFileNameFromUpload().size());
    }

    @Test
    @DisplayName("存储文件 - 无扩展名文件冲突处理")
    void storeFile_conflictNoExtension() {
        dao.storeFile(file("README", "first"), 1L);
        dao.storeFile(file("README", "second"), 1L);

        assertTrue(Files.exists(uploadDir.resolve("README")));
        assertTrue(Files.exists(uploadDir.resolve("README (1)")));
    }

    @Test
    @DisplayName("移动文件 - 从 upload 移到 finish，索引同步")
    void moveFile_movesFileAndUpdatesIndex() {
        dao.storeFile(file("doc.txt", "content"), 1L);
        dao.moveFile("doc.txt");

        assertFalse(Files.exists(uploadDir.resolve("doc.txt")));
        assertTrue(Files.exists(finishDir.resolve("doc.txt")));
        assertTrue(Files.exists(finishDir.resolve("index.txt")));

        assertTrue(dao.getFileNameFromUpload().isEmpty());
        assertEquals(List.of("doc.txt"), dao.getFileNameFromFinish());
    }

    @Test
    @DisplayName("移动文件 - 不存在的文件抛异常")
    void moveFile_nonExistentThrows() {
        assertThrows(FileException.class, () -> dao.moveFile("ghost.txt"));
    }

    @Test
    @DisplayName("删除 upload 文件 - 删文件并清理索引")
    void deleteFileFromUpload_removesFileAndIndex() {
        dao.storeFile(file("a.txt", "1"), 1L);
        dao.storeFile(file("b.txt", "2"), 1L);
        dao.deleteFileFromUpload(List.of("a.txt"));

        assertFalse(Files.exists(uploadDir.resolve("a.txt")));
        assertTrue(Files.exists(uploadDir.resolve("b.txt")));
        assertEquals(List.of("b.txt"), dao.getFileNameFromUpload());
    }

    @Test
    @DisplayName("删除 finish 文件 - 删文件并清理索引")
    void deleteFileFromFinish_removesFileAndIndex() {
        dao.storeFile(file("doc.txt", "content"), 1L);
        dao.moveFile("doc.txt");
        dao.deleteFileFromFinish(List.of("doc.txt"));

        assertFalse(Files.exists(finishDir.resolve("doc.txt")));
        assertTrue(dao.getFileNameFromFinish().isEmpty());
    }

    @Test
    @DisplayName("列表 upload - 空目录返回空列表")
    void getFileNameFromUpload_emptyDir() {
        assertTrue(dao.getFileNameFromUpload().isEmpty());
    }

    @Test
    @DisplayName("文件存储 - 写入内容与读取一致")
    void storeFile_contentMatches() throws IOException {
        dao.storeFile(file("data.txt", "hello world"), 1L);

        String actual = Files.readString(uploadDir.resolve("data.txt"), StandardCharsets.UTF_8);
        assertEquals("hello world", actual);
    }

    @Test
    @DisplayName("存储文件 - userId 写入索引文件")
    void storeFile_recordsUserIdInIndex() throws IOException {
        dao.storeFile(file("user.txt", "data"), 42L);

        String indexContent = Files.readString(uploadDir.resolve("index.txt"), StandardCharsets.UTF_8);
        String[] parts = indexContent.trim().split("\\|");
        assertEquals("user.txt", parts[0]);
        assertEquals("user.txt", parts[1]);
        assertEquals("42", parts[2]);

        // 验证空 userId
        dao.storeFile(file("anon.txt", "data"), null);
        indexContent = Files.readString(uploadDir.resolve("index.txt"), StandardCharsets.UTF_8);
        String[] lines = indexContent.trim().split("\n");
        parts = lines[1].split("\\|");
        assertEquals("anon.txt", parts[0]);
        assertEquals("anon.txt", parts[1]);
        assertEquals("", parts[2]);
    }
}
