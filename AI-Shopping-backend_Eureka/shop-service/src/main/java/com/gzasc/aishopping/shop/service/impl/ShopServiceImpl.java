package com.gzasc.aishopping.shop.service.impl;

import com.gzasc.aishopping.common.dto.product.ProductDTO;
import com.gzasc.aishopping.common.feign.auth.AuthFeignClient;
import com.gzasc.aishopping.common.feign.product.ProductFeignClient;
import com.gzasc.aishopping.shop.dto.AddEmployeeRequest;
import com.gzasc.aishopping.shop.dto.CreateShopRequest;
import com.gzasc.aishopping.shop.exception.ShopException;
import com.gzasc.aishopping.shop.mapper.MerchantRoleMapper;
import com.gzasc.aishopping.shop.mapper.ShopMapper;
import com.gzasc.aishopping.shop.model.MerchantRole;
import com.gzasc.aishopping.shop.model.ProductShop;
import com.gzasc.aishopping.shop.model.Shop;
import com.gzasc.aishopping.shop.service.MerchantRoleService;
import com.gzasc.aishopping.shop.service.ProductShopService;
import com.gzasc.aishopping.shop.service.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShopServiceImpl implements ShopService {

    private final ShopMapper shopMapper;
    private final MerchantRoleMapper merchantRoleMapper;
    private final ProductFeignClient productFeignClient;
    private final AuthFeignClient authFeignClient;
    private final ProductShopService productShopService;
    private final MerchantRoleService merchantRoleService;

    @Override
    public Shop getShopById(String shopId) {
        return shopMapper.selectShopById(shopId);
    }

    @Override
    public List<Shop> getShopsByMerchantId(String merchantId) {
        return shopMapper.selectShopsByMerchantId(merchantId);
    }

    @Override
    public List<Shop> getShopsByUserId(String userId) {
        return shopMapper.selectShopsByUserId(userId);
    }

    @Override
    public List<Shop> getAllShops(int page) {
        int offset = (page - 1) * 20;
        return shopMapper.selectShopsByPage(offset);
    }

    @Override
    @Transactional
    public int createShop(Shop shop) {
        try {
            int result = shopMapper.insertShop(shop);
            if (result > 0) {
                MerchantRole merchantRole = new MerchantRole();
                merchantRole.setMerchantId(shop.getMerchantId());
                merchantRole.setShopId(shop.getId());
                merchantRole.setRole("1");
                merchantRole.setAssignedBy(shop.getMerchantId());
                merchantRoleMapper.insert(merchantRole);
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException("创建店铺失败", e);
        }
    }

    @Override
    @Transactional
    public Shop createShop(CreateShopRequest request, String userId) {
        Shop shop = new Shop();
        shop.setId(UUID.randomUUID().toString().replace("-", ""));
        shop.setMerchantId(userId);
        shop.setName(request.getName());
        shop.setDescription(request.getDescription());
        shop.setLogoId(request.getLogoId());
        shop.setStatus(1);
        int result = shopMapper.insertShop(shop);
        if (result > 0) {
            MerchantRole merchantRole = new MerchantRole();
            merchantRole.setMerchantId(userId);
            merchantRole.setShopId(shop.getId());
            merchantRole.setRole("1");
            merchantRole.setAssignedBy(userId);
            merchantRoleMapper.insert(merchantRole);
        }
        if (result <= 0) {
            throw new ShopException("创建店铺失败");
        }
        return shop;
    }

    @Override
    public int updateShop(Shop shop) {
        return shopMapper.updateShop(shop);
    }

    @Override
    public int closeShop(String shopId) {
        return shopMapper.closeShop(shopId);
    }

    @Override
    public int countActiveShops() {
        return shopMapper.countActiveShops();
    }

    @Override
    public List<Shop> getActiveShops(int page, int size) {
        int offset = (page - 1) * size;
        return shopMapper.selectActiveShops(offset, size);
    }

    @Override
    @Transactional
    public void createProduct(String shopId, ProductDTO productDTO, String userId) {
        checkShopOwner(shopId, userId);
        try {
            Map<String, Object> result = productFeignClient.createProduct(productDTO);
            if (result == null || !"创建商品成功".equals(result.get("message"))) {
                throw new ShopException("创建商品失败");
            }
            String productId = (String) result.get("id");
            ProductShop ps = new ProductShop();
            ps.setId(UUID.randomUUID().toString().replace("-", ""));
            ps.setProductId(productId);
            ps.setShopId(shopId);
            productShopService.insert(ps);
        } catch (ShopException e) {
            throw e;
        } catch (Exception e) {
            throw new ShopException("创建商品失败: " + e.getMessage());
        }
    }

    @Override
    public void updateProduct(String shopId, String productId, ProductDTO productDTO, String userId) {
        checkShopOwner(shopId, userId);
        String shopIdFromDb = productShopService.selectShopIdByProductId(productId);
        if (shopIdFromDb == null || !shopIdFromDb.equals(shopId)) {
            throw new ShopException("商品不存在");
        }
        try {
            productFeignClient.updateProduct(productId, productDTO);
        } catch (Exception e) {
            throw new ShopException("更新商品失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void deleteProduct(String shopId, String productId, String userId) {
        checkShopOwner(shopId, userId);
        String shopIdFromDb = productShopService.selectShopIdByProductId(productId);
        if (shopIdFromDb == null || !shopIdFromDb.equals(shopId)) {
            throw new ShopException("商品不存在");
        }
        try {
            productShopService.deleteByShopAndProduct(shopId, productId);
            productFeignClient.deleteProduct(productId);
        } catch (Exception e) {
            throw new ShopException("删除商品失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void addEmployee(String shopId, AddEmployeeRequest request, String userId) {
        checkShopOwner(shopId, userId);
        try {
            Map<String, Object> registerRequest = new HashMap<>();
            registerRequest.put("username", request.getUsername());
            if (request.getPassword() != null) registerRequest.put("password", request.getPassword());
            if (request.getPhone() != null) registerRequest.put("phone", request.getPhone());
            if (request.getName() != null) registerRequest.put("nickname", request.getName());

            Map<String, Object> registerResult = authFeignClient.registerEmployee(registerRequest);
            if (registerResult == null || !registerResult.containsKey("merchantId")) {
                String errorMsg = registerResult != null ? (String) registerResult.get("message") : "注册失败";
                throw new ShopException("添加店员失败: " + errorMsg);
            }

            String merchantId = String.valueOf(registerResult.get("merchantId"));
            MerchantRole role = new MerchantRole();
            role.setMerchantId(merchantId);
            role.setShopId(shopId);
            role.setRole("2");
            role.setAssignedBy(userId);
            merchantRoleService.insert(role);
        } catch (ShopException e) {
            throw e;
        } catch (Exception e) {
            throw new ShopException("添加店员失败: " + e.getMessage());
        }
    }

    @Override
    public void removeEmployee(String shopId, String merchantId, String userId) {
        checkShopOwner(shopId, userId);
        MerchantRole mr = merchantRoleService.selectByMerchantAndShop(merchantId, shopId);
        if (mr != null) {
            merchantRoleService.deleteById(mr.getId());
        }
    }

    @Override
    @Transactional
    public void updateShop(String shopId, Shop shop, String userId) {
        checkShopOwner(userId, shopId);
        shop.setId(shopId);
        int result = shopMapper.updateShop(shop);
        if (result <= 0) {
            throw new ShopException("更新店铺失败");
        }
    }

    @Override
    @Transactional
    public void closeShop(String shopId, String userId) {
        checkShopOwner(userId, shopId);
        int result = shopMapper.closeShop(shopId);
        if (result <= 0) {
            throw new ShopException("关闭店铺失败");
        }
    }

    @Override
    public Shop getShopWithAccessCheck(String shopId, String userId) {
        checkShopAccess(userId, shopId);
        Shop shop = shopMapper.selectShopById(shopId);
        if (shop == null) {
            throw new ShopException("店铺不存在");
        }
        return shop;
    }

    @Override
    public List<Map<String, Object>> getShopProductsWithDetails(String shopId, String userId) {
        checkShopAccess(userId, shopId);
        List<ProductShop> productShops = productShopService.selectByShopId(shopId);
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
                // skip failed product
            }
        }
        return products;
    }

    @Override
    public List<Map<String, Object>> getShopEmployees(String shopId, String userId) {
        checkShopAccess(userId, shopId);
        List<MerchantRole> employees = merchantRoleService.selectByShopId(shopId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (MerchantRole mr : employees) {
            Map<String, Object> emp = new HashMap<>();
            emp.put("merchantId", mr.getMerchantId());
            emp.put("shopId", mr.getShopId());
            emp.put("role", mr.getRole());
            emp.put("assignedBy", mr.getAssignedBy());
            result.add(emp);
        }
        return result;
    }

    private void checkShopOwner(String userId, String shopId) {
        if (merchantRoleService.selectByMerchantShopAndRole(userId, shopId, "1") == null) {
            throw new ShopException("仅店长可操作");
        }
    }

    private void checkShopAccess(String userId, String shopId) {
        if (merchantRoleService.selectByMerchantAndShop(userId, shopId) == null) {
            throw new ShopException("无权限访问该店铺");
        }
    }
}