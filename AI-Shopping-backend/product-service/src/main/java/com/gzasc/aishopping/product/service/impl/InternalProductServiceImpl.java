package com.gzasc.aishopping.product.service.impl;

import com.gzasc.aishopping.common.dto.product.ProductDTO;
import com.gzasc.aishopping.common.dto.shop.ShopInfoDTO;
import com.gzasc.aishopping.product.converter.ProductConverter;
import com.gzasc.aishopping.product.dto.ProductWithImageAbstractDTO;
import com.gzasc.aishopping.product.dto.ProductWithImageDetailDTO;
import com.gzasc.aishopping.product.mapper.ProductImageInfoMapper;
import com.gzasc.aishopping.product.mapper.ProductMapper;
import com.gzasc.aishopping.product.model.Product;
import com.gzasc.aishopping.product.model.ProductImageInfo;
import com.gzasc.aishopping.product.service.InternalProductService;
import com.gzasc.aishopping.product.service.ProductShopInfoService;
import com.gzasc.aishopping.product.service.InventoryService;
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
public class InternalProductServiceImpl implements InternalProductService {

    private final ProductMapper productMapper;
    private final ProductImageInfoMapper productImageInfoMapper;
    private final ProductConverter productConverter;
    private final ProductShopInfoService productShopInfoService;
    private final InventoryService inventoryService;

    @Override
    public ProductWithImageDetailDTO getInternalProductDetail(Long productId) {
        Product product = productMapper.selectProductById(productId);
        if (product == null) {
            return null;
        }
        String imageUrl = getImageUrl(product.getImageId());
        ShopInfoDTO shopInfo = productShopInfoService.getCachedShopInfo(product.getShopId());
        return productConverter.toDetailWithImageDTO(product, imageUrl, shopInfo);
    }

    @Override
    public ProductDTO getBasicProductById(Long productId) {
        Product product = productMapper.selectProductById(productId);
        if (product == null) {
            return null;
        }
        String imageUrl = getImageUrl(product.getImageId());
        ProductDTO dto = new ProductDTO();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setPrice(product.getPrice());
        dto.setTags(product.getTags());
        dto.setDescription(product.getDescription());
        dto.setStock(product.getStock());
        dto.setShopId(product.getShopId());
        dto.setCreatedAt(product.getCreatedAt());
        dto.setUpdatedAt(product.getUpdatedAt());
        dto.setImageUrl(imageUrl);
        return dto;
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
    @Transactional
    public boolean restoreStock(Long productId, int quantity) {
        return inventoryService.restoreStock(productId, quantity);
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
