package com.gzasc.aishopping.product.converter;

import com.gzasc.aishopping.common.dto.shop.ShopInfoDTO;
import com.gzasc.aishopping.product.dto.ProductAbstractDTO;
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

    public ProductImageDTO toImageDTO(ProductImageInfo info) {
        if (info == null) return null;
        return new ProductImageDTO(info.getId(), info.getUrl());
    }

    public List<ProductAbstractDTO> toAbstractDTOList(List<Product> products) {
        if (products == null) return List.of();
        return products.stream().map(this::toAbstractDTO).collect(Collectors.toList());
    }
    public List<ProductImageDTO> toImageDTOList(List<ProductImageInfo> infos) {
        if (infos == null) return List.of();
        return infos.stream().map(this::toImageDTO).collect(Collectors.toList());
    }

    public ProductWithImageAbstractDTO toAbstractWithImageDTO(Product product, String imageUrl) {
        return toAbstractWithImageDTO(product, imageUrl, null);
    }

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

    public ProductWithImageDetailDTO toDetailWithImageDTO(Product product, String imageUrl) {
        return toDetailWithImageDTO(product, imageUrl, null);
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

    public List<ProductWithImageAbstractDTO> toAbstractWithImageDTOList(List<Product> products, Map<Integer, String> imageUrlMap) {
        return toAbstractWithImageDTOList(products, imageUrlMap, null);
    }

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

    public List<ProductWithImageDetailDTO> toDetailWithImageDTOList(List<Product> products, Map<Integer, String> imageUrlMap) {
        return toDetailWithImageDTOList(products, imageUrlMap, null);
    }

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
