package com.gzasc.aishopping.product.service;

import com.gzasc.aishopping.product.model.Product;

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
}
