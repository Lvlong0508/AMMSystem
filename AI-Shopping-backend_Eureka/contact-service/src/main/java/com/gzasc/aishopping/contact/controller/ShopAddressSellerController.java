package com.gzasc.aishopping.contact.controller;

import com.gzasc.aishopping.contact.model.ShopAddress;
import com.gzasc.aishopping.contact.service.ShopAddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/seller/address")
@RequiredArgsConstructor
public class ShopAddressSellerController {

    private final ShopAddressService shopAddressService;

    @GetMapping("/list")
    public Map<String, Object> getAddressList(
            @RequestHeader(value = "X-Shop-Id", required = false) String shopId) {
        if (shopId == null || shopId.trim().isEmpty()) {
            return Map.of("message", "查询地址错误：未获取到店铺ID");
        }
        try {
            List<ShopAddress> addresses = shopAddressService.getAddressesByShopId(shopId);
            return Map.of("message", "查询成功", "data", addresses, "total", addresses.size());
        } catch (Exception e) {
            return Map.of("message", "查询地址列表错误：" + e.getMessage());
        }
    }

    @PostMapping("/add")
    public Map<String, Object> addAddress(
            @RequestBody ShopAddress address,
            @RequestHeader(value = "X-Shop-Id", required = false) String shopId) {
        if (shopId == null || shopId.trim().isEmpty()) {
            return Map.of("message", "新增地址错误：未获取到店铺ID");
        }
        if (address.getName() == null || address.getPhone() == null || address.getAddress() == null) {
            return Map.of("message", "新增地址错误：信息不完整");
        }
        if (address.getAddressType() != 1 && address.getAddressType() != 2) {
            address.setAddressType(1);
        }
        if (address.getIsDefault() != 1) {
            address.setIsDefault(0);
        }
        try {
            int rows = shopAddressService.createAddress(address, shopId);
            if (rows > 0) {
                return Map.of("message", "新增成功", "data", address);
            } else {
                return Map.of("message", "新增失败");
            }
        } catch (Exception e) {
            return Map.of("message", "新增地址错误：" + e.getMessage());
        }
    }

    @PutMapping("/update/{id}")
    public Map<String, Object> updateAddress(
            @PathVariable("id") int id,
            @RequestBody ShopAddress address,
            @RequestHeader(value = "X-Shop-Id", required = false) String shopId) {
        if (shopId == null || shopId.trim().isEmpty()) {
            return Map.of("message", "修改地址错误：未获取到店铺ID");
        }
        address.setId(id);
        if (address.getName() == null || address.getPhone() == null || address.getAddress() == null) {
            return Map.of("message", "修改地址错误：信息不完整");
        }
        try {
            int rows = shopAddressService.updateAddress(address, shopId);
            if (rows > 0) {
                return Map.of("message", "修改成功");
            } else {
                return Map.of("message", "修改失败：地址不存在或不属于该店铺");
            }
        } catch (Exception e) {
            return Map.of("message", "修改地址错误：" + e.getMessage());
        }
    }

    @DeleteMapping("/delete/{id}")
    public Map<String, Object> deleteAddress(
            @PathVariable("id") int id,
            @RequestHeader(value = "X-Shop-Id", required = false) String shopId) {
        if (shopId == null || shopId.trim().isEmpty()) {
            return Map.of("message", "删除地址错误：未获取到店铺ID");
        }
        try {
            int rows = shopAddressService.deleteAddress(id, shopId);
            if (rows > 0) {
                return Map.of("message", "删除成功");
            } else {
                return Map.of("message", "删除失败：地址不存在或不属于该店铺");
            }
        } catch (Exception e) {
            return Map.of("message", "删除地址错误：" + e.getMessage());
        }
    }

    @PutMapping("/set-default/{id}")
    public Map<String, Object> setDefaultAddress(
            @PathVariable("id") int id,
            @RequestHeader(value = "X-Shop-Id", required = false) String shopId) {
        if (shopId == null || shopId.trim().isEmpty()) {
            return Map.of("message", "设置默认地址错误：未获取到店铺ID");
        }
        try {
            int rows = shopAddressService.setDefaultAddress(id, shopId);
            if (rows > 0) {
                return Map.of("message", "设置成功");
            } else {
                return Map.of("message", "设置失败：地址不存在或不属于该店铺");
            }
        } catch (Exception e) {
            return Map.of("message", "设置默认地址错误：" + e.getMessage());
        }
    }
}