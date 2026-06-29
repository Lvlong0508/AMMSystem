package com.gzasc.aishopping.chat.service;

import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface FileService {
    List<String> save(List<MultipartFile> files);
    void deleteFilesFromUpload(List<String> fileNames);
    void move(String fileName);
}
