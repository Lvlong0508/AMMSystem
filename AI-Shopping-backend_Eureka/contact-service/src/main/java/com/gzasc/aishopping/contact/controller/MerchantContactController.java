package com.gzasc.aishopping.contact.controller;

import com.gzasc.aishopping.contact.model.ShopAddress;
import com.gzasc.aishopping.contact.model.dto.*;
import com.gzasc.aishopping.contact.service.ShopAddressService;
import com.gzasc.aishopping.contact.service.impl.ContactException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
    public ApiResponse<AddressResponse> createAddress(
            @RequestBody @Valid CreateAddressRequest request,
            @RequestHeader(value = "X-Shop-Id", required = false) String shopId) {
        if (shopId == null || shopId.trim().isEmpty()) {
            throw new ContactException("新增地址错误：未获取到店铺ID");
        }
        ShopAddress address = toShopAddress(request);
        shopAddressService.createAddress(address, shopId);
        return ApiResponse.success("新增成功", AddressResponse.fromShopAddress(address));
    }

    @PutMapping("/update/{id}")
    public ApiResponse<Void> updateAddress(
            @PathVariable("id") int id,
            @RequestBody @Valid UpdateAddressRequest request,
            @RequestHeader(value = "X-Shop-Id", required = false) String shopId) {
        if (shopId == null || shopId.trim().isEmpty()) {
            throw new ContactException("修改地址错误：未获取到店铺ID");
        }
        ShopAddress address = toShopAddress(request);
        address.setId(id);
        int rows = shopAddressService.updateAddress(address, shopId);
        if (rows > 0) {
            return ApiResponse.success("修改成功");
        }
        throw new ContactException("修改失败：地址不存在或不属于该店铺");
    }

    @DeleteMapping("/delete/{id}")
    public ApiResponse<Void> deleteAddress(
            @PathVariable("id") int id,
            @RequestHeader(value = "X-Shop-Id", required = false) String shopId) {
        if (shopId == null || shopId.trim().isEmpty()) {
            throw new ContactException("删除地址错误：未获取到店铺ID");
        }
        int rows = shopAddressService.deleteAddress(id, shopId);
        if (rows > 0) {
            return ApiResponse.success("删除成功");
        }
        throw new ContactException("删除失败：地址不存在或不属于该店铺");
    }

    @GetMapping("/list")
    public ApiResponse<Map<String, Object>> getAddressList(
            @RequestHeader(value = "X-Shop-Id", required = false) String shopId) {
        if (shopId == null || shopId.trim().isEmpty()) {
            throw new ContactException("查询地址错误：未获取到店铺ID");
        }
        List<ShopAddress> addresses = shopAddressService.getAddressesByShopId(shopId);
        List<AddressResponse> data = addresses.stream()
                .map(AddressResponse::fromShopAddress)
                .collect(Collectors.toList());
        return ApiResponse.success("查询成功", Map.of("addresses", data, "total", data.size()));
    }

    @GetMapping("/ship-list")
    public ApiResponse<Map<String, Object>> getShipAddressList(
            @RequestHeader(value = "X-Shop-Id", required = false) String shopId) {
        if (shopId == null || shopId.trim().isEmpty()) {
            throw new ContactException("查询发货地址错误：未获取到店铺ID");
        }
        List<ShopAddress> addresses = shopAddressService.getShipAddressesByShopId(shopId);
        List<AddressResponse> data = addresses.stream()
                .map(AddressResponse::fromShopAddress)
                .collect(Collectors.toList());
        return ApiResponse.success("查询成功", Map.of("addresses", data, "total", data.size()));
    }

    @PutMapping("/set-default/{id}")
    public ApiResponse<Void> setDefaultAddress(
            @PathVariable("id") int id,
            @RequestHeader(value = "X-Shop-Id", required = false) String shopId) {
        if (shopId == null || shopId.trim().isEmpty()) {
            throw new ContactException("设置默认地址错误：未获取到店铺ID");
        }
        int rows = shopAddressService.setDefaultAddress(id, shopId);
        if (rows > 0) {
            return ApiResponse.success("设置成功");
        }
        throw new ContactException("设置失败：地址不存在或不属于该店铺");
    }

    private ShopAddress toShopAddress(CreateAddressRequest request) {
        ShopAddress address = new ShopAddress();
        address.setName(request.getName());
        address.setPhone(request.getPhone());
        address.setAddress(request.getAddress());
        address.setAddressType(request.getAddressType() != null ? request.getAddressType() : 1);
        address.setIsDefault(request.getIsDefault() != null && request.getIsDefault() == 1 ? 1 : 0);
        return address;
    }

    private ShopAddress toShopAddress(UpdateAddressRequest request) {
        ShopAddress address = new ShopAddress();
        address.setName(request.getName());
        address.setPhone(request.getPhone());
        address.setAddress(request.getAddress());
        address.setAddressType(request.getAddressType());
        address.setIsDefault(request.getIsDefault() != null && request.getIsDefault() == 1 ? 1 : 0);
        return address;
    }
}