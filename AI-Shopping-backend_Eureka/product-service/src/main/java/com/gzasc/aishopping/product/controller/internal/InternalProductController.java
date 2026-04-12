package com.gzasc.aishopping.product.controller.internal;

import com.gzasc.aishopping.product.model.Product;
import com.gzasc.aishopping.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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
