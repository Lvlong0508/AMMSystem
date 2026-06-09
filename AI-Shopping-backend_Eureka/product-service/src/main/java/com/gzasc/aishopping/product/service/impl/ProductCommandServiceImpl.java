package com.gzasc.aishopping.product.service.impl;

import com.gzasc.aishopping.common.util.SafeIdGenerator;
import com.gzasc.aishopping.product.exception.ProductException;
import com.gzasc.aishopping.product.mapper.ProductImageInfoMapper;
import com.gzasc.aishopping.product.mapper.ProductMapper;
import com.gzasc.aishopping.product.model.Product;
import com.gzasc.aishopping.product.model.ProductImageInfo;
import com.gzasc.aishopping.product.service.ImageStorageService;
import com.gzasc.aishopping.product.service.ProductCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductCommandServiceImpl implements ProductCommandService {

    private final ProductMapper productMapper;
    private final ProductImageInfoMapper productImageInfoMapper;
    private final ImageStorageService imageStorageService;


    @Override
    @Transactional
    public int createProductWithImage(Product product, MultipartFile imageFile) {
        long productId = SafeIdGenerator.nextId();
        product.setId(productId);

        String fullUrl = imageStorageService.saveImage(productId, imageFile);

        ProductImageInfo image = new ProductImageInfo();
        image.setUrl(fullUrl);
        productImageInfoMapper.insert(image);
        product.setImageId(image.getId());

        return productMapper.insertProduct(product);
    }

    @Override
    @Transactional
    public int updateProductWithImage(Product product, MultipartFile image) {
        Product existingProduct = productMapper.selectProductById(product.getId());
        if (existingProduct == null) {
            throw new ProductException(404, "商品不存在 " + product.getId());
        }

        if (image != null && !image.isEmpty()) {
            String fullUrl = imageStorageService.saveImage(product.getId(), image);

            Integer oldImageId = existingProduct.getImageId();

            String oldImageUrl = (oldImageId != null && oldImageId > 0)
                ? getImageUrl(oldImageId) : null;

            if (oldImageId != null && oldImageId > 0) {
                ProductImageInfo imageInfo = new ProductImageInfo();
                imageInfo.setId(oldImageId);
                imageInfo.setUrl(fullUrl);
                productImageInfoMapper.updateUrl(imageInfo);
            } else {
                ProductImageInfo newImage = new ProductImageInfo();
                newImage.setUrl(fullUrl);
                productImageInfoMapper.insert(newImage);
                product.setImageId(newImage.getId());
            }

            if (oldImageUrl != null) {
                imageStorageService.deleteImage(oldImageUrl);
            }
        } else {
            product.setImageId(existingProduct.getImageId());
        }

        return productMapper.updateProduct(product);
    }

    private String getImageUrl(Integer imageId) {
        if (imageId == null || imageId <= 0) {
            return null;
        }
        ProductImageInfo imageInfo = productImageInfoMapper.selectURLById(imageId);
        return imageInfo != null ? imageInfo.getUrl() : null;
    }
}
