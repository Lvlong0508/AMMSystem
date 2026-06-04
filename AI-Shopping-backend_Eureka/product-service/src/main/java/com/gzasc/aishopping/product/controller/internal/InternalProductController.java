package com.gzasc.aishopping.product.controller.internal;

import com.gzasc.aishopping.common.dto.product.ProductDTO;
import com.gzasc.aishopping.common.dto.product.StockDeductRequest;
import com.gzasc.aishopping.common.dto.product.StockReserveRequest;
import com.gzasc.aishopping.common.response.ApiResponse;
import com.gzasc.aishopping.product.dto.ProductWithImageAbstractDTO;
import com.gzasc.aishopping.product.exception.ProductException;
import com.gzasc.aishopping.product.mapper.ProductImageInfoMapper;
import com.gzasc.aishopping.product.mapper.ProductMapper;
import com.gzasc.aishopping.product.model.Product;
import com.gzasc.aishopping.product.model.ProductImageInfo;
import com.gzasc.aishopping.product.service.ProductReservationService;
import com.gzasc.aishopping.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/internal/product")
@RequiredArgsConstructor
public class InternalProductController {

    private final ProductService productService;
    private final ProductMapper productMapper;
    private final ProductImageInfoMapper productImageInfoMapper;
    private final ProductReservationService reservationService;

    // 内部接口：根据ID查询商品详情（订单服务构建订单信息进行抽象商品信息获取）
    @GetMapping("/{productId}")
    public ApiResponse<ProductDTO> getProductById(@PathVariable("productId") Long productId) {
        Product product = productMapper.selectProductById(productId);
        if (product == null) {
            return ApiResponse.error(404, "商品不存在");
        }
        ProductDTO dto = new ProductDTO();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setPrice(product.getPrice());
        dto.setTags(product.getTags());
        dto.setDescription(product.getDescription());
        dto.setStock(product.getStock());
        dto.setShopId(product.getShopId());
        dto.setCreatedAt(product.getCreatedAt());
        dto.setUpdatedAt(product.getUpdatedAt());
        if (product.getImageId() != null && product.getImageId() > 0) {
            ProductImageInfo img = productImageInfoMapper.selectURLById(product.getImageId());
            if (img != null) {
                dto.setImageUrl(img.getUrl());
            }
        }
        return ApiResponse.success(dto);
    }

    // 内部接口：批量查询商品抽象信息（订单服务构建订单信息进行抽象商品信息获取）
    @GetMapping("/batch")
    public ApiResponse<List<ProductWithImageAbstractDTO>> getProductsByIds(@RequestParam("ids") String ids) {
        if (ids == null || ids.trim().isEmpty()) {
            return ApiResponse.success(List.of());
        }
        List<Long> idList = Arrays.stream(ids.split(","))
                .filter(s -> !s.trim().isEmpty())
                .map(s -> Long.valueOf(s.trim()))
                .toList();
        List<ProductWithImageAbstractDTO> products = productService.getAbstractProductsForBuyer(idList);
        return ApiResponse.success(products);
    }

    // 内部接口：恢复库存（订单服务取消订单时执行）
    @PostMapping("/restore-stock")
    public ApiResponse<Void> restoreStock(@RequestBody @Valid StockDeductRequest request) {
        boolean success = productService.restoreStock(request.getProductId(), request.getQuantity());
        if (success) {
            return ApiResponse.success(null);
        }
        return ApiResponse.error("恢复失败");
    }

    // 内部接口：预占库存（订单服务下单成功后执行）
    @PostMapping("/reserve-stock")
    public ApiResponse<Void> reserveStock(@RequestBody @Valid StockReserveRequest req) {
        try {
            reservationService.reserve(req.getOrderId(), String.valueOf(req.getProductId()), req.getQuantity());
            return ApiResponse.success(null);
        } catch (ProductException e) {
            return ApiResponse.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    // 内部接口：确认预占并扣减库存（订单服务支付时执行）
    @PostMapping("/confirm-reservation")
    public ApiResponse<Void> confirmReservation(@RequestParam("orderId") String orderId) {
        try {
            reservationService.confirm(orderId);
            return ApiResponse.success(null);
        } catch (ProductException e) {
            return ApiResponse.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    // 内部接口：释放预占（订单服务取消订单或超时取消时执行）
    @PostMapping("/release-reservation")
    public ApiResponse<Void> releaseReservation(@RequestParam("orderId") String orderId) {
        try {
            reservationService.release(orderId);
            return ApiResponse.success(null);
        } catch (ProductException e) {
            return ApiResponse.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}
