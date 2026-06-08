package com.gzasc.aishopping.shop.service.impl;

import com.gzasc.aishopping.shop.exception.ShopException;
import com.gzasc.aishopping.shop.service.ImageStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.HexFormat;

@Slf4j
@Service
public class ImageStorageServiceImpl implements ImageStorageService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final Path storagePath;

    @Value("${app.image.base-url:http://localhost:8087}")
    private String imageBaseUrl;

    public ImageStorageServiceImpl(@Value("${app.image.storage-path:./AI-Shopping-backend_Eureka/static/image/shop/logo}") String storagePath) {
        this.storagePath = Paths.get(storagePath).toAbsolutePath().normalize();
    }

    @Override
    public String saveImage(Long shopId, MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String ext = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        Path shopDir = storagePath.resolve(String.valueOf(shopId));
        try {
            Files.createDirectories(shopDir);
        } catch (IOException e) {
            throw new ShopException(500, "创建图片目录失败");
        }

        String fileName;
        Path targetPath;
        do {
            String randomHex = HexFormat.of().formatHex(generateRandomBytes(4));
            fileName = shopId + "_" + randomHex + ext;
            targetPath = shopDir.resolve(fileName);
        } while (Files.exists(targetPath));

        try {
            file.transferTo(targetPath.toFile());
        } catch (IOException e) {
            log.error("保存店铺 Logo 失败", e);
            throw new ShopException(500, "保存店铺 Logo 失败");
        }

        return imageBaseUrl + "/image/shop/logo/" + shopId + "/" + fileName;
    }

    @Async
    @Override
    public void deleteImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return;
        }
        String relativePath = imageUrl.replace(imageBaseUrl, "");
        String logoPrefix = "/image/shop/logo/";
        if (relativePath.startsWith(logoPrefix)) {
            relativePath = relativePath.substring(logoPrefix.length());
        } else if (relativePath.startsWith("/")) {
            relativePath = relativePath.substring(1);
        }
        Path filePath = storagePath.resolve(relativePath).normalize();
        try {
            boolean deleted = Files.deleteIfExists(filePath);
            if (deleted) {
                log.info("删除旧店铺 Logo 成功: {}", filePath);
            } else {
                log.warn("旧店铺 Logo 文件不存在: {}", filePath);
            }
        } catch (IOException e) {
            log.error("删除旧店铺 Logo 失败: {}", filePath, e);
        }
    }

    private byte[] generateRandomBytes(int count) {
        byte[] bytes = new byte[count];
        RANDOM.nextBytes(bytes);
        return bytes;
    }
}
