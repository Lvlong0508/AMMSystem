package com.gzasc.aishopping.product.controller;

import com.gzasc.aishopping.common.dto.product.ProductCardDTO;
import com.gzasc.aishopping.common.response.ApiResponse;
import com.gzasc.aishopping.product.dto.ProductWithImageDetailDTO;
import com.gzasc.aishopping.product.exception.ProductException;
import com.gzasc.aishopping.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 商品用户端控制器
 * 提供商品查询功能，供普通用户使用
 */
@RestController
@RequestMapping("/api/user/product")
@RequiredArgsConstructor
@Slf4j
public class ProductUserController {

    private final ProductService productService;

    // 分页查询可售商品列表
    @GetMapping("/all")
    public ApiResponse<Map<String, Object>> getAllSalableProducts(@RequestParam(name = "page", defaultValue = "0") int page) {
        try {
            List<ProductCardDTO> products = productService.getSalableProductCards(page);
            log.info("查询所有商品成功, page={}, size={}", page, products.size());
            return ApiResponse.success(Map.of("products", products, "page", page, "size", products.size()));
        } catch (ProductException e) {
            throw e;
        } catch (Exception e) {
            log.error("查询所有商品失败: {}", e.getMessage());
            throw new ProductException(500, "查询错误：" + e.getMessage());
        }
    }

    @GetMapping("/{productId}")
    public ApiResponse<ProductWithImageDetailDTO> getProductById(@PathVariable("productId") Long productId) {
        try {
            ProductWithImageDetailDTO product = productService.getProductById(productId);
            if (product != null) {
                log.info("查询商品成功, productId={}", productId);
                return ApiResponse.success(product);
            } else {
                log.warn("商品不存在, productId={}", productId);
                throw new ProductException(404, "商品不存在");
            }
        } catch (ProductException e) {
            throw e;
        } catch (Exception e) {
            log.error("查询商品失败: {}", e.getMessage());
            throw new ProductException(500, "查询错误：" + e.getMessage());
        }
    }

    @GetMapping("/search")
    public ApiResponse<Map<String, Object>> getProductsByName(@RequestParam("name") String name) {
        try {
            List<ProductWithImageDetailDTO> products = productService.getProductsByName(name);
            log.info("搜索商品成功, name={}, size={}", name, products.size());
            return ApiResponse.success(Map.of("products", products, "total", products.size()));
        } catch (Exception e) {
            log.error("搜索商品失败: {}", e.getMessage());
            throw new ProductException(500, "查询错误：" + e.getMessage());
        }
    }

    @GetMapping("/shop/{shopId}")
    public ApiResponse<Map<String, Object>> getProductsByShop(@PathVariable("shopId") Long shopId) {
        try {
            List<ProductCardDTO> products = productService.getSalableProductCardsByShopId(shopId);
            log.info("按店铺查询商品成功, shopId={}, size={}", shopId, products.size());
            return ApiResponse.success(Map.of("products", products));
        } catch (Exception e) {
            log.error("按店铺查询商品失败: {}", e.getMessage());
            throw new ProductException(500, "查询错误：" + e.getMessage());
        }
    }

    @GetMapping("/price-range")
    public ApiResponse<Map<String, Object>> getProductsByPriceRange(
            @RequestParam("minPrice") BigDecimal minPrice,
            @RequestParam("maxPrice") BigDecimal maxPrice,
            @RequestParam(name = "page", defaultValue = "0") int page) {
        try {
            List<ProductCardDTO> products = productService.getProductCardsByPriceRange(minPrice, maxPrice, page);
            log.info("按价格范围查询商品成功, minPrice={}, maxPrice={}, page={}, size={}", minPrice, maxPrice, page, products.size());
            return ApiResponse.success(Map.of("products", products, "page", page, "size", products.size()));
        } catch (ProductException e) {
            throw e;
        } catch (Exception e) {
            log.error("按价格范围查询商品失败: {}", e.getMessage());
            throw new ProductException(500, "查询错误：" + e.getMessage());
        }
    }
}
