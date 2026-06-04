package com.gzasc.aishopping.product.service.impl;

import com.gzasc.aishopping.product.exception.ProductException;
import com.gzasc.aishopping.product.service.ImageStorageService;
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

    @Value("${app.image.base-url}")
    private String imageBaseUrl;

    public ImageStorageServiceImpl(@Value("${app.image.storage-path}") String storagePath) {
        this.storagePath = Paths.get(storagePath).toAbsolutePath().normalize();
    }

    @Override
    public String saveImage(Long productId, MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String ext = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        Path productDir = storagePath.resolve(String.valueOf(productId));
        try {
            Files.createDirectories(productDir);
        } catch (IOException e) {
            throw new ProductException(500, "创建图片目录失败");
        }

        String fileName;
        Path targetPath;
        do {
            String randomHex = HexFormat.of().formatHex(generateRandomBytes(4));
            fileName = productId + "_" + randomHex + ext;
            targetPath = productDir.resolve(fileName);
        } while (Files.exists(targetPath));

        try {
            file.transferTo(targetPath.toFile());
        } catch (IOException e) {
            log.error("保存图片失败", e);
            throw new ProductException(500, "保存图片失败");
        }

        return "/image/goods/main/" + productId + "/" + fileName;
    }

    @Async
    @Override
    public void deleteImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return;
        }
        String relativePath = imageUrl.replace(imageBaseUrl, "");
        if (relativePath.startsWith("/")) {
            relativePath = relativePath.substring(1);
        }
        Path filePath = storagePath.resolve(relativePath).normalize();
        try {
            boolean deleted = Files.deleteIfExists(filePath);
            if (deleted) {
                log.info("删除旧图片成功: {}", filePath);
            } else {
                log.warn("旧图片文件不存在: {}", filePath);
            }
        } catch (IOException e) {
            log.error("删除旧图片失败: {}", filePath, e);
        }
    }

    private byte[] generateRandomBytes(int count) {
        byte[] bytes = new byte[count];
        RANDOM.nextBytes(bytes);
        return bytes;
    }
}
