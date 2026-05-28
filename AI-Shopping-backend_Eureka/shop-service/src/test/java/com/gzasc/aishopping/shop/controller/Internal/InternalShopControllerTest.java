package com.gzasc.aishopping.shop.controller.Internal;

import com.gzasc.aishopping.common.dto.shop.ShopInfoDTO;
import com.gzasc.aishopping.shop.controller.GlobalExceptionHandler;
import com.gzasc.aishopping.shop.model.MerchantRole;
import com.gzasc.aishopping.shop.service.MerchantRoleService;
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
import java.util.Set;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class InternalShopControllerTest {

    private MockMvc mockMvc;

    @Mock
    private MerchantRoleService merchantRoleService;

    @Mock
    private ShopService shopService;

    @BeforeEach
    void setUp() {
        var controller = new InternalShopController(merchantRoleService, shopService);
        var validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = standaloneSetup(controller)
                .setValidator(validator)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("SH-035 查询商家角色列表")
    void getMerchantRoles() throws Exception {
        MerchantRole r1 = new MerchantRole(1L, 1001L, 1L, 1, 1001L, null);
        MerchantRole r2 = new MerchantRole(2L, 1001L, 2L, 1, 1001L, null);
        MerchantRole r3 = new MerchantRole(3L, 1001L, 3L, 2, 1001L, null);
        when(merchantRoleService.selectByMerchantId(1001L)).thenReturn(List.of(r1, r2, r3));

        mockMvc.perform(get("/internal/shop/employees/roles/1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.roles.length()").value(3))
                .andExpect(jsonPath("$.data.roles[0].merchantId").value(1001))
                .andExpect(jsonPath("$.data.roles[0].shopId").value(1))
                .andExpect(jsonPath("$.data.roles[0].role").value(1));
    }

    @Test
    @DisplayName("SH-036 查询商家角色列表 - 无角色")
    void getMerchantRoles_empty() throws Exception {
        when(merchantRoleService.selectByMerchantId(999L)).thenReturn(List.of());

        mockMvc.perform(get("/internal/shop/employees/roles/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.roles").isEmpty());
    }

    @Test
    @DisplayName("SH-037 查询店铺信息")
    void getShopInfo() throws Exception {
        ShopInfoDTO dto = new ShopInfoDTO(10L, "测试店铺", "测试描述", "logo");
        when(shopService.getShopInfoById(1L)).thenReturn(dto);

        mockMvc.perform(get("/internal/shop/info/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.name").value("测试店铺"))
                .andExpect(jsonPath("$.data.description").value("测试描述"));
    }

    @Test
    @DisplayName("SH-038 查询店铺信息 - 无关联info")
    void getShopInfo_null() throws Exception {
        when(shopService.getShopInfoById(1L)).thenReturn(null);

        mockMvc.perform(get("/internal/shop/info/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("SH-039 批量查询店铺信息")
    void batchGetShopInfo() throws Exception {
        ShopInfoDTO dto1 = new ShopInfoDTO(10L, "店铺A", "描述A", "logoA");
        ShopInfoDTO dto2 = new ShopInfoDTO(20L, "店铺B", "描述B", "logoB");
        when(shopService.batchGetShopInfo(Set.of(1L, 2L, 3L)))
                .thenReturn(Map.of(1L, dto1, 2L, dto2));

        mockMvc.perform(post("/internal/shop/info/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[1,2,3]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.['1'].name").value("店铺A"))
                .andExpect(jsonPath("$.data.['2'].name").value("店铺B"))
                .andExpect(jsonPath("$.data.['3']").doesNotExist());
    }

    @Test
    @DisplayName("SH-040 批量查询 - 空集合")
    void batchGetShopInfo_empty() throws Exception {
        when(shopService.batchGetShopInfo(Set.of())).thenReturn(Map.of());

        mockMvc.perform(post("/internal/shop/info/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isEmpty());
    }
}
