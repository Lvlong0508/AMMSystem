package com.gzasc.aishopping.chat.service;

import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface FileService {

    /**
     * 存储文件到 upload 目录，返回实际存储的文件名列表
     * <p>返回空列表表示全部成功，否则返回失败的文件原始名列表</p>
     */
    List<String> save(List<MultipartFile> files, Long userId);

    /**
     * 从 upload 目录删除指定文件
     */
    void deleteFilesFromUpload(List<String> fileNames);

    /**
     * 从 finish 目录删除指定文件
     */
    void deleteFilesFromFinish(List<String> fileNames);

    /**
     * 异步将文件从 upload 移至 finish 目录
     * <p>内部由单线程执行器排队执行，不阻塞调用方</p>
     */
    void move(String fileName);

    /**
     * 获取 upload 目录的文件名列表
     */
    List<String> getFileNamesFromUpload();

    /**
     * 获取 finish 目录的文件名列表
     */
    List<String> getFileNamesFromFinish();
}
