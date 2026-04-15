package com.gzasc.aishopping.chat.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(name = "product-service")
public interface ProductFeignClient {

    @GetMapping("/api/product/all")
    Map<String, Object> getAllProducts(@RequestParam("page") int page);

    @GetMapping("/api/product/{productId}")
    Map<String, Object> getProductById(@PathVariable("productId") String productId);
}
