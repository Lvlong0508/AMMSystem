package com.gzasc.aishopping.product.service.impl;

import com.gzasc.aishopping.common.dto.product.ProductCardDTO;
import com.gzasc.aishopping.common.dto.shop.ShopInfoDTO;
import com.gzasc.aishopping.product.converter.ProductConverter;
import com.gzasc.aishopping.product.dto.ProductWithImageAbstractDTO;
import com.gzasc.aishopping.product.dto.ProductWithImageDetailDTO;
import com.gzasc.aishopping.product.exception.ProductException;
import com.gzasc.aishopping.product.mapper.ProductImageInfoMapper;
import com.gzasc.aishopping.product.mapper.ProductMapper;
import com.gzasc.aishopping.product.model.Product;
import com.gzasc.aishopping.product.model.ProductImageInfo;
import com.gzasc.aishopping.product.service.BuyerProductService;
import com.gzasc.aishopping.product.service.ProductShopInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BuyerProductServiceImpl implements BuyerProductService {

    private final ProductMapper productMapper;
    private final ProductImageInfoMapper productImageInfoMapper;
    private final ProductConverter productConverter;
    private final ProductShopInfoService productShopInfoService;

    @Override
    public ProductWithImageDetailDTO getBuyerVisibleProductDetail(Long productId) {
        Product product = productMapper.selectProductById(productId);
        if (product == null || !product.isSale()) {
            return null;
        }
        String imageUrl = getImageUrl(product.getImageId());
        ShopInfoDTO shopInfo = productShopInfoService.getCachedShopInfo(product.getShopId());
        return productConverter.toDetailWithImageDTO(product, imageUrl, shopInfo);
    }

    @Override
    public List<ProductWithImageDetailDTO> getProductsByName(String name) {
        List<Product> products = productMapper.selectProductsByName(name);
        if (products.isEmpty()) {
            return List.of();
        }
        Map<Integer, String> imageUrlMap = buildImageUrlMap(products);
        Set<Long> shopIds = products.stream()
            .map(Product::getShopId)
            .filter(id -> id != null)
            .collect(Collectors.toSet());
        Map<Long, ShopInfoDTO> shopInfoMap = productShopInfoService.batchGetShopInfo(shopIds);
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
        Map<Long, ShopInfoDTO> shopInfoMap = productShopInfoService.batchGetShopInfo(shopIds);
        return productConverter.toAbstractWithImageDTOList(products, imageUrlMap, shopInfoMap);
    }

    @Override
    public List<ProductCardDTO> getSalableProductCards(int page) {
        if (page < 0) {
            throw new ProductException(400, "页码不能为负数");
        }
        int pageSize = 20;
        List<Product> products = productMapper.selectCardProductsPage(page * pageSize, pageSize);
        if (products.isEmpty()) return List.of();
        Map<Integer, String> imageUrlMap = buildImageUrlMap(products);
        return productConverter.toCardDTOList(products, imageUrlMap);
    }

    @Override
    public List<ProductCardDTO> getSalableProductCardsByShopId(Long shopId) {
        List<Product> products = productMapper.selectSalableByShopId(shopId);
        if (products.isEmpty()) return List.of();
        Map<Integer, String> imageUrlMap = buildImageUrlMap(products);
        return productConverter.toCardDTOList(products, imageUrlMap);
    }

    @Override
    public List<ProductCardDTO> getProductCardsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, int page) {
        if (page < 0) {
            throw new ProductException(400, "页码不能为负数");
        }
        List<Product> products = productMapper.selectByPriceRangeWithPage(minPrice, maxPrice, page * 20);
        if (products.isEmpty()) return List.of();
        Map<Integer, String> imageUrlMap = buildImageUrlMap(products);
        return productConverter.toCardDTOList(products, imageUrlMap);
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
