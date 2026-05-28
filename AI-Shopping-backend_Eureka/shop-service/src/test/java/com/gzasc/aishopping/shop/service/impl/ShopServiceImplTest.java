package com.gzasc.aishopping.shop.service.impl;

import com.gzasc.aishopping.common.dto.shop.ShopInfoDTO;
import com.gzasc.aishopping.common.feign.auth.AuthFeignClient;
import com.gzasc.aishopping.shop.dto.AddEmployeeRequest;
import com.gzasc.aishopping.shop.dto.CreateShopRequest;
import com.gzasc.aishopping.shop.dto.UpdateShopRequest;
import com.gzasc.aishopping.shop.exception.ShopException;
import com.gzasc.aishopping.shop.mapper.ShopMapper;
import com.gzasc.aishopping.shop.model.MerchantRole;
import com.gzasc.aishopping.shop.model.Shop;
import com.gzasc.aishopping.shop.model.ShopInfo;
import com.gzasc.aishopping.shop.service.MerchantRoleService;
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
    private AuthFeignClient authFeignClient;

    @Mock
    private MerchantRoleService merchantRoleService;

    @Mock
    private ShopInfoService shopInfoService;

    @InjectMocks
    private ShopServiceImpl shopService;

    // ========== 3.1 店铺创建 ==========

    @Test
    @DisplayName("SH-001 创建店铺成功 - 完整参数")
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

        verify(merchantRoleService).insert(argThat(mr ->
                userId.equals(mr.getMerchantId()) &&
                mr.getRole() == 1 &&
                userId.equals(mr.getAssignedBy())));
    }

    @Test
    @DisplayName("SH-002 创建店铺成功 - 仅必填参数")
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
    @DisplayName("SH-006 创建店铺 - 数据回滚(MerchantRole插入异常)")
    void createShop_transactionRollback() {
        CreateShopRequest request = new CreateShopRequest();
        request.setName("回滚测试");
        Long userId = 1001L;

        when(shopMapper.insertShop(any())).thenReturn(1);
        doThrow(new RuntimeException("DB error")).when(merchantRoleService).insert(any());

        assertThrows(RuntimeException.class, () -> shopService.createShop(request, userId));

        verify(shopInfoService).insert(any(ShopInfo.class));
        verify(shopMapper).insertShop(any(Shop.class));
        verify(merchantRoleService).insert(any(MerchantRole.class));
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

        verify(merchantRoleService, never()).insert(any());
    }

    // ========== 3.2 店铺管理 ==========

    @Test
    @DisplayName("SH-007 更新店铺成功")
    void updateShop_success() {
        UpdateShopRequest request = new UpdateShopRequest();
        request.setName("新店铺名");
        request.setDescription("新描述");
        request.setLogoId("new-logo");
        Long userId = 1001L;
        Long shopId = 1L;

        Shop existingShop = new Shop(shopId, userId, 10L, 1, null, null);
        when(merchantRoleService.selectByMerchantShopAndRole(userId, shopId, 1)).thenReturn(new MerchantRole());
        when(shopMapper.selectShopById(shopId)).thenReturn(existingShop);

        shopService.updateShop(shopId, request, userId);

        verify(shopInfoService).update(argThat(si ->
                "新店铺名".equals(si.getName()) &&
                "新描述".equals(si.getDescription()) &&
                "new-logo".equals(si.getLogoUrl())));
    }

    @Test
    @DisplayName("SH-008 更新店铺 - 仅更新名称，其他字段被全量覆盖为null")
    void updateShop_onlyName() {
        UpdateShopRequest request = new UpdateShopRequest();
        request.setName("仅改名称");
        Long userId = 1001L;
        Long shopId = 1L;

        Shop existingShop = new Shop(shopId, userId, 10L, 1, null, null);
        when(merchantRoleService.selectByMerchantShopAndRole(userId, shopId, 1)).thenReturn(new MerchantRole());
        when(shopMapper.selectShopById(shopId)).thenReturn(existingShop);

        shopService.updateShop(shopId, request, userId);

        verify(shopInfoService).update(argThat(si ->
                "仅改名称".equals(si.getName()) &&
                si.getDescription() == null &&
                si.getLogoUrl() == null));
    }

    @Test
    @DisplayName("SH-009 更新店铺 - 店铺不存在")
    void updateShop_shopNotFound() {
        UpdateShopRequest request = new UpdateShopRequest();
        request.setName("不存在");
        Long userId = 1001L;
        Long shopId = 999L;

        when(merchantRoleService.selectByMerchantShopAndRole(userId, shopId, 1)).thenReturn(new MerchantRole());
        when(shopMapper.selectShopById(shopId)).thenReturn(null);

        ShopException ex = assertThrows(ShopException.class,
                () -> shopService.updateShop(shopId, request, userId));
        assertEquals("店铺不存在", ex.getMessage());
    }

    @Test
    @DisplayName("SH-010 更新店铺 - 非店长操作")
    void updateShop_notOwner() {
        UpdateShopRequest request = new UpdateShopRequest();
        request.setName("无权限");
        Long userId = 1002L;
        Long shopId = 1L;

        when(merchantRoleService.selectByMerchantShopAndRole(userId, shopId, 1)).thenReturn(null);

        ShopException ex = assertThrows(ShopException.class,
                () -> shopService.updateShop(shopId, request, userId));
        assertEquals("仅店长可操作", ex.getMessage());

        verify(shopMapper, never()).selectShopById(any());
    }

    @Test
    @DisplayName("SH-011 关闭店铺成功")
    void closeShop_success() {
        Long userId = 1001L;
        Long shopId = 1L;

        when(merchantRoleService.selectByMerchantShopAndRole(userId, shopId, 1)).thenReturn(new MerchantRole());
        when(shopMapper.closeShop(shopId)).thenReturn(1);

        shopService.closeShop(shopId, userId);

        verify(shopMapper).closeShop(shopId);
    }

    @Test
    @DisplayName("SH-012 关闭店铺 - 非店长操作")
    void closeShop_notOwner() {
        Long userId = 1002L;
        Long shopId = 1L;

        when(merchantRoleService.selectByMerchantShopAndRole(userId, shopId, 1)).thenReturn(null);

        ShopException ex = assertThrows(ShopException.class,
                () -> shopService.closeShop(shopId, userId));
        assertEquals("仅店长可操作", ex.getMessage());

        verify(shopMapper, never()).closeShop(any());
    }

    @Test
    @DisplayName("SH-013 关闭店铺 - 已关闭店铺再次关闭")
    void closeShop_alreadyClosed() {
        Long userId = 1001L;
        Long shopId = 1L;

        when(merchantRoleService.selectByMerchantShopAndRole(userId, shopId, 1)).thenReturn(new MerchantRole());
        when(shopMapper.closeShop(shopId)).thenReturn(0);

        ShopException ex = assertThrows(ShopException.class,
                () -> shopService.closeShop(shopId, userId));
        assertEquals("关闭店铺失败", ex.getMessage());
    }

    @Test
    @DisplayName("SH-014 重新开店成功")
    void openShop_success() {
        Long userId = 1001L;
        Long shopId = 1L;

        when(merchantRoleService.selectByMerchantShopAndRole(userId, shopId, 1)).thenReturn(new MerchantRole());
        when(shopMapper.openShop(shopId)).thenReturn(1);

        shopService.openShop(shopId, userId);

        verify(shopMapper).openShop(shopId);
    }

    @Test
    @DisplayName("SH-015 重新开店 - 非店长操作")
    void openShop_notOwner() {
        Long userId = 1002L;
        Long shopId = 1L;

        when(merchantRoleService.selectByMerchantShopAndRole(userId, shopId, 1)).thenReturn(null);

        ShopException ex = assertThrows(ShopException.class,
                () -> shopService.openShop(shopId, userId));
        assertEquals("仅店长可操作", ex.getMessage());

        verify(shopMapper, never()).openShop(any());
    }

    // ========== 3.3 员工管理 ==========

    @Test
    @DisplayName("SH-016 添加店员成功")
    void addEmployee_success() {
        Long userId = 1001L;
        Long shopId = 1L;
        AddEmployeeRequest request = new AddEmployeeRequest();
        request.setUsername("emp01");
        request.setPassword("Abc123");
        request.setPhone("13800138001");
        request.setName("店员小王");

        when(merchantRoleService.selectByMerchantShopAndRole(userId, shopId, 1)).thenReturn(new MerchantRole());
        Map<String, Object> feignResult = new HashMap<>();
        feignResult.put("merchantId", 2001L);
        when(authFeignClient.registerEmployee(any())).thenReturn(feignResult);

        shopService.addEmployee(shopId, request, userId);

        verify(authFeignClient).registerEmployee(argThat(m ->
                "emp01".equals(m.get("username")) &&
                "Abc123".equals(m.get("password")) &&
                "13800138001".equals(m.get("phone"))));
        verify(merchantRoleService).insert(argThat(mr ->
                mr.getMerchantId().equals(2001L) &&
                mr.getShopId().equals(shopId) &&
                mr.getRole() == 2 &&
                mr.getAssignedBy().equals(userId)));
    }

    @Test
    @DisplayName("SH-017 添加店员 - Feign返回错误消息")
    void addEmployee_feignError() {
        Long userId = 1001L;
        Long shopId = 1L;
        AddEmployeeRequest request = new AddEmployeeRequest();
        request.setUsername("emp01");
        request.setPassword("Abc123");

        when(merchantRoleService.selectByMerchantShopAndRole(userId, shopId, 1)).thenReturn(new MerchantRole());
        Map<String, Object> feignResult = new HashMap<>();
        feignResult.put("message", "用户名已存在");
        when(authFeignClient.registerEmployee(any())).thenReturn(feignResult);

        ShopException ex = assertThrows(ShopException.class,
                () -> shopService.addEmployee(shopId, request, userId));
        assertTrue(ex.getMessage().contains("添加店员失败: 用户名已存在"));
    }

    @Test
    @DisplayName("SH-018 添加店员 - Feign返回null")
    void addEmployee_feignNull() {
        Long userId = 1001L;
        Long shopId = 1L;
        AddEmployeeRequest request = new AddEmployeeRequest();
        request.setUsername("emp01");
        request.setPassword("Abc123");

        when(merchantRoleService.selectByMerchantShopAndRole(userId, shopId, 1)).thenReturn(new MerchantRole());
        when(authFeignClient.registerEmployee(any())).thenReturn(null);

        ShopException ex = assertThrows(ShopException.class,
                () -> shopService.addEmployee(shopId, request, userId));
        assertTrue(ex.getMessage().contains("添加店员失败: 注册失败"));
    }

    @Test
    @DisplayName("SH-019 添加店员 - 非店长操作")
    void addEmployee_notOwner() {
        Long userId = 1002L;
        Long shopId = 1L;
        AddEmployeeRequest request = new AddEmployeeRequest();
        request.setUsername("emp01");

        when(merchantRoleService.selectByMerchantShopAndRole(userId, shopId, 1)).thenReturn(null);

        ShopException ex = assertThrows(ShopException.class,
                () -> shopService.addEmployee(shopId, request, userId));
        assertEquals("仅店长可操作", ex.getMessage());

        verify(authFeignClient, never()).registerEmployee(any());
    }

    @Test
    @DisplayName("SH-020 移除店员成功")
    void removeEmployee_success() {
        Long userId = 1001L;
        Long shopId = 1L;
        Long targetMerchantId = 2001L;

        when(merchantRoleService.selectByMerchantShopAndRole(userId, shopId, 1)).thenReturn(new MerchantRole());

        shopService.removeEmployee(shopId, targetMerchantId, userId);

        verify(merchantRoleService).deleteByMerchantAndShop(targetMerchantId, shopId);
    }

    @Test
    @DisplayName("SH-021 移除店员 - 非店长操作")
    void removeEmployee_notOwner() {
        Long userId = 1002L;
        Long shopId = 1L;
        Long targetMerchantId = 2001L;

        when(merchantRoleService.selectByMerchantShopAndRole(userId, shopId, 1)).thenReturn(null);

        ShopException ex = assertThrows(ShopException.class,
                () -> shopService.removeEmployee(shopId, targetMerchantId, userId));
        assertEquals("仅店长可操作", ex.getMessage());

        verify(merchantRoleService, never()).deleteByMerchantAndShop(any(), any());
    }

    // ========== 3.4 店铺查询 (商家端) ==========

    @Test
    @DisplayName("SH-023 按商家查询店铺ID列表")
    void getShopIdsByMerchantId() {
        Long merchantId = 1001L;
        MerchantRole r1 = new MerchantRole(1L, merchantId, 10L, 1, 1001L, null);
        MerchantRole r2 = new MerchantRole(2L, merchantId, 20L, 2, 1001L, null);
        when(merchantRoleService.selectByMerchantId(merchantId)).thenReturn(List.of(r1, r2));

        List<Long> result = shopService.getShopIdsByMerchantId(merchantId);

        assertEquals(2, result.size());
        assertTrue(result.contains(10L));
        assertTrue(result.contains(20L));
    }

    @Test
    @DisplayName("SH-024 按商家查询 - 无店铺")
    void getShopIdsByMerchantId_empty() {
        Long merchantId = 999L;
        when(merchantRoleService.selectByMerchantId(merchantId)).thenReturn(List.of());

        List<Long> result = shopService.getShopIdsByMerchantId(merchantId);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("SH-025 查询店铺详情 - 有权限")
    void getShopWithAccessCheck_success() {
        Long userId = 1001L;
        Long shopId = 1L;
        Shop shop = new Shop(shopId, 1001L, 10L, 1, null, null);

        when(merchantRoleService.selectByMerchantAndShop(userId, shopId)).thenReturn(new MerchantRole());
        when(shopMapper.selectShopById(shopId)).thenReturn(shop);

        Shop result = shopService.getShopWithAccessCheck(shopId, userId);

        assertNotNull(result);
        assertEquals(shopId, result.getId());
    }

    @Test
    @DisplayName("SH-026 查询店铺详情 - 无权限")
    void getShopWithAccessCheck_noAccess() {
        Long userId = 9999L;
        Long shopId = 1L;

        when(merchantRoleService.selectByMerchantAndShop(userId, shopId)).thenReturn(null);

        ShopException ex = assertThrows(ShopException.class,
                () -> shopService.getShopWithAccessCheck(shopId, userId));
        assertEquals("无权限访问该店铺", ex.getMessage());
    }

    @Test
    @DisplayName("SH-027 查询店铺详情 - 店铺不存在")
    void getShopWithAccessCheck_notFound() {
        Long userId = 1001L;
        Long shopId = 999L;

        when(merchantRoleService.selectByMerchantAndShop(userId, shopId)).thenReturn(new MerchantRole());
        when(shopMapper.selectShopById(shopId)).thenReturn(null);

        ShopException ex = assertThrows(ShopException.class,
                () -> shopService.getShopWithAccessCheck(shopId, userId));
        assertEquals("店铺不存在", ex.getMessage());
    }

    @Test
    @DisplayName("SH-028 查询员工列表")
    void getShopEmployees() {
        Long userId = 1001L;
        Long shopId = 1L;

        when(merchantRoleService.selectByMerchantAndShop(userId, shopId)).thenReturn(new MerchantRole());
        MerchantRole e1 = new MerchantRole(1L, 1001L, shopId, 1, 1001L, null);
        MerchantRole e2 = new MerchantRole(2L, 2001L, shopId, 2, 1001L, null);
        MerchantRole e3 = new MerchantRole(3L, 2002L, shopId, 2, 1001L, null);
        when(merchantRoleService.selectByShopId(shopId)).thenReturn(List.of(e1, e2, e3));

        Map<String, Object> result = shopService.getShopEmployees(shopId, userId);

        assertEquals(3, result.get("total"));
        List<?> employees = (List<?>) result.get("employees");
        assertEquals(3, employees.size());
    }

    @Test
    @DisplayName("SH-029 查询员工列表 - 无权限")
    void getShopEmployees_noAccess() {
        Long userId = 9999L;
        Long shopId = 1L;

        when(merchantRoleService.selectByMerchantAndShop(userId, shopId)).thenReturn(null);

        ShopException ex = assertThrows(ShopException.class,
                () -> shopService.getShopEmployees(shopId, userId));
        assertEquals("无权限访问该店铺", ex.getMessage());
    }

    // ========== 3.5 店铺查询 (用户端) ==========

    @Test
    @DisplayName("SH-030 查询活跃店铺列表 - 有数据")
    void getUserShopList() {
        int page = 1, size = 10;
        Shop s1 = new Shop(1L, 1001L, 10L, 1, null, null);
        Shop s2 = new Shop(2L, 1002L, 20L, 1, null, null);
        Shop s3 = new Shop(3L, 1003L, 30L, 1, null, null);

        when(shopMapper.selectActiveShops(0, 10)).thenReturn(List.of(s1, s2, s3));
        when(shopMapper.countActiveShops()).thenReturn(3);

        Map<String, Object> result = shopService.getUserShopList(page, size);

        assertEquals(3, result.get("total"));
        assertEquals(1, result.get("page"));
        assertEquals(10, result.get("size"));
        List<?> shops = (List<?>) result.get("shops");
        assertEquals(3, shops.size());
    }

    @Test
    @DisplayName("SH-031 查询活跃店铺列表 - 分页第二页")
    void getUserShopList_pagination() {
        int page = 2, size = 10;
        Shop s1 = new Shop(11L, 1001L, 10L, 1, null, null);
        Shop s2 = new Shop(12L, 1002L, 20L, 1, null, null);
        Shop s3 = new Shop(13L, 1003L, 30L, 1, null, null);
        Shop s4 = new Shop(14L, 1004L, 40L, 1, null, null);
        Shop s5 = new Shop(15L, 1005L, 50L, 1, null, null);

        when(shopMapper.selectActiveShops(10, 10)).thenReturn(List.of(s1, s2, s3, s4, s5));
        when(shopMapper.countActiveShops()).thenReturn(15);

        Map<String, Object> result = shopService.getUserShopList(page, size);

        assertEquals(15, result.get("total"));
        assertEquals(2, result.get("page"));
        List<?> shops = (List<?>) result.get("shops");
        assertEquals(5, shops.size());
    }

    @Test
    @DisplayName("SH-032 查询活跃店铺列表 - 无数据")
    void getUserShopList_empty() {
        int page = 1, size = 10;

        when(shopMapper.selectActiveShops(0, 10)).thenReturn(List.of());
        when(shopMapper.countActiveShops()).thenReturn(0);

        Map<String, Object> result = shopService.getUserShopList(page, size);

        assertEquals(0, result.get("total"));
        List<?> shops = (List<?>) result.get("shops");
        assertTrue(shops.isEmpty());
    }

    @Test
    @DisplayName("SH-033 查询店铺详情 - 活跃店铺")
    void getActiveShopById_active() {
        Long shopId = 1L;
        Shop shop = new Shop(shopId, 1001L, 10L, 1, null, null);
        ShopInfo shopInfo = new ShopInfo(10L, "测试店铺", "描述", "logo-url", null, null);

        when(shopMapper.selectShopById(shopId)).thenReturn(shop);
        when(shopInfoService.getById(10L)).thenReturn(shopInfo);

        Map<String, Object> result = shopService.getActiveShopById(shopId);

        assertSame(shop, result.get("shop"));
        ShopInfoDTO dto = (ShopInfoDTO) result.get("shopInfo");
        assertNotNull(dto);
        assertEquals("测试店铺", dto.getName());
        assertEquals("描述", dto.getDescription());
        assertEquals("logo-url", dto.getLogoUrl());
    }

    @Test
    @DisplayName("SH-034 查询店铺详情 - 已关闭")
    void getActiveShopById_closed() {
        Long shopId = 2L;
        Shop shop = new Shop(shopId, 1001L, 10L, 0, null, null);

        when(shopMapper.selectShopById(shopId)).thenReturn(shop);

        ShopException ex = assertThrows(ShopException.class,
                () -> shopService.getActiveShopById(shopId));
        assertEquals("店铺不存在或已关闭", ex.getMessage());
    }

    @Test
    @DisplayName("活跃店铺详情 - 店铺为null抛出异常")
    void getActiveShopById_nullShop() {
        Long shopId = 999L;

        when(shopMapper.selectShopById(shopId)).thenReturn(null);

        ShopException ex = assertThrows(ShopException.class,
                () -> shopService.getActiveShopById(shopId));
        assertEquals("店铺不存在或已关闭", ex.getMessage());
    }

    // ========== 3.6 内部接口 ==========

    @Test
    @DisplayName("SH-037 查询店铺信息")
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
    @DisplayName("SH-038 查询店铺信息 - 无关联info或shopId为null")
    void getShopInfoById_noInfo() {
        Long shopId = 1L;

        when(shopMapper.selectShopById(shopId)).thenReturn(null);

        ShopInfoDTO result = shopService.getShopInfoById(shopId);

        assertNull(result);
    }

    @Test
    @DisplayName("SH-039 批量查询店铺信息")
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
    @DisplayName("SH-040 批量查询 - 空集合")
    void batchGetShopInfo_empty() {
        Map<Long, ShopInfoDTO> result = shopService.batchGetShopInfo(Collections.emptySet());

        assertTrue(result.isEmpty());

        verify(shopMapper, never()).selectShopsByIds(any());
    }

    // ========== 3.7 权限校验 ==========

    @Test
    @DisplayName("SH-041 店长可执行管理操作(update)")
    void checkShopOwner_ownerCanUpdate() {
        Long userId = 1001L;
        Long shopId = 1L;

        when(merchantRoleService.selectByMerchantShopAndRole(userId, shopId, 1)).thenReturn(new MerchantRole());
        when(shopMapper.selectShopById(shopId)).thenReturn(new Shop(shopId, userId, 10L, 1, null, null));

        shopService.updateShop(shopId, new UpdateShopRequest(), userId);

        verify(shopInfoService).update(any());
    }

    @Test
    @DisplayName("SH-042 店员无法执行管理操作(update)")
    void checkShopOwner_staffCannotUpdate() {
        Long userId = 1002L;
        Long shopId = 1L;

        when(merchantRoleService.selectByMerchantShopAndRole(userId, shopId, 1)).thenReturn(null);

        ShopException ex = assertThrows(ShopException.class,
                () -> shopService.updateShop(shopId, new UpdateShopRequest(), userId));
        assertEquals("仅店长可操作", ex.getMessage());
    }

    @Test
    @DisplayName("SH-043 店员可以查看店铺")
    void checkShopAccess_staffCanAccess() {
        Long userId = 1002L;
        Long shopId = 1L;

        when(merchantRoleService.selectByMerchantAndShop(userId, shopId)).thenReturn(new MerchantRole());
        when(shopMapper.selectShopById(shopId)).thenReturn(new Shop(shopId, 1001L, 10L, 1, null, null));

        Shop result = shopService.getShopWithAccessCheck(shopId, userId);

        assertNotNull(result);
    }

    @Test
    @DisplayName("SH-044 无任何角色无法访问店铺")
    void checkShopAccess_noRoleCannotAccess() {
        Long userId = 9999L;
        Long shopId = 1L;

        when(merchantRoleService.selectByMerchantAndShop(userId, shopId)).thenReturn(null);

        ShopException ex = assertThrows(ShopException.class,
                () -> shopService.getShopWithAccessCheck(shopId, userId));
        assertEquals("无权限访问该店铺", ex.getMessage());
    }
}
