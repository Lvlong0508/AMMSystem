package com.gzasc.aishopping.shop.controller;

import com.gzasc.aishopping.common.dto.product.ProductDTO;
import com.gzasc.aishopping.common.feign.auth.AuthFeignClient;
import com.gzasc.aishopping.common.feign.auth.UserInfoFeignClient;
import com.gzasc.aishopping.common.feign.contact.ContactFeignClient;
import com.gzasc.aishopping.common.feign.order.OrderFeignClient;
import com.gzasc.aishopping.common.feign.product.ProductFeignClient;
import com.gzasc.aishopping.shop.model.MerchantRole;
import com.gzasc.aishopping.shop.model.OrderShop;
import com.gzasc.aishopping.shop.model.ProductShop;
import com.gzasc.aishopping.shop.model.Shop;
import com.gzasc.aishopping.shop.service.MerchantRoleService;
import com.gzasc.aishopping.shop.service.OrderShopService;
import com.gzasc.aishopping.shop.service.ProductShopService;
import com.gzasc.aishopping.shop.service.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/seller/shop")
@RequiredArgsConstructor
public class ShopSellerController {

    private final ShopService shopService;
    private final MerchantRoleService merchantRoleService;
    private final ProductShopService productShopService;
    private final OrderShopService orderShopService;
    private final ProductFeignClient productFeignClient;
    private final OrderFeignClient orderFeignClient;
    private final ContactFeignClient contactFeignClient;
    private final AuthFeignClient authFeignClient;
    private final UserInfoFeignClient userInfoFeignClient;

    @DeleteMapping("/{shopId}")
    public Map<String, String> deleteShop(
            @PathVariable("shopId") String shopId,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            if (userId == null || userId.trim().isEmpty()) {
                return Map.of("message", "关闭店铺失败：未提供用户ID");
            }
            if (merchantRoleService.selectByMerchantShopAndRole(userId, shopId, "1") == null) {
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
            shop.setId(UUID.randomUUID().toString().replace("-", ""));
            shop.setMerchantId(userId);
            shop.setStatus(1);
            int result = shopService.createShop(shop);
            if (result > 0) {
                return Map.of("message", "创建店铺成功", "id", shop.getId());
            } else {
                return Map.of("message", "创建店铺失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
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
            if (merchantRoleService.selectByMerchantShopAndRole(userId, shopId, "1") == null) {
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
            List<Shop> shops = shopService.getShopsByUserId(userId);
            return Map.of("success", true, "shops", shops);
        } catch (Exception e) {
            return Map.of("success", false, "message", "获取店铺列表失败，请稍后重试");
        }
    }

    @GetMapping("/{shopId}/products")
    public Map<String, Object> getShopProducts(@PathVariable("shopId") String shopId) {
        try {
            Shop shop = shopService.getShopById(shopId);
            if (shop == null) {
                return Map.of("success", false, "message", "店铺不存在");
            }
            List<ProductShop> productShops = productShopService.selectByShopId(shopId);
            
            List<Map<String, Object>> productDetails = new java.util.ArrayList<>();
            for (ProductShop ps : productShops) {
                try {
                    Map<String, Object> productMap = productFeignClient.getProductById(ps.getProductId());
                    if (productMap != null && productMap.containsKey("id")) {
                        Map<String, Object> detail = new java.util.HashMap<>();
                        detail.put("productId", productMap.get("id"));
                        detail.put("name", productMap.get("name"));
                        detail.put("description", productMap.get("description"));
                        detail.put("price", productMap.get("price"));
                        detail.put("stock", productMap.get("stock"));
                        detail.put("tags", productMap.get("tags"));
                        productDetails.add(detail);
                    }
                } catch (Exception e) {
                }
            }
            
            return Map.of("success", true, "products", productDetails, "total", productDetails.size());
        } catch (Exception e) {
            return Map.of("success", false, "message", "查询商品失败: " + e.getMessage());
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
        if (merchantRoleService.selectByMerchantShopAndRole(userId, shopId, "1") == null) {
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
                productShopService.insert(ps);
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
            String shopIdFromDb = productShopService.selectShopIdByProductId(productId);
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
        if (merchantRoleService.selectByMerchantShopAndRole(userId, shopId, "1") == null) {
            return Map.of("success", false, "message", "无权限操作");
        }
        try {
            String shopIdFromDb = productShopService.selectShopIdByProductId(productId);
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
        if (merchantRoleService.selectByMerchantShopAndRole(userId, shopId, "1") == null) {
            return Map.of("success", false, "message", "无权限操作");
        }
        try {
            productShopService.deleteByShopAndProduct(shopId, productId);
            productFeignClient.deleteProduct(productId);
            return Map.of("success", true, "message", "删除商品成功");
        } catch (Exception e) {
            return Map.of("success", false, "message", "删除商品失败");
        }
    }

    @GetMapping("/{shopId}/orders/all")
    public Map<String, Object> getShopOrders(
            @PathVariable("shopId") String shopId,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        if (merchantRoleService.selectByMerchantAndShop(userId, shopId) == null) {
            return Map.of("success", false, "message", "无权限访问该店铺");
        }
        try {
            List<OrderShop> orderShops = orderShopService.selectByShopId(shopId);
            if (orderShops.isEmpty()) {
                return Map.of("success", true, "orders", new ArrayList<>(), "total", 0);
            }
            // 提取订单ID列表
            List<String> orderIds = orderShops.stream()
                    .map(OrderShop::getOrderId)
                    .collect(java.util.stream.Collectors.toList());
            // 批量获取订单详情
            List<Map<String, Object>> orderDetails = new ArrayList<>();
            for (String orderId : orderIds) {
                try {
                    Map<String, Object> orderMap = orderFeignClient.getOrderById(orderId);
                    if (orderMap != null && orderMap.containsKey("order")) {
                        Object orderObj = orderMap.get("order");
                        if (orderObj instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> orderData = (Map<String, Object>) orderObj;
                            orderData.put("orderId", orderId);
                            // 获取商品名称
                            Object productIdObj = orderData.get("productId");
                            if (productIdObj != null) {
                                String productId = String.valueOf(productIdObj);
                                try {
                                    Map<String, Object> productMap = productFeignClient.getProductById(productId);
                                    if (productMap != null && productMap.containsKey("name")) {
                                        orderData.put("productName", productMap.get("name"));
                                    }
                                } catch (Exception ex) {
                                    // 商品信息不影响订单展示
                                }
                            }
                            // 获取联系人信息
                            Object contactIdObj = orderData.get("contactId");
                            if (contactIdObj != null) {
                                int contactId = 0;
                                if (contactIdObj instanceof Number) {
                                    contactId = ((Number) contactIdObj).intValue();
                                }
                                if (contactId > 0 && userId != null) {
                                    try {
                                        Map<String, Object> contactMap = contactFeignClient.getContactByIdWithUser(contactId, userId);
                                        if (contactMap != null && contactMap.containsKey("data")) {
                                            Object contactObj = contactMap.get("data");
                                            if (contactObj instanceof Map) {
                                                @SuppressWarnings("unchecked")
                                                Map<String, Object> contactData = (Map<String, Object>) contactObj;
                                                Map<String, Object> contact = new HashMap<>();
                                                contact.put("name", contactData.get("name"));
                                                contact.put("phone", contactData.get("phone"));
                                                contact.put("address", contactData.get("address"));
                                                orderData.put("contact", contact);
                                            }
                                        }
                                    } catch (Exception ex) {
                                        System.err.println("获取联系人失败: " + ex.getMessage());
                                    }
                                }
                            }
                            orderDetails.add(orderData);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("获取订单详情失败: " + e.getMessage());
                }
            }
            return Map.of("success", true, "orders", orderDetails, "total", orderDetails.size());
        } catch (Exception e) {
            return Map.of("success", false, "message", "查询订单失败: " + e.getMessage());
        }
    }

    @GetMapping("/{shopId}/orders/{orderId}")
    public Map<String, Object> getOrderDetail(
            @PathVariable("shopId") String shopId,
            @PathVariable("orderId") String orderId,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        if (merchantRoleService.selectByMerchantAndShop(userId, shopId) == null) {
            return Map.of("success", false, "message", "无权限访问该店铺");
        }
        try {
            List<OrderShop> orderShops = orderShopService.selectByOrderId(orderId);
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
            List<MerchantRole> employees = merchantRoleService.selectByShopId(shopId);
            return Map.of("success", true, "employees", employees, "total", employees.size());
        } catch (Exception e) {
            return Map.of("success", false, "message", "查询店员失败");
        }
    }

    @PostMapping("/{shopId}/employees/register")
    public Map<String, Object> addEmployee(
            @PathVariable("shopId") String shopId,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestBody Map<String, String> request) {
        if (userId == null || userId.trim().isEmpty()) {
            return Map.of("success", false, "message", "未提供用户ID");
        }
        if (merchantRoleService.selectByMerchantShopAndRole(userId, shopId, "1") == null) {
            return Map.of("success", false, "message", "仅店长可添加店员");
        }

        String username = request.get("username");
        String password = request.get("password");
        String phone = request.get("phone");
        String name = request.get("name");

        if (username == null || username.trim().isEmpty()) {
            return Map.of("success", false, "message", "账号不能为空");
        }
        if (password == null || password.trim().isEmpty()) {
            return Map.of("success", false, "message", "密码不能为空");
        }

        try {
            Integer infoId = null;
            if (name != null && !name.trim().isEmpty()) {
                Map<String, String> userInfoRequest = new HashMap<>();
                userInfoRequest.put("nickname", name);
                Map<String, Object> userInfoResult = userInfoFeignClient.createUserInfo(userInfoRequest);
                if (userInfoResult == null || !Boolean.TRUE.equals(userInfoResult.get("success"))) {
                    String errorMsg = userInfoResult != null ? (String) userInfoResult.get("message") : "创建用户信息失败";
                    return Map.of("success", false, "message", "创建用户信息失败：" + errorMsg);
                }
                infoId = (Integer) userInfoResult.get("infoId");
            }

            Map<String, Object> registerRequest = new HashMap<>();
            registerRequest.put("username", username);
            registerRequest.put("password", password);
            if (phone != null && !phone.trim().isEmpty()) {
                registerRequest.put("phone", phone);
            }
            if (infoId != null) {
                registerRequest.put("infoId", infoId);
            }

            Map<String, Object> registerResult = authFeignClient.registerEmployee(registerRequest);
            if (registerResult == null || !registerResult.containsKey("merchantId")) {
                String errorMsg = registerResult != null ? (String) registerResult.get("message") : "注册失败";
                return Map.of("success", false, "message", "注册店员账号失败：" + errorMsg);
            }

            String merchantId = String.valueOf(registerResult.get("merchantId"));

            MerchantRole merchantRole = new MerchantRole();
            merchantRole.setId(UUID.randomUUID().toString().replace("-", ""));
            merchantRole.setMerchantId(merchantId);
            merchantRole.setShopId(shopId);
            merchantRole.setRole("2");
            merchantRole.setAssignedBy(userId);

            int result = merchantRoleService.insert(merchantRole);
            return Map.of("success", result > 0, "message", result > 0 ? "添加店员成功" : "添加店员失败");
        } catch (Exception e) {
            e.printStackTrace();
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
        if (merchantRoleService.selectByMerchantShopAndRole(userId, shopId, "1") == null) {
            return Map.of("success", false, "message", "仅店长可移除店员");
        }
        try {
            MerchantRole mr = merchantRoleService.selectByMerchantAndShop(merchantId, shopId);
            if (mr != null) {
                merchantRoleService.deleteById(mr.getId());
            }
            return Map.of("success", true, "message", "移除店员成功");
        } catch (Exception e) {
            return Map.of("success", false, "message", "移除店员失败");
        }
    }
}