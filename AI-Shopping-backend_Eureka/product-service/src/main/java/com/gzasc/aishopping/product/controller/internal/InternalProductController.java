package com.gzasc.aishopping.product.controller.internal;

import com.gzasc.aishopping.common.dto.product.ProductDTO;
import com.gzasc.aishopping.common.dto.product.StockDeductRequest;
import com.gzasc.aishopping.common.dto.product.StockReserveRequest;
import com.gzasc.aishopping.common.response.ApiResponse;
import com.gzasc.aishopping.product.dto.ProductWithImageAbstractDTO;
import com.gzasc.aishopping.product.mapper.ProductMapper;
import com.gzasc.aishopping.product.model.Product;
import com.gzasc.aishopping.product.service.ProductReservationService;
import com.gzasc.aishopping.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/internal/product")
@RequiredArgsConstructor
public class InternalProductController {

    private final ProductService productService;
    private final ProductMapper productMapper;
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
        dto.setPrice(product.getPrice() != null ? product.getPrice().doubleValue() : null);
        dto.setTags(product.getTags());
        dto.setDescription(product.getDescription());
        dto.setStock(product.getStock());
        dto.setShopId(product.getShopId());
        dto.setCreatedAt(product.getCreatedAt());
        dto.setUpdatedAt(product.getUpdatedAt());
        return ApiResponse.success(dto);
    }

    // 内部接口：批量查询商品抽象信息（订单服务构建订单信息进行抽象商品信息获取）
    @GetMapping("/batch")
    public List<ProductWithImageAbstractDTO> getProductsByIds(@RequestParam("ids") String ids) {
        List<Long> idList = Arrays.stream(ids.split(","))
                .map(Long::valueOf)
                .toList();
        return productService.getAbstractProductsForBuyer(idList);
    }

    // 内部接口：扣减库存（订单服务下单成功就执行）
    @PostMapping("/deduct-stock")
    public ApiResponse<Void> deductStock(@RequestBody StockDeductRequest request) {
        boolean success = productService.deductStock(request.getProductId(), request.getQuantity());
        if (success) {
            return ApiResponse.success(null);
        }
        return ApiResponse.error("扣减失败：库存不足");
    }

    // 内部接口：恢复库存（订单服务取消订单时执行）
    @PostMapping("/restore-stock")
    public ApiResponse<Void> restoreStock(@RequestBody StockDeductRequest request) {
        boolean success = productService.restoreStock(request.getProductId(), request.getQuantity());
        if (success) {
            return ApiResponse.success(null);
        }
        return ApiResponse.error("恢复失败");
    }

    // 内部接口：预占库存（订单服务下单成功后执行）
    @PostMapping("/reserve-stock")
    public ApiResponse<Void> reserveStock(@RequestBody StockReserveRequest req) {
        try {
            reservationService.reserve(req.getOrderId(), String.valueOf(req.getProductId()), req.getQuantity());
            return ApiResponse.success(null);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    // 内部接口：确认预占并扣减库存（订单服务支付时执行）
    @PostMapping("/confirm-reservation")
    public ApiResponse<Void> confirmReservation(@RequestParam String orderId) {
        try {
            reservationService.confirm(orderId);
            return ApiResponse.success(null);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    // 内部接口：创建商品
    @PostMapping("/create")
    public ApiResponse<Map<String, Object>> createProduct(@RequestBody Product product) {
        int result = productService.createProduct(product);
        if (result > 0) {
            return ApiResponse.success("创建商品成功", Map.of("id", product.getId()));
        }
        return ApiResponse.error("创建商品失败");
    }

    // 内部接口：根据店铺ID分页查询商品
    @GetMapping("/by-shop/{shopId}")
    public ApiResponse<List<ProductWithImageAbstractDTO>> getProductsByShopId(
            @PathVariable("shopId") Long shopId,
            @RequestParam("page") int page,
            @RequestParam("size") int size) {
        List<ProductWithImageAbstractDTO> products = productService.getProductsByShopId(shopId, page, size);
        return ApiResponse.success(products);
    }

    // 内部接口：释放预占（订单服务取消订单或超时取消时执行）
    @PostMapping("/release-reservation")
    public ApiResponse<Void> releaseReservation(@RequestParam String orderId) {
        try {
            reservationService.release(orderId);
            return ApiResponse.success(null);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}
