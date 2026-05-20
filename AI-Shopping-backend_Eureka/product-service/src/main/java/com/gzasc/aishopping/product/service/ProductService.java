package com.gzasc.aishopping.product.service;

import com.gzasc.aishopping.product.model.Product;
import com.gzasc.aishopping.product.model.ProductImageInfo;

import java.util.List;

public interface ProductService {
    Product getProductById(String productId);
    List<Product> getProductsByName(String name);
    List<Product> getAllProducts(int page);
    int createProduct(Product product);
    int deleteProduct(String productId);
    int updateProduct(Product product);
    boolean deductStock(String productId, int quantity);
    boolean restoreStock(String productId, int quantity);
    List<Product> getProductsByIds(List<String> ids);

    int addImage(ProductImageInfo image);
    int removeImage(int imageId);
    ProductImageInfo getImageById(int imageId);
    List<ProductImageInfo> getImagesByIds(List<Integer> ids);

    boolean listProduct(String productId);
    boolean unlistProduct(String productId);
    boolean isProductSalable(String productId);
    List<String> getAllSalableProductIds();

    List<Product> getProductsBySaleStatus(boolean isSale);
    List<Product> getProductsByPriceRange(Double minPrice, Double maxPrice);
}