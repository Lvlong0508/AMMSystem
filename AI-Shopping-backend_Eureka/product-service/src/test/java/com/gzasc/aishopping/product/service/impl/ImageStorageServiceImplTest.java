package com.gzasc.aishopping.product.service.impl;

import com.gzasc.aishopping.product.exception.ProductException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.unit.DataSize;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImageStorageServiceImplTest {

    private ImageStorageServiceImpl imageStorageService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        imageStorageService = new ImageStorageServiceImpl(tempDir.toString(), DataSize.ofMegabytes(5));
        ReflectionTestUtils.setField(imageStorageService, "imageBaseUrl", "");
    }

    @Test
    @DisplayName("saveImage - 正常保存图片成功")
    void testSaveImageSuccess() {
        MockMultipartFile file = new MockMultipartFile("image", "test.jpg", "image/jpeg", "fake-content".getBytes());

        String url = imageStorageService.saveImage(1L, file);

        assertNotNull(url);
        assertTrue(url.startsWith("/image/goods/main/1/"));
        assertTrue(url.endsWith(".jpg"));
    }

    @Test
    @DisplayName("saveImage - 文件无扩展名")
    void testSaveImageNoExtension() {
        MockMultipartFile file = new MockMultipartFile("image", "testfile", "image/jpeg", "content".getBytes());

        String url = imageStorageService.saveImage(1L, file);

        assertFalse(url.endsWith("."));
    }

    @Test
    @DisplayName("saveImage - 文件名为空")
    void testSaveImageNullFilename() {
        MockMultipartFile file = new MockMultipartFile("image", (String) null, "image/jpeg", "content".getBytes());

        String url = imageStorageService.saveImage(1L, file);

        assertNotNull(url);
    }

    @Test
    @DisplayName("saveImage - 图片目录创建失败抛出异常")
    void testSaveImageCreateDirFailed() {
        try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
            filesMock.when(() -> Files.createDirectories(any(Path.class)))
                    .thenThrow(new IOException("磁盘空间不足"));

            MockMultipartFile file = new MockMultipartFile("image", "test.jpg", "image/jpeg", "content".getBytes());

            ProductException exception = assertThrows(ProductException.class,
                    () -> imageStorageService.saveImage(1L, file));
            assertEquals(500, exception.getCode());
            assertTrue(exception.getMessage().contains("创建图片目录失败"));
        }
    }

    @Test
    @DisplayName("saveImage - 文件保存失败抛出异常")
    void testSaveImageTransferFailed() {
        MockMultipartFile file = new MockMultipartFile("image", "test.jpg", "image/jpeg", "content".getBytes()) {
            @Override
            public void transferTo(java.io.File dest) throws IOException {
                throw new IOException("磁盘空间不足");
            }
        };

        ProductException exception = assertThrows(ProductException.class,
                () -> imageStorageService.saveImage(1L, file));
        assertEquals(500, exception.getCode());
        assertTrue(exception.getMessage().contains("保存图片失败"));
    }
}
