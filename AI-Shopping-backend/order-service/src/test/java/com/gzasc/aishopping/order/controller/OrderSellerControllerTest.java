package com.gzasc.aishopping.order.controller;

import com.gzasc.aishopping.order.dto.OrderDetailDTO;
import com.gzasc.aishopping.order.dto.SellerOrderCardDTO;
import com.gzasc.aishopping.order.dto.ReturnRequestDTO;
import com.gzasc.aishopping.order.dto.ReviewReturnRequest;
import com.gzasc.aishopping.common.dto.order.ShipOrderRequest;
import com.gzasc.aishopping.order.exception.OrderException;
import com.gzasc.aishopping.order.model.ReturnRequest;
import com.gzasc.aishopping.order.service.OrderService;
import com.gzasc.aishopping.order.service.ReturnRequestService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class OrderSellerControllerTest {

    private MockMvc mockMvc;

    @Mock
    private OrderService orderService;
    @Mock
    private ReturnRequestService returnRequestService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        var controller = new OrderSellerController(orderService, returnRequestService);
        var validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = standaloneSetup(controller)
                .setValidator(validator)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ==================== 发货 (OR-019 ~ OR-022) ====================

    @Test
    @DisplayName("OR-019 正常发货 - PAID→SHIPPED")
    void shipOrder_success() throws Exception {
        doNothing().when(orderService).shipOrder(anyString(), anyString(), any(ShipOrderRequest.class));

        mockMvc.perform(put("/api/seller/order/{orderId}/ship", "ORDER001")
                        .param("shopId", "SHOP001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"trackingNumber":"SF1234567890","contactId":1}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("发货成功"));
    }

    @Test
    @DisplayName("OR-020 发货 - 非PAID状态")
    void shipOrder_wrongStatus() throws Exception {
        doThrow(new OrderException("订单状态异常，发货失败"))
                .when(orderService).shipOrder(anyString(), anyString(), any(ShipOrderRequest.class));

        mockMvc.perform(put("/api/seller/order/{orderId}/ship", "PENDING001")
                        .param("shopId", "SHOP001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"trackingNumber":"SF1234567890","contactId":1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("订单状态异常，发货失败"));
    }

    @Test
    @DisplayName("OR-021 发货 - 商家不属于该店铺")
    void shipOrder_wrongShop() throws Exception {
        doThrow(new OrderException("订单不存在或无权限发货"))
                .when(orderService).shipOrder(anyString(), anyString(), any(ShipOrderRequest.class));

        mockMvc.perform(put("/api/seller/order/{orderId}/ship", "ORDER001")
                        .param("shopId", "WRONG_SHOP")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"trackingNumber":"SF1234567890","contactId":1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("订单不存在或无权限发货"));
    }

    @Test
    @DisplayName("OR-022 发货 - trackingNumber为空")
    void shipOrder_missingTrackingNumber() throws Exception {
        mockMvc.perform(put("/api/seller/order/{orderId}/ship", "ORDER001")
                        .param("shopId", "SHOP001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"trackingNumber":"","contactId":1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("OR-022 发货 - contactId为空")
    void shipOrder_nullContactId() throws Exception {
        mockMvc.perform(put("/api/seller/order/{orderId}/ship", "ORDER001")
                        .param("shopId", "SHOP001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"trackingNumber":"SF1234567890"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    // ==================== 确认退货 (OR-033) ====================

    @Test
    @DisplayName("OR-033 确认退货 - RETURNING→RETURNED")
    void confirmReturn_success() throws Exception {
        doNothing().when(orderService).confirmReturn(anyString(), anyString());

        mockMvc.perform(put("/api/seller/order/{orderId}/confirm-return", "ORDER001")
                        .param("shopId", "SHOP001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("退货已确认"));
    }

    @Test
    @DisplayName("OR-033 确认退货 - 失败场景")
    void confirmReturn_failure() throws Exception {
        doThrow(new OrderException("退货确认失败"))
                .when(orderService).confirmReturn(anyString(), anyString());

        mockMvc.perform(put("/api/seller/order/{orderId}/confirm-return", "ORDER001")
                        .param("shopId", "SHOP001"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("退货确认失败"));
    }

    @Nested
    @DisplayName("退货审核")
    class ReturnReviewTests {
        @Test
        @DisplayName("商家查看待审核列表")
        void listPendingReturns() throws Exception {
            ReturnRequestDTO dto = new ReturnRequestDTO();
            dto.setOrderId("ORDER001");
            dto.setReturnReason("瑕疵");
            dto.setStatus(ReturnRequest.APPLYING);
            when(returnRequestService.listByShop("SHOP001", ReturnRequest.APPLYING))
                    .thenReturn(List.of(dto));
            mockMvc.perform(get("/api/seller/order/return-requests/pending")
                            .param("shopId", "SHOP001"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].orderId").value("ORDER001"));
            verify(returnRequestService).listByShop("SHOP001", ReturnRequest.APPLYING);
        }

        @Test
        @DisplayName("商家查看已处理列表")
        void listProcessedReturns() throws Exception {
            ReturnRequestDTO agreed = new ReturnRequestDTO();
            agreed.setOrderId("ORDER001");
            agreed.setStatus(ReturnRequest.AGREED);
            ReturnRequestDTO rejected = new ReturnRequestDTO();
            rejected.setOrderId("ORDER002");
            rejected.setStatus(ReturnRequest.REJECTED);
            when(returnRequestService.listByShop("SHOP001", ReturnRequest.AGREED))
                    .thenReturn(List.of(agreed));
            when(returnRequestService.listByShop("SHOP001", ReturnRequest.REJECTED))
                    .thenReturn(List.of(rejected));
            mockMvc.perform(get("/api/seller/order/return-requests/processed")
                            .param("shopId", "SHOP001"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].orderId").value("ORDER001"))
                    .andExpect(jsonPath("$.data[1].orderId").value("ORDER002"));
            verify(returnRequestService).listByShop("SHOP001", ReturnRequest.AGREED);
            verify(returnRequestService).listByShop("SHOP001", ReturnRequest.REJECTED);
        }

        @Test
        @DisplayName("商家审核同意")
        void reviewReturnRequest_agreed() throws Exception {
            ReviewReturnRequest req = new ReviewReturnRequest();
            req.setStatus(ReturnRequest.AGREED);
            doNothing().when(returnRequestService).reviewReturnRequest(anyString(), anyString(), any());
            mockMvc.perform(put("/api/seller/order/return-requests/{orderId}/review", "ORDER001")
                            .param("shopId", "SHOP001")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("审核完成"));
            verify(returnRequestService).reviewReturnRequest("SHOP001", "ORDER001", req);
        }

        @Test
        @DisplayName("商家审核拒绝")
        void reviewReturnRequest_rejected() throws Exception {
            ReviewReturnRequest req = new ReviewReturnRequest();
            req.setStatus(ReturnRequest.REJECTED);
            doNothing().when(returnRequestService).reviewReturnRequest(anyString(), anyString(), any());
            mockMvc.perform(put("/api/seller/order/return-requests/{orderId}/review", "ORDER001")
                            .param("shopId", "SHOP001")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("审核完成"));
            verify(returnRequestService).reviewReturnRequest("SHOP001", "ORDER001", req);
        }

        @Test
        @DisplayName("审核 - 审核结果为空")
        void reviewReturnRequest_validationFail() throws Exception {
            ReviewReturnRequest req = new ReviewReturnRequest();
            req.setStatus("");
            mockMvc.perform(put("/api/seller/order/return-requests/{orderId}/review", "ORDER001")
                            .param("shopId", "SHOP001")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ==================== 查询 (OR-050 ~ OR-052, OR-055) ====================

    @Test
    @DisplayName("OR-050 商家查询店铺订单列表")
    void listShopOrders_success() throws Exception {
        SellerOrderCardDTO dto = new SellerOrderCardDTO();
        dto.setOrderId("ORDER001");
        dto.setOrderStatus("PAID");
        when(orderService.getOrdersByShopId("SHOP001")).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/seller/order/shop/{shopId}/list", "SHOP001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].orderId").value("ORDER001"));
    }

    @Test
    @DisplayName("OR-055 商家查询空列表")
    void listShopOrders_empty() throws Exception {
        when(orderService.getOrdersByShopId("SHOP001")).thenReturn(List.of());

        mockMvc.perform(get("/api/seller/order/shop/{shopId}/list", "SHOP001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("OR-051 商家查询订单详情")
    void getShopOrderDetail_success() throws Exception {
        OrderDetailDTO dto = new OrderDetailDTO();
        dto.setOrderId("ORDER001");
        dto.setOrderStatus("PAID");
        when(orderService.getOrderDetailByShop("SHOP001", "ORDER001")).thenReturn(dto);

        mockMvc.perform(get("/api/seller/order/shop/{shopId}/{orderId}", "SHOP001", "ORDER001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.orderId").value("ORDER001"));
    }

    @Test
    @DisplayName("OR-052 商家查询详情 - 订单不属于该店铺")
    void getShopOrderDetail_wrongShop() throws Exception {
        when(orderService.getOrderDetailByShop("WRONG_SHOP", "ORDER001"))
                .thenThrow(new OrderException("订单不存在或无权限查看"));

        mockMvc.perform(get("/api/seller/order/shop/{shopId}/{orderId}", "WRONG_SHOP", "ORDER001"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("订单不存在或无权限查看"));
    }
}
