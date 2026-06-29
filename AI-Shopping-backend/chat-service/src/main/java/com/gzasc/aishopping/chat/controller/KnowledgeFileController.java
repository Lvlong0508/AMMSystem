package com.gzasc.aishopping.chat.controller;

import com.gzasc.aishopping.chat.service.EmbeddingService;
import com.gzasc.aishopping.chat.service.FileService;
import com.gzasc.aishopping.chat.util.FileTypeUtil;
import com.gzasc.aishopping.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/file")
@RequiredArgsConstructor
public class KnowledgeFileController {

    private final FileService fileService;
    private final EmbeddingService embeddingService;

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

    // 删除 upload 目录文件
    @PostMapping("/delete/upload")
    public ApiResponse<String> deleteFilesFromUpload(@RequestBody List<String> fileNames) {
        if (fileNames == null || fileNames.isEmpty()) {
            return ApiResponse.error("请指定要删除的文件");
        }
        fileService.deleteFilesFromUpload(fileNames);
        return ApiResponse.success("删除成功");
    }

    // 删除 finish 目录文件
    @PostMapping("/delete/finish")
    public ApiResponse<String> deleteFilesFromFinish(@RequestBody List<String> fileNames) {
        if (fileNames == null || fileNames.isEmpty()) {
            return ApiResponse.error("请指定要删除的文件");
        }
        fileService.deleteFilesFromFinish(fileNames);
        return ApiResponse.success("删除成功");
    }

    // 获取 upload 目录文件列表
    @PostMapping("/list/upload")
    public ApiResponse<List<String>> listUploadFiles() {
        return ApiResponse.success(fileService.getFileNamesFromUpload());
    }

    // 获取 finish 目录文件列表
    @PostMapping("/list/finish")
    public ApiResponse<List<String>> listFinishFiles() {
        return ApiResponse.success(fileService.getFileNamesFromFinish());
    }

    // 导入文件到向量库
    @PostMapping("/ingest")
    public ApiResponse<List<Map<String, String>>> ingestFiles(@RequestBody List<String> fileNames) {
        if (fileNames == null || fileNames.isEmpty()) {
            return ApiResponse.error("请指定要导入的文件");
        }
        List<Map<String, String>> failed = embeddingService.ingest(fileNames);
        if (failed.isEmpty()) {
            return ApiResponse.success(failed);
        }
        return ApiResponse.success("导入完成，部分文件失败", failed);
    }
}
