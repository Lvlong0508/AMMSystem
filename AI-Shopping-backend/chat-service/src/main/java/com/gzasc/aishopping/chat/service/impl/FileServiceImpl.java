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

    ExecutorService moveExecutor = Executors.newSingleThreadExecutor();

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
