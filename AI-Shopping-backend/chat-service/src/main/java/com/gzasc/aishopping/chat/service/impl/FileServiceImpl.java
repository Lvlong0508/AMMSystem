package com.gzasc.aishopping.chat.service.impl;

import com.gzasc.aishopping.chat.dao.FileStorageDao;
import com.gzasc.aishopping.chat.exception.FileException;
import com.gzasc.aishopping.chat.service.FileService;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final FileStorageDao fileStorageDao;

    // 单线程执行器，保证文件移动按顺序逐个执行，避免并发操作 index.txt
    ExecutorService moveExecutor = Executors.newSingleThreadExecutor();

    /**
     * 异步将文件从 upload 移至 finish，内部异常由 CompletableFuture 消化，不抛给调用方
     */
    @Override
    public void move(String fileName) {
        CompletableFuture.runAsync(() -> {
            try {
                fileStorageDao.moveFile(fileName);
                log.info("文件移至 finish：{}", fileName);
            } catch (Exception e) {
                log.error("文件移动失败：{}", fileName, e);
            }
        }, moveExecutor);
    }

    /** 优雅关闭异步移动线程池 */
    @PreDestroy
    public void shutdown() {
        moveExecutor.shutdown();
        try {
            if (!moveExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                moveExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            moveExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 保存文件到 upload 目录，遍历过程中任意文件失败不中断整体流程
     * @return 失败的文件原始名列表，空表示全部成功
     */
    @Override
    public List<String> save(List<MultipartFile> files) {
        List<String> failedFiles = new ArrayList<>();
        for (MultipartFile file : files) {
            try {
                fileStorageDao.storeFile(file);
            } catch (FileException e) {
                log.error("文件存储失败：{}", e.getMessage());
                failedFiles.add(file.getOriginalFilename());
            }
        }
        return failedFiles;
    }

    /** 批量删除 upload 目录中的文件，null/空列表时直接返回 */
    @Override
    public void deleteFilesFromUpload(List<String> fileNames) {
        if (fileNames == null || fileNames.isEmpty()) {
            return;
        }
        try {
            fileStorageDao.deleteFileFromUpload(fileNames);
            log.info("补偿删除完成，已删除文件：{}", fileNames);
        } catch (FileException e) {
            log.error("补偿删除失败：" + e.getMessage());
        }
    }
}
