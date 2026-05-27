package com.gzasc.aishopping.shop.service.impl;

import com.gzasc.aishopping.common.dto.product.ProductDTO;
import com.gzasc.aishopping.common.dto.shop.ShopInfoDTO;
import com.gzasc.aishopping.common.feign.auth.AuthFeignClient;
import com.gzasc.aishopping.common.feign.product.ProductFeignClient;
import com.gzasc.aishopping.common.response.ApiResponse;
import com.gzasc.aishopping.common.util.SnowflakeIdGenerator;
import com.gzasc.aishopping.shop.dto.AddEmployeeRequest;
import com.gzasc.aishopping.shop.dto.CreateShopRequest;
import com.gzasc.aishopping.shop.dto.UpdateShopRequest;
import com.gzasc.aishopping.shop.exception.ShopException;
import com.gzasc.aishopping.shop.mapper.MerchantRoleMapper;
import com.gzasc.aishopping.shop.mapper.ShopMapper;
import com.gzasc.aishopping.shop.model.MerchantRole;
import com.gzasc.aishopping.shop.model.Shop;
import com.gzasc.aishopping.shop.model.ShopInfo;
import com.gzasc.aishopping.shop.service.MerchantRoleService;
import com.gzasc.aishopping.shop.service.ShopInfoService;
import com.gzasc.aishopping.shop.service.ShopService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ShopServiceImpl implements ShopService {

    private static final Logger log = LoggerFactory.getLogger(ShopServiceImpl.class);

    private final ShopMapper shopMapper;
    private final MerchantRoleMapper merchantRoleMapper;
    private final ProductFeignClient productFeignClient;
    private final AuthFeignClient authFeignClient;
    private final MerchantRoleService merchantRoleService;
    private final ShopInfoService shopInfoService;

    @Override
    public Shop getShopById(Long shopId) {
        return shopMapper.selectShopById(shopId);
    }

    @Override
    public List<Shop> getShopsByMerchantId(Long merchantId) {
        return shopMapper.selectShopsByMerchantId(merchantId);
    }

    @Override
    public List<Shop> getShopsByUserId(Long userId) {
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
                merchantRole.setRole(1);
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
    public Shop createShop(CreateShopRequest request, Long userId) {
        ShopInfo shopInfo = new ShopInfo();
        shopInfo.setId(SnowflakeIdGenerator.nextId());
        shopInfo.setName(request.getName());
        shopInfo.setDescription(request.getDescription());
        shopInfo.setLogoUrl(request.getLogoId());
        shopInfoService.insert(shopInfo);

        Shop shop = new Shop();
        shop.setId(SnowflakeIdGenerator.nextId());
        shop.setMerchantId(userId);
        shop.setShopInfoId(shopInfo.getId());
        shop.setStatus(1);
        int result = shopMapper.insertShop(shop);
        if (result > 0) {
            MerchantRole merchantRole = new MerchantRole();
            merchantRole.setMerchantId(userId);
            merchantRole.setShopId(shop.getId());
            merchantRole.setRole(1);
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
    public int closeShop(Long shopId) {
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
    public void createProduct(Long shopId, ProductDTO productDTO, Long userId) {
        checkShopOwner(shopId, userId);
        try {
            productDTO.setShopId(shopId);
            ApiResponse<Map<String, Object>> response = productFeignClient.createProduct(productDTO);
            if (response == null || response.getCode() != 200) {
                throw new ShopException("创建商品失败");
            }
        } catch (ShopException e) {
            throw e;
        } catch (Exception e) {
            throw new ShopException("创建商品失败: " + e.getMessage());
        }
    }

    @Override
    public void updateProduct(Long shopId, Long productId, ProductDTO productDTO, Long userId) {
        checkShopOwner(shopId, userId);
        Map<String, Object> productMap = productFeignClient.getProductById(productId);
        if (productMap == null || !shopId.equals(productMap.get("shopId"))) {
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
    public void deleteProduct(Long shopId, Long productId, Long userId) {
        checkShopOwner(shopId, userId);
        Map<String, Object> productMap = productFeignClient.getProductById(productId);
        if (productMap == null || !shopId.equals(productMap.get("shopId"))) {
            throw new ShopException("商品不存在");
        }
        try {
            productFeignClient.deleteProduct(productId);
        } catch (Exception e) {
            throw new ShopException("删除商品失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void addEmployee(Long shopId, AddEmployeeRequest request, Long userId) {
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

            Long merchantId = Long.valueOf(String.valueOf(registerResult.get("merchantId")));
            MerchantRole role = new MerchantRole();
            role.setMerchantId(merchantId);
            role.setShopId(shopId);
            role.setRole(2);
            role.setAssignedBy(userId);
            merchantRoleService.insert(role);
        } catch (ShopException e) {
            throw e;
        } catch (Exception e) {
            throw new ShopException("添加店员失败: " + e.getMessage());
        }
    }

    @Override
    public void removeEmployee(Long shopId, Long merchantId, Long userId) {
        checkShopOwner(shopId, userId);
        MerchantRole mr = merchantRoleService.selectByMerchantAndShop(merchantId, shopId);
        if (mr != null) {
            merchantRoleService.deleteById(mr.getId());
        }
    }

    @Override
    @Transactional
    public void updateShop(Long shopId, UpdateShopRequest request, Long userId) {
        checkShopOwner(userId, shopId);
        Shop shop = shopMapper.selectShopById(shopId);
        if (shop == null) {
            throw new ShopException("店铺不存在");
        }
        if (shop.getShopInfoId() != null) {
            ShopInfo shopInfo = new ShopInfo();
            shopInfo.setId(shop.getShopInfoId());
            shopInfo.setName(request.getName());
            shopInfo.setDescription(request.getDescription());
            shopInfo.setLogoUrl(request.getLogoId());
            shopInfoService.update(shopInfo);
        }
    }

    @Override
    public Map<String, Object> getShopProductsWithPagination(Long shopId, int page, int size) {
        ApiResponse<List<Map<String, Object>>> response = productFeignClient.getProductsByShopId(shopId, page, size);
        if (response == null || response.getCode() != 200) {
            Map<String, Object> emptyResult = new HashMap<>();
            emptyResult.put("products", Collections.emptyList());
            emptyResult.put("total", 0);
            emptyResult.put("page", page);
            emptyResult.put("size", size);
            return emptyResult;
        }
        List<Map<String, Object>> products = response.getData();
        Map<String, Object> result = new HashMap<>();
        result.put("products", products != null ? products : Collections.emptyList());
        result.put("total", products != null ? products.size() : 0);
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    @Override
    public ProductDTO getProductDetailByShop(Long shopId, Long productId) {
        Map<String, Object> productMap = productFeignClient.getProductById(productId);
        if (productMap == null || !shopId.equals(productMap.get("shopId"))) {
            throw new ShopException("商品不存在");
        }
        ProductDTO product = new ProductDTO();
        product.setId(productMap.get("id") != null ? ((Number) productMap.get("id")).longValue() : null);
        product.setName((String) productMap.get("name"));
        product.setDescription((String) productMap.get("description"));
        product.setPrice(productMap.get("price") != null ? ((Number) productMap.get("price")).doubleValue() : 0.0);
        product.setStock(productMap.get("stock") != null ? ((Number) productMap.get("stock")).intValue() : 0);
        return product;
    }

    @Override
    @Transactional
    public void closeShop(Long shopId, Long userId) {
        checkShopOwner(userId, shopId);
        int result = shopMapper.closeShop(shopId);
        if (result <= 0) {
            throw new ShopException("关闭店铺失败");
        }
    }

    @Override
    public Shop getShopWithAccessCheck(Long shopId, Long userId) {
        checkShopAccess(userId, shopId);
        Shop shop = shopMapper.selectShopById(shopId);
        if (shop == null) {
            throw new ShopException("店铺不存在");
        }
        return shop;
    }

    @Override
    public Map<String, Object> getShopProductsWithDetails(Long shopId, Long userId, int page, int size) {
        checkShopAccess(userId, shopId);
        ApiResponse<List<Map<String, Object>>> response = productFeignClient.getProductsByShopId(shopId, page, size);
        if (response == null || response.getCode() != 200) {
            Map<String, Object> emptyResult = new HashMap<>();
            emptyResult.put("products", Collections.emptyList());
            emptyResult.put("total", 0);
            emptyResult.put("page", page);
            emptyResult.put("size", size);
            return emptyResult;
        }
        List<Map<String, Object>> products = response.getData();
        Map<String, Object> result = new HashMap<>();
        result.put("products", products != null ? products : Collections.emptyList());
        result.put("total", products != null ? products.size() : 0);
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    @Override
    public Map<String, Object> getShopEmployees(Long shopId, Long userId) {
        checkShopAccess(userId, shopId);
        List<MerchantRole> employees = merchantRoleService.selectByShopId(shopId);
        List<Map<String, Object>> employeeList = new ArrayList<>();
        for (MerchantRole mr : employees) {
            Map<String, Object> emp = new HashMap<>();
            emp.put("merchantId", mr.getMerchantId());
            emp.put("shopId", mr.getShopId());
            emp.put("role", mr.getRole());
            emp.put("assignedBy", mr.getAssignedBy());
            employeeList.add(emp);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("employees", employeeList);
        result.put("total", employeeList.size());
        return result;
    }

    @Override
    public Map<String, Object> getActiveShopById(Long shopId) {
        Shop shop = shopMapper.selectShopById(shopId);
        if (shop == null || shop.getStatus() != 1) {
            throw new ShopException("店铺不存在或已关闭");
        }
        ShopInfoDTO shopInfoDTO = null;
        if (shop.getShopInfoId() != null) {
            ShopInfo shopInfo = shopInfoService.getById(shop.getShopInfoId());
            if (shopInfo != null) {
                shopInfoDTO = new ShopInfoDTO(shopInfo.getId(), shopInfo.getName(), shopInfo.getDescription(), shopInfo.getLogoUrl());
            }
        }
        return Map.of("shop", shop, "shopInfo", shopInfoDTO);
    }

    @Override
    public Map<String, Object> getUserShopList(int page, int size) {
        List<Shop> shops = getActiveShops(page, size);
        int total = countActiveShops();
        Map<String, Object> result = new HashMap<>();
        result.put("shops", shops);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    @Override
    public Map<String, Object> getUserShopProducts(Long shopId, int page, int size) {
        getActiveShopById(shopId);
        return getShopProductsWithPagination(shopId, page, size);
    }

    @Override
    public Map<String, Object> getUserShopProductDetail(Long shopId, Long productId) {
        getActiveShopById(shopId);
        ProductDTO product = getProductDetailByShop(shopId, productId);
        return Map.of("product", product);
    }

    private void checkShopOwner(Long userId, Long shopId) {
        if (merchantRoleService.selectByMerchantShopAndRole(userId, shopId, 1) == null) {
            throw new ShopException("仅店长可操作");
        }
    }

    private void checkShopAccess(Long userId, Long shopId) {
        if (merchantRoleService.selectByMerchantAndShop(userId, shopId) == null) {
            throw new ShopException("无权限访问该店铺");
        }
    }
}