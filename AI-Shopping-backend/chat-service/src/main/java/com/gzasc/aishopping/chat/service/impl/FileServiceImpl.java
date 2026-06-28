package com.gzasc.aishopping.chat.service.impl;

import com.gzasc.aishopping.chat.dao.FileStorageDao;
import com.gzasc.aishopping.chat.exception.FileException;
import com.gzasc.aishopping.chat.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final FileStorageDao fileStorageDao;

    @Override
    public List<String> save(List<MultipartFile> files) {
        try {
            // 重命名
        }catch (FileException e){
            log.error("文件重命名发生错误："+e.getMessage());
        }
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

    private void rag(List<String> filePaths) {
        log.info("执行rag:"+filePaths.toString());
    }
}
