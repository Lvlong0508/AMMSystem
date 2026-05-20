package com.gzasc.aishopping.product.service.impl;

import com.gzasc.aishopping.common.util.SnowflakeIdGenerator;
import com.gzasc.aishopping.product.mapper.ProductImageInfoMapper;
import com.gzasc.aishopping.product.mapper.ProductMapper;
import com.gzasc.aishopping.product.mapper.SalableProductMapper;
import com.gzasc.aishopping.product.model.Product;
import com.gzasc.aishopping.product.model.ProductImageInfo;
import com.gzasc.aishopping.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductMapper productMapper;
    private final ProductImageInfoMapper productImageInfoMapper;
    private final SalableProductMapper salableProductMapper;

    @Override
    public Product getProductById(String productId) {
        System.out.println(new Date() + ": run getProductById, id=" + productId);
        return productMapper.selectProductById(productId);
    }

    @Override
    public List<Product> getProductsByName(String name) {
        System.out.println(new Date() + ": run getProductsByName, name=" + name);
        return productMapper.selectProductsByName(name);
    }

    @Override
    public List<Product> getAllProducts(int page) {
        System.out.println(new Date() + ": run getAllProducts, page=" + page);
        int offset = page * 20;
        return productMapper.selectProductsByPage(offset);
    }

    @Override
    public int createProduct(Product product) {
        System.out.println(new Date() + ": run createProduct");
        product.setId(SnowflakeIdGenerator.nextIdStr());
        return productMapper.insertProduct(product);
    }

    @Override
    public int deleteProduct(String productId) {
        System.out.println(new Date() + ": run deleteProduct, id=" + productId);
        Product product = productMapper.selectProductById(productId);
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }
        if (product.isSale()) {
            throw new RuntimeException("商品在上架中，请先下架");
        }
        return productMapper.deleteProduct(productId);
    }

    @Override
    public int updateProduct(Product product) {
        System.out.println(new Date() + ": run updateProduct, id=" + product.getId());
        return productMapper.updateProduct(product);
    }

    @Override
    public boolean deductStock(String productId, int quantity) {
        System.out.println(new Date() + ": run deductStock, id=" + productId + ", quantity=" + quantity);
        return productMapper.deductStock(productId, quantity) > 0;
    }

    @Override
    public boolean restoreStock(String productId, int quantity) {
        System.out.println(new Date() + ": run restoreStock, id=" + productId + ", quantity=" + quantity);
        return productMapper.restoreStock(productId, quantity) > 0;
    }

    @Override
    public List<Product> getProductsByIds(List<String> ids) {
        System.out.println(new Date() + ": run getProductsByIds, ids=" + ids);
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return productMapper.selectProductsByIds(ids);
    }

    @Override
    public int addImage(ProductImageInfo image) {
        System.out.println(new Date() + ": run addImage, url=" + image.getUrl());
        return productImageInfoMapper.insert(image);
    }

    @Override
    public int removeImage(int imageId) {
        System.out.println(new Date() + ": run removeImage, id=" + imageId);
        return productImageInfoMapper.deleteById(imageId);
    }

    @Override
    public ProductImageInfo getImageById(int imageId) {
        return productImageInfoMapper.selectById(imageId);
    }

    @Override
    public List<ProductImageInfo> getImagesByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return productImageInfoMapper.selectByIds(ids);
    }

    @Override
    public boolean listProduct(String productId) {
        System.out.println(new Date() + ": run listProduct, id=" + productId);
        Product product = productMapper.selectProductById(productId);
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }
        product.setSale(true);
        productMapper.updateProduct(product);
        salableProductMapper.addSalable(productId);
        return true;
    }

    @Override
    public boolean unlistProduct(String productId) {
        System.out.println(new Date() + ": run unlistProduct, id=" + productId);
        Product product = productMapper.selectProductById(productId);
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }
        product.setSale(false);
        productMapper.updateProduct(product);
        salableProductMapper.removeSalable(productId);
        return true;
    }

    @Override
    public boolean isProductSalable(String productId) {
        return salableProductMapper.isSalable(productId);
    }

    @Override
    public List<String> getAllSalableProductIds() {
        return salableProductMapper.selectAll();
    }

    @Override
    public List<Product> getProductsBySaleStatus(boolean isSale) {
        return productMapper.selectBySaleStatus(isSale);
    }

    @Override
    public List<Product> getProductsByPriceRange(Double minPrice, Double maxPrice) {
        return productMapper.selectByPriceRange(minPrice, maxPrice);
    }
}