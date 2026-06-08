package com.gzasc.aishopping.shop.service.impl;

import com.gzasc.aishopping.common.dto.shop.ShopInfoDTO;
import com.gzasc.aishopping.common.response.ApiResponse;
import com.gzasc.aishopping.shop.dto.CreateShopRequest;
import com.gzasc.aishopping.shop.dto.SimpleShopDTO;
import com.gzasc.aishopping.shop.dto.UpdateShopRequest;
import com.gzasc.aishopping.shop.exception.ShopException;
import com.gzasc.aishopping.shop.mapper.ShopMapper;
import com.gzasc.aishopping.shop.model.Shop;
import com.gzasc.aishopping.shop.model.ShopInfo;
import com.gzasc.aishopping.shop.service.ShopInfoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShopServiceImplTest {

    @Mock
    private ShopMapper shopMapper;

    @Mock
    private ShopInfoService shopInfoService;

    @InjectMocks
    private ShopServiceImpl shopService;

    // ========== 店铺创建 ==========

    @Test
    @DisplayName("创建店铺成功 - 完整参数")
    void createShop_success() {
        CreateShopRequest request = new CreateShopRequest();
        request.setName("测试店铺");
        request.setDescription("这是一个测试店铺");
        request.setLogoId("logo-abc-123");
        Long userId = 1001L;

        when(shopMapper.insertShop(any())).thenReturn(1);

        Shop result = shopService.createShop(request, userId);

        assertNotNull(result.getId());
        assertEquals(userId, result.getMerchantId());
        assertEquals(1, result.getStatus());

        verify(shopInfoService).insert(argThat(si ->
                "测试店铺".equals(si.getName()) &&
                "这是一个测试店铺".equals(si.getDescription()) &&
                "logo-abc-123".equals(si.getLogoUrl())));
    }

    @Test
    @DisplayName("创建店铺成功 - 仅必填参数")
    void createShop_onlyRequired() {
        CreateShopRequest request = new CreateShopRequest();
        request.setName("最小店铺");
        Long userId = 1001L;

        when(shopMapper.insertShop(any())).thenReturn(1);

        shopService.createShop(request, userId);

        verify(shopInfoService).insert(argThat(si ->
                "最小店铺".equals(si.getName()) &&
                si.getDescription() == null &&
                si.getLogoUrl() == null));
    }

    @Test
    @DisplayName("insertShop返回0时抛出异常")
    void createShop_insertShopReturnsZero() {
        CreateShopRequest request = new CreateShopRequest();
        request.setName("测试");
        Long userId = 1001L;

        when(shopMapper.insertShop(any())).thenReturn(0);

        ShopException ex = assertThrows(ShopException.class,
                () -> shopService.createShop(request, userId));
        assertEquals("创建店铺失败", ex.getMessage());
    }

    // ========== 店铺管理 ==========

    @Test
    @DisplayName("更新店铺成功")
    void updateShop_success() {
        UpdateShopRequest request = new UpdateShopRequest();
        request.setName("新店铺名");
        request.setDescription("新描述");
        request.setLogoId("new-logo");
        Long userId = 1001L;
        Long shopId = 1L;

        Shop existingShop = new Shop(shopId, userId, 10L, 1, null, null);
        when(shopMapper.selectShopById(shopId)).thenReturn(existingShop);

        shopService.updateShop(shopId, request, userId);

        verify(shopInfoService).update(argThat(si ->
                "新店铺名".equals(si.getName()) &&
                "新描述".equals(si.getDescription()) &&
                "new-logo".equals(si.getLogoUrl())));
    }

    @Test
    @DisplayName("更新店铺 - 无权操作")
    void updateShop_notOwner() {
        UpdateShopRequest request = new UpdateShopRequest();
        request.setName("新店铺名");
        Long userId = 1002L;
        Long shopId = 1L;

        Shop existingShop = new Shop(shopId, 1001L, 10L, 1, null, null);
        when(shopMapper.selectShopById(shopId)).thenReturn(existingShop);

        ShopException ex = assertThrows(ShopException.class,
                () -> shopService.updateShop(shopId, request, userId));
        assertEquals("无权操作该店铺", ex.getMessage());
    }

    @Test
    @DisplayName("更新店铺 - 店铺不存在")
    void updateShop_notFound() {
        Long userId = 1001L;
        Long shopId = 999L;

        when(shopMapper.selectShopById(shopId)).thenReturn(null);

        ShopException ex = assertThrows(ShopException.class,
                () -> shopService.updateShop(shopId, new UpdateShopRequest(), userId));
        assertEquals("店铺不存在", ex.getMessage());
    }

    @Test
    @DisplayName("更新店铺 - 名称为空字符串")
    void updateShop_nameBlank() {
        UpdateShopRequest request = new UpdateShopRequest();
        request.setName("   ");
        Long userId = 1001L;
        Long shopId = 1L;

        when(shopMapper.selectShopById(shopId)).thenReturn(new Shop(shopId, userId, 10L, 1, null, null));

        ShopException ex = assertThrows(ShopException.class,
                () -> shopService.updateShop(shopId, request, userId));
        assertEquals("店铺名称不能为空", ex.getMessage());

        verify(shopInfoService, never()).update(any());
    }

    @Test
    @DisplayName("关闭店铺成功")
    void closeShop_success() {
        Long userId = 1001L;
        Long shopId = 1L;

        when(shopMapper.selectShopById(shopId)).thenReturn(new Shop(shopId, userId, null, 1, null, null));
        when(shopMapper.closeShop(shopId)).thenReturn(1);

        shopService.closeShop(shopId, userId);

        verify(shopMapper).closeShop(shopId);
    }

    @Test
    @DisplayName("关闭店铺 - 无权操作")
    void closeShop_notOwner() {
        Long userId = 1002L;
        Long shopId = 1L;

        when(shopMapper.selectShopById(shopId)).thenReturn(new Shop(shopId, 1001L, null, 1, null, null));

        ShopException ex = assertThrows(ShopException.class,
                () -> shopService.closeShop(shopId, userId));
        assertEquals("无权操作该店铺", ex.getMessage());
    }

    @Test
    @DisplayName("开启店铺成功")
    void openShop_success() {
        Long userId = 1001L;
        Long shopId = 1L;

        when(shopMapper.selectShopById(shopId)).thenReturn(new Shop(shopId, userId, null, 0, null, null));
        when(shopMapper.openShop(shopId)).thenReturn(1);

        shopService.openShop(shopId, userId);

        verify(shopMapper).openShop(shopId);
    }

    @Test
    @DisplayName("开启店铺 - 无权操作")
    void openShop_notOwner() {
        Long userId = 1002L;
        Long shopId = 1L;

        when(shopMapper.selectShopById(shopId)).thenReturn(new Shop(shopId, 1001L, null, 0, null, null));

        ShopException ex = assertThrows(ShopException.class,
                () -> shopService.openShop(shopId, userId));
        assertEquals("无权操作该店铺", ex.getMessage());
    }

    // ========== 查询（含权限检查） ==========

    @Test
    @DisplayName("有权限查看店铺")
    void getShopWithAccessCheck_success() {
        Long userId = 1001L;
        Long shopId = 1L;
        Shop shop = new Shop(shopId, userId, 10L, 1, null, null);

        when(shopMapper.selectShopById(shopId)).thenReturn(shop);

        Shop result = shopService.getShopWithAccessCheck(shopId, userId);

        assertNotNull(result);
        assertEquals(shopId, result.getId());
    }

    @Test
    @DisplayName("无权限查看店铺")
    void getShopWithAccessCheck_noAccess() {
        Long userId = 1002L;
        Long shopId = 1L;
        Shop shop = new Shop(shopId, 1001L, 10L, 1, null, null);

        when(shopMapper.selectShopById(shopId)).thenReturn(shop);

        ShopException ex = assertThrows(ShopException.class,
                () -> shopService.getShopWithAccessCheck(shopId, userId));
        assertEquals("无权限访问该店铺", ex.getMessage());
    }

    @Test
    @DisplayName("查询不存在的店铺")
    void getShopWithAccessCheck_notFound() {
        when(shopMapper.selectShopById(999L)).thenReturn(null);

        ShopException ex = assertThrows(ShopException.class,
                () -> shopService.getShopWithAccessCheck(999L, 1001L));
        assertEquals("店铺不存在", ex.getMessage());
    }

    // ========== 商家端查询（一对一） ==========

    @Test
    @DisplayName("查询我的店铺 - 有店铺")
    void getMyShop_found() {
        Long userId = 1001L;
        Shop shop = new Shop(1L, userId, 10L, 1, null, null);

        when(shopMapper.selectShopByMerchantId(userId)).thenReturn(shop);
        ShopInfo shopInfo = new ShopInfo(10L, "我的小店", "描述", "logo", null, null);
        when(shopInfoService.getById(10L)).thenReturn(shopInfo);

        SimpleShopDTO result = shopService.getMyShop(userId);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("我的小店", result.getName());
        assertEquals(1, result.getStatus());
    }

    @Test
    @DisplayName("查询我的店铺 - 无店铺")
    void getMyShop_notFound() {
        Long userId = 1001L;

        when(shopMapper.selectShopByMerchantId(userId)).thenReturn(null);

        SimpleShopDTO result = shopService.getMyShop(userId);

        assertNull(result);
    }

    // ========== 查询活跃店铺 ==========

    @Test
    @DisplayName("查询活跃店铺详情")
    void getActiveShopById_success() {
        Long shopId = 1L;
        Shop shop = new Shop(shopId, 1001L, 10L, 1, null, null);
        ShopInfo shopInfo = new ShopInfo(10L, "测试店铺", "测试描述", "logo", null, null);

        when(shopMapper.selectShopById(shopId)).thenReturn(shop);
        when(shopInfoService.getById(10L)).thenReturn(shopInfo);

        Map<String, Object> result = shopService.getActiveShopById(shopId);

        assertNotNull(result);
        assertEquals(shop, result.get("shop"));
        assertNotNull(result.get("shopInfo"));
    }

    @Test
    @DisplayName("查询已关闭的店铺抛出异常")
    void getActiveShopById_closed() {
        Long shopId = 1L;
        Shop shop = new Shop(shopId, 1001L, null, 0, null, null);

        when(shopMapper.selectShopById(shopId)).thenReturn(shop);

        ShopException ex = assertThrows(ShopException.class,
                () -> shopService.getActiveShopById(shopId));
        assertEquals("店铺不存在或已关闭", ex.getMessage());
    }

    // ========== 用户端分页查询 ==========

    @Test
    @DisplayName("分页查询活跃店铺")
    void getUserShopList_success() {
        when(shopMapper.selectActiveShops(0, 10)).thenReturn(List.of(
                new Shop(1L, 1001L, null, 1, null, null),
                new Shop(2L, 1002L, null, 1, null, null)
        ));
        when(shopMapper.countActiveShops()).thenReturn(2);

        Map<String, Object> result = shopService.getUserShopList(1, 10);

        assertEquals(2, result.get("total"));
        assertEquals(1, result.get("page"));
        assertEquals(10, result.get("size"));
        List<Shop> shops = (List<Shop>) result.get("shops");
        assertEquals(2, shops.size());
    }

    @Test
    @DisplayName("分页参数错误")
    void getUserShopList_invalidPage() {
        assertThrows(ShopException.class, () -> shopService.getUserShopList(0, 10));
        assertThrows(ShopException.class, () -> shopService.getUserShopList(1, 0));
    }

    // ========== 店铺信息查询 ==========

    @Test
    @DisplayName("查询店铺信息")
    void getShopInfoById() {
        Long shopId = 1L;
        Shop shop = new Shop(shopId, 1001L, 10L, 1, null, null);
        ShopInfo shopInfo = new ShopInfo(10L, "测试店铺", "测试描述", "logo", null, null);

        when(shopMapper.selectShopById(shopId)).thenReturn(shop);
        when(shopInfoService.getById(10L)).thenReturn(shopInfo);

        ShopInfoDTO result = shopService.getShopInfoById(shopId);

        assertNotNull(result);
        assertEquals("测试店铺", result.getName());
        assertEquals("测试描述", result.getDescription());
        assertEquals("logo", result.getLogoUrl());
    }

    @Test
    @DisplayName("查询店铺信息 - 无关联info或shopId为null")
    void getShopInfoById_noInfo() {
        Long shopId = 1L;

        when(shopMapper.selectShopById(shopId)).thenReturn(null);

        ShopInfoDTO result = shopService.getShopInfoById(shopId);

        assertNull(result);
    }

    @Test
    @DisplayName("批量查询店铺信息")
    void batchGetShopInfo() {
        Set<Long> shopIds = new HashSet<>(Set.of(1L, 2L, 3L));
        Shop s1 = new Shop(1L, 1001L, 10L, 1, null, null);
        Shop s2 = new Shop(2L, 1002L, 20L, 1, null, null);
        ShopInfo si1 = new ShopInfo(10L, "店铺A", "描述A", "logoA", null, null);
        ShopInfo si2 = new ShopInfo(20L, "店铺B", "描述B", "logoB", null, null);

        when(shopMapper.selectShopsByIds(shopIds)).thenReturn(List.of(s1, s2));
        when(shopInfoService.getByIds(List.of(10L, 20L))).thenReturn(List.of(si1, si2));

        Map<Long, ShopInfoDTO> result = shopService.batchGetShopInfo(shopIds);

        assertEquals(2, result.size());
        assertEquals("店铺A", result.get(1L).getName());
        assertEquals("店铺B", result.get(2L).getName());
        assertNull(result.get(3L));
    }

    @Test
    @DisplayName("批量查询 - 空集合")
    void batchGetShopInfo_empty() {
        Map<Long, ShopInfoDTO> result = shopService.batchGetShopInfo(Collections.emptySet());

        assertTrue(result.isEmpty());

        verify(shopMapper, never()).selectShopsByIds(any());
    }
}