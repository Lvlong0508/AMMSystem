package com.gzasc.aishopping.contact.controller;

import com.gzasc.aishopping.contact.model.ShopAddress;
import com.gzasc.aishopping.contact.service.ShopAddressService;
import com.gzasc.aishopping.contact.service.impl.ContactException;
import jakarta.validation.Valid;
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
            throw new ContactException("查询地址错误：未获取到店铺ID");
        }
        List<ShopAddress> addresses = shopAddressService.getAddressesByShopId(shopId);
        return Map.of("code", 200, "message", "查询成功", "data", addresses, "total", addresses.size());
    }

    @GetMapping("/ship-list")
    public Map<String, Object> getShipAddressList(
            @RequestHeader(value = "X-Shop-Id", required = false) String shopId) {
        if (shopId == null || shopId.trim().isEmpty()) {
            throw new ContactException("查询发货地址错误：未获取到店铺ID");
        }
        List<ShopAddress> addresses = shopAddressService.getShipAddressesByShopId(shopId);
        return Map.of("code", 200, "message", "查询成功", "data", addresses, "total", addresses.size());
    }

    @PostMapping("/add")
    public Map<String, Object> addAddress(
            @RequestBody @Valid ShopAddress address,
            @RequestHeader(value = "X-Shop-Id", required = false) String shopId) {
        if (shopId == null || shopId.trim().isEmpty()) {
            throw new ContactException("新增地址错误：未获取到店铺ID");
        }
        if (address.getAddressType() == null || (address.getAddressType() != 1 && address.getAddressType() != 2)) {
            address.setAddressType(1);
        }
        if (address.getIsDefault() == null || address.getIsDefault() != 1) {
            address.setIsDefault(0);
        }
        int rows = shopAddressService.createAddress(address, shopId);
        if (rows > 0) {
            return Map.of("code", 200, "message", "新增成功", "data", address);
        }
        throw new ContactException("新增失败");
    }

    @PutMapping("/update/{id}")
    public Map<String, Object> updateAddress(
            @PathVariable("id") int id,
            @RequestBody @Valid ShopAddress address,
            @RequestHeader(value = "X-Shop-Id", required = false) String shopId) {
        if (shopId == null || shopId.trim().isEmpty()) {
            throw new ContactException("修改地址错误：未获取到店铺ID");
        }
        address.setId(id);
        int rows = shopAddressService.updateAddress(address, shopId);
        if (rows > 0) {
            return Map.of("code", 200, "message", "修改成功");
        }
        throw new ContactException("修改失败：地址不存在或不属于该店铺");
    }

    @DeleteMapping("/delete/{id}")
    public Map<String, Object> deleteAddress(
            @PathVariable("id") int id,
            @RequestHeader(value = "X-Shop-Id", required = false) String shopId) {
        if (shopId == null || shopId.trim().isEmpty()) {
            throw new ContactException("删除地址错误：未获取到店铺ID");
        }
        int rows = shopAddressService.deleteAddress(id, shopId);
        if (rows > 0) {
            return Map.of("code", 200, "message", "删除成功");
        }
        throw new ContactException("删除失败：地址不存在或不属于该店铺");
    }

    @PutMapping("/set-default/{id}")
    public Map<String, Object> setDefaultAddress(
            @PathVariable("id") int id,
            @RequestHeader(value = "X-Shop-Id", required = false) String shopId) {
        if (shopId == null || shopId.trim().isEmpty()) {
            throw new ContactException("设置默认地址错误：未获取到店铺ID");
        }
        int rows = shopAddressService.setDefaultAddress(id, shopId);
        if (rows > 0) {
            return Map.of("code", 200, "message", "设置成功");
        }
        throw new ContactException("设置失败：地址不存在或不属于该店铺");
    }
}