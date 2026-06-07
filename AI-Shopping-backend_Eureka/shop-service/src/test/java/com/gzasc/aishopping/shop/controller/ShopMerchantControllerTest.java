package com.gzasc.aishopping.shop.controller;

import com.gzasc.aishopping.shop.dto.AddEmployeeRequest;
import com.gzasc.aishopping.shop.dto.CreateShopRequest;
import com.gzasc.aishopping.shop.dto.SimpleShopDTO;
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
                                {"name":"   ","description":"测试"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    // ========== 更新店铺 ==========

    @Test
    @DisplayName("SH-009 更新店铺成功")
    void updateShop_success() throws Exception {
        mockMvc.perform(put("/api/seller/shop/1")
                        .header("X-User-Id", 1001L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"新名称","description":"新描述","logoId":"logo-new"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("更新店铺成功"));
    }

    // ========== 关闭/开启店铺 ==========

    @Test
    @DisplayName("SH-015 关闭店铺成功")
    void closeShop_success() throws Exception {
        mockMvc.perform(patch("/api/seller/shop/1/close")
                        .header("X-User-Id", 1001L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("关闭店铺成功"));
    }

    @Test
    @DisplayName("SH-019 开启店铺成功")
    void openShop_success() throws Exception {
        mockMvc.perform(patch("/api/seller/shop/1/open")
                        .header("X-User-Id", 1001L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("开启店铺成功"));
    }

    // ========== 员工管理 ==========

    @Test
    @DisplayName("SH-011 添加店员成功")
    void addEmployee_success() throws Exception {
        mockMvc.perform(post("/api/seller/shop/1/employees")
                        .header("X-User-Id", 1001L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"emp001","password":"123456","phone":"13800138000","name":"店员小王"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("添加店员成功"));
    }

    @Test
    @DisplayName("SH-021 移除店员成功")
    void removeEmployee_success() throws Exception {
        mockMvc.perform(delete("/api/seller/shop/1/employees/2001")
                        .header("X-User-Id", 1001L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("移除店员成功"));
    }

    // ========== 查询(商家端) ==========

    @Test
    @DisplayName("SH-023 查询商户店铺列表")
    void getShopsByMerchant() throws Exception {
        List<SimpleShopDTO> shops = List.of(
                new SimpleShopDTO(1L, "店铺A", 1),
                new SimpleShopDTO(2L, "店铺B", 0),
                new SimpleShopDTO(3L, null, 1)
        );
        when(shopService.getSimpleShop(1001L)).thenReturn(shops);

        mockMvc.perform(get("/api/seller/shop/my-shops")
                        .header("X-User-Id", 1001L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.shops.length()").value(3))
                .andExpect(jsonPath("$.data.shops[0].id").value(1))
                .andExpect(jsonPath("$.data.shops[0].name").value("店铺A"))
                .andExpect(jsonPath("$.data.shops[0].status").value(1))
                .andExpect(jsonPath("$.data.shops[1].id").value(2))
                .andExpect(jsonPath("$.data.shops[1].name").value("店铺B"))
                .andExpect(jsonPath("$.data.shops[1].status").value(0))
                .andExpect(jsonPath("$.data.shops[2].id").value(3))
                .andExpect(jsonPath("$.data.shops[2].name").isEmpty())
                .andExpect(jsonPath("$.data.shops[2].status").value(1));
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
                .andExpect(jsonPath("$.data.shop.shopInfoId").value(10))
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
        when(shopService.getSimpleShop(any()))
                .thenThrow(new RuntimeException("未知错误"));

        mockMvc.perform(get("/api/seller/shop/my-shops")
                        .header("X-User-Id", 1001L))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("系统错误，请稍后重试"));
    }
}