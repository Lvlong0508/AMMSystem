package com.gzasc.aishopping.product.converter;

import com.gzasc.aishopping.product.dto.ProductAbstractDTO;
import com.gzasc.aishopping.product.dto.ProductDetailDTO;
import com.gzasc.aishopping.product.dto.ProductImageDTO;
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

    public List<ProductDetailDTO> toDetailDTOList(List<Product> products) {
        if (products == null) return List.of();
        return products.stream().map(this::toDetailDTO).collect(Collectors.toList());
    }

    public List<ProductImageDTO> toImageDTOList(List<ProductImageInfo> infos) {
        if (infos == null) return List.of();
        return infos.stream().map(this::toImageDTO).collect(Collectors.toList());
    }

    public com.gzasc.aishopping.product.dto.ProductWithImageAbstractDTO toAbstractWithImageDTO(Product product, String imageUrl) {
        if (product == null) return null;
        return new com.gzasc.aishopping.product.dto.ProductWithImageAbstractDTO(
            product.getId(),
            product.getName(),
            product.getPrice(),
            product.getTags(),
            product.getImageId(),
            imageUrl
        );
    }

    public com.gzasc.aishopping.product.dto.ProductWithImageDetailDTO toDetailWithImageDTO(Product product, String imageUrl) {
        if (product == null) return null;
        return new com.gzasc.aishopping.product.dto.ProductWithImageDetailDTO(
            product.getId(),
            product.getName(),
            product.getPrice(),
            product.getTags(),
            product.getDescription(),
            product.getStock(),
            product.isSale(),
            product.getImageId(),
            imageUrl,
            product.getCreatedAt(),
            product.getUpdatedAt()
        );
    }

    public List<com.gzasc.aishopping.product.dto.ProductWithImageAbstractDTO> toAbstractWithImageDTOList(List<Product> products, Map<Integer, String> imageUrlMap) {
        if (products == null) return List.of();
        return products.stream()
            .map(p -> {
                String url = null;
                if (p.getImageId() != null && imageUrlMap != null) {
                    url = imageUrlMap.get(p.getImageId());
                }
                return toAbstractWithImageDTO(p, url);
            })
            .collect(Collectors.toList());
    }

    public List<com.gzasc.aishopping.product.dto.ProductWithImageDetailDTO> toDetailWithImageDTOList(List<Product> products, Map<Integer, String> imageUrlMap) {
        if (products == null) return List.of();
        return products.stream()
            .map(p -> {
                String url = null;
                if (p.getImageId() != null && imageUrlMap != null) {
                    url = imageUrlMap.get(p.getImageId());
                }
                return toDetailWithImageDTO(p, url);
            })
            .collect(Collectors.toList());
    }
}
