package com.gzasc.aishopping.shop.controller;

import com.gzasc.aishopping.shop.dto.AddEmployeeRequest;
import com.gzasc.aishopping.shop.dto.CreateShopRequest;
import com.gzasc.aishopping.shop.dto.UpdateShopRequest;
import com.gzasc.aishopping.shop.exception.ShopException;
import com.gzasc.aishopping.shop.model.Shop;
import com.gzasc.aishopping.shop.service.ShopService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class ShopMerchantControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ShopService shopService;

    @BeforeEach
    void setUp() {
        var controller = new ShopMerchantController(shopService);
        var validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = standaloneSetup(controller)
                .setValidator(validator)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ========== 创建店铺 ==========

    @Test
    @DisplayName("SH-001 创建店铺成功 - 完整参数")
    void createShop_success() throws Exception {
        Shop shop = new Shop();
        shop.setId(10001L);
        when(shopService.createShop(any(), eq(1001L))).thenReturn(shop);

        mockMvc.perform(post("/api/seller/shop/register")
                        .header("X-User-Id", 1001L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"测试店铺","description":"这是一个测试店铺","logoId":"logo-abc-123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("创建店铺成功"))
                .andExpect(jsonPath("$.data.id").value(10001));
    }

    @Test
    @DisplayName("SH-003 创建店铺 - name为空")
    void createShop_nameNull() throws Exception {
        mockMvc.perform(post("/api/seller/shop/register")
                        .header("X-User-Id", 1001L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"测试"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("SH-004 创建店铺 - name超长")
    void createShop_nameTooLong() throws Exception {
        String longName = "a".repeat(101);

        mockMvc.perform(post("/api/seller/shop/register")
                        .header("X-User-Id", 1001L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + longName + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("SH-005 创建店铺 - description超长")
    void createShop_descTooLong() throws Exception {
        String longDesc = "a".repeat(501);

        mockMvc.perform(post("/api/seller/shop/register")
                        .header("X-User-Id", 1001L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"正常\",\"description\":\"" + longDesc + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("SH-045 创建店铺 - name为空白字符串")
    void createShop_nameBlank() throws Exception {
        mockMvc.perform(post("/api/seller/shop/register")
                        .header("X-User-Id", 1001L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    // ========== 更新店铺 ==========

    @Test
    @DisplayName("SH-007 更新店铺成功")
    void updateShop_success() throws Exception {
        doNothing().when(shopService).updateShop(eq(1L), any(), eq(1001L));

        mockMvc.perform(put("/api/seller/shop/1")
                        .header("X-User-Id", 1001L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"新店铺名","description":"新描述","logoId":"new-logo"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("更新店铺成功"));
    }

    @Test
    @DisplayName("SH-009 更新店铺 - 店铺不存在")
    void updateShop_notFound() throws Exception {
        doThrow(new ShopException("店铺不存在"))
                .when(shopService).updateShop(eq(999L), any(), eq(1001L));

        mockMvc.perform(put("/api/seller/shop/999")
                        .header("X-User-Id", 1001L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"不存在"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("店铺不存在"));
    }

    @Test
    @DisplayName("SH-010 更新店铺 - 非店长操作")
    void updateShop_notOwner() throws Exception {
        doThrow(new ShopException("仅店长可操作"))
                .when(shopService).updateShop(eq(1L), any(), eq(1002L));

        mockMvc.perform(put("/api/seller/shop/1")
                        .header("X-User-Id", 1002L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"无权限"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("仅店长可操作"));
    }

    // ========== 关闭/开启店铺 ==========

    @Test
    @DisplayName("SH-011 关闭店铺成功")
    void closeShop_success() throws Exception {
        doNothing().when(shopService).closeShop(1L, 1001L);

        mockMvc.perform(delete("/api/seller/shop/1")
                        .header("X-User-Id", 1001L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("关闭店铺成功"));
    }

    @Test
    @DisplayName("SH-012 关闭店铺 - 非店长操作")
    void closeShop_notOwner() throws Exception {
        doThrow(new ShopException("仅店长可操作"))
                .when(shopService).closeShop(1L, 1002L);

        mockMvc.perform(delete("/api/seller/shop/1")
                        .header("X-User-Id", 1002L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("仅店长可操作"));
    }

    @Test
    @DisplayName("SH-013 关闭店铺 - 已关闭店铺再次关闭")
    void closeShop_alreadyClosed() throws Exception {
        doThrow(new ShopException("店铺已关闭或不存在"))
                .when(shopService).closeShop(1L, 1001L);

        mockMvc.perform(delete("/api/seller/shop/1")
                        .header("X-User-Id", 1001L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("店铺已关闭或不存在"));
    }

    @Test
    @DisplayName("SH-014 重新开店成功")
    void openShop_success() throws Exception {
        doNothing().when(shopService).openShop(1L, 1001L);

        mockMvc.perform(put("/api/seller/shop/1/open")
                        .header("X-User-Id", 1001L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("重新开店成功"));
    }

    @Test
    @DisplayName("SH-015 重新开店 - 非店长操作")
    void openShop_notOwner() throws Exception {
        doThrow(new ShopException("仅店长可操作"))
                .when(shopService).openShop(1L, 1002L);

        mockMvc.perform(put("/api/seller/shop/1/open")
                        .header("X-User-Id", 1002L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("仅店长可操作"));
    }

    // ========== 员工管理 ==========

    @Test
    @DisplayName("SH-016 添加店员成功")
    void addEmployee_success() throws Exception {
        doNothing().when(shopService).addEmployee(eq(1L), any(), eq(1001L));

        mockMvc.perform(post("/api/seller/shop/1/employees/register")
                        .header("X-User-Id", 1001L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"emp01","password":"Abc123","phone":"13800138001","name":"店员小王"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("添加店员成功"));
    }

    @Test
    @DisplayName("SH-019 添加店员 - 非店长操作")
    void addEmployee_notOwner() throws Exception {
        doThrow(new ShopException("仅店长可操作"))
                .when(shopService).addEmployee(eq(1L), any(), eq(1002L));

        mockMvc.perform(post("/api/seller/shop/1/employees/register")
                        .header("X-User-Id", 1002L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"emp01","password":"Abc123"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("仅店长可操作"));
    }

    @Test
    @DisplayName("SH-022 添加店员 - 请求参数校验(用户名为空)")
    void addEmployee_validation_usernameNull() throws Exception {
        mockMvc.perform(post("/api/seller/shop/1/employees/register")
                        .header("X-User-Id", 1001L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"password":"Abc123","phone":"13800138001","name":"测试"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("SH-046 添加店员 - username过短")
    void addEmployee_validation_usernameTooShort() throws Exception {
        mockMvc.perform(post("/api/seller/shop/1/employees/register")
                        .header("X-User-Id", 1001L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"ab"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("SH-047 添加店员 - username含特殊字符")
    void addEmployee_validation_usernameSpecialChars() throws Exception {
        mockMvc.perform(post("/api/seller/shop/1/employees/register")
                        .header("X-User-Id", 1001L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"user@name"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("SH-020 移除店员成功")
    void removeEmployee_success() throws Exception {
        doNothing().when(shopService).removeEmployee(1L, 2001L, 1001L);

        mockMvc.perform(delete("/api/seller/shop/1/employees/2001")
                        .header("X-User-Id", 1001L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("移除店员成功"));
    }

    // ========== 查询(商家端) ==========

    @Test
    @DisplayName("SH-023 按商家查询店铺ID列表")
    void getShopsByMerchant() throws Exception {
        when(shopService.getShopIdsByMerchantId(1001L)).thenReturn(List.of(1L, 2L, 3L));

        mockMvc.perform(get("/api/seller/shop/merchant/1001")
                        .header("X-User-Id", 1001L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.shopIds[0]").value(1))
                .andExpect(jsonPath("$.data.shopIds[1]").value(2))
                .andExpect(jsonPath("$.data.shopIds[2]").value(3));
    }

    @Test
    @DisplayName("SH-025 查询店铺详情 - 有权限")
    void getShop_success() throws Exception {
        Shop shop = new Shop(1L, 1001L, 10L, 1, null, null);
        when(shopService.getShopWithAccessCheck(1L, 1001L)).thenReturn(shop);

        mockMvc.perform(get("/api/seller/shop/1")
                        .header("X-User-Id", 1001L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.shop.id").value(1))
                .andExpect(jsonPath("$.data.shop.merchantId").value(1001))
                .andExpect(jsonPath("$.data.shop.status").value(1));
    }

    @Test
    @DisplayName("SH-026 查询店铺详情 - 无权限")
    void getShop_noAccess() throws Exception {
        when(shopService.getShopWithAccessCheck(1L, 9999L))
                .thenThrow(new ShopException("无权限访问该店铺"));

        mockMvc.perform(get("/api/seller/shop/1")
                        .header("X-User-Id", 9999L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("无权限访问该店铺"));
    }

    @Test
    @DisplayName("SH-027 查询店铺详情 - 店铺不存在")
    void getShop_notFound() throws Exception {
        when(shopService.getShopWithAccessCheck(999L, 1001L))
                .thenThrow(new ShopException("店铺不存在"));

        mockMvc.perform(get("/api/seller/shop/999")
                        .header("X-User-Id", 1001L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("店铺不存在"));
    }

    @Test
    @DisplayName("SH-028 查询员工列表")
    void getEmployees() throws Exception {
        Map<String, Object> employees = Map.of(
                "employees", List.of(
                        Map.of("merchantId", 1001, "shopId", 1, "role", 1),
                        Map.of("merchantId", 2001, "shopId", 1, "role", 2)
                ),
                "total", 2
        );
        when(shopService.getShopEmployees(1L, 1001L)).thenReturn(employees);

        mockMvc.perform(get("/api/seller/shop/1/employees")
                        .header("X-User-Id", 1001L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.employees.length()").value(2));
    }

    // ========== 异常处理 ==========

    @Test
    @DisplayName("SH-048 未知异常返回500")
    void unknownException() throws Exception {
        when(shopService.getShopIdsByMerchantId(any()))
                .thenThrow(new RuntimeException("未知错误"));

        mockMvc.perform(get("/api/seller/shop/merchant/1001")
                        .header("X-User-Id", 1001L))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("系统错误，请稍后重试"));
    }

    @Test
    @DisplayName("SH-049 按商家查询 - userId != merchantId 无权限")
    void getShopsByMerchant_noPermission() throws Exception {
        mockMvc.perform(get("/api/seller/shop/merchant/2001")
                        .header("X-User-Id", 1001L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("无权限查看该商户的店铺列表"));
    }
}
