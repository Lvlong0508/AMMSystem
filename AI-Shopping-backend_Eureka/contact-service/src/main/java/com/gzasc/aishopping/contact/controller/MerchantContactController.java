package com.gzasc.aishopping.contact.controller;

import com.gzasc.aishopping.contact.dto.AddressResponse;
import com.gzasc.aishopping.contact.dto.CreateAddressRequest;
import com.gzasc.aishopping.contact.dto.UpdateAddressRequest;
import com.gzasc.aishopping.contact.model.ShopAddress;
import com.gzasc.aishopping.contact.service.ShopAddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/merchant/address")
@RequiredArgsConstructor
public class MerchantContactController {

    private final ShopAddressService shopAddressService;

    @PostMapping("/create")
    public Map<String, Object> createAddress(
            @RequestBody @Valid CreateAddressRequest request,
            BindingResult bindingResult,
            @RequestHeader(value = "X-Shop-Id", required = false) String shopIdStr) {
        String shopId = getShopId(shopIdStr);
        if (shopId == null) {
            return Map.of("code", 401, "message", "未获取到店铺ID");
        }
        if (bindingResult.hasErrors()) {
            return Map.of("code", 400, "message", "参数错误：" + bindingResult.getFieldError().getDefaultMessage());
        }

        ShopAddress address = toShopAddress(request);
        int id = shopAddressService.createAddress(address, shopId);
        return Map.of("code", 200, "message", "新增成功", "data", Map.of("id", id));
    }

    @PutMapping("/update/{id}")
    public Map<String, Object> updateAddress(
            @PathVariable("id") int id,
            @RequestBody @Valid UpdateAddressRequest request,
            BindingResult bindingResult,
            @RequestHeader(value = "X-Shop-Id", required = false) String shopIdStr) {
        String shopId = getShopId(shopIdStr);
        if (shopId == null) {
            return Map.of("code", 401, "message", "未获取到店铺ID");
        }
        if (bindingResult.hasErrors()) {
            return Map.of("code", 400, "message", "参数错误：" + bindingResult.getFieldError().getDefaultMessage());
        }

        ShopAddress address = toShopAddress(request);
        address.setId(id);
        int rows = shopAddressService.updateAddress(address, shopId);
        if (rows > 0) {
            return Map.of("code", 200, "message", "修改成功");
        }
        return Map.of("code", 400, "message", "修改失败：地址不存在或不属于该店铺");
    }

    @DeleteMapping("/delete/{id}")
    public Map<String, Object> deleteAddress(
            @PathVariable("id") int id,
            @RequestHeader(value = "X-Shop-Id", required = false) String shopIdStr) {
        String shopId = getShopId(shopIdStr);
        if (shopId == null) {
            return Map.of("code", 401, "message", "未获取到店铺ID");
        }

        int rows = shopAddressService.deleteAddress(id, shopId);
        if (rows > 0) {
            return Map.of("code", 200, "message", "删除成功");
        }
        return Map.of("code", 400, "message", "删除失败：地址不存在或不属于该店铺");
    }

    @GetMapping("/list")
    public Map<String, Object> getAddressList(
            @RequestHeader(value = "X-Shop-Id", required = false) String shopIdStr) {
        String shopId = getShopId(shopIdStr);
        if (shopId == null) {
            return Map.of("code", 401, "message", "未获取到店铺ID");
        }

        List<ShopAddress> addresses = shopAddressService.getAddressesByShopId(shopId);
        List<AddressResponse> data = addresses.stream()
                .map(AddressResponse::fromShopAddress)
                .collect(Collectors.toList());
        return Map.of("code", 200, "message", "查询成功", "data", Map.of("addresses", data, "total", data.size()));
    }

    @GetMapping("/ship-default")
    public Map<String, Object> getDefaultShipAddress(
            @RequestHeader(value = "X-Shop-Id", required = false) String shopIdStr) {
        String shopId = getShopId(shopIdStr);
        if (shopId == null) {
            return Map.of("code", 401, "message", "未获取到店铺ID");
        }

        ShopAddress address = shopAddressService.getDefaultShipAddressByShopId(shopId);
        return Map.of("code", 200, "message", "查询成功", "data",
                address != null ? AddressResponse.fromShopAddress(address) : null);
    }

    @PutMapping("/set-default/{id}")
    public Map<String, Object> setDefaultAddress(
            @PathVariable("id") int id,
            @RequestHeader(value = "X-Shop-Id", required = false) String shopIdStr) {
        String shopId = getShopId(shopIdStr);
        if (shopId == null) {
            return Map.of("code", 401, "message", "未获取到店铺ID");
        }

        int rows = shopAddressService.setDefaultAddress(id, shopId);
        if (rows > 0) {
            return Map.of("code", 200, "message", "设置成功");
        }
        return Map.of("code", 400, "message", "设置失败：地址不存在或不属于该店铺");
    }

    private String getShopId(String shopIdStr) {
        if (shopIdStr == null || shopIdStr.trim().isEmpty()) {
            return null;
        }
        return shopIdStr;
    }

    private ShopAddress toShopAddress(CreateAddressRequest request) {
        return toShopAddress(request.getName(), request.getPhone(), request.getAddress(),
                request.getAddressType() != null ? request.getAddressType() : 1,
                request.getIsDefault() != null && request.getIsDefault() == 1 ? 1 : 0);
    }

    private ShopAddress toShopAddress(UpdateAddressRequest request) {
        return toShopAddress(request.getName(), request.getPhone(), request.getAddress(),
                request.getAddressType(),
                request.getIsDefault() != null && request.getIsDefault() == 1 ? 1 : 0);
    }

    private ShopAddress toShopAddress(String name, String phone, String address, Integer addressType, Integer isDefault) {
        ShopAddress shopAddress = new ShopAddress();
        shopAddress.setName(name);
        shopAddress.setPhone(phone);
        shopAddress.setAddress(address);
        shopAddress.setAddressType(addressType);
        shopAddress.setIsDefault(isDefault);
        return shopAddress;
    }
}