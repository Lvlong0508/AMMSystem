package com.gzasc.aishopping.product.service.impl;

import com.gzasc.aishopping.common.util.SnowflakeIdGenerator;
import com.gzasc.aishopping.product.cache.ProductCache;
import com.gzasc.aishopping.product.converter.ProductConverter;
import com.gzasc.aishopping.product.dto.ProductAbstractDTO;
import com.gzasc.aishopping.product.dto.ProductDetailDTO;
import com.gzasc.aishopping.product.dto.ProductImageDTO;
import com.gzasc.aishopping.product.dto.ProductWithImageAbstractDTO;
import com.gzasc.aishopping.product.dto.ProductWithImageDetailDTO;

import com.gzasc.aishopping.product.mapper.ProductImageInfoMapper;
import com.gzasc.aishopping.product.mapper.ProductMapper;
import com.gzasc.aishopping.product.mapper.SalableProductMapper;
import com.gzasc.aishopping.product.exception.ProductException;
import com.gzasc.aishopping.product.model.Product;
import com.gzasc.aishopping.product.model.ProductImageInfo;
import com.gzasc.aishopping.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private static final String DEFAULT_IMAGE_URL = "/image/default/product/0001.jpg";

    private final ProductMapper productMapper;
    private final ProductImageInfoMapper productImageInfoMapper;
    private final SalableProductMapper salableProductMapper;
    private final ProductConverter productConverter;
    private final ProductCache productCache;

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
    public ProductWithImageDetailDTO getProductById(String productId) {
        Product product = productMapper.selectProductById(productId);
        if (product == null) {
            return null;
        }
        String imageUrl = getImageUrl(product.getImageId());
        return productConverter.toDetailWithImageDTO(product, imageUrl);
    }

    @Override
    public List<ProductWithImageDetailDTO> getProductsByName(String name) {
        List<Product> products = productMapper.selectProductsByName(name);
        if (products.isEmpty()) {
            return List.of();
        }
        Map<Integer, String> imageUrlMap = buildImageUrlMap(products);
        return productConverter.toDetailWithImageDTOList(products, imageUrlMap);
    }

    @Override
    public List<ProductWithImageAbstractDTO> getAbstractProductsForBuyer(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<Product> products = productMapper.selectAbstractProductsByIds(ids);
        if (products.isEmpty()) {
            return List.of();
        }
        Map<Integer, String> imageUrlMap = buildImageUrlMap(products);
        return productConverter.toAbstractWithImageDTOList(products, imageUrlMap);
    }

    @Override
    public List<ProductWithImageAbstractDTO> getSalableProductsAbstract(int page) {
        List<String> salableIds = salableProductMapper.selectAll(page * 20);
        if (salableIds.isEmpty()) {
            return List.of();
        }
        List<Product> products = productMapper.selectAbstractProductsByIds(salableIds);
        if (products.isEmpty()) {
            return List.of();
        }
        Map<Integer, String> imageUrlMap = buildImageUrlMap(products);
        return productConverter.toAbstractWithImageDTOList(products, imageUrlMap);
    }

    @Override
    @Transactional
    public int createProduct(Product product) {
        product.setId(SnowflakeIdGenerator.nextId());
        return productMapper.insertProduct(product);
    }

    @Override
    @Transactional
    public int deleteProduct(String productId) {
        Product product = productMapper.selectProductById(productId);
        if (product == null) {
            throw new ProductException(404, "商品不存在: " + productId);
        }
        if (product.isSale()) {
            throw new ProductException(400, "商品在上架中，请先下架: " + productId);
        }
        return productMapper.deleteProduct(productId);
    }

    @Override
    @Transactional
    public int updateProduct(Product product) {
        return productMapper.updateProduct(product);
    }

    @Override
    @Transactional
    public boolean deductStock(String productId, int quantity) {
        return productMapper.deductStock(productId, quantity) > 0;
    }

    @Override
    @Transactional
    public boolean restoreStock(String productId, int quantity) {
        return productMapper.restoreStock(productId, quantity) > 0;
    }

    @Override
    public List<ProductWithImageAbstractDTO> getAbstractProductsForMerchant(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<Product> products = productMapper.selectAbstractProductsByIdsJustMerchant(ids);
        if (products.isEmpty()) {
            return List.of();
        }
        Map<Integer, String> imageUrlMap = buildImageUrlMap(products);
        return productConverter.toAbstractWithImageDTOList(products, imageUrlMap);
    }

    @Override
    public int addImage(ProductImageInfo image) {
        return productImageInfoMapper.insert(image);
    }

    @Override
    public int removeImage(int imageId) {
        return productImageInfoMapper.deleteById(imageId);
    }

    @Override
    public ProductImageInfo getImageById(int imageId) {
        return productImageInfoMapper.selectURLById(imageId);
    }

    @Override
    public List<ProductImageInfo> getImagesByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return productImageInfoMapper.selectByIds(ids);
    }

    @Override
    @Transactional
    public boolean listProduct(String productId) {
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
    public boolean unlistProduct(String productId) {
        Product product = productMapper.selectProductById(productId);
        if (product == null) {
            throw new ProductException(404, "商品不存在: " + productId);
        }
        productMapper.updateSaleStatus(productId, false);
        salableProductMapper.removeSalable(productId);
        return true;
    }

    @Override
    public boolean isProductSalable(String productId) {
        return salableProductMapper.isSalable(productId);
    }

    @Override
    public List<ProductWithImageAbstractDTO> getProductsByPriceRange(Double minPrice, Double maxPrice) {
        List<Product> products = productMapper.selectByPriceRange(minPrice, maxPrice);
        if (products.isEmpty()) {
            return List.of();
        }
        Map<Integer, String> imageUrlMap = buildImageUrlMap(products);
        return productConverter.toAbstractWithImageDTOList(products, imageUrlMap);
    }

    @Override
    public List<ProductWithImageAbstractDTO> getProductsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, int page) {
        List<Product> products = productMapper.selectByPriceRangeWithPage(minPrice, maxPrice, page * 20);
        if (products.isEmpty()) {
            return List.of();
        }
        Map<Integer, String> imageUrlMap = buildImageUrlMap(products);
        return productConverter.toAbstractWithImageDTOList(products, imageUrlMap);
    }

    @Override
    public List<ProductAbstractDTO> getAbstractProductDTOs(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<Product> products = productMapper.selectAbstractProductsByIds(ids);
        return productConverter.toAbstractDTOList(products);
    }

    @Override
    public ProductDetailDTO getProductDetailDTO(String productId) {
        Product product = productMapper.selectProductById(productId);
        if (product == null) {
            throw new ProductException(404, "商品不存在: " + productId);
        }
        // TODO: 启用缓存
        // ProductDetailDTO cached = (ProductDetailDTO) productCache.get("detail:" + productId);
        // if (cached != null) return cached;
        // ProductDetailDTO dto = productConverter.toDetailDTO(product);
        // productCache.put("detail:" + productId, dto);
        // return dto;
        return productConverter.toDetailDTO(product);
    }

    @Override
    public List<ProductAbstractDTO> getMerchantAbstractProductDTOs(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<Product> products = productMapper.selectAbstractProductsByIdsJustMerchant(ids);
        return productConverter.toAbstractDTOList(products);
    }

    @Override
    public ProductImageDTO getImageDTO(int imageId) {
        ProductImageInfo info = productImageInfoMapper.selectURLById(imageId);
        return productConverter.toImageDTO(info);
    }

    @Override
    public List<ProductImageDTO> getImageDTOs(List<Integer> ids) {
        List<ProductImageInfo> infos = getImagesByIds(ids);
        return productConverter.toImageDTOList(infos);
    }

    @Override
    @Transactional
    public int createProductWithImage(Product product, String imageUrl) {
        if (imageUrl != null && !imageUrl.isBlank()) {
            ProductImageInfo image = new ProductImageInfo();
            image.setUrl(imageUrl);
            productImageInfoMapper.insert(image);
            product.setImageId(image.getId());
        } else {
            product.setImageId(0);
        }
        product.setId(SnowflakeIdGenerator.nextId());
        return productMapper.insertProduct(product);
    }

    @Override
    @Transactional
    public int updateProductWithImage(Product product, String imageUrl) {
        Product existingProduct = productMapper.selectProductById(String.valueOf(product.getId()));
        if (existingProduct == null) {
            throw new ProductException(404, "商品不存在: " + String.valueOf(product.getId()));
        }

        if (imageUrl != null && !imageUrl.isBlank()) {
            if (existingProduct.getImageId() != null && existingProduct.getImageId() > 0) {
                ProductImageInfo imageInfo = new ProductImageInfo();
                imageInfo.setId(existingProduct.getImageId());
                imageInfo.setUrl(imageUrl);
                productImageInfoMapper.updateUrl(imageInfo);
            }
        }

        product.setImageId(existingProduct.getImageId());
        return productMapper.updateProduct(product);
    }
}