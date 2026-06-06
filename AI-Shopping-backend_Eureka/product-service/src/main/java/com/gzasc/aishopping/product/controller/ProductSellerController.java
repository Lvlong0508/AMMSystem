package com.gzasc.aishopping.product.controller;

import com.gzasc.aishopping.common.dto.shop.ShopInfoDTO;
import com.gzasc.aishopping.common.response.ApiResponse;
import com.gzasc.aishopping.product.dto.CreateProductRequest;
import com.gzasc.aishopping.product.dto.UpdateProductRequest;
import com.gzasc.aishopping.product.dto.ProductWithImageAbstractDTO;
import com.gzasc.aishopping.product.dto.ProductWithImageDetailDTO;
import com.gzasc.aishopping.product.exception.ProductException;
import com.gzasc.aishopping.product.model.Product;
import com.gzasc.aishopping.product.service.ProductService;
import com.gzasc.aishopping.product.vo.ShopInfoVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/seller/product")
@RequiredArgsConstructor
public class ProductSellerController {


    private final ProductService productService;

    @GetMapping("/{productId}")
    public ApiResponse<Map<String, Object>> getProductDetail(@PathVariable("productId") Long productId) {
        log.info("商家查询商品详情, productId={}", productId);
        ProductWithImageDetailDTO product = productService.getProductById(productId);
        if (product == null) {
            throw new ProductException(404, "商品不存在");
        }
        return ApiResponse.success(toDetailVO(product));
    }

    @GetMapping("/batch")
    public ApiResponse<List<ProductWithImageAbstractDTO>> getProductsAbstract(@RequestParam("ids") String ids) {
        log.info("商家批量查询商品, ids={}", ids);
        List<Long> idList = Arrays.stream(ids.split(","))
                .map(Long::valueOf)
                .toList();
        List<ProductWithImageAbstractDTO> products = productService.getAbstractProductsForMerchant(idList);
        return ApiResponse.success(products);
    }

    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<String> createProduct(
            @RequestPart("product") @Valid CreateProductRequest request,
            @RequestPart("image") MultipartFile image) {
        if (image.isEmpty()) {
            throw new ProductException(400, "图片不能为空");
        }
        String contentType = image.getContentType();
        if (!"image/jpeg".equals(contentType) && !"image/png".equals(contentType)) {
            throw new ProductException(400, "仅支持 JPG 和 PNG 格式");
        }

        log.info("创建商品, name={}, shopId={}", request.getName(), request.getShopId());
        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setSale(false);
        product.setShopId(request.getShopId());

        int result = productService.createProductWithImage(product, image);
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
            String contentType = image.getContentType();
            if (!"image/jpeg".equals(contentType) && !"image/png".equals(contentType)) {
                throw new ProductException(400, "仅支持 JPG 和 PNG 格式");
            }
        }

        Product product = new Product();
        product.setId(productId);
        if (request.getName() != null) product.setName(request.getName());
        if (request.getDescription() != null) product.setDescription(request.getDescription());
        if (request.getPrice() != null) product.setPrice(request.getPrice());
        if (request.getStock() != null) product.setStock(request.getStock());

        int result = productService.updateProductWithImage(product, image);
        if (result > 0) {
            return ApiResponse.success("更新商品成功", null);
        }
        throw new ProductException(404, "商品不存在");
    }

    @DeleteMapping("/{productId}")
    public ApiResponse<Void> deleteProduct(@PathVariable("productId") Long productId) {
        log.info("删除商品, productId={}", productId);
        int result = productService.deleteProduct(productId);
        if (result > 0) {
            return ApiResponse.success("删除商品成功", null);
        }
        throw new ProductException(404, "商品不存在");
    }

    @PostMapping("/{productId}/list")
    public ApiResponse<Void> listProduct(@PathVariable("productId") Long productId) {
        log.info("上架商品, productId={}", productId);
        boolean result = productService.listProduct(productId);
        if (!result) {
            throw new ProductException(404, "商品不存在或上架失败");
        }
        return ApiResponse.success("上架成功", null);
    }

    @PostMapping("/{productId}/unlist")
    public ApiResponse<Void> unlistProduct(@PathVariable("productId") Long productId) {
        log.info("下架商品, productId={}", productId);
        boolean result = productService.unlistProduct(productId);
        if (!result) {
            throw new ProductException(404, "商品不存在或下架失败");
        }
        return ApiResponse.success("下架成功", null);
    }

    private ShopInfoVO shopInfoToVO(ShopInfoDTO dto) {
        if (dto == null) return null;
        return new ShopInfoVO(String.valueOf(dto.getId()), dto.getName(), dto.getDescription(), dto.getLogoUrl());
    }

    private Map<String, Object> toDetailVO(ProductWithImageDetailDTO dto) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", String.valueOf(dto.getId()));
        map.put("name", dto.getName());
        map.put("price", dto.getPrice());
        map.put("tags", dto.getTags());
        map.put("description", dto.getDescription());
        map.put("stock", dto.getStock());
        map.put("isSale", dto.isSale());
        map.put("imageId", dto.getImageId());
        map.put("imageUrl", dto.getImageUrl());
        map.put("shop", dto.getShop() != null ? shopInfoToVO(dto.getShop()) : null);
        map.put("createdAt", dto.getCreatedAt());
        map.put("updatedAt", dto.getUpdatedAt());
        return map;
    }
}