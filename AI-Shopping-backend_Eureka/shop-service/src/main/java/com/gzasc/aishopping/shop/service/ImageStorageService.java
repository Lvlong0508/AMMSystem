package com.gzasc.aishopping.shop.service;

import org.springframework.web.multipart.MultipartFile;

public interface ImageStorageService {
    String saveImage(Long shopId, MultipartFile file);

    void deleteImage(String imageUrl);
}
