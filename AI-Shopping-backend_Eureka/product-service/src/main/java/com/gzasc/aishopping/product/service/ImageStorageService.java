package com.gzasc.aishopping.product.service;

import org.springframework.web.multipart.MultipartFile;

public interface ImageStorageService {
    String saveImage(Long productId, MultipartFile file);

    void deleteImage(String imageUrl);
}
