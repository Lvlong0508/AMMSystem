package com.gzasc.aishopping.service.impl;

import com.gzasc.aishopping.mapper.ProductMapper;
import com.gzasc.aishopping.model.Product;
import com.gzasc.aishopping.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class ProductServiceImpl implements ProductService {
    @Autowired
    private ProductMapper productMapper;

    @Override
    public Product getProductById(String productId) {
        System.out.println(new Date()+"：run getProductById");
        return productMapper.selectProductById(productId);
    }

    @Override
    public List<Product> getProductsByName(String name) {
        System.out.println(new Date()+"：run getProductsByName");
        return productMapper.selectProductsByName(name);
    }

    @Override
    public List<Product> getAllProducts(int page) {
        System.out.println(new Date()+"：run getAllProducts, page=" + page);
        int offset = page * 20;
        return productMapper.selectProductsByPage(offset);
    }

    @Override
    public int createProduct(Product product) {
        System.out.println(new Date()+"：run createProduct");
        // 生成唯一ID
        product.setId(java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        return productMapper.insertProduct(product);
    }

    @Override
    public int deleteProduct(String productId) {
        System.out.println(new Date()+"：run deleteProduct");
        return productMapper.deleteProduct(productId);
    }

    @Override
    public int updateProduct(Product product) {
        System.out.println(new Date()+"：run updateProduct");
        return productMapper.updateProduct(product);
    }
}
