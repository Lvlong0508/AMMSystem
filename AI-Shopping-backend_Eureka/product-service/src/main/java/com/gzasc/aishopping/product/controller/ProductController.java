package com.gzasc.aishopping.product.controller;

import com.gzasc.aishopping.product.model.Product;
import com.gzasc.aishopping.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class ProductController {

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

    @PostMapping("/create")
    public Map<String, String> createProduct(@RequestBody Product product) {
        if (product == null || product.getName() == null || product.getName().trim().isEmpty()) {
            return Map.of("message", "创建商品错误：商品名称为空");
        }
        if (product.getPrice() == null || product.getPrice() <= 0) {
            return Map.of("message", "创建商品错误：商品价格必须大于0");
        }
        if (product.getStock() < 0) {
            return Map.of("message", "创建商品错误：商品库存不能小于0");
        }
        try {
            int result = productService.createProduct(product);
            if (result > 0) {
                return Map.of("message", "创建商品成功", "id", product.getId());
            } else {
                return Map.of("message", "创建商品失败");
            }
        } catch (Exception e) {
            return Map.of("message", "创建商品错误：" + e.getMessage());
        }
    }

    @DeleteMapping("/{productId}")
    public Map<String, String> deleteProduct(@PathVariable("productId") String productId) {
        try {
            int result = productService.deleteProduct(productId);
            if (result > 0) {
                return Map.of("message", "删除商品成功");
            } else {
                return Map.of("message", "删除商品失败：商品不存在");
            }
        } catch (Exception e) {
            return Map.of("message", "删除商品错误：" + e.getMessage());
        }
    }

    @PutMapping("/{productId}")
    public Map<String, String> updateProduct(
            @PathVariable("productId") String productId,
            @RequestBody Product product) {
        if (product == null) {
            return Map.of("message", "更新商品错误：商品信息为空");
        }
        if (product.getPrice() != null && product.getPrice() <= 0) {
            return Map.of("message", "更新商品错误：商品价格必须大于0");
        }
        if (product.getStock() < 0) {
            return Map.of("message", "更新商品错误：商品库存不能小于0");
        }
        try {
            product.setId(productId);
            int result = productService.updateProduct(product);
            if (result > 0) {
                return Map.of("message", "更新商品成功");
            } else {
                return Map.of("message", "更新商品失败：商品不存在");
            }
        } catch (Exception e) {
            return Map.of("message", "更新商品错误：" + e.getMessage());
        }
    }
}
