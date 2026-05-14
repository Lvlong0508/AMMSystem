package com.gzasc.aishopping.common.feign.product;

import com.gzasc.aishopping.common.dto.product.ProductDTO;
import com.gzasc.aishopping.common.dto.product.StockDeductRequest;
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

/**
 * 商品服务 Feign 客户端
 * 供其他服务调用商品相关接口
 */
@FeignClient(name = "product-service")
public interface ProductFeignClient {

    /**
     * 获取所有商品（用户端接口）
     */
    @GetMapping("/api/user/product/all")
    Map<String, Object> getAllProducts(@RequestParam("page") int page);

    /**
     * 根据ID查询商品（用户端接口）
     */
    @GetMapping("/api/user/product/{productId}")
    Map<String, Object> getProductByIdExternal(@PathVariable("productId") String productId);

    /**
     * 根据ID查询商品（内部服务调用）
     */
    @GetMapping("/internal/product/{productId}")
    Map<String, Object> getProductById(@PathVariable("productId") String productId);

    /**
     * 扣减库存
     */
    @PostMapping("/internal/product/deduct-stock")
    Map<String, Object> deductStock(@RequestBody StockDeductRequest request);

    /**
     * 恢复库存
     */
    @PostMapping("/internal/product/restore-stock")
    Map<String, Object> restoreStock(@RequestBody StockDeductRequest request);

    /**
     * 创建商品（内部服务调用）
     */
    @PostMapping("/internal/product/create")
    Map<String, Object> createProduct(@RequestBody ProductDTO request);

    /**
     * 更新商品（内部服务调用）
     */
    @PutMapping("/internal/product/{productId}")
    Map<String, Object> updateProduct(@PathVariable("productId") String productId, @RequestBody ProductDTO request);

    /**
     * 删除商品（内部服务调用）
     */
    @DeleteMapping("/internal/product/{productId}")
    Map<String, Object> deleteProduct(@PathVariable("productId") String productId);
}
