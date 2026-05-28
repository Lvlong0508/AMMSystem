package com.gzasc.aishopping.logistics.controller;

import com.gzasc.aishopping.logistics.dto.CreateLogisticsRequest;
import com.gzasc.aishopping.logistics.dto.LogisticsResponse;
import com.gzasc.aishopping.logistics.exception.LogisticsException;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class LogisticsControllerTest {

    private MockMvc mockMvc;

    @Mock
    private LogisticsService logisticsService;

    @BeforeEach
    void setUp() {
        var controller = new LogisticsController(logisticsService);
        var validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = standaloneSetup(controller)
                .setValidator(validator)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("LG-001 正常创建交付类型物流记录")
    void createLogistics_delivery_success() throws Exception {
        LogisticsResponse response = LogisticsResponse.builder()
                .id(1)
                .orderId("ORD001")
                .type("DELIVERY")
                .contactId(1)
                .trackingNumber("SF1234567890")
                .build();

        when(logisticsService.createLogistics(any(CreateLogisticsRequest.class))).thenReturn(response);

        mockMvc.perform(post("/logistics/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId":"ORD001","type":"DELIVERY","contactId":1,"trackingNumber":"SF1234567890"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("创建物流信息成功"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.orderId").value("ORD001"))
                .andExpect(jsonPath("$.data.type").value("DELIVERY"))
                .andExpect(jsonPath("$.data.trackingNumber").value("SF1234567890"));
    }

    @Test
    @DisplayName("LG-002 正常创建退货类型物流记录")
    void createLogistics_return_success() throws Exception {
        LogisticsResponse response = LogisticsResponse.builder()
                .id(2)
                .orderId("ORD002")
                .type("RETURN")
                .contactId(2)
                .trackingNumber("SF0987654321")
                .build();

        when(logisticsService.createLogistics(any(CreateLogisticsRequest.class))).thenReturn(response);

        mockMvc.perform(post("/logistics/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId":"ORD002","type":"RETURN","contactId":2,"trackingNumber":"SF0987654321"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.type").value("RETURN"))
                .andExpect(jsonPath("$.data.trackingNumber").value("SF0987654321"));
    }

    @Test
    @DisplayName("LG-003 创建物流记录 - orderId为空")
    void createLogistics_orderIdBlank_validationError() throws Exception {
        mockMvc.perform(post("/logistics/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId":"","type":"DELIVERY","contactId":1,"trackingNumber":"SF123"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("LG-003 创建物流记录 - orderId为null")
    void createLogistics_orderIdNull_validationError() throws Exception {
        mockMvc.perform(post("/logistics/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"DELIVERY","contactId":1,"trackingNumber":"SF123"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("LG-004 创建物流记录 - trackingNumber为空")
    void createLogistics_trackingNumberBlank_validationError() throws Exception {
        mockMvc.perform(post("/logistics/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId":"ORD001","type":"DELIVERY","contactId":1,"trackingNumber":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("LG-004 创建物流记录 - trackingNumber为null")
    void createLogistics_trackingNumberNull_validationError() throws Exception {
        mockMvc.perform(post("/logistics/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId":"ORD001","type":"DELIVERY","contactId":1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("LG-021 @Valid 校验失败 - contactId为null")
    void createLogistics_contactIdNull_validationError() throws Exception {
        mockMvc.perform(post("/logistics/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId":"ORD001","type":"DELIVERY","trackingNumber":"SF123"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("LG-006 查询全部物流列表")
    void getAllLogistics_success() throws Exception {
        List<LogisticsResponse> list = List.of(
                LogisticsResponse.builder().id(1).orderId("ORD001").trackingNumber("SF001").build(),
                LogisticsResponse.builder().id(2).orderId("ORD002").trackingNumber("SF002").build()
        );
        when(logisticsService.getAllLogistics()).thenReturn(list);

        mockMvc.perform(get("/logistics/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].orderId").value("ORD001"))
                .andExpect(jsonPath("$.data[1].orderId").value("ORD002"));
    }

    @Test
    @DisplayName("LG-014 查询全部物流列表 - 空数据")
    void getAllLogistics_empty_success() throws Exception {
        when(logisticsService.getAllLogistics()).thenReturn(List.of());

        mockMvc.perform(get("/logistics/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("LG-007 按运单号查询物流记录 - 存在")
    void getLogisticsByTrackingNumber_found_success() throws Exception {
        LogisticsResponse response = LogisticsResponse.builder()
                .id(1).orderId("ORD001").trackingNumber("SF1234567890").build();
        when(logisticsService.getLogisticsByTrackingNumber("SF1234567890")).thenReturn(response);

        mockMvc.perform(get("/logistics/search/tracking")
                        .param("trackingNumber", "SF1234567890"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.trackingNumber").value("SF1234567890"));
    }

    @Test
    @DisplayName("LG-011 按不存在的运单号查询 - 抛出LogisticsException")
    void getLogisticsByTrackingNumber_notFound_throwsException() throws Exception {
        when(logisticsService.getLogisticsByTrackingNumber("NONEXIST"))
                .thenThrow(new LogisticsException("物流信息不存在"));

        mockMvc.perform(get("/logistics/search/tracking")
                        .param("trackingNumber", "NONEXIST"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("物流信息不存在"));
    }

    @Test
    @DisplayName("LG-008 按订单ID查询物流列表")
    void getLogisticsByOrderId_success() throws Exception {
        List<LogisticsResponse> list = List.of(
                LogisticsResponse.builder().id(1).orderId("ORD001").trackingNumber("SF001").build()
        );
        when(logisticsService.getLogisticsByOrderId("ORD001")).thenReturn(list);

        mockMvc.perform(get("/logistics/order/ORD001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].orderId").value("ORD001"));
    }

    @Test
    @DisplayName("LG-012 按不存在的订单ID查询 - 空列表不抛异常")
    void getLogisticsByOrderId_empty_success() throws Exception {
        when(logisticsService.getLogisticsByOrderId("ORD999")).thenReturn(List.of());

        mockMvc.perform(get("/logistics/order/ORD999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("LG-009 查询订单最新物流记录 - DELIVERY")
    void getLatestLogistics_delivery_success() throws Exception {
        LogisticsResponse response = LogisticsResponse.builder()
                .id(1).orderId("ORD001").type("DELIVERY").trackingNumber("SF001").build();
        when(logisticsService.getLatestLogistics("ORD001", "DELIVERY")).thenReturn(response);

        mockMvc.perform(get("/logistics/order/ORD001/latest")
                        .param("type", "DELIVERY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.type").value("DELIVERY"));
    }

    @Test
    @DisplayName("LG-010 查询订单最新物流记录 - RETURN")
    void getLatestLogistics_return_success() throws Exception {
        LogisticsResponse response = LogisticsResponse.builder()
                .id(2).orderId("ORD002").type("RETURN").trackingNumber("SF002").build();
        when(logisticsService.getLatestLogistics("ORD002", "RETURN")).thenReturn(response);

        mockMvc.perform(get("/logistics/order/ORD002/latest")
                        .param("type", "RETURN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.type").value("RETURN"));
    }

    @Test
    @DisplayName("LG-013 查询最新物流 - 不存在返回LogisticsException")
    void getLatestLogistics_notFound_throwsException() throws Exception {
        when(logisticsService.getLatestLogistics("ORD001", "RETURN"))
                .thenThrow(new LogisticsException("物流信息不存在"));

        mockMvc.perform(get("/logistics/order/ORD001/latest")
                        .param("type", "RETURN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("物流信息不存在"));
    }

    @Test
    @DisplayName("LG-015 删除存在的物流记录")
    void deleteLogisticsById_success() throws Exception {
        doNothing().when(logisticsService).deleteLogisticsById(1);

        mockMvc.perform(delete("/logistics/delete/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("删除物流信息成功"));
    }

    @Test
    @DisplayName("LG-016 删除不存在的物流记录")
    void deleteLogisticsById_notFound_throwsException() throws Exception {
        doThrow(new LogisticsException("物流信息不存在")).when(logisticsService).deleteLogisticsById(9999);

        mockMvc.perform(delete("/logistics/delete/9999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("物流信息不存在"));
    }

    @Test
    @DisplayName("LG-022 LogisticsException 返回 400 - 按运单号查询")
    void logisticsException_returns400() throws Exception {
        when(logisticsService.getLogisticsByTrackingNumber("UNKNOWN"))
                .thenThrow(new LogisticsException("物流信息不存在"));

        mockMvc.perform(get("/logistics/search/tracking")
                        .param("trackingNumber", "UNKNOWN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("LG-023 未预期异常返回 500")
    void unexpectedException_returns500() throws Exception {
        when(logisticsService.getAllLogistics()).thenThrow(new RuntimeException("DB连接失败"));

        mockMvc.perform(get("/logistics/list"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    @DisplayName("LG-005 无效type - type为纯字符串无校验, 可正常调用service")
    void createLogistics_invalidType_passesToService() throws Exception {
        LogisticsResponse response = LogisticsResponse.builder()
                .id(3).orderId("ORD003").type("INVALID").trackingNumber("SF999").build();

        when(logisticsService.createLogistics(any(CreateLogisticsRequest.class))).thenReturn(response);

        mockMvc.perform(post("/logistics/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId":"ORD003","type":"INVALID","contactId":1,"trackingNumber":"SF999"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.type").value("INVALID"));
    }
}
