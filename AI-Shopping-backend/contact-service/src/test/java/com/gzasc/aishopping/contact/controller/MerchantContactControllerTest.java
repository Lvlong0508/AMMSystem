package com.gzasc.aishopping.contact.controller;

import com.gzasc.aishopping.contact.dto.AddressResponse;
import com.gzasc.aishopping.contact.model.ShopAddress;
import com.gzasc.aishopping.contact.service.ShopAddressService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class MerchantContactControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ShopAddressService shopAddressService;

    @BeforeEach
    void setUp() {
        var controller = new MerchantContactController(shopAddressService);
        var validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = standaloneSetup(controller)
                .setValidator(validator)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ==================== POST /api/merchant/address/create ====================

    @Test
    @DisplayName("CT-026 正常创建发货地址")
    void createAddress_ship_success() throws Exception {
        when(shopAddressService.createAddress(any(ShopAddress.class), eq("5001"))).thenReturn(1);

        mockMvc.perform(post("/api/merchant/address/create")
                        .header("X-Shop-Id", "5001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"店铺仓库","phone":"021-12345678","address":"上海市嘉定区物流园A区","addressType":1,"isDefault":0}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("新增成功"))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("CT-027 正常创建退货地址（设为默认）")
    void createAddress_return_success() throws Exception {
        when(shopAddressService.createAddress(any(ShopAddress.class), eq("5001"))).thenReturn(2);

        mockMvc.perform(post("/api/merchant/address/create")
                        .header("X-Shop-Id", "5001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"售后部","phone":"021-87654321","address":"上海市浦东新区售后中心","addressType":2,"isDefault":1}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(2));
    }

    @Test
    @DisplayName("CT-028 创建地址时缺少 addressType")
    void createAddress_missingAddressType() throws Exception {
        mockMvc.perform(post("/api/merchant/address/create")
                        .header("X-Shop-Id", "5001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"店铺","phone":"02112345678","address":"上海市","isDefault":0}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("参数错误：地址类型不能为空"));
    }

    @Test
    @DisplayName("CT-030 创建地址时缺少 X-Shop-Id")
    void createAddress_missingShopId() throws Exception {
        mockMvc.perform(post("/api/merchant/address/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"店铺","phone":"02112345678","address":"上海市","addressType":1,"isDefault":0}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("未获取到店铺ID"));
    }

    // ==================== PUT /api/merchant/address/update/{id} ====================

    @Test
    @DisplayName("CT-032 正常更新店铺地址")
    void updateAddress_success() throws Exception {
        when(shopAddressService.updateAddress(any(ShopAddress.class), eq("5001"))).thenReturn(1);

        mockMvc.perform(put("/api/merchant/address/update/10")
                        .header("X-Shop-Id", "5001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id":10,"name":"新仓库名","phone":"0212222222","address":"上海市松江区","addressType":1,"isDefault":1}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("修改成功"));
    }

    @Test
    @DisplayName("CT-033 更新不存在地址")
    void updateAddress_notFound() throws Exception {
        when(shopAddressService.updateAddress(any(ShopAddress.class), eq("5001"))).thenReturn(0);

        mockMvc.perform(put("/api/merchant/address/update/99999")
                        .header("X-Shop-Id", "5001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id":99999,"name":"测试","phone":"0210000000","address":"上海市","addressType":1,"isDefault":0}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("修改失败：地址不存在或不属于该店铺"));
    }

    @Test
    @DisplayName("CT-034 更新不属于当前店铺的地址")
    void updateAddress_noPermission() throws Exception {
        when(shopAddressService.updateAddress(any(ShopAddress.class), eq("5001"))).thenReturn(0);

        mockMvc.perform(put("/api/merchant/address/update/20")
                        .header("X-Shop-Id", "5001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id":20,"name":"测试","phone":"0210000000","address":"上海市","addressType":1,"isDefault":0}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("修改失败：地址不存在或不属于该店铺"));
    }

    // ==================== DELETE /api/merchant/address/delete/{id} ====================

    @Test
    @DisplayName("CT-035 正常删除店铺地址")
    void deleteAddress_success() throws Exception {
        when(shopAddressService.deleteAddress(10, "5001")).thenReturn(1);

        mockMvc.perform(delete("/api/merchant/address/delete/10")
                        .header("X-Shop-Id", "5001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("删除成功"));
    }

    @Test
    @DisplayName("CT-036 删除不存在的店铺地址")
    void deleteAddress_notFound() throws Exception {
        when(shopAddressService.deleteAddress(99999, "5001")).thenReturn(0);

        mockMvc.perform(delete("/api/merchant/address/delete/99999")
                        .header("X-Shop-Id", "5001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("删除失败：地址不存在或不属于该店铺"));
    }

    @Test
    @DisplayName("CT-037 删除不属于当前店铺的地址")
    void deleteAddress_noPermission() throws Exception {
        when(shopAddressService.deleteAddress(20, "5001")).thenReturn(0);

        mockMvc.perform(delete("/api/merchant/address/delete/20")
                        .header("X-Shop-Id", "5001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("删除失败：地址不存在或不属于该店铺"));
    }

    // ==================== GET /api/merchant/address/list ====================

    @Test
    @DisplayName("CT-038 正常查询店铺地址列表")
    void listAddress_hasData() throws Exception {
        ShopAddress a1 = new ShopAddress(1, "仓库A", "0211111111", "上海市嘉定区", 1, 0, LocalDateTime.now(), LocalDateTime.now());
        ShopAddress a2 = new ShopAddress(2, "仓库B", "0212222222", "上海市浦东新区", 1, 1, LocalDateTime.now(), LocalDateTime.now());
        ShopAddress a3 = new ShopAddress(3, "售后部", "0213333333", "上海市徐汇区", 2, 0, LocalDateTime.now(), LocalDateTime.now());
        when(shopAddressService.getAddressesByShopId("5001")).thenReturn(List.of(a1, a2, a3));

        mockMvc.perform(get("/api/merchant/address/list")
                        .header("X-Shop-Id", "5001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.addresses.length()").value(3))
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.addresses[0].addressType").value(1))
                .andExpect(jsonPath("$.data.addresses[1].isDefault").value(1))
                .andExpect(jsonPath("$.data.addresses[2].addressType").value(2));
    }

    @Test
    @DisplayName("CT-039 查询无地址的店铺")
    void listAddress_empty() throws Exception {
        when(shopAddressService.getAddressesByShopId("6000")).thenReturn(List.of());

        mockMvc.perform(get("/api/merchant/address/list")
                        .header("X-Shop-Id", "6000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.addresses.length()").value(0))
                .andExpect(jsonPath("$.data.total").value(0));
    }

    // ==================== GET /api/merchant/address/ship-default ====================

    @Test
    @DisplayName("CT-041 正常查询默认发货地址")
    void getDefaultShipAddress_success() throws Exception {
        ShopAddress ship = new ShopAddress(1, "默认仓库", "0211111111", "上海市嘉定区", 1, 1, LocalDateTime.now(), LocalDateTime.now());
        when(shopAddressService.getDefaultShipAddressByShopId("5001")).thenReturn(ship);

        mockMvc.perform(get("/api/merchant/address/ship-default")
                        .header("X-Shop-Id", "5001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.addressType").value(1))
                .andExpect(jsonPath("$.data.isDefault").value(1));
    }

    @Test
    @DisplayName("CT-042 店铺无默认发货地址")
    void getDefaultShipAddress_notFound() throws Exception {
        when(shopAddressService.getDefaultShipAddressByShopId("5001")).thenReturn(null);

        mockMvc.perform(get("/api/merchant/address/ship-default")
                        .header("X-Shop-Id", "5001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    // ==================== PUT /api/merchant/address/set-default/{id} ====================

    @Test
    @DisplayName("CT-043 正常设置发货默认地址")
    void setDefault_ship_success() throws Exception {
        when(shopAddressService.setDefaultAddress(30, "5001")).thenReturn(1);

        mockMvc.perform(put("/api/merchant/address/set-default/30")
                        .header("X-Shop-Id", "5001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("设置成功"));
    }

    @Test
    @DisplayName("CT-044 设置退货地址为默认")
    void setDefault_return_success() throws Exception {
        when(shopAddressService.setDefaultAddress(40, "5001")).thenReturn(1);

        mockMvc.perform(put("/api/merchant/address/set-default/40")
                        .header("X-Shop-Id", "5001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("设置成功"));
    }

    @Test
    @DisplayName("CT-045 设置不存在的地址为默认")
    void setDefault_notFound() throws Exception {
        when(shopAddressService.setDefaultAddress(99999, "5001")).thenReturn(0);

        mockMvc.perform(put("/api/merchant/address/set-default/99999")
                        .header("X-Shop-Id", "5001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("设置失败：地址不存在或不属于该店铺"));
    }

    // ==================== 参数校验 400 - CreateAddress ====================

    @Test
    @DisplayName("CT-046 创建地址时 name 为空")
    void createAddress_emptyName() throws Exception {
        mockMvc.perform(post("/api/merchant/address/create")
                        .header("X-Shop-Id", "5001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","phone":"02112345678","address":"上海市","addressType":1,"isDefault":0}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("参数错误：收货人不能为空"));
    }

    @Test
    @DisplayName("CT-047 创建地址时 phone 为空")
    void createAddress_emptyPhone() throws Exception {
        mockMvc.perform(post("/api/merchant/address/create")
                        .header("X-Shop-Id", "5001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"店铺","phone":"","address":"上海市","addressType":1,"isDefault":0}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("参数错误：电话不能为空"));
    }

    @Test
    @DisplayName("CT-048 创建地址时 address 为空")
    void createAddress_emptyAddress() throws Exception {
        mockMvc.perform(post("/api/merchant/address/create")
                        .header("X-Shop-Id", "5001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"店铺","phone":"02112345678","address":"","addressType":1,"isDefault":0}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("参数错误：地址不能为空"));
    }

    // ==================== Service异常 500 - CreateAddress ====================

    @Test
    @DisplayName("CT-049 创建地址 Service 异常")
    void createAddress_serviceError() throws Exception {
        when(shopAddressService.createAddress(any(ShopAddress.class), eq("5001"))).thenThrow(new RuntimeException("db error"));
        mockMvc.perform(post("/api/merchant/address/create")
                        .header("X-Shop-Id", "5001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"店铺","phone":"02112345678","address":"上海市","addressType":1,"isDefault":0}
                                """))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("系统错误，请稍后重试"));
    }

    // ==================== 参数校验 400 - UpdateAddress ====================

    @Test
    @DisplayName("CT-050 更新地址时 name 为空")
    void updateAddress_emptyName() throws Exception {
        mockMvc.perform(put("/api/merchant/address/update/1")
                        .header("X-Shop-Id", "5001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id":1,"name":"","phone":"02112345678","address":"上海市","addressType":1,"isDefault":0}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("参数错误：收货人不能为空"));
    }

    @Test
    @DisplayName("CT-051 更新地址时 phone 为空")
    void updateAddress_emptyPhone() throws Exception {
        mockMvc.perform(put("/api/merchant/address/update/1")
                        .header("X-Shop-Id", "5001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id":1,"name":"店铺","phone":"","address":"上海市","addressType":1,"isDefault":0}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("参数错误：电话不能为空"));
    }

    @Test
    @DisplayName("CT-052 更新地址时 address 为空")
    void updateAddress_emptyAddress() throws Exception {
        mockMvc.perform(put("/api/merchant/address/update/1")
                        .header("X-Shop-Id", "5001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id":1,"name":"店铺","phone":"02112345678","address":"","addressType":1,"isDefault":0}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("参数错误：地址不能为空"));
    }

    @Test
    @DisplayName("CT-053 更新地址时 addressType 为空")
    void updateAddress_emptyAddressType() throws Exception {
        mockMvc.perform(put("/api/merchant/address/update/1")
                        .header("X-Shop-Id", "5001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id":1,"name":"店铺","phone":"02112345678","address":"上海市","isDefault":0}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("参数错误：地址类型不能为空"));
    }

    // ==================== 缺少 Header 401 - UpdateAddress ====================

    @Test
    @DisplayName("CT-054 更新地址时缺少 X-Shop-Id")
    void updateAddress_missingShopId() throws Exception {
        mockMvc.perform(put("/api/merchant/address/update/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id":1,"name":"店铺","phone":"02112345678","address":"上海市","addressType":1,"isDefault":0}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("未获取到店铺ID"));
    }

    // ==================== Service异常 500 - UpdateAddress ====================

    @Test
    @DisplayName("CT-055 更新地址 Service 异常")
    void updateAddress_serviceError() throws Exception {
        when(shopAddressService.updateAddress(any(ShopAddress.class), eq("5001"))).thenThrow(new RuntimeException("db error"));
        mockMvc.perform(put("/api/merchant/address/update/1")
                        .header("X-Shop-Id", "5001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id":1,"name":"店铺","phone":"02112345678","address":"上海市","addressType":1,"isDefault":0}
                                """))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("系统错误，请稍后重试"));
    }

    // ==================== 缺少 Header 401 - DeleteAddress ====================

    @Test
    @DisplayName("CT-056 删除地址时缺少 X-Shop-Id")
    void deleteAddress_missingShopId() throws Exception {
        mockMvc.perform(delete("/api/merchant/address/delete/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("未获取到店铺ID"));
    }

    // ==================== Service异常 500 - DeleteAddress ====================

    @Test
    @DisplayName("CT-057 删除地址 Service 异常")
    void deleteAddress_serviceError() throws Exception {
        when(shopAddressService.deleteAddress(1, "5001")).thenThrow(new RuntimeException("db error"));
        mockMvc.perform(delete("/api/merchant/address/delete/1")
                        .header("X-Shop-Id", "5001"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("系统错误，请稍后重试"));
    }

    // ==================== 缺少 Header 401 - GetAddressList ====================

    @Test
    @DisplayName("CT-058 查询地址列表时缺少 X-Shop-Id")
    void listAddress_missingShopId() throws Exception {
        mockMvc.perform(get("/api/merchant/address/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("未获取到店铺ID"));
    }

    // ==================== Service异常 500 - GetAddressList ====================

    @Test
    @DisplayName("CT-059 查询地址列表 Service 异常")
    void listAddress_serviceError() throws Exception {
        when(shopAddressService.getAddressesByShopId("5001")).thenThrow(new RuntimeException("db error"));
        mockMvc.perform(get("/api/merchant/address/list")
                        .header("X-Shop-Id", "5001"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("系统错误，请稍后重试"));
    }

    // ==================== 缺少 Header 401 - GetDefaultShipAddress ====================

    @Test
    @DisplayName("CT-060 查询默认发货地址时缺少 X-Shop-Id")
    void getDefaultShipAddress_missingShopId() throws Exception {
        mockMvc.perform(get("/api/merchant/address/ship-default"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("未获取到店铺ID"));
    }

    // ==================== Service异常 500 - GetDefaultShipAddress ====================

    @Test
    @DisplayName("CT-061 查询默认发货地址 Service 异常")
    void getDefaultShipAddress_serviceError() throws Exception {
        when(shopAddressService.getDefaultShipAddressByShopId("5001")).thenThrow(new RuntimeException("db error"));
        mockMvc.perform(get("/api/merchant/address/ship-default")
                        .header("X-Shop-Id", "5001"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("系统错误，请稍后重试"));
    }

    // ==================== 业务异常 400 - SetDefaultAddress ====================

    @Test
    @DisplayName("CT-062 设置不属于当前店铺的地址为默认")
    void setDefaultAddress_noPermission() throws Exception {
        when(shopAddressService.setDefaultAddress(20, "5001")).thenReturn(0);
        mockMvc.perform(put("/api/merchant/address/set-default/20")
                        .header("X-Shop-Id", "5001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("设置失败：地址不存在或不属于该店铺"));
    }

    // ==================== 缺少 Header 401 - SetDefaultAddress ====================

    @Test
    @DisplayName("CT-063 设置默认地址时缺少 X-Shop-Id")
    void setDefaultAddress_missingShopId() throws Exception {
        mockMvc.perform(put("/api/merchant/address/set-default/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("未获取到店铺ID"));
    }

    // ==================== Service异常 500 - SetDefaultAddress ====================

    @Test
    @DisplayName("CT-064 设置默认地址 Service 异常")
    void setDefaultAddress_serviceError() throws Exception {
        when(shopAddressService.setDefaultAddress(1, "5001")).thenThrow(new RuntimeException("db error"));
        mockMvc.perform(put("/api/merchant/address/set-default/1")
                        .header("X-Shop-Id", "5001"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("系统错误，请稍后重试"));
    }
}
