package com.gzasc.aishopping.service;

import com.gzasc.aishopping.model.Product;
import org.springframework.stereotype.Service;

import java.util.List;

public interface ProductService {
    Product getProductById(String productId);
    List<Product> getProductsByName(String name);
    List<Product> getAllProducts(int page);
    int createProduct(Product product);
    int deleteProduct(String productId);
    int updateProduct(Product product);
}