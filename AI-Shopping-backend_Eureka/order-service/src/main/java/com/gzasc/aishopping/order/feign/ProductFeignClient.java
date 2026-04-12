package com.gzasc.aishopping.order.feign;

import com.gzasc.aishopping.order.dto.ProductDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "product-service")
public interface ProductFeignClient {

    @GetMapping("/internal/product/{productId}")
    ProductDTO getProductById(@PathVariable("productId") String productId);

    @PostMapping("/internal/product/deduct-stock")
    Map<String, Object> deductStock(@RequestBody StockDeductRequest request);

    @PostMapping("/internal/product/restore-stock")
    Map<String, Object> restoreStock(@RequestBody StockDeductRequest request);

    class StockDeductRequest {
        private String productId;
        private int quantity;

        public StockDeductRequest() {}

        public StockDeductRequest(String productId, int quantity) {
            this.productId = productId;
            this.quantity = quantity;
        }

        public String getProductId() { return productId; }
        public void setProductId(String productId) { this.productId = productId; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
    }
}
