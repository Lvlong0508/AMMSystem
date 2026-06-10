package com.gzasc.aishopping.product.converter;

import com.gzasc.aishopping.common.dto.product.ProductCardDTO;
import com.gzasc.aishopping.common.dto.shop.ShopInfoDTO;
import com.gzasc.aishopping.product.dto.ProductWithImageAbstractDTO;
import com.gzasc.aishopping.product.dto.SellerProductAbstractDTO;
import com.gzasc.aishopping.product.dto.ProductWithImageDetailDTO;
import com.gzasc.aishopping.product.model.Product;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ProductConverter {

    private String resolveImageUrl(Integer imageId, Map<Integer, String> imageUrlMap) {
        return imageUrlMap != null && imageId != null
            ? imageUrlMap.get(imageId)
            : null;
    }

    private ShopInfoDTO resolveShopInfo(Long shopId, Map<Long, ShopInfoDTO> shopInfoMap) {
        return shopInfoMap != null && shopId != null
            ? shopInfoMap.get(shopId)
            : null;
    }

    // ==================== 抽象/详情 DTO 转换（带图片和店铺信息） ====================

    public ProductWithImageAbstractDTO toAbstractWithImageDTO(Product product, String imageUrl, ShopInfoDTO shop) {
        if (product == null) return null;
        return new ProductWithImageAbstractDTO(
            product.getId(),
            product.getName(),
            product.getPrice(),
            product.getTags(),
            product.getImageId(),
            imageUrl,
            shop
        );
    }

    public ProductWithImageDetailDTO toDetailWithImageDTO(Product product, String imageUrl, ShopInfoDTO shop) {
        if (product == null) return null;
        return new ProductWithImageDetailDTO(
            product.getId(),
            product.getName(),
            product.getPrice(),
            product.getTags(),
            product.getDescription(),
            product.getStock(),
            product.isSale(),
            product.getImageId(),
            imageUrl,
            shop,
            product.getCreatedAt(),
            product.getUpdatedAt()
        );
    }

    public List<ProductWithImageAbstractDTO> toAbstractWithImageDTOList(List<Product> products, Map<Integer, String> imageUrlMap, Map<Long, ShopInfoDTO> shopInfoMap) {
        if (products == null) return List.of();
        return products.stream()
            .map(p -> toAbstractWithImageDTO(p, resolveImageUrl(p.getImageId(), imageUrlMap), resolveShopInfo(p.getShopId(), shopInfoMap)))
            .collect(Collectors.toList());
    }

    public List<ProductWithImageDetailDTO> toDetailWithImageDTOList(List<Product> products, Map<Integer, String> imageUrlMap, Map<Long, ShopInfoDTO> shopInfoMap) {
        if (products == null) return List.of();
        return products.stream()
            .map(p -> toDetailWithImageDTO(p, resolveImageUrl(p.getImageId(), imageUrlMap), resolveShopInfo(p.getShopId(), shopInfoMap)))
            .collect(Collectors.toList());
    }

    // ==================== 商家端抽象DTO 转换（含上下架状态） ====================

    public SellerProductAbstractDTO toSellerAbstractDTO(Product product, String imageUrl, ShopInfoDTO shop) {
        if (product == null) return null;
        return new SellerProductAbstractDTO(
            product.getId(),
            product.getName(),
            product.getPrice(),
            product.getTags(),
            product.getImageId(),
            imageUrl,
            product.isSale(),
            shop
        );
    }

    public List<SellerProductAbstractDTO> toSellerAbstractDTOList(List<Product> products, Map<Integer, String> imageUrlMap, Map<Long, ShopInfoDTO> shopInfoMap) {
        if (products == null) return List.of();
        return products.stream()
            .map(p -> toSellerAbstractDTO(p, resolveImageUrl(p.getImageId(), imageUrlMap), resolveShopInfo(p.getShopId(), shopInfoMap)))
            .collect(Collectors.toList());
    }

    // ==================== 用户端卡片DTO 转换 ====================

    public ProductCardDTO toCardDTO(Product product, String imageUrl) {
        if (product == null) return null;
        return new ProductCardDTO(
            product.getId(), product.getName(), imageUrl,
            product.getStock(), product.getPrice()
        );
    }

    public List<ProductCardDTO> toCardDTOList(List<Product> products, Map<Integer, String> imageUrlMap) {
        if (products == null) return List.of();
        return products.stream()
            .map(p -> toCardDTO(p, resolveImageUrl(p.getImageId(), imageUrlMap)))
            .collect(Collectors.toList());
    }
}
