package com.gzasc.aishopping.shop.controller;

import com.gzasc.aishopping.common.feign.contact.ContactFeignClient;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/seller/shop/query")
@RequiredArgsConstructor
public class ShopQueryController {

    private final ShopService shopService;
    private final MerchantRoleMapper merchantRoleMapper;
    private final ProductShopMapper productShopMapper;
    private final OrderShopMapper orderShopMapper;
    private final ProductFeignClient productFeignClient;
    private final OrderFeignClient orderFeignClient;
    private final ContactFeignClient contactFeignClient;

    private boolean hasShopAccess(String userId, String shopId) {
        return merchantRoleMapper.selectByMerchantAndShop(userId, shopId) != null;
    }

    @GetMapping("/shop/{shopId}")
    public Map<String, Object> getShop(
            @PathVariable("shopId") String shopId,
            @RequestHeader("userId") String userId) {
        if (!hasShopAccess(userId, shopId)) {
            return Map.of("success", false, "message", "无权限访问该店铺");
        }
        Shop shop = shopService.getShopById(shopId);
        if (shop == null) {
            return Map.of("success", false, "message", "店铺不存在");
        }
        return Map.of("success", true, "shop", shop);
    }

    @GetMapping("/{shopId}/products")
    public Map<String, Object> getProducts(
            @PathVariable("shopId") String shopId,
            @RequestHeader("userId") String userId) {
        if (!hasShopAccess(userId, shopId)) {
            return Map.of("success", false, "message", "无权限访问该店铺");
        }
        List<ProductShop> productShops = productShopMapper.selectByShopId(shopId);
        List<Map<String, Object>> products = new ArrayList<>();
        for (ProductShop ps : productShops) {
            try {
                Map<String, Object> productMap = productFeignClient.getProductById(ps.getProductId());
                if (productMap != null && productMap.containsKey("id")) {
                    Map<String, Object> detail = new HashMap<>();
                    detail.put("productId", productMap.get("id"));
                    detail.put("name", productMap.get("name"));
                    detail.put("description", productMap.get("description"));
                    detail.put("price", productMap.get("price"));
                    detail.put("stock", productMap.get("stock"));
                    products.add(detail);
                }
            } catch (Exception e) {
            }
        }
        return Map.of("success", true, "products", products, "total", products.size());
    }

    @GetMapping("/{shopId}/orders")
    public Map<String, Object> getOrders(
            @PathVariable("shopId") String shopId,
            @RequestHeader("userId") String userId) {
        if (!hasShopAccess(userId, shopId)) {
            return Map.of("success", false, "message", "无权限访问该店铺");
        }
        List<OrderShop> orderShops = orderShopMapper.selectByShopId(shopId);
        if (orderShops.isEmpty()) {
            return Map.of("success", true, "orders", new ArrayList<>(), "total", 0);
        }

        List<Map<String, Object>> orderDetails = new ArrayList<>();
        for (OrderShop os : orderShops) {
            try {
                Map<String, Object> orderMap = orderFeignClient.getOrderById(os.getOrderId());
                if (orderMap != null && orderMap.containsKey("order")) {
                    Map<String, Object> orderData = (Map<String, Object>) orderMap.get("order");
                    orderData.put("orderId", os.getOrderId());

                    Object productIdObj = orderData.get("productId");
                    if (productIdObj != null) {
                        try {
                            Map<String, Object> productMap = productFeignClient.getProductById(String.valueOf(productIdObj));
                            if (productMap != null && productMap.containsKey("name")) {
                                orderData.put("productName", productMap.get("name"));
                            }
                        } catch (Exception e) {
                        }
                    }

                    Object contactIdObj = orderData.get("contactId");
                    if (contactIdObj != null && contactIdObj instanceof Number) {
                        int contactId = ((Number) contactIdObj).intValue();
                        if (contactId > 0) {
                            try {
                                Map<String, Object> contactMap = contactFeignClient.getContactByIdWithUser(contactId, userId);
                                if (contactMap != null && contactMap.containsKey("data")) {
                                    Map<String, Object> contactData = (Map<String, Object>) contactMap.get("data");
                                    Map<String, Object> contact = new HashMap<>();
                                    contact.put("name", contactData.get("name"));
                                    contact.put("phone", contactData.get("phone"));
                                    contact.put("address", contactData.get("address"));
                                    orderData.put("contact", contact);
                                }
                            } catch (Exception e) {
                            }
                        }
                    }

                    orderDetails.add(orderData);
                }
            } catch (Exception e) {
            }
        }
        return Map.of("success", true, "orders", orderDetails, "total", orderDetails.size());
    }

    @GetMapping("/{shopId}/orders/{orderId}")
    public Map<String, Object> getOrderDetail(
            @PathVariable("shopId") String shopId,
            @PathVariable("orderId") String orderId,
            @RequestHeader("userId") String userId) {
        if (!hasShopAccess(userId, shopId)) {
            return Map.of("success", false, "message", "无权限访问该店铺");
        }
        List<OrderShop> orderShops = orderShopMapper.selectByOrderId(orderId);
        if (orderShops.isEmpty() || !orderShops.get(0).getShopId().equals(shopId)) {
            return Map.of("success", false, "message", "订单不存在");
        }
        Map<String, Object> order = orderFeignClient.getOrderById(orderId);
        return Map.of("success", true, "order", order);
    }

    @GetMapping("/{shopId}/employees")
    public Map<String, Object> getEmployees(
            @PathVariable("shopId") String shopId,
            @RequestHeader("userId") String userId) {
        if (!hasShopAccess(userId, shopId)) {
            return Map.of("success", false, "message", "无权限访问该店铺");
        }
        List<MerchantRole> employees = merchantRoleMapper.selectByShopId(shopId);
        return Map.of("success", true, "employees", employees, "total", employees.size());
    }
}