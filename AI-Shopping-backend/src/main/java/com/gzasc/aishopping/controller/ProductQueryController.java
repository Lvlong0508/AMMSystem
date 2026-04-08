package com.gzasc.aishopping.controller;

import com.gzasc.aishopping.model.Product;
import com.gzasc.aishopping.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class ProductQueryController {
    private final ProductService productService;

    @GetMapping("/all")
    public Map<String, Object> getAllProducts(@RequestParam(name = "page", defaultValue = "0") int page) {
        try {
            List<Product> products = productService.getAllProducts(page);
            return Map.of("message", "查询成功", "data", products, "page", page, "size", products.size());
        } catch (Exception e) {
            return Map.of("message", "查询错误：" + e.getMessage());
        }
    }

    @GetMapping("/{productId}")
    public Map<String, Object> getProductById(@PathVariable("productId") String productId) {
        try {
            Product product = productService.getProductById(productId);
            if (product != null) {
                return Map.of("message", "查询成功", "data", product);
            } else {
                return Map.of("message", "查询失败：商品不存在");
            }
        } catch (Exception e) {
            return Map.of("message", "查询错误：" + e.getMessage());
        }
    }

    @GetMapping("/search")
    public Map<String, Object> getProductsByName(@RequestParam("name") String name) {
        try {
            List<Product> products = productService.getProductsByName(name);
            return Map.of("message", "查询成功", "data", products, "total", products.size());
        } catch (Exception e) {
            return Map.of("message", "查询错误：" + e.getMessage());
        }
    }
}
