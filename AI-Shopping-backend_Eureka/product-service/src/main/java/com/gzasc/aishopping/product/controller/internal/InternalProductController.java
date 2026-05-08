package com.gzasc.aishopping.product.controller.internal;

import com.gzasc.aishopping.product.model.Product;
import com.gzasc.aishopping.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/internal/product")
@RequiredArgsConstructor
public class InternalProductController {

    private final ProductService productService;

    @GetMapping("/{productId}")
    public Product getProductById(@PathVariable("productId") String productId) {
        return productService.getProductById(productId);
    }

    @GetMapping("/batch")
    public List<Product> getProductsByIds(@RequestParam("ids") String ids) {
        List<String> idList = Arrays.asList(ids.split(","));
        return productService.getProductsByIds(idList);
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

    @PostMapping("/deduct-stock")
    public Map<String, Object> deductStock(@RequestBody StockDeductRequest request) {
        boolean success = productService.deductStock(request.getProductId(), request.getQuantity());
        return Map.of("success", success, "message", success ? "扣减成功" : "扣减失败：库存不足");
    }

    @PostMapping("/restore-stock")
    public Map<String, Object> restoreStock(@RequestBody StockDeductRequest request) {
        boolean success = productService.restoreStock(request.getProductId(), request.getQuantity());
        return Map.of("success", success, "message", success ? "恢复成功" : "恢复失败");
    }

    @lombok.Data
    public static class StockDeductRequest {
        private String productId;
        private int quantity;
    }
}
