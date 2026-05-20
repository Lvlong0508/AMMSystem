package com.gzasc.aishopping.product.controller;

import com.gzasc.aishopping.product.common.ApiResponse;
import com.gzasc.aishopping.product.dto.CreateProductRequest;
import com.gzasc.aishopping.product.dto.UpdateProductRequest;
import com.gzasc.aishopping.product.exception.ProductException;
import com.gzasc.aishopping.product.model.Product;
import com.gzasc.aishopping.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/seller/product")
@RequiredArgsConstructor
public class ProductSellerController {

    private static final Logger log = LoggerFactory.getLogger(ProductSellerController.class);
    private final ProductService productService;

    @PostMapping("/create")
    public ApiResponse<String> createProduct(@RequestBody @Valid CreateProductRequest request) {
        log.info("创建商品, name={}", request.getName());
        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setImageId(0);
        product.setSale(false);

        int result = productService.createProduct(product);
        if (result > 0) {
            return ApiResponse.success("创建商品成功", product.getId() != null ? product.getId().toString() : null);
        }
        throw new ProductException(500, "创建商品失败");
    }

    @PutMapping("/{productId}")
    public ApiResponse<Void> updateProduct(
            @PathVariable("productId") String productId,
            @RequestBody @Valid UpdateProductRequest request) {
        log.info("更新商品, productId={}", productId);
        Product product = new Product();
        product.setId(Long.parseLong(productId));
        if (request.getName() != null) product.setName(request.getName());
        if (request.getDescription() != null) product.setDescription(request.getDescription());
        if (request.getPrice() != null) product.setPrice(request.getPrice());
        if (request.getStock() != null) product.setStock(request.getStock());

        int result = productService.updateProduct(product);
        if (result > 0) {
            return ApiResponse.success("更新商品成功", null);
        }
        throw new ProductException(404, "商品不存在");
    }

    @DeleteMapping("/{productId}")
    public ApiResponse<Void> deleteProduct(@PathVariable("productId") String productId) {
        log.info("删除商品, productId={}", productId);
        int result = productService.deleteProduct(productId);
        if (result > 0) {
            return ApiResponse.success("删除商品成功", null);
        }
        throw new ProductException(404, "商品不存在");
    }

    @PostMapping("/{productId}/list")
    public ApiResponse<Void> listProduct(@PathVariable("productId") String productId) {
        log.info("上架商品, productId={}", productId);
        Product product = productService.getProductById(productId);
        if (product == null) {
            throw new ProductException(404, "商品不存在");
        }
        product.setSale(true);
        productService.updateProduct(product);
        return ApiResponse.success("上架成功", null);
    }

    @PostMapping("/{productId}/unlist")
    public ApiResponse<Void> unlistProduct(@PathVariable("productId") String productId) {
        log.info("下架商品, productId={}", productId);
        Product product = productService.getProductById(productId);
        if (product == null) {
            throw new ProductException(404, "商品不存在");
        }
        product.setSale(false);
        productService.updateProduct(product);
        return ApiResponse.success("下架成功", null);
    }

    @PostMapping("/{productId}/image")
    public ApiResponse<Void> addImage(
            @PathVariable("productId") String productId,
            @RequestParam("imageUrl") String imageUrl) {
        log.info("添加商品图片, productId={}, imageUrl={}", productId, imageUrl);
        return ApiResponse.success("添加图片成功", null);
    }

    @DeleteMapping("/{productId}/image/{imageId}")
    public ApiResponse<Void> deleteImage(
            @PathVariable("productId") String productId,
            @PathVariable("imageId") String imageId) {
        log.info("删除商品图片, productId={}, imageId={}", productId, imageId);
        return ApiResponse.success("删除图片成功", null);
    }
}