package com.gzasc.aishopping.product.service.impl;

import com.gzasc.aishopping.common.dto.shop.ShopInfoDTO;
import com.gzasc.aishopping.product.converter.ProductConverter;
import com.gzasc.aishopping.product.dto.ProductWithImageDetailDTO;
import com.gzasc.aishopping.product.dto.SellerProductAbstractDTO;
import com.gzasc.aishopping.product.exception.ProductException;
import com.gzasc.aishopping.product.mapper.ProductImageInfoMapper;
import com.gzasc.aishopping.product.mapper.ProductMapper;
import com.gzasc.aishopping.product.model.Product;
import com.gzasc.aishopping.product.model.ProductImageInfo;
import com.gzasc.aishopping.product.service.ImageStorageService;
import com.gzasc.aishopping.product.service.ProductShopInfoService;
import com.gzasc.aishopping.product.service.SellerProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SellerProductServiceImpl implements SellerProductService {

    private final ProductMapper productMapper;
    private final ProductImageInfoMapper productImageInfoMapper;
    private final ImageStorageService imageStorageService;
    private final ProductConverter productConverter;
    private final ProductShopInfoService productShopInfoService;

    @Override
    public ProductWithImageDetailDTO getSellerProductDetail(Long productId) {
        Product product = productMapper.selectProductById(productId);
        if (product == null) {
            return null;
        }
        String imageUrl = getImageUrl(product.getImageId());
        ShopInfoDTO shopInfo = productShopInfoService.getCachedShopInfo(product.getShopId());
        return productConverter.toDetailWithImageDTO(product, imageUrl, shopInfo);
    }

    @Override
    public List<SellerProductAbstractDTO> getSellerProductsByShopId(Long shopId) {
        List<Product> products = productMapper.selectByShopId(shopId);
        if (products.isEmpty()) {
            return List.of();
        }
        Map<Integer, String> imageUrlMap = buildImageUrlMap(products);
        Set<Long> shopIds = Set.of(shopId);
        Map<Long, ShopInfoDTO> shopInfoMap = productShopInfoService.batchGetShopInfo(shopIds);
        return productConverter.toSellerAbstractDTOList(products, imageUrlMap, shopInfoMap);
    }

    @Override
    public List<SellerProductAbstractDTO> getSellerProductsAbstract(List<Long> ids) {
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
        Map<Long, ShopInfoDTO> shopInfoMap = productShopInfoService.batchGetShopInfo(shopIds);
        return productConverter.toSellerAbstractDTOList(products, imageUrlMap, shopInfoMap);
    }

    @Override
    @Transactional
    public boolean listProduct(Long productId) {
        Product product = productMapper.selectProductById(productId);
        if (product == null) {
            throw new ProductException(404, "商品不存在 " + productId);
        }
        productMapper.updateSaleStatus(productId, true);
        return true;
    }

    @Override
    @Transactional
    public boolean unlistProduct(Long productId) {
        Product product = productMapper.selectProductById(productId);
        if (product == null) {
            throw new ProductException(404, "商品不存在 " + productId);
        }
        productMapper.updateSaleStatus(productId, false);
        return true;
    }

    @Override
    @Transactional
    public int deleteProduct(Long productId) {
        Product product = productMapper.selectProductById(productId);
        if (product == null) {
            throw new ProductException(404, "商品不存在 " + productId);
        }
        if (product.isSale()) {
            throw new ProductException(400, "商品在上架中，请先下架 " + productId);
        }

        // 删除磁盘上的图片文件夹
        imageStorageService.deleteProductFolder(productId);

        // 删除数据库中的图片记录
        if (product.getImageId() != null && product.getImageId() > 0) {
            productImageInfoMapper.deleteById(product.getImageId());
        }

        // 删除商品记录
        return productMapper.deleteProduct(productId);
    }

    private String getImageUrl(Integer imageId) {
        if (imageId == null || imageId <= 0) {
            return null;
        }
        ProductImageInfo imageInfo = productImageInfoMapper.selectURLById(imageId);
        return imageInfo != null ? imageInfo.getUrl() : null;
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
}