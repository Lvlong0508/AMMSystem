package com.gzasc.aishopping.product.controller;

import com.gzasc.aishopping.common.response.ApiResponse;
import com.gzasc.aishopping.product.dto.CreateProductRequest;
import com.gzasc.aishopping.product.dto.UpdateProductRequest;
import com.gzasc.aishopping.product.dto.SellerProductAbstractDTO;
import com.gzasc.aishopping.product.dto.ProductWithImageDetailDTO;
import com.gzasc.aishopping.product.exception.ProductException;
import com.gzasc.aishopping.product.model.Product;
import com.gzasc.aishopping.product.service.ImageStorageService;
import com.gzasc.aishopping.product.service.ProductCommandService;
import com.gzasc.aishopping.product.service.SellerProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/seller/product")
@RequiredArgsConstructor
public class ProductSellerController {

    private final SellerProductService sellerProductService;
    private final ProductCommandService productCommandService;
    private final ImageStorageService imageStorageService;

    @GetMapping("/{productId}")
    public ApiResponse<ProductWithImageDetailDTO> getProductDetail(@PathVariable("productId") Long productId) {
        log.info("商家查询商品详情, productId={}", productId);
        ProductWithImageDetailDTO product = sellerProductService.getSellerProductDetail(productId);
        if (product == null) {
            throw new ProductException(404, "商品不存在");
        }
        return ApiResponse.success(product);
    }

    @GetMapping("/shop/{shopId}")
    public ApiResponse<List<SellerProductAbstractDTO>> getProductsByShop(@PathVariable("shopId") Long shopId) {
        log.info("商家查询店铺商品列表, shopId={}", shopId);
        List<SellerProductAbstractDTO> products = sellerProductService.getSellerProductsByShopId(shopId);
        return ApiResponse.success(products);
    }

    @GetMapping("/batch")
    public ApiResponse<List<SellerProductAbstractDTO>> getProductsAbstract(@RequestParam("ids") List<Long> ids) {
        log.info("商家批量查询商品, ids={}", ids);
        List<SellerProductAbstractDTO> products = sellerProductService.getSellerProductsAbstract(ids);
        return ApiResponse.success(products);
    }

    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<String> createProduct(
            @RequestPart("product") @Valid CreateProductRequest request,
            @RequestPart("image") MultipartFile image) {
        imageStorageService.validateImage(image);

        log.info("创建商品, name={}, shopId={}", request.getName(), request.getShopId());
        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setSale(false);
        product.setTags(request.getTags());
        product.setShopId(request.getShopId());

        int result = productCommandService.createProductWithImage(product, image);
        if (result > 0) {
            return ApiResponse.success("创建商品成功", product.getId().toString());
        }
        throw new ProductException(500, "创建商品失败");
    }

    @PutMapping(value = "/{productId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Void> updateProduct(
            @PathVariable("productId") Long productId,
            @RequestPart("product") @Valid UpdateProductRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        log.info("更新商品, productId={}", productId);

        if (image != null && !image.isEmpty()) {
            imageStorageService.validateImage(image);
        }

        Product product = new Product();
        product.setId(productId);
        if (request.getName() != null) product.setName(request.getName());
        if (request.getDescription() != null) product.setDescription(request.getDescription());
        if (request.getPrice() != null) product.setPrice(request.getPrice());
        if (request.getStock() != null) product.setStock(request.getStock());
        if (request.getTags() != null) product.setTags(request.getTags());

        int result = productCommandService.updateProductWithImage(product, image);
        if (result > 0) {
            return ApiResponse.success("更新商品成功", null);
        }
        throw new ProductException(404, "商品不存在");
    }

    @DeleteMapping("/{productId}")
    public ApiResponse<Void> deleteProduct(@PathVariable("productId") Long productId) {
        log.info("删除商品, productId={}", productId);
        int result = sellerProductService.deleteProduct(productId);
        if (result > 0) {
            return ApiResponse.success("删除商品成功", null);
        }
        throw new ProductException(404, "商品不存在");
    }

    @PostMapping("/{productId}/list")
    public ApiResponse<Void> listProduct(@PathVariable("productId") Long productId) {
        log.info("上架商品, productId={}", productId);
        boolean result = sellerProductService.listProduct(productId);
        if (!result) {
            throw new ProductException(404, "商品不存在或上架失败");
        }
        return ApiResponse.success("上架成功", null);
    }

    @PostMapping("/{productId}/unlist")
    public ApiResponse<Void> unlistProduct(@PathVariable("productId") Long productId) {
        log.info("下架商品, productId={}", productId);
        boolean result = sellerProductService.unlistProduct(productId);
        if (!result) {
            throw new ProductException(404, "商品不存在或下架失败");
        }
        return ApiResponse.success("下架成功", null);
    }
}
