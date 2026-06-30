package com.gzasc.aishopping.chat.controller;

import com.gzasc.aishopping.chat.service.EmbeddingService;
import com.gzasc.aishopping.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/embedding")
@RequiredArgsConstructor
public class EmbeddingController {

    private final EmbeddingService embeddingService;

    @PostMapping("/collections")
    public ApiResponse<Map<String, Object>> getCollectionStats() {
        return ApiResponse.success(embeddingService.getCollectionStats());
    }

    @PostMapping("/documents")
    public ApiResponse<List<Map<String, Object>>> getDocuments() {
        return ApiResponse.success(embeddingService.getDocuments());
    }

    @PostMapping("/search")
    public ApiResponse<List<Map<String, Object>>> search(@RequestBody Map<String, Object> params) {
        String query = (String) params.get("query");
        if (query == null || query.isBlank()) {
            return ApiResponse.error("搜索关键词不能为空");
        }
        int topK = params.get("topK") instanceof Number n ? n.intValue() : 5;
        return ApiResponse.success(embeddingService.search(query, topK));
    }

    @PostMapping("/delete")
    public ApiResponse<Map<String, Object>> delete(@RequestBody List<String> fileNames) {
        if (fileNames == null || fileNames.isEmpty()) {
            return ApiResponse.error("请指定要删除的文件");
        }
        int total = 0;
        for (String fileName : fileNames) {
            total += embeddingService.deleteFromVector(fileName);
        }
        return ApiResponse.success(Map.of("deleted", total));
    }
}
