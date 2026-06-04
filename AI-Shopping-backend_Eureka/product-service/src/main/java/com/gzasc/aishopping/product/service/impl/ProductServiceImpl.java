package com.gzasc.aishopping.product.service.impl;

import com.gzasc.aishopping.common.dto.shop.ShopInfoDTO;
import com.gzasc.aishopping.common.feign.shop.ShopFeignClient;
import com.gzasc.aishopping.common.response.ApiResponse;
import com.gzasc.aishopping.common.util.SnowflakeIdGenerator;
import com.gzasc.aishopping.product.converter.ProductConverter;
import com.gzasc.aishopping.product.dto.ProductWithImageAbstractDTO;
import com.gzasc.aishopping.product.dto.ProductWithImageDetailDTO;

import com.gzasc.aishopping.product.mapper.ProductImageInfoMapper;
import com.gzasc.aishopping.product.mapper.ProductMapper;
import com.gzasc.aishopping.product.mapper.SalableProductMapper;
import com.gzasc.aishopping.product.exception.ProductException;
import com.gzasc.aishopping.product.model.Product;
import com.gzasc.aishopping.product.model.ProductImageInfo;
import com.gzasc.aishopping.product.service.ImageStorageService;
import com.gzasc.aishopping.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Map;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private static final String DEFAULT_IMAGE_URL = "/image/default/product/0001.jpg";

    private final ProductMapper productMapper;
    private final ProductImageInfoMapper productImageInfoMapper;
    private final SalableProductMapper salableProductMapper;
    private final ProductConverter productConverter;
    private final ShopFeignClient shopFeignClient;
    private final ImageStorageService imageStorageService;

    @Value("${app.image.base-url}")
    private String imageBaseUrl;

    private final Cache<Long, ShopInfoDTO> shopInfoCache = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .maximumSize(1000)
            .build();

    private ShopInfoDTO getCachedShopInfo(Long shopId) {
        if (shopId == null) return null;
        try {
            return shopInfoCache.get(shopId, id -> {
                ApiResponse<ShopInfoDTO> response = shopFeignClient.getShopInfo(id);
                return response != null ? response.getData() : null;
            });
        } catch (Exception e) {
            log.warn("获取店铺信息失败, shopId={}", shopId, e);
            return null;
        }
    }

    private Map<Long, ShopInfoDTO> batchGetShopInfo(Set<Long> shopIds) {
        if (shopIds == null || shopIds.isEmpty()) return Map.of();
        Set<Long> uncached = shopIds.stream()
            .filter(id -> shopInfoCache.getIfPresent(id) == null)
            .collect(Collectors.toSet());
        if (!uncached.isEmpty()) {
            try {
                ApiResponse<Map<Long, ShopInfoDTO>> response = shopFeignClient.batchGetShopInfo(uncached);
                if (response != null && response.getData() != null) {
                    response.getData().forEach(shopInfoCache::put);
                }
            } catch (Exception e) {
                log.warn("批量获取店铺信息失败, shopIds={}", uncached, e);
            }
        }
        return shopIds.stream()
            .filter(id -> shopInfoCache.getIfPresent(id) != null)
            .collect(Collectors.toMap(id -> id, shopInfoCache::getIfPresent));
    }

    private String getImageUrl(Integer imageId) {
        if (imageId == null || imageId <= 0) {
            return DEFAULT_IMAGE_URL;
        }
        ProductImageInfo imageInfo = productImageInfoMapper.selectURLById(imageId);
        return imageInfo != null ? imageInfo.getUrl() : DEFAULT_IMAGE_URL;
    }

    private Map<Integer, String> buildImageUrlMap(List<Product> products) {
        List<Integer> imageIds = products.stream()
            .map(Product::getImageId)
            .filter(id -> id != null && id > 0)
            .distinct()
            .collect(Collectors.toList());

        if (imageIds.isEmpty()) {
            return Map.of();
        }

        return productImageInfoMapper.selectByIds(imageIds).stream()
            .collect(Collectors.toMap(ProductImageInfo::getId, ProductImageInfo::getUrl));
    }

    @Override
    public ProductWithImageDetailDTO getProductById(Long productId) {
        Product product = productMapper.selectProductById(productId);
        if (product == null) {
            return null;
        }
        String imageUrl = getImageUrl(product.getImageId());
        ShopInfoDTO shopInfo = getCachedShopInfo(product.getShopId());
        return productConverter.toDetailWithImageDTO(product, imageUrl, shopInfo);
    }

    @Override
    public List<ProductWithImageDetailDTO> getProductsByName(String name) {
        List<Product> products = productMapper.selectProductsByName(name)
            .stream()
            .filter(Product::isSale)
            .toList();
        if (products.isEmpty()) {
            return List.of();
        }
        Map<Integer, String> imageUrlMap = buildImageUrlMap(products);
        Set<Long> shopIds = products.stream()
            .map(Product::getShopId)
            .filter(id -> id != null)
            .collect(Collectors.toSet());
        Map<Long, ShopInfoDTO> shopInfoMap = batchGetShopInfo(shopIds);
        return productConverter.toDetailWithImageDTOList(products, imageUrlMap, shopInfoMap);
    }

    @Override
    public List<ProductWithImageAbstractDTO> getAbstractProductsForBuyer(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<Product> products = productMapper.selectAbstractProductsByIds(ids);
        if (products.isEmpty()) {
            return List.of();
        }
        Map<Integer, String> imageUrlMap = buildImageUrlMap(products);
        Set<Long> shopIds = products.stream()
            .map(Product::getShopId)
            .filter(id -> id != null)
            .collect(Collectors.toSet());
        Map<Long, ShopInfoDTO> shopInfoMap = batchGetShopInfo(shopIds);
        return productConverter.toAbstractWithImageDTOList(products, imageUrlMap, shopInfoMap);
    }

    @Override
    public List<ProductWithImageAbstractDTO> getSalableProductsAbstract(int page) {
        if (page < 0) {
            throw new ProductException(400, "页码不能为负数");
        }
        int pageSize = 20;
        List<Long> salableIds = salableProductMapper.selectAll(page * pageSize, pageSize);
        if (salableIds.isEmpty()) {
            return List.of();
        }
        List<Product> products = productMapper.selectAbstractProductsByIds(salableIds);
        if (products.isEmpty()) {
            return List.of();
        }
        Map<Integer, String> imageUrlMap = buildImageUrlMap(products);
        Set<Long> shopIds = products.stream()
            .map(Product::getShopId)
            .filter(id -> id != null)
            .collect(Collectors.toSet());
        Map<Long, ShopInfoDTO> shopInfoMap = batchGetShopInfo(shopIds);
        return productConverter.toAbstractWithImageDTOList(products, imageUrlMap, shopInfoMap);
    }

    @Override
    @Transactional
    public int deleteProduct(Long productId) {
        Product product = productMapper.selectProductById(productId);
        if (product == null) {
            throw new ProductException(404, "商品不存在: " + productId);
        }
        if (product.isSale()) {
            throw new ProductException(400, "商品在上架中，请先下架: " + productId);
        }
        if (product.getImageId() != null && product.getImageId() > 0) {
            productImageInfoMapper.deleteById(product.getImageId());
        }
        return productMapper.deleteProduct(productId);
    }

    @Override
    @Transactional
    public boolean restoreStock(Long productId, int quantity) {
        return productMapper.restoreStock(productId, quantity) > 0;
    }

    @Override
    public List<ProductWithImageAbstractDTO> getAbstractProductsForMerchant(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<Product> products = productMapper.selectAbstractProductsByIdsJustMerchant(ids);
        if (products.isEmpty()) {
            return List.of();
        }
        Map<Integer, String> imageUrlMap = buildImageUrlMap(products);
        Set<Long> shopIds = products.stream()
            .map(Product::getShopId)
            .filter(id -> id != null)
            .collect(Collectors.toSet());
        Map<Long, ShopInfoDTO> shopInfoMap = batchGetShopInfo(shopIds);
        return productConverter.toAbstractWithImageDTOList(products, imageUrlMap, shopInfoMap);
    }

    @Override
    @Transactional
    public boolean listProduct(Long productId) {
        Product product = productMapper.selectProductById(productId);
        if (product == null) {
            throw new ProductException(404, "商品不存在: " + productId);
        }
        productMapper.updateSaleStatus(productId, true);
        salableProductMapper.addSalable(productId);
        return true;
    }

    @Override
    @Transactional
    public boolean unlistProduct(Long productId) {
        Product product = productMapper.selectProductById(productId);
        if (product == null) {
            throw new ProductException(404, "商品不存在: " + productId);
        }
        productMapper.updateSaleStatus(productId, false);
        salableProductMapper.removeSalable(productId);
        return true;
    }

    @Override
    public List<ProductWithImageAbstractDTO> getProductsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, int page) {
        if (page < 0) {
            throw new ProductException(400, "页码不能为负数");
        }
        List<Product> products = productMapper.selectByPriceRangeWithPage(minPrice, maxPrice, page * 20)
            .stream()
            .filter(Product::isSale)
            .toList();
        if (products.isEmpty()) {
            return List.of();
        }
        Map<Integer, String> imageUrlMap = buildImageUrlMap(products);
        Set<Long> shopIds = products.stream()
            .map(Product::getShopId)
            .filter(id -> id != null)
            .collect(Collectors.toSet());
        Map<Long, ShopInfoDTO> shopInfoMap = batchGetShopInfo(shopIds);
        return productConverter.toAbstractWithImageDTOList(products, imageUrlMap, shopInfoMap);
    }

    @Override
    @Transactional
    public int createProductWithImage(Product product, MultipartFile imageFile) {
        long productId = SnowflakeIdGenerator.nextId();
        product.setId(productId);

        String relativePath = imageStorageService.saveImage(productId, imageFile);
        String fullUrl = imageBaseUrl + relativePath;

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
            throw new ProductException(404, "商品不存在: " + product.getId());
        }

        if (image != null && !image.isEmpty()) {
            String relativePath = imageStorageService.saveImage(product.getId(), image);
            String fullUrl = imageBaseUrl + relativePath;

            Integer oldImageId = existingProduct.getImageId();
            String oldImageUrl = (oldImageId != null && oldImageId > 0) ? getImageUrl(oldImageId) : null;

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

            if (oldImageUrl != null && !DEFAULT_IMAGE_URL.equals(oldImageUrl)) {
                imageStorageService.deleteImage(oldImageUrl);
            }
        } else {
            product.setImageId(existingProduct.getImageId());
        }

        return productMapper.updateProduct(product);
    }
}