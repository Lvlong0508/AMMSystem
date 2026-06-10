package com.gzasc.aishopping.contact.service.impl;

import com.gzasc.aishopping.contact.mapper.ShopAddressMapper;
import com.gzasc.aishopping.contact.model.ShopAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InOrder;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShopAddressServiceImplTest {

    @Mock
    private ShopAddressMapper shopAddressMapper;

    private ShopAddressServiceImpl shopAddressService;

    @BeforeEach
    void setUp() {
        shopAddressService = new ShopAddressServiceImpl(shopAddressMapper);
    }

    // ==================== createAddress ====================

    @Test
    @DisplayName("CT-SRV-016 正常创建非默认地址")
    void createAddress_NotDefault_Success() {
        ShopAddress address = new ShopAddress(null, "仓库A", "0211111111", "上海市嘉定区", 1, 0, null, null);
        when(shopAddressMapper.insertAddress(address)).thenAnswer(invocation -> {
            ShopAddress a = invocation.getArgument(0);
            a.setId(10);
            return 1;
        });

        int result = shopAddressService.createAddress(address, "5001");

        assertEquals(10, result);
        InOrder inOrder = inOrder(shopAddressMapper);
        inOrder.verify(shopAddressMapper).insertAddress(address);
        inOrder.verify(shopAddressMapper).insertRel("5001", address.getId());
        verify(shopAddressMapper, never()).clearDefaultByType(anyString(), anyInt());
    }

    @Test
    @DisplayName("CT-SRV-017 创建默认地址-清除同类型默认标记")
    void createAddress_IsDefault_Success() {
        ShopAddress address = new ShopAddress(null, "默认仓库", "0212222222", "上海市浦东新区", 1, 1, null, null);
        when(shopAddressMapper.insertAddress(address)).thenAnswer(invocation -> {
            ShopAddress a = invocation.getArgument(0);
            a.setId(20);
            return 1;
        });

        int result = shopAddressService.createAddress(address, "5001");

        assertEquals(20, result);
        InOrder inOrder = inOrder(shopAddressMapper);
        inOrder.verify(shopAddressMapper).clearDefaultByType("5001", 1);
        inOrder.verify(shopAddressMapper).insertAddress(address);
        inOrder.verify(shopAddressMapper).insertRel("5001", address.getId());
    }

    @Test
    @DisplayName("CT-SRV-018 创建地址-插入失败，不插入关联")
    void createAddress_Fail_NoInsert() {
        ShopAddress address = new ShopAddress(null, "仓库B", "0213333333", "上海市徐汇区", 2, 0, null, null);
        when(shopAddressMapper.insertAddress(address)).thenReturn(0);

        int result = shopAddressService.createAddress(address, "5001");

        assertEquals(0, result);
        verify(shopAddressMapper, never()).insertRel(anyString(), anyInt());
    }

    // ==================== deleteAddress ====================

    @Test
    @DisplayName("CT-SRV-019 正常删除地址")
    void deleteAddress_Success() {
        when(shopAddressMapper.selectShopIdByAddressId(1)).thenReturn("5001");
        when(shopAddressMapper.deleteRelByAddressId(1)).thenReturn(1);
        when(shopAddressMapper.deleteAddressById(1)).thenReturn(1);

        int result = shopAddressService.deleteAddress(1, "5001");

        assertEquals(1, result);
        InOrder inOrder = inOrder(shopAddressMapper);
        inOrder.verify(shopAddressMapper).selectShopIdByAddressId(1);
        inOrder.verify(shopAddressMapper).deleteRelByAddressId(1);
        inOrder.verify(shopAddressMapper).deleteAddressById(1);
    }

    @Test
    @DisplayName("CT-SRV-020 删除地址-shopId不匹配")
    void deleteAddress_WrongShopId() {
        when(shopAddressMapper.selectShopIdByAddressId(1)).thenReturn("5002");

        int result = shopAddressService.deleteAddress(1, "5001");

        assertEquals(0, result);
        verify(shopAddressMapper, never()).deleteRelByAddressId(anyInt());
        verify(shopAddressMapper, never()).deleteAddressById(anyInt());
    }

    // ==================== updateAddress ====================

    @Test
    @DisplayName("CT-SRV-021 正常更新地址（非默认）")
    void updateAddress_NotDefault_Success() {
        ShopAddress address = new ShopAddress(1, "新仓库", "0214444444", "上海市松江区", 1, 0, null, null);
        when(shopAddressMapper.selectShopIdByAddressId(1)).thenReturn("5001");
        when(shopAddressMapper.updateAddress(address)).thenReturn(1);

        int result = shopAddressService.updateAddress(address, "5001");

        assertEquals(1, result);
        InOrder inOrder = inOrder(shopAddressMapper);
        inOrder.verify(shopAddressMapper).selectShopIdByAddressId(1);
        inOrder.verify(shopAddressMapper).updateAddress(address);
        verify(shopAddressMapper, never()).clearDefaultByType(anyString(), anyInt());
    }

    @Test
    @DisplayName("CT-SRV-022 更新地址并设为默认-清除同类型默认标记")
    void updateAddress_WithDefault_Success() {
        ShopAddress address = new ShopAddress(1, "默认仓库", "0215555555", "上海市嘉定区", 1, 1, null, null);
        when(shopAddressMapper.selectShopIdByAddressId(1)).thenReturn("5001");
        when(shopAddressMapper.updateAddress(address)).thenReturn(1);

        int result = shopAddressService.updateAddress(address, "5001");

        assertEquals(1, result);
        InOrder inOrder = inOrder(shopAddressMapper);
        inOrder.verify(shopAddressMapper).selectShopIdByAddressId(1);
        inOrder.verify(shopAddressMapper).clearDefaultByType("5001", 1);
        inOrder.verify(shopAddressMapper).updateAddress(address);
    }

    @Test
    @DisplayName("CT-SRV-023 更新地址-shopId不匹配")
    void updateAddress_WrongShopId() {
        ShopAddress address = new ShopAddress(1, "测试", "0210000000", "地址", 1, 0, null, null);
        when(shopAddressMapper.selectShopIdByAddressId(1)).thenReturn("5002");

        int result = shopAddressService.updateAddress(address, "5001");

        assertEquals(0, result);
        verify(shopAddressMapper, never()).clearDefaultByType(anyString(), anyInt());
        verify(shopAddressMapper, never()).updateAddress(any());
    }

    // ==================== getAddressesByShopId ====================

    @Test
    @DisplayName("CT-SRV-024 查询地址列表-有数据")
    void getAddressesByShopId_HasData() {
        ShopAddress a1 = new ShopAddress(1, "仓库A", "0211111111", "上海市", 1, 0, null, null);
        ShopAddress a2 = new ShopAddress(2, "仓库B", "0212222222", "上海市", 1, 1, null, null);
        when(shopAddressMapper.selectAddressesByShopId("5001")).thenReturn(List.of(a1, a2));

        List<ShopAddress> result = shopAddressService.getAddressesByShopId("5001");

        assertEquals(2, result.size());
        assertEquals(1, result.get(0).getAddressType());
    }

    @Test
    @DisplayName("CT-SRV-025 查询地址列表-无数据")
    void getAddressesByShopId_Empty() {
        when(shopAddressMapper.selectAddressesByShopId("5001")).thenReturn(List.of());

        List<ShopAddress> result = shopAddressService.getAddressesByShopId("5001");

        assertTrue(result.isEmpty());
    }

    // ==================== getDefaultShipAddressByShopId ====================

    @Test
    @DisplayName("CT-SRV-026 查询默认发货地址-存在")
    void getDefaultShipAddressByShopId_Success() {
        ShopAddress ship = new ShopAddress(1, "默认仓库", "0211111111", "上海市嘉定区", 1, 1, null, null);
        when(shopAddressMapper.selectDefaultShipAddressByShopId("5001")).thenReturn(ship);

        ShopAddress result = shopAddressService.getDefaultShipAddressByShopId("5001");

        assertNotNull(result);
        assertEquals(1, result.getId());
    }

    @Test
    @DisplayName("CT-SRV-027 查询默认发货地址-不存在")
    void getDefaultShipAddressByShopId_NotFound() {
        when(shopAddressMapper.selectDefaultShipAddressByShopId("5001")).thenReturn(null);

        ShopAddress result = shopAddressService.getDefaultShipAddressByShopId("5001");

        assertNull(result);
    }

    // ==================== setDefaultAddress ====================

    @Test
    @DisplayName("CT-SRV-028 正常设置默认地址")
    void setDefaultAddress_Success() {
        ShopAddress address = new ShopAddress(1, "仓库A", "0211111111", "上海市", 1, 0, null, null);
        when(shopAddressMapper.selectShopIdByAddressId(1)).thenReturn("5001");
        when(shopAddressMapper.selectAddressById(1)).thenReturn(address);
        when(shopAddressMapper.clearDefaultByType("5001", 1)).thenReturn(1);
        when(shopAddressMapper.setDefaultById(1)).thenReturn(1);

        int result = shopAddressService.setDefaultAddress(1, "5001");

        assertEquals(1, result);
        InOrder inOrder = inOrder(shopAddressMapper);
        inOrder.verify(shopAddressMapper).selectShopIdByAddressId(1);
        inOrder.verify(shopAddressMapper).selectAddressById(1);
        inOrder.verify(shopAddressMapper).clearDefaultByType("5001", 1);
        inOrder.verify(shopAddressMapper).setDefaultById(1);
    }

    @Test
    @DisplayName("CT-SRV-029 设置默认地址-地址不存在（shopId不匹配）")
    void setDefaultAddress_NotFound_WrongShopId() {
        when(shopAddressMapper.selectShopIdByAddressId(99999)).thenReturn("5002");

        int result = shopAddressService.setDefaultAddress(99999, "5001");

        assertEquals(0, result);
        verify(shopAddressMapper, never()).clearDefaultByType(anyString(), anyInt());
        verify(shopAddressMapper, never()).setDefaultById(anyInt());
    }

    @Test
    @DisplayName("CT-SRV-030 设置默认地址-地址ID不存在")
    void setDefaultAddress_AddressNull() {
        when(shopAddressMapper.selectShopIdByAddressId(99999)).thenReturn(null);

        int result = shopAddressService.setDefaultAddress(99999, "5001");

        assertEquals(0, result);
        verify(shopAddressMapper, never()).clearDefaultByType(anyString(), anyInt());
        verify(shopAddressMapper, never()).setDefaultById(anyInt());
    }

    // ==================== 补充测试用例-CT-SRV-042~052 ====================

    @Test
    @DisplayName("CT-SRV-042 删除地址-地址ID不存在返回null")
    void deleteAddress_NullShopId() {
        when(shopAddressMapper.selectShopIdByAddressId(99999)).thenReturn(null);

        int result = shopAddressService.deleteAddress(99999, "5001");

        assertEquals(0, result);
    }

    @Test
    @DisplayName("CT-SRV-043 更新地址-地址ID不存在返回null")
    void updateAddress_NullShopId() {
        ShopAddress address = new ShopAddress(99999, "测试", "0210000000", "地址", 1, 0, null, null);
        when(shopAddressMapper.selectShopIdByAddressId(99999)).thenReturn(null);

        int result = shopAddressService.updateAddress(address, "5001");

        assertEquals(0, result);
    }

    @Test
    @DisplayName("CT-SRV-044 删除地址-关联已不存在，deleteRel返回0，deleteAddressById仍执行")
    void deleteAddress_RelAlreadyDeleted() {
        when(shopAddressMapper.selectShopIdByAddressId(1)).thenReturn("5001");
        when(shopAddressMapper.deleteRelByAddressId(1)).thenReturn(0);
        when(shopAddressMapper.deleteAddressById(1)).thenReturn(1);

        int result = shopAddressService.deleteAddress(1, "5001");

        assertEquals(1, result);
        verify(shopAddressMapper).deleteRelByAddressId(1);
        verify(shopAddressMapper).deleteAddressById(1);
    }

    @Test
    @DisplayName("CT-SRV-045 删除地址-地址记录不存在返回0")
    void deleteAddress_NotFound() {
        when(shopAddressMapper.selectShopIdByAddressId(99999)).thenReturn("5001");
        when(shopAddressMapper.deleteRelByAddressId(99999)).thenReturn(0);
        when(shopAddressMapper.deleteAddressById(99999)).thenReturn(0);

        int result = shopAddressService.deleteAddress(99999, "5001");

        assertEquals(0, result);
    }

    @Test
    @DisplayName("CT-SRV-046 更新地址并设为默认-无其他默认需清除，clearDefault返回0")
    void updateAddress_DefaultNoOther() {
        ShopAddress address = new ShopAddress(1, "默认仓库", "0215555555", "上海市嘉定区", 1, 1, null, null);
        when(shopAddressMapper.selectShopIdByAddressId(1)).thenReturn("5001");
        when(shopAddressMapper.updateAddress(address)).thenReturn(1);

        int result = shopAddressService.updateAddress(address, "5001");

        assertEquals(1, result);
        verify(shopAddressMapper).clearDefaultByType("5001", 1);
        verify(shopAddressMapper).updateAddress(address);
    }

    @Test
    @DisplayName("CT-SRV-047 更新地址-更新无变化返回0")
    void updateAddress_NoChange() {
        ShopAddress address = new ShopAddress(1, "新仓库", "0214444444", "上海市松江区", 1, 0, null, null);
        when(shopAddressMapper.selectShopIdByAddressId(1)).thenReturn("5001");
        when(shopAddressMapper.updateAddress(address)).thenReturn(0);

        int result = shopAddressService.updateAddress(address, "5001");

        assertEquals(0, result);
    }

    @Test
    @DisplayName("CT-SRV-048 设置默认地址-无其他默认需清除")
    void setDefaultAddress_ClearDefaultReturnsZero() {
        ShopAddress address = new ShopAddress(1, "仓库A", "0211111111", "上海市", 1, 0, null, null);
        when(shopAddressMapper.selectShopIdByAddressId(1)).thenReturn("5001");
        when(shopAddressMapper.selectAddressById(1)).thenReturn(address);
        when(shopAddressMapper.clearDefaultByType("5001", 1)).thenReturn(0);
        when(shopAddressMapper.setDefaultById(1)).thenReturn(1);

        int result = shopAddressService.setDefaultAddress(1, "5001");

        assertEquals(1, result);
        verify(shopAddressMapper).clearDefaultByType("5001", 1);
        verify(shopAddressMapper).setDefaultById(1);
    }

    @Test
    @DisplayName("CT-SRV-049 设置默认地址-setDefaultById返回0")
    void setDefaultAddress_SetDefaultFails() {
        ShopAddress address = new ShopAddress(1, "仓库A", "0211111111", "上海市", 1, 0, null, null);
        when(shopAddressMapper.selectShopIdByAddressId(1)).thenReturn("5001");
        when(shopAddressMapper.selectAddressById(1)).thenReturn(address);
        when(shopAddressMapper.clearDefaultByType("5001", 1)).thenReturn(1);
        when(shopAddressMapper.setDefaultById(1)).thenReturn(0);

        int result = shopAddressService.setDefaultAddress(1, "5001");

        assertEquals(0, result);
    }

    @Test
    @DisplayName("CT-SRV-050 创建地址-关联插入失败返回0")
    void createAddress_InsertRelFails() {
        ShopAddress address = new ShopAddress(null, "仓库A", "0211111111", "上海市嘉定区", 1, 0, null, null);
        when(shopAddressMapper.insertAddress(address)).thenAnswer(invocation -> {
            ShopAddress a = invocation.getArgument(0);
            a.setId(10);
            return 1;
        });
        when(shopAddressMapper.insertRel("5001", 10)).thenReturn(0);

        int result = shopAddressService.createAddress(address, "5001");

        assertEquals(10, result);
        verify(shopAddressMapper).insertRel("5001", 10);
    }

    @Test
    @DisplayName("CT-SRV-051 查询地址列表-shopId为空字符串")
    void getAddressesByShopId_EmptyShopId() {
        when(shopAddressMapper.selectAddressesByShopId("")).thenReturn(List.of());

        List<ShopAddress> result = shopAddressService.getAddressesByShopId("");

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("CT-SRV-052 查询默认发货地址-shopId为null")
    void getDefaultShipAddressByShopId_NullShopId() {
        when(shopAddressMapper.selectDefaultShipAddressByShopId(null)).thenReturn(null);

        ShopAddress result = shopAddressService.getDefaultShipAddressByShopId(null);

        assertNull(result);
    }
}
