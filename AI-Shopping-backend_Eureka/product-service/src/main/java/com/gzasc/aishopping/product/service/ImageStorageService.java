package com.gzasc.aishopping.product.service;

import org.springframework.web.multipart.MultipartFile;

public interface ImageStorageService {

    /**
     * 校验图片文件，失败时抛出 ProductException
     */
    void validateImage(MultipartFile imageFile);

    /**
     * 保存图片并返回完整的可访问 URL（含 baseUrl）
     */
    String saveImage(Long productId, MultipartFile file);

    /**
     * 删除图片，支持完整 URL 或相对路径
     */
    void deleteImage(String imageUrl);
}
