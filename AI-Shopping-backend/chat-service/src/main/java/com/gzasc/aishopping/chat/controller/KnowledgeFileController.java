package com.gzasc.aishopping.chat.controller;

import com.gzasc.aishopping.chat.exception.FileException;
import com.gzasc.aishopping.chat.service.FileService;
import com.gzasc.aishopping.chat.util.FileTypeUtil;
import com.gzasc.aishopping.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/file")
@RequiredArgsConstructor
public class KnowledgeFileController {

    private final FileService fileService;

    // 上传文件
    @PostMapping("/upload")
    public ApiResponse<String> storageFile(@RequestParam("files") List<MultipartFile> files) {
        // 1. 前置空值校验，避免 NPE 和无效调用
        if (files == null || files.isEmpty()) {
            return ApiResponse.error("请至少选择一个文件");
        }

        // 2. 校验文件格式逻辑
        boolean flag = FileTypeUtil.isSupported(files);
        if (!flag) {
            return ApiResponse.error("存在文件格式不支持，仅支持：" + FileTypeUtil.SUPPORTED_EXTENSIONS_DISPLAY);
        }

        List<String> failedFiles = fileService.save(files);
        if (failedFiles.isEmpty()) {
            return ApiResponse.success("上传成功");
        }
        return ApiResponse.success("上传完成，部分文件失败：" + String.join(", ", failedFiles));
    }
}
