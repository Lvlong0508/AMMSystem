package com.gzasc.aishopping.product.controller.internal;

import com.gzasc.aishopping.common.dto.product.ProductCardDTO;
import com.gzasc.aishopping.common.dto.product.ProductDTO;
import com.gzasc.aishopping.common.dto.product.StockDeductRequest;
import com.gzasc.aishopping.common.dto.product.StockReserveRequest;
import com.gzasc.aishopping.common.response.ApiResponse;
import com.gzasc.aishopping.product.dto.ProductWithImageAbstractDTO;
import com.gzasc.aishopping.product.dto.ProductWithImageDetailDTO;
import com.gzasc.aishopping.product.service.BuyerProductService;
import com.gzasc.aishopping.product.service.InternalProductService;
import com.gzasc.aishopping.product.service.ProductReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/internal/product")
@RequiredArgsConstructor
public class InternalProductController {

    private final InternalProductService internalProductService;
    private final BuyerProductService buyerProductService;
    private final ProductReservationService reservationService;

    // 内部接口：根据ID查询商品详情（订单服务构建订单信息进行抽象商品信息获取）
    @GetMapping("/{productId}")
    public ApiResponse<ProductDTO> getProductById(@PathVariable("productId") Long productId) {
        ProductDTO dto = internalProductService.getBasicProductById(productId);
        if (dto == null) {
            return ApiResponse.error(404, "商品不存在");
        }
        return ApiResponse.success(dto);
    }

    @GetMapping("/page")
    public ApiResponse<Map<String, Object>> getProductPage(@RequestParam(name = "page", defaultValue = "0") int page) {
        List<ProductCardDTO> products = buyerProductService.getSalableProductCards(page);
        return ApiResponse.success(Map.of("products", products, "page", page, "size", products.size()));
    }

    @GetMapping("/detail/{productId}")
    public ApiResponse<ProductWithImageDetailDTO> getProductDetail(@PathVariable("productId") Long productId) {
        ProductWithImageDetailDTO product = internalProductService.getInternalProductDetail(productId);
        if (product == null) {
            return ApiResponse.error(404, "商品不存在");
        }
        return ApiResponse.success(product);
    }

    // 内部接口：批量查询商品抽象信息（订单服务构建订单信息进行抽象商品信息获取）
    @GetMapping("/batch")
    public ApiResponse<List<ProductWithImageAbstractDTO>> getProductsByIds(@RequestParam("ids") List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return ApiResponse.success(List.of());
        }
        List<ProductWithImageAbstractDTO> products = internalProductService.getAbstractProductsForBuyer(ids);
        return ApiResponse.success(products);
    }

    // 内部接口：恢复库存（订单服务取消订单时执行）
    @PostMapping("/restore-stock")
    public ApiResponse<Void> restoreStock(@RequestBody @Valid StockDeductRequest request) {
        boolean success = internalProductService.restoreStock(request.getProductId(), request.getQuantity());
        if (success) {
            return ApiResponse.success(null);
        }
        return ApiResponse.error("恢复失败");
    }

    // 内部接口：预占库存（订单服务下单成功后执行）
    @PostMapping("/reserve-stock")
    public ApiResponse<Void> reserveStock(@RequestBody @Valid StockReserveRequest req) {
        reservationService.reserve(req.getOrderId(), String.valueOf(req.getProductId()), req.getQuantity());
        return ApiResponse.success(null);
    }

    // 内部接口：确认预占并扣减库存（订单服务支付时执行）
    @PostMapping("/confirm-reservation")
    public ApiResponse<Void> confirmReservation(@RequestParam("orderId") String orderId) {
        reservationService.confirm(orderId);
        return ApiResponse.success(null);
    }

    // 内部接口：释放预占（订单服务取消订单或超时取消时执行）
    @PostMapping("/release-reservation")
    public ApiResponse<Void> releaseReservation(@RequestParam("orderId") String orderId) {
        reservationService.release(orderId);
        return ApiResponse.success(null);
    }
}
