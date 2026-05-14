package com.gzasc.aishopping.shop.controller;

import com.gzasc.aishopping.common.dto.product.ProductDTO;
import com.gzasc.aishopping.common.feign.order.OrderFeignClient;
import com.gzasc.aishopping.common.feign.product.ProductFeignClient;
import com.gzasc.aishopping.shop.mapper.MerchantRoleMapper;
import com.gzasc.aishopping.shop.mapper.OrderShopMapper;
import com.gzasc.aishopping.shop.mapper.ProductShopMapper;
import com.gzasc.aishopping.shop.model.MerchantRole;
import com.gzasc.aishopping.shop.model.OrderShop;
import com.gzasc.aishopping.shop.model.ProductShop;
import com.gzasc.aishopping.shop.model.Shop;
import com.gzasc.aishopping.shop.service.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/seller/shop")
@RequiredArgsConstructor
public class ShopSellerController {

    private final ShopService shopService;
    private final MerchantRoleMapper merchantRoleMapper;
    private final ProductShopMapper productShopMapper;
    private final OrderShopMapper orderShopMapper;
    private final ProductFeignClient productFeignClient;
    private final OrderFeignClient orderFeignClient;

    @DeleteMapping("/{shopId}")
    public Map<String, String> deleteShop(
            @PathVariable("shopId") String shopId,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            if (userId == null || userId.trim().isEmpty()) {
                return Map.of("message", "关闭店铺失败：未提供用户ID");
            }
            if (merchantRoleMapper.selectByMerchantShopAndRole(userId, shopId, "1") == null) {
                return Map.of("message", "关闭店铺失败：仅店长可操作");
            }
            int result = shopService.closeShop(shopId);
            if (result > 0) {
                return Map.of("message", "关闭店铺成功");
            } else {
                return Map.of("message", "关闭店铺失败：店铺不存在");
            }
        } catch (Exception e) {
            return Map.of("message", "关闭店铺失败，请稍后重试");
        }
    }

    @PostMapping("/register")
    public Map<String, String> createShop(
            @RequestBody Shop shop,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        if (shop == null || shop.getName() == null || shop.getName().trim().isEmpty()) {
            return Map.of("message", "创建店铺错误：店铺名称为空");
        }
        if (userId == null || userId.trim().isEmpty()) {
            return Map.of("message", "创建店铺错误：未提供用户ID");
        }
        try {
            shop.setMerchantId(userId);
            shop.setStatus(1);
            int result = shopService.createShop(shop);
            if (result > 0) {
                return Map.of("message", "创建店铺成功", "id", shop.getId());
            } else {
                return Map.of("message", "创建店铺失败");
            }
        } catch (Exception e) {
            return Map.of("message", "创建店铺失败，请稍后重试");
        }
    }

    @PutMapping("/{shopId}")
    public Map<String, String> updateShop(
            @PathVariable("shopId") String shopId,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestBody Shop shop) {
        if (shop == null) {
            return Map.of("message", "更新店铺错误：店铺信息为空");
        }
        try {
            if (userId == null || userId.trim().isEmpty()) {
                return Map.of("message", "更新店铺失败：未提供用户ID");
            }
            if (merchantRoleMapper.selectByMerchantShopAndRole(userId, shopId, "1") == null) {
                return Map.of("message", "更新店铺失败：仅店长可操作");
            }
            shop.setId(shopId);
            int result = shopService.updateShop(shop);
            if (result > 0) {
                return Map.of("message", "更新店铺成功");
            } else {
                return Map.of("message", "更新店铺失败：店铺不存在");
            }
        } catch (Exception e) {
            return Map.of("message", "更新店铺失败，请稍后重试");
        }
    }

    @GetMapping("/{shopId}")
    public Map<String, Object> getShop(@PathVariable("shopId") String shopId) {
        try {
            Shop shop = shopService.getShopById(shopId);
            if (shop != null) {
                return Map.of("message", "获取店铺成功", "shop", shop);
            } else {
                return Map.of("message", "获取店铺失败：店铺不存在");
            }
        } catch (Exception e) {
            return Map.of("message", "获取店铺失败，请稍后重试");
        }
    }

    @GetMapping("/list")
    public Map<String, Object> getMyShops(@RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            if (userId == null || userId.trim().isEmpty()) {
                return Map.of("success", false, "message", "未提供用户ID");
            }
            List<Shop> shops = shopService.getShopsByMerchantId(userId);
            return Map.of("success", true, "shops", shops);
        } catch (Exception e) {
            return Map.of("success", false, "message", "获取店员列表失败，请稍后重试");
        }
    }

    @GetMapping("/{shopId}/products")
    public Map<String, Object> getShopProducts(@PathVariable("shopId") String shopId) {
        try {
            List<ProductShop> productShops = productShopMapper.selectByShopId(shopId);
            return Map.of("success", true, "products", productShops, "total", productShops.size());
        } catch (Exception e) {
            return Map.of("success", false, "message", "查询商品失败");
        }
    }

    @PostMapping("/{shopId}/products")
    public Map<String, Object> createProduct(
            @PathVariable("shopId") String shopId,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestBody ProductDTO productDTO) {
        if (userId == null || userId.trim().isEmpty()) {
            return Map.of("success", false, "message", "未提供用户ID");
        }
        if (merchantRoleMapper.selectByMerchantShopAndRole(userId, shopId, "1") == null) {
            return Map.of("success", false, "message", "无权限操作");
        }
        try {
            Map<String, Object> result = productFeignClient.createProduct(productDTO);
            if (result != null && "创建商品成功".equals(result.get("message"))) {
                String productId = (String) result.get("id");
                ProductShop ps = new ProductShop();
                ps.setId(UUID.randomUUID().toString().replace("-", ""));
                ps.setProductId(productId);
                ps.setShopId(shopId);
                productShopMapper.insert(ps);
                return Map.of("success", true, "message", "创建商品成功", "id", productId);
            }
            return Map.of("success", false, "message", "创建商品失败");
        } catch (Exception e) {
            return Map.of("success", false, "message", "创建商品失败：" + e.getMessage());
        }
    }

    @GetMapping("/{shopId}/products/{productId}")
    public Map<String, Object> getProductDetail(
            @PathVariable("shopId") String shopId,
            @PathVariable("productId") String productId) {
        try {
            String shopIdFromDb = productShopMapper.selectShopIdByProductId(productId);
            if (shopIdFromDb == null || !shopIdFromDb.equals(shopId)) {
                return Map.of("success", false, "message", "商品不存在");
            }
            Map<String, Object> productMap = productFeignClient.getProductById(productId);
            if (productMap == null) {
                return Map.of("success", false, "message", "商品不存在");
            }
            // 从 Map 转换为 ProductDTO
            ProductDTO product = new ProductDTO();
            product.setId((String) productMap.get("id"));
            product.setName((String) productMap.get("name"));
            product.setDescription((String) productMap.get("description"));
            product.setPrice(productMap.get("price") != null ? ((Number) productMap.get("price")).doubleValue() : 0.0);
            product.setStock(productMap.get("stock") != null ? ((Number) productMap.get("stock")).intValue() : 0);
            return Map.of("success", true, "product", product);
        } catch (Exception e) {
            return Map.of("success", false, "message", "查询商品失败");
        }
    }

    @PutMapping("/{shopId}/products/{productId}")
    public Map<String, Object> updateProduct(
            @PathVariable("shopId") String shopId,
            @PathVariable("productId") String productId,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestBody ProductDTO productDTO) {
        if (userId == null || userId.trim().isEmpty()) {
            return Map.of("success", false, "message", "未提供用户ID");
        }
        if (merchantRoleMapper.selectByMerchantShopAndRole(userId, shopId, "1") == null) {
            return Map.of("success", false, "message", "无权限操作");
        }
        try {
            String shopIdFromDb = productShopMapper.selectShopIdByProductId(productId);
            if (shopIdFromDb == null || !shopIdFromDb.equals(shopId)) {
                return Map.of("success", false, "message", "商品不存在");
            }
            productFeignClient.updateProduct(productId, productDTO);
            return Map.of("success", true, "message", "更新商品成功");
        } catch (Exception e) {
            return Map.of("success", false, "message", "更新商品失败");
        }
    }

    @DeleteMapping("/{shopId}/products/{productId}")
    public Map<String, Object> deleteProduct(
            @PathVariable("shopId") String shopId,
            @PathVariable("productId") String productId,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return Map.of("success", false, "message", "未提供用户ID");
        }
        if (merchantRoleMapper.selectByMerchantShopAndRole(userId, shopId, "1") == null) {
            return Map.of("success", false, "message", "无权限操作");
        }
        try {
            productShopMapper.deleteByShopAndProduct(shopId, productId);
            productFeignClient.deleteProduct(productId);
            return Map.of("success", true, "message", "删除商品成功");
        } catch (Exception e) {
            return Map.of("success", false, "message", "删除商品失败");
        }
    }

    @GetMapping("/{shopId}/orders/all")
    public Map<String, Object> getShopOrders(@PathVariable("shopId") String shopId) {
        try {
            List<OrderShop> orderShops = orderShopMapper.selectByShopId(shopId);
            return Map.of("success", true, "orders", orderShops, "total", orderShops.size());
        } catch (Exception e) {
            return Map.of("success", false, "message", "查询订单失败");
        }
    }

    @GetMapping("/{shopId}/orders/{orderId}")
    public Map<String, Object> getOrderDetail(
            @PathVariable("shopId") String shopId,
            @PathVariable("orderId") String orderId) {
        try {
            List<OrderShop> orderShops = orderShopMapper.selectByOrderId(orderId);
            if (orderShops.isEmpty() || !orderShops.get(0).getShopId().equals(shopId)) {
                return Map.of("success", false, "message", "订单不存在");
            }
            Map<String, Object> order = orderFeignClient.getOrderById(orderId);
            return Map.of("success", true, "order", order);
        } catch (Exception e) {
            return Map.of("success", false, "message", "查询订单失败");
        }
    }

    @GetMapping("/{shopId}/employees")
    public Map<String, Object> getEmployees(@PathVariable("shopId") String shopId) {
        try {
            List<MerchantRole> employees = merchantRoleMapper.selectByShopId(shopId);
            return Map.of("success", true, "employees", employees, "total", employees.size());
        } catch (Exception e) {
            return Map.of("success", false, "message", "查询店员失败");
        }
    }

    @PostMapping("/{shopId}/employees/register")
    public Map<String, Object> addEmployee(
            @PathVariable("shopId") String shopId,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestBody MerchantRole merchantRole) {
        if (userId == null || userId.trim().isEmpty()) {
            return Map.of("success", false, "message", "未提供用户ID");
        }
        if (merchantRoleMapper.selectByMerchantShopAndRole(userId, shopId, "1") == null) {
            return Map.of("success", false, "message", "仅店长可添加店员");
        }
        try {
            merchantRole.setId(UUID.randomUUID().toString().replace("-", ""));
            merchantRole.setShopId(shopId);
            merchantRole.setAssignedBy(userId);
            int result = merchantRoleMapper.insert(merchantRole);
            return Map.of("success", result > 0, "message", result > 0 ? "添加店员成功" : "添加店员失败");
        } catch (Exception e) {
            return Map.of("success", false, "message", "添加店员失败：" + e.getMessage());
        }
    }

    @DeleteMapping("/{shopId}/employees/{merchantId}")
    public Map<String, Object> removeEmployee(
            @PathVariable("shopId") String shopId,
            @PathVariable("merchantId") String merchantId,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return Map.of("success", false, "message", "未提供用户ID");
        }
        if (merchantRoleMapper.selectByMerchantShopAndRole(userId, shopId, "1") == null) {
            return Map.of("success", false, "message", "仅店长可移除店员");
        }
        try {
            MerchantRole mr = merchantRoleMapper.selectByMerchantAndShop(merchantId, shopId);
            if (mr != null) {
                merchantRoleMapper.deleteById(mr.getId());
            }
            return Map.of("success", true, "message", "移除店员成功");
        } catch (Exception e) {
            return Map.of("success", false, "message", "移除店员失败");
        }
    }
}