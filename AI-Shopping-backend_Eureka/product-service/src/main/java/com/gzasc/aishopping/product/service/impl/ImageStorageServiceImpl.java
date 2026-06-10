package com.gzasc.aishopping.product.service.impl;

import com.gzasc.aishopping.product.exception.ProductException;
import com.gzasc.aishopping.product.service.ImageStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.unit.DataSize;
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
    private final DataSize maxFileSize;

    @Value("${app.image.base-url:http://localhost:8081}")
    private String imageBaseUrl;

    public ImageStorageServiceImpl(@Value("${app.image.storage-path}") String storagePath,
                                   @Value("${app.image.max-file-size:5MB}") DataSize maxFileSize) {
        this.storagePath = Paths.get(storagePath).toAbsolutePath().normalize();
        this.maxFileSize = maxFileSize;
    }

    @Override
    public void validateImage(MultipartFile imageFile) {
        if (imageFile == null || imageFile.isEmpty()) {
            throw new ProductException(400, "图片不能为空");
        }
        String contentType = imageFile.getContentType();
        if (!"image/jpeg".equals(contentType) && !"image/png".equals(contentType)) {
            throw new ProductException(400, "仅支持 JPG 或 PNG 格式");
        }
        if (imageFile.getSize() > maxFileSize.toBytes()) {
            throw new ProductException(400, "图片大小不能超过 " + formatMaxFileSize(maxFileSize));
        }
    }

    @Override
    public String saveImage(Long productId, MultipartFile file) {
        validateImage(file);

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

        String relativePath = "/image/goods/main/" + productId + "/" + fileName;
        return imageBaseUrl + relativePath;
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

    private String formatMaxFileSize(DataSize size) {
        long bytes = size.toBytes();
        if (bytes % (1024 * 1024) == 0) {
            return bytes / (1024 * 1024) + "MB";
        }
        if (bytes % 1024 == 0) {
            return bytes / 1024 + "KB";
        }
        return bytes + "B";
    }
}
