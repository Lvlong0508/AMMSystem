package com.gzasc.aishopping.logistics.controller;

import com.gzasc.aishopping.common.dto.logistics.LogisticsRequest;
import com.gzasc.aishopping.logistics.converter.LogisticsConverter;
import com.gzasc.aishopping.logistics.dto.LogisticsResponse;
import com.gzasc.aishopping.logistics.exception.LogisticsException;
import com.gzasc.aishopping.logistics.model.Logistics;
import com.gzasc.aishopping.logistics.service.LogisticsService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class InternalLogisticsControllerTest {

    private MockMvc mockMvc;

    @Mock
    private LogisticsService logisticsService;

    @Mock
    private LogisticsConverter logisticsConverter;

    @BeforeEach
    void setUp() {
        var controller = new InternalLogisticsController(logisticsService, logisticsConverter);
        var validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = standaloneSetup(controller)
                .setValidator(validator)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("LG-017 内部创建物流记录")
    void createLogistics_success() throws Exception {
        Logistics logistics = new Logistics();
        logistics.setOrderId("ORD003");
        logistics.setType("DELIVERY");
        logistics.setContactId(1);
        logistics.setTrackingNumber("SF1111111111");

        LogisticsResponse response = LogisticsResponse.builder()
                .id(1)
                .orderId("ORD003")
                .type("DELIVERY")
                .contactId(1)
                .trackingNumber("SF1111111111")
                .build();

        when(logisticsConverter.toModel(any(LogisticsRequest.class))).thenReturn(logistics);
        when(logisticsService.createLogistics(logistics)).thenReturn(response);

        mockMvc.perform(post("/internal/logistics/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId":"ORD003","type":"DELIVERY","contactId":1,"trackingNumber":"SF1111111111"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("创建物流信息成功"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.orderId").value("ORD003"))
                .andExpect(jsonPath("$.data.type").value("DELIVERY"))
                .andExpect(jsonPath("$.data.trackingNumber").value("SF1111111111"));
    }

    @Test
    @DisplayName("LG-017 内部创建物流记录 - 默认type为DELIVERY")
    void createLogistics_defaultType_success() throws Exception {
        Logistics logistics = new Logistics();
        logistics.setOrderId("ORD004");
        logistics.setType("DELIVERY");
        logistics.setContactId(1);
        logistics.setTrackingNumber("SF2222222222");

        LogisticsResponse response = LogisticsResponse.builder()
                .id(2).orderId("ORD004").type("DELIVERY").trackingNumber("SF2222222222").build();

        when(logisticsConverter.toModel(any(LogisticsRequest.class))).thenReturn(logistics);
        when(logisticsService.createLogistics(logistics)).thenReturn(response);

        mockMvc.perform(post("/internal/logistics/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId":"ORD004","contactId":1,"trackingNumber":"SF2222222222"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.type").value("DELIVERY"));
    }

    @Test
    @DisplayName("LG-018 内部查询订单物流列表")
    void getLogisticsByOrder_success() throws Exception {
        List<LogisticsResponse> list = List.of(
                LogisticsResponse.builder().id(1).orderId("ORD003").trackingNumber("SF001").build()
        );
        when(logisticsService.getLogisticsByOrderId("ORD003")).thenReturn(list);

        mockMvc.perform(get("/internal/logistics/order/ORD003"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].orderId").value("ORD003"));
    }

    @Test
    @DisplayName("LG-020 内部查询不存在的订单 - 空列表")
    void getLogisticsByOrder_empty_success() throws Exception {
        when(logisticsService.getLogisticsByOrderId("ORD_NOT_EXIST")).thenReturn(List.of());

        mockMvc.perform(get("/internal/logistics/order/ORD_NOT_EXIST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("LG-019 内部查询订单最新物流")
    void getLatestLogistics_success() throws Exception {
        LogisticsResponse response = LogisticsResponse.builder()
                .id(1).orderId("ORD003").type("DELIVERY").trackingNumber("SF001").build();
        when(logisticsService.getLatestLogistics("ORD003", "DELIVERY")).thenReturn(response);

        mockMvc.perform(get("/internal/logistics/order/ORD003/latest")
                        .param("type", "DELIVERY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.type").value("DELIVERY"));
    }

    @Test
    @DisplayName("LG-019 内部查询最新物流 - 不存在返回LogisticsException")
    void getLatestLogistics_notFound_throwsException() throws Exception {
        when(logisticsService.getLatestLogistics("ORD003", "RETURN"))
                .thenThrow(new LogisticsException("物流信息不存在"));

        mockMvc.perform(get("/internal/logistics/order/ORD003/latest")
                        .param("type", "RETURN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("物流信息不存在"));
    }

    @Test
    @DisplayName("LG-023 内部接口 - 未预期异常返回 500")
    void unexpectedException_returns500() throws Exception {
        when(logisticsService.getLogisticsByOrderId("ORD003"))
                .thenThrow(new RuntimeException("DB连接失败"));

        mockMvc.perform(get("/internal/logistics/order/ORD003"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500));
    }
}
