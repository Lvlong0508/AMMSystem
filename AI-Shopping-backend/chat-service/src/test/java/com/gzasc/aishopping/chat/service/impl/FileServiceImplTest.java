package com.gzasc.aishopping.chat.service.impl;

import com.gzasc.aishopping.chat.dao.FileStorageDao;
import com.gzasc.aishopping.chat.exception.FileException;
import com.gzasc.aishopping.chat.service.FileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileServiceImplTest {

    @Mock
    private FileStorageDao fileStorageDao;

    private FileServiceImpl fileService;

    @BeforeEach
    void setUp() {
        fileService = new FileServiceImpl(fileStorageDao);
    }

    private MultipartFile file(String name) {
        return new MockMultipartFile("file", name, "text/plain", "content".getBytes());
    }

    @Test
    @DisplayName("保存全部成功 - 返回空列表")
    void save_allSuccess_returnsEmptyList() {
        when(fileStorageDao.storeFile(any(), any())).thenReturn("a.txt");

        List<String> result = fileService.save(List.of(file("a.txt"), file("b.txt")), 1L);

        assertTrue(result.isEmpty());
        verify(fileStorageDao, times(2)).storeFile(any(), any());
    }

    @Test
    @DisplayName("部分文件失败 - 返回失败文件名列表")
    void save_oneFails_returnsFailedFileNames() {
        when(fileStorageDao.storeFile(any(), any()))
                .thenReturn("a.txt")
                .thenThrow(new FileException("存储失败"));

        List<String> result = fileService.save(List.of(file("a.txt"), file("b.txt")), 1L);

        assertEquals(List.of("b.txt"), result);
        verify(fileStorageDao, times(2)).storeFile(any(), any());
    }

    @Test
    @DisplayName("全部失败 - 返回全部文件名")
    void save_allFail_returnsAllNames() {
        when(fileStorageDao.storeFile(any(), any()))
                .thenThrow(new FileException("存储失败"))
                .thenThrow(new FileException("存储失败"));

        List<String> result = fileService.save(List.of(file("a.txt"), file("b.txt")), 1L);

        assertEquals(List.of("a.txt", "b.txt"), result);
        verify(fileStorageDao, times(2)).storeFile(any(), any());
    }

    @Test
    @DisplayName("空文件列表 - 返回空列表")
    void save_emptyList_returnsEmptyList() {
        List<String> result = fileService.save(List.of(), 1L);

        assertTrue(result.isEmpty());
        verify(fileStorageDao, never()).storeFile(any(), any());
    }

    @Test
    @DisplayName("删除上传文件 - 委托给 DAO")
    void deleteFilesFromUpload_delegatesToDao() {
        fileService.deleteFilesFromUpload(List.of("a.txt", "b.txt"));

        verify(fileStorageDao).deleteFileFromUpload(List.of("a.txt", "b.txt"));
    }

    @Test
    @DisplayName("删除上传文件 - 空列表不调用 DAO")
    void deleteFilesFromUpload_emptyList_doesNothing() {
        fileService.deleteFilesFromUpload(List.of());

        verify(fileStorageDao, never()).deleteFileFromUpload(any());
    }

    @Test
    @DisplayName("删除上传文件 - null 不调用 DAO")
    void deleteFilesFromUpload_null_doesNothing() {
        fileService.deleteFilesFromUpload(null);

        verify(fileStorageDao, never()).deleteFileFromUpload(any());
    }

    @Test
    @DisplayName("move 委托给 DAO 的 moveFile")
    void move_delegatesToDao() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        doAnswer(invocation -> {
            latch.countDown();
            return null;
        }).when(fileStorageDao).moveFile("test.txt");

        fileService.move("test.txt");

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        verify(fileStorageDao).moveFile("test.txt");
    }

    @Test
    @DisplayName("move 异常不向外抛")
    void move_exception_doesNotThrow() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        doAnswer(invocation -> {
            latch.countDown();
            throw new FileException("move failed");
        }).when(fileStorageDao).moveFile("bad.txt");

        fileService.move("bad.txt");

        assertTrue(latch.await(2, TimeUnit.SECONDS));
    }
}
