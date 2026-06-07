package com.gzasc.aishopping.shop.service.impl;

import com.gzasc.aishopping.common.dto.shop.ShopInfoDTO;
import com.gzasc.aishopping.common.feign.auth.AuthFeignClient;
import com.gzasc.aishopping.common.response.ApiResponse;
import com.gzasc.aishopping.common.util.SafeIdGenerator;
import com.gzasc.aishopping.shop.dto.AddEmployeeRequest;
import com.gzasc.aishopping.shop.dto.CreateShopRequest;
import com.gzasc.aishopping.shop.dto.SimpleShopDTO;
import com.gzasc.aishopping.shop.dto.UpdateShopRequest;
import com.gzasc.aishopping.shop.exception.ShopException;
import com.gzasc.aishopping.shop.mapper.ShopMapper;
import com.gzasc.aishopping.shop.model.MerchantRole;
import com.gzasc.aishopping.shop.model.Shop;
import com.gzasc.aishopping.shop.model.ShopInfo;
import com.gzasc.aishopping.shop.service.MerchantRoleService;
import com.gzasc.aishopping.shop.service.ShopInfoService;
import com.gzasc.aishopping.shop.service.ShopService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShopServiceImpl implements ShopService {

    private final ShopMapper shopMapper;
    private final AuthFeignClient authFeignClient;
    private final MerchantRoleService merchantRoleService;
    private final ShopInfoService shopInfoService;

    @Override
    @Transactional
    public Shop createShop(CreateShopRequest request, Long userId) {
        ShopInfo shopInfo = new ShopInfo();
        shopInfo.setName(request.getName());
        shopInfo.setDescription(request.getDescription());
        shopInfo.setLogoUrl(request.getLogoId());
        shopInfoService.insert(shopInfo);

        Shop shop = new Shop();
        shop.setId(SafeIdGenerator.nextId());
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
            merchantRoleService.insert(merchantRole);
        }
        if (result <= 0) {
            throw new ShopException("创建店铺失败");
        }
        return shop;
    }

    @Override
    @Transactional
    public void updateShop(Long shopId, UpdateShopRequest request, Long userId) {
        checkShopOwner(userId, shopId);
        if (request.getName() != null && request.getName().trim().isEmpty()) {
            throw new ShopException("店铺名称不能为空");
        }
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
    @Transactional
    public void closeShop(Long shopId, Long userId) {
        checkShopOwner(userId, shopId);
        int result = shopMapper.closeShop(shopId);
        if (result <= 0) {
            throw new ShopException("店铺已关闭或不存在");
        }
    }

    @Override
    @Transactional
    public void openShop(Long shopId, Long userId) {
        checkShopOwner(userId, shopId);
        int result = shopMapper.openShop(shopId);
        if (result <= 0) {
            throw new ShopException("店铺已开启或不存在");
        }
    }

    @Override
    @Transactional
    public void addEmployee(Long shopId, AddEmployeeRequest request, Long userId) {
        checkShopOwner(userId, shopId);
        try {
            Map<String, Object> registerRequest = new HashMap<>();
            registerRequest.put("username", request.getUsername());
            if (request.getPassword() != null) registerRequest.put("password", request.getPassword());
            if (request.getPhone() != null) registerRequest.put("phone", request.getPhone());
            if (request.getName() != null) registerRequest.put("nickname", request.getName());

            ApiResponse<Map<String, Object>> registerResponse = authFeignClient.registerEmployee(registerRequest);
            Map<String, Object> registerResult = registerResponse != null ? registerResponse.getData() : null;
            if (registerResult == null) {
                throw new ShopException("注册店员账号失败");
            }
            Object employeeIdObj = registerResult.get("id");
            if (employeeIdObj == null) {
                throw new ShopException("注册店员账号返回数据异常: 缺少id");
            }
            Long employeeId = ((Number) employeeIdObj).longValue();

            MerchantRole merchantRole = new MerchantRole();
            merchantRole.setMerchantId(employeeId);
            merchantRole.setShopId(shopId);
            merchantRole.setRole(2);
            merchantRole.setAssignedBy(userId);
            merchantRoleService.insert(merchantRole);
        } catch (ShopException e) {
            throw e;
        } catch (Exception e) {
            log.error("添加员工失败, shopId={}, username={}", shopId, request.getUsername(), e);
            throw new ShopException("添加员工失败");
        }
    }

    @Override
    @Transactional
    public void removeEmployee(Long shopId, Long merchantId, Long userId) {
        checkShopOwner(userId, shopId);
        merchantRoleService.deleteByMerchantAndShop(merchantId, shopId);
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
        if (page < 1) {
            throw new ShopException("分页参数错误: page 必须 >= 1");
        }
        if (size < 1) {
            throw new ShopException("分页参数错误: size 必须 >= 1");
        }
        List<Shop> shops = getActiveShops(page, size);
        int total = countActiveShops();
        Map<String, Object> result = new HashMap<>();
        result.put("shops", shops);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    private int countActiveShops() {
        return shopMapper.countActiveShops();
    }

    private List<Shop> getActiveShops(int page, int size) {
        int offset = (page - 1) * size;
        return shopMapper.selectActiveShops(offset, size);
    }

    @Override
    public ShopInfoDTO getShopInfoById(Long shopId) {
        if (shopId == null) return null;
        Shop shop = shopMapper.selectShopById(shopId);
        if (shop == null || shop.getShopInfoId() == null) {
            return null;
        }
        ShopInfo shopInfo = shopInfoService.getById(shop.getShopInfoId());
        if (shopInfo == null) {
            return null;
        }
        return new ShopInfoDTO(shopInfo.getId(), shopInfo.getName(),
                shopInfo.getDescription(), shopInfo.getLogoUrl());
    }

    @Override
    public List<Long> getShopIdsByMerchantId(Long merchantId) {
        return merchantRoleService.selectByMerchantId(merchantId)
                .stream()
                .map(MerchantRole::getShopId)
                .toList();
    }

    @Override
    public List<SimpleShopDTO> getSimpleShop(Long userId) {
        return shopMapper.selectSimpleShopsByMerchantId(userId);
    }

    @Override
    public Map<Long, ShopInfoDTO> batchGetShopInfo(Set<Long> shopIds) {
        if (shopIds == null || shopIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Shop> shops = shopMapper.selectShopsByIds(shopIds);
        List<Long> infoIds = shops.stream()
                .map(Shop::getShopInfoId)
                .filter(Objects::nonNull)
                .toList();
        List<ShopInfo> shopInfos = shopInfoService.getByIds(infoIds);
        Map<Long, ShopInfo> infoMap = shopInfos.stream()
                .collect(Collectors.toMap(ShopInfo::getId, si -> si));
        Map<Long, ShopInfoDTO> result = new HashMap<>();
        for (Shop shop : shops) {
            if (shop.getShopInfoId() == null) continue;
            ShopInfo si = infoMap.get(shop.getShopInfoId());
            if (si == null) continue;
            result.put(shop.getId(), new ShopInfoDTO(si.getId(), si.getName(),
                    si.getDescription(), si.getLogoUrl()));
        }
        return result;
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