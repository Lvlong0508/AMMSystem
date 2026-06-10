package com.gzasc.aishopping.contact.dto;

import com.gzasc.aishopping.contact.model.ShopAddress;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AddressResponseTest {

    @Test
    @DisplayName("CT-SRV-034 fromShopAddress-正常映射所有字段")
    void fromShopAddress_Normal() {
        LocalDateTime now = LocalDateTime.now();
        ShopAddress address = new ShopAddress(1, "仓库A", "0211111111", "上海市嘉定区", 1, 1, now, now);

        AddressResponse response = AddressResponse.fromShopAddress(address);

        assertNotNull(response);
        assertEquals(1, response.getId());
        assertEquals("仓库A", response.getName());
        assertEquals("0211111111", response.getPhone());
        assertEquals("上海市嘉定区", response.getAddress());
        assertEquals(1, response.getAddressType());
        assertEquals(1, response.getIsDefault());
        assertEquals(now, response.getCreatedAt());
        assertEquals(now, response.getUpdatedAt());
    }

    @Test
    @DisplayName("CT-SRV-035 fromShopAddress-输入为null返回null")
    void fromShopAddress_NullInput() {
        AddressResponse response = AddressResponse.fromShopAddress(null);

        assertNull(response);
    }

    @Test
    @DisplayName("CT-SRV-036 fromShopAddress-部分字段为null时正常处理")
    void fromShopAddress_PartialNullFields() {
        ShopAddress address = new ShopAddress(null, "仓库B", null, "地址", null, null, null, null);

        AddressResponse response = AddressResponse.fromShopAddress(address);

        assertNotNull(response);
        assertNull(response.getId());
        assertEquals("仓库B", response.getName());
        assertNull(response.getPhone());
        assertEquals("地址", response.getAddress());
        assertNull(response.getAddressType());
        assertNull(response.getIsDefault());
        assertNull(response.getCreatedAt());
        assertNull(response.getUpdatedAt());
    }
}
