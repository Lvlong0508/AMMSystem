package com.gzasc.aishopping.common.feign.product;

import com.gzasc.aishopping.common.dto.product.ProductDTO;
import com.gzasc.aishopping.common.dto.product.StockDeductRequest;
import com.gzasc.aishopping.common.dto.product.StockReserveRequest;
import com.gzasc.aishopping.common.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(name = "product-service")
public interface ProductFeignClient {

    @GetMapping("/api/user/product/all")
    ApiResponse<Map<String, Object>> getAllProducts(@RequestParam("page") int page);

    @GetMapping("/api/user/product/{productId}")
    ApiResponse<Map<String, Object>> getProductByIdExternal(@PathVariable("productId") Long productId);

    @GetMapping("/internal/product/{productId}")
    ApiResponse<ProductDTO> getProductById(@PathVariable("productId") Long productId);

    @PostMapping("/internal/product/deduct-stock")
    ApiResponse<Void> deductStock(@RequestBody StockDeductRequest request);

    @PostMapping("/internal/product/restore-stock")
    ApiResponse<Void> restoreStock(@RequestBody StockDeductRequest request);

    @PostMapping("/internal/product/create")
    ApiResponse<Map<String, Object>> createProduct(@RequestBody ProductDTO request);

    @PutMapping("/internal/product/{productId}")
    ApiResponse<Map<String, Object>> updateProduct(@PathVariable("productId") Long productId, @RequestBody ProductDTO request);

    @DeleteMapping("/internal/product/{productId}")
    ApiResponse<Void> deleteProduct(@PathVariable("productId") Long productId);

    @PostMapping("/internal/product/reserve-stock")
    ApiResponse<Void> reserveStock(@RequestBody StockReserveRequest request);

    @PostMapping("/internal/product/confirm-reservation")
    ApiResponse<Void> confirmReservation(@RequestParam("orderId") String orderId);

    @PostMapping("/internal/product/release-reservation")
    ApiResponse<Void> releaseReservation(@RequestParam("orderId") String orderId);

    @GetMapping("/internal/product/by-shop/{shopId}")
    ApiResponse<List<Map<String, Object>>> getProductsByShopId(@PathVariable("shopId") Long shopId,
                                                               @RequestParam("page") int page,
                                                               @RequestParam("size") int size);
}
