package com.gzasc.aishopping.chat.dao;

import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface FileStorageDao {

    // 文件存储本地upload，返回实际存储的文件名
    String storeFile(MultipartFile file, Long userId);

    // 文件移动到本地finish
    void moveFile(String fileName);

    // 删除upload目录里指定文件名的文件
    void deleteFileFromUpload(List<String> fileNameList);

    // 删除finish目录里指定文件名的文件
    void deleteFileFromFinish(List<String> fileNameList);

    // 获取upload目录的文件名列表
    List<String> getFileNameFromUpload();

    // 获取finish目录的文件名列表
    List<String> getFileNameFromFinish();
}
