package com.gzasc.aishopping.product.converter;

import com.gzasc.aishopping.common.dto.shop.ShopInfoDTO;
import com.gzasc.aishopping.product.dto.ProductAbstractDTO;
import com.gzasc.aishopping.product.dto.ProductDTO;
import com.gzasc.aishopping.product.dto.ProductDetailDTO;
import com.gzasc.aishopping.product.dto.ProductImageDTO;
import com.gzasc.aishopping.product.dto.ProductWithImageAbstractDTO;
import com.gzasc.aishopping.product.dto.ProductWithImageDetailDTO;
import com.gzasc.aishopping.product.model.Product;
import com.gzasc.aishopping.product.model.ProductImageInfo;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ProductConverter {

    // ==================== 统一 ProductDTO 转换 ====================

    public ProductDTO toDTO(Product product) {
        return toDTO(product, null, null);
    }

    public ProductDTO toDTO(Product product, String imageUrl) {
        return toDTO(product, imageUrl, null);
    }

    public ProductDTO toDTO(Product product, String imageUrl, ShopInfoDTO shop) {
        if (product == null) return null;
        return new ProductDTO(
            product.getId(),
            product.getName(),
            product.getPrice(),
            product.getTags(),
            imageUrl,
            product.getImageId(),
            product.getDescription(),
            product.getStock(),
            product.isSale(),
            shop,
            product.getCreatedAt(),
            product.getUpdatedAt()
        );
    }

    public List<ProductDTO> toDTOList(List<Product> products, Map<Integer, String> imageUrlMap) {
        return toDTOList(products, imageUrlMap, null);
    }

    public List<ProductDTO> toDTOList(List<Product> products, Map<Integer, String> imageUrlMap, Map<Long, ShopInfoDTO> shopInfoMap) {
        if (products == null) return List.of();
        return products.stream()
            .map(p -> {
                String url = null;
                if (p.getImageId() != null && imageUrlMap != null) {
                    url = imageUrlMap.get(p.getImageId());
                }
                ShopInfoDTO shop = null;
                if (shopInfoMap != null && p.getShopId() != null) {
                    shop = shopInfoMap.get(p.getShopId());
                }
                return toDTO(p, url, shop);
            })
            .collect(Collectors.toList());
    }

    // ==================== 废弃的旧 DTO 转换（兼容） ====================

    @Deprecated
    public ProductAbstractDTO toAbstractDTO(Product product) {
        if (product == null) return null;
        return new ProductAbstractDTO(
            product.getId(),
            product.getName(),
            product.getPrice(),
            product.getTags(),
            product.getImageId()
        );
    }

    @Deprecated
    public ProductDetailDTO toDetailDTO(Product product) {
        if (product == null) return null;
        return new ProductDetailDTO(
            product.getId(),
            product.getName(),
            product.getPrice(),
            product.getTags(),
            product.getDescription(),
            product.getStock(),
            product.isSale(),
            product.getImageId(),
            product.getCreatedAt(),
            product.getUpdatedAt()
        );
    }

    @Deprecated
    public ProductImageDTO toImageDTO(ProductImageInfo info) {
        if (info == null) return null;
        return new ProductImageDTO(info.getId(), info.getUrl());
    }

    @Deprecated
    public List<ProductAbstractDTO> toAbstractDTOList(List<Product> products) {
        if (products == null) return List.of();
        return products.stream().map(this::toAbstractDTO).collect(Collectors.toList());
    }
    @Deprecated
    public List<ProductImageDTO> toImageDTOList(List<ProductImageInfo> infos) {
        if (infos == null) return List.of();
        return infos.stream().map(this::toImageDTO).collect(Collectors.toList());
    }

    @Deprecated
    public ProductWithImageAbstractDTO toAbstractWithImageDTO(Product product, String imageUrl) {
        return toAbstractWithImageDTO(product, imageUrl, null);
    }

    @Deprecated
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

    @Deprecated
    public ProductWithImageDetailDTO toDetailWithImageDTO(Product product, String imageUrl) {
        return toDetailWithImageDTO(product, imageUrl, null);
    }

    @Deprecated
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

    @Deprecated
    public List<ProductWithImageAbstractDTO> toAbstractWithImageDTOList(List<Product> products, Map<Integer, String> imageUrlMap) {
        return toAbstractWithImageDTOList(products, imageUrlMap, null);
    }

    @Deprecated
    public List<ProductWithImageAbstractDTO> toAbstractWithImageDTOList(List<Product> products, Map<Integer, String> imageUrlMap, Map<Long, ShopInfoDTO> shopInfoMap) {
        if (products == null) return List.of();
        return products.stream()
            .map(p -> {
                String url = null;
                if (p.getImageId() != null && imageUrlMap != null) {
                    url = imageUrlMap.get(p.getImageId());
                }
                ShopInfoDTO shop = null;
                if (shopInfoMap != null && p.getShopId() != null) {
                    shop = shopInfoMap.get(p.getShopId());
                }
                return toAbstractWithImageDTO(p, url, shop);
            })
            .collect(Collectors.toList());
    }

    @Deprecated
    public List<ProductWithImageDetailDTO> toDetailWithImageDTOList(List<Product> products, Map<Integer, String> imageUrlMap) {
        return toDetailWithImageDTOList(products, imageUrlMap, null);
    }

    @Deprecated
    public List<ProductWithImageDetailDTO> toDetailWithImageDTOList(List<Product> products, Map<Integer, String> imageUrlMap, Map<Long, ShopInfoDTO> shopInfoMap) {
        if (products == null) return List.of();
        return products.stream()
            .map(p -> {
                String url = null;
                if (p.getImageId() != null && imageUrlMap != null) {
                    url = imageUrlMap.get(p.getImageId());
                }
                ShopInfoDTO shop = null;
                if (shopInfoMap != null && p.getShopId() != null) {
                    shop = shopInfoMap.get(p.getShopId());
                }
                return toDetailWithImageDTO(p, url, shop);
            })
            .collect(Collectors.toList());
    }
}
