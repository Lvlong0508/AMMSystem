package com.gzasc.aishopping.order.controller;

import com.gzasc.aishopping.order.dto.CreateReturnRequest;
import com.gzasc.aishopping.order.dto.OrderDetailDTO;
import com.gzasc.aishopping.order.dto.UserOrderCardDTO;
import com.gzasc.aishopping.order.dto.PlaceOrderRequest;
import com.gzasc.aishopping.order.dto.SubmitReturnLogisticsRequest;
import com.gzasc.aishopping.order.exception.OrderException;
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

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class OrderUserControllerTest {

    private MockMvc mockMvc;

    @Mock
    private OrderService orderService;
    @Mock
    private ReturnRequestService returnRequestService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        var controller = new OrderUserController(orderService, returnRequestService);
        var validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = standaloneSetup(controller)
                .setValidator(validator)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ==================== 下单 (OR-001 ~ OR-007) ====================

    @Test
    @DisplayName("OR-001 正常下单 - 所有参数合法")
    void placeOrder_success() throws Exception {
        when(orderService.createOrder(any(), anyLong())).thenReturn("2026052800001ABCDE");

        mockMvc.perform(post("/api/user/order/place")
                        .header("X-User-Id", "100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":"1","quantity":2,"contactId":1}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("2026052800001ABCDE"));
    }

    @Test
    @DisplayName("OR-006 下单 - 数量为0")
    void placeOrder_quantityZero() throws Exception {
        mockMvc.perform(post("/api/user/order/place")
                        .header("X-User-Id", "100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":"1","quantity":0,"contactId":1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("OR-007 下单 - X-User-Id 缺失")
    void placeOrder_missingUserId() throws Exception {
        mockMvc.perform(post("/api/user/order/place")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":"1","quantity":1,"contactId":1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("OR-002 下单 - 抛出OrderException返回400")
    void placeOrder_orderException() throws Exception {
        when(orderService.createOrder(any(), anyLong()))
                .thenThrow(new OrderException("商品不存在（错误代码：O-003）"));

        mockMvc.perform(post("/api/user/order/place")
                        .header("X-User-Id", "100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":"999","quantity":1,"contactId":1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("商品不存在（错误代码：O-003）"));
    }

    // ==================== 支付 (OR-008 ~ OR-011) ====================

    @Test
    @DisplayName("OR-008 正常支付 - PENDING→PAID")
    void payOrder_success() throws Exception {
        doNothing().when(orderService).payOrder(anyLong(), anyString());

        mockMvc.perform(put("/api/user/order/{orderId}/pay", "ORDER001")
                        .header("X-User-Id", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("支付成功"));
    }

    @Test
    @DisplayName("OR-009 支付 - 订单不属于当前用户")
    void payOrder_notOwner() throws Exception {
        doThrow(new OrderException("订单不存在或无权限操作"))
                .when(orderService).payOrder(anyLong(), anyString());

        mockMvc.perform(put("/api/user/order/{orderId}/pay", "ORDER001")
                        .header("X-User-Id", "999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("订单不存在或无权限操作"));
    }

    @Test
    @DisplayName("OR-010 支付 - 状态不是PENDING")
    void payOrder_wrongStatus() throws Exception {
        doThrow(new OrderException("订单状态异常，支付失败"))
                .when(orderService).payOrder(anyLong(), anyString());

        mockMvc.perform(put("/api/user/order/{orderId}/pay", "ORDER001")
                        .header("X-User-Id", "100"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("订单状态异常，支付失败"));
    }

    @Test
    @DisplayName("OR-011 支付 - 订单不存在")
    void payOrder_notFound() throws Exception {
        doThrow(new OrderException("订单不存在或无权限操作"))
                .when(orderService).payOrder(anyLong(), anyString());

        mockMvc.perform(put("/api/user/order/{orderId}/pay", "NONEXISTENT")
                        .header("X-User-Id", "100"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("订单不存在或无权限操作"));
    }

    // ==================== 取消 (OR-014 ~ OR-018) ====================

    @Test
    @DisplayName("OR-014 取消 PENDING 订单")
    void cancelOrder_pending() throws Exception {
        doNothing().when(orderService).cancelOrder(anyLong(), anyString());

        mockMvc.perform(put("/api/user/order/{orderId}/cancel", "ORDER001")
                        .header("X-User-Id", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("取消订单成功"));
    }

    @Test
    @DisplayName("OR-016 取消 - 不允许的状态")
    void cancelOrder_wrongStatus() throws Exception {
        doThrow(new OrderException("订单状态已变更，取消失败"))
                .when(orderService).cancelOrder(anyLong(), anyString());

        mockMvc.perform(put("/api/user/order/{orderId}/cancel", "ORDER001")
                        .header("X-User-Id", "100"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("订单状态已变更，取消失败"));
    }

    @Test
    @DisplayName("OR-017 取消 - 订单不属于当前用户")
    void cancelOrder_notOwner() throws Exception {
        doThrow(new OrderException("订单不存在或无权限取消"))
                .when(orderService).cancelOrder(anyLong(), anyString());

        mockMvc.perform(put("/api/user/order/{orderId}/cancel", "ORDER001")
                        .header("X-User-Id", "999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("订单不存在或无权限取消"));
    }

    // ==================== 删除 (OR-036 ~ OR-039) ====================

    @Test
    @DisplayName("OR-036 删除 DELIVERED 订单")
    void deleteOrder_delivered() throws Exception {
        doNothing().when(orderService).deleteOrder(anyLong(), anyString());

        mockMvc.perform(delete("/api/user/order/{orderId}", "ORDER001")
                        .header("X-User-Id", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("删除订单成功"));
    }

    @Test
    @DisplayName("OR-038 删除 - 不允许的状态")
    void deleteOrder_wrongStatus() throws Exception {
        doThrow(new OrderException("当前订单状态不允许删除"))
                .when(orderService).deleteOrder(anyLong(), anyString());

        mockMvc.perform(delete("/api/user/order/{orderId}", "ORDER001")
                        .header("X-User-Id", "100"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("当前订单状态不允许删除"));
    }

    // ==================== 确认收货 (OR-025 ~ OR-027) ====================

    @Test
    @DisplayName("OR-025 正常确认收货 - SHIPPED→DELIVERED")
    void deliverOrder_success() throws Exception {
        doNothing().when(orderService).deliverOrder(anyLong(), anyString());

        mockMvc.perform(put("/api/user/order/{orderId}/deliver", "ORDER001")
                        .header("X-User-Id", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("确认收货成功"));
    }

    @Test
    @DisplayName("OR-026 确认收货 - 非SHIPPED状态")
    void deliverOrder_wrongStatus() throws Exception {
        doThrow(new OrderException("订单确认收货失败"))
                .when(orderService).deliverOrder(anyLong(), anyString());

        mockMvc.perform(put("/api/user/order/{orderId}/deliver", "ORDER001")
                        .header("X-User-Id", "100"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("订单确认收货失败"));
    }

    // ==================== 退货申请 (OR-028 ~ OR-030) ====================

    @Test
    @DisplayName("OR-028 申请退货 - 正常提交")
    void requestReturn_success() throws Exception {
        CreateReturnRequest req = new CreateReturnRequest();
        req.setReturnReason("商品有瑕疵");
        doNothing().when(returnRequestService).createReturnRequest(anyLong(), anyString(), any());
        mockMvc.perform(post("/api/user/order/{orderId}/return-request", "ORDER001")
                        .header("X-User-Id", "100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("退货申请已提交"));
        verify(returnRequestService).createReturnRequest(100L, "ORDER001", req);
    }

    @Test
    @DisplayName("OR-029 申请退货 - 参数校验失败（原因空）")
    void requestReturn_validationFail() throws Exception {
        CreateReturnRequest req = new CreateReturnRequest();
        req.setReturnReason("");
        mockMvc.perform(post("/api/user/order/{orderId}/return-request", "ORDER001")
                        .header("X-User-Id", "100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Nested
    @DisplayName("退货物流")
    class ReturnLogisticsTests {
        @Test
        @DisplayName("提交退货物流成功")
        void submitReturnLogistics_success() throws Exception {
            SubmitReturnLogisticsRequest req = new SubmitReturnLogisticsRequest();
            req.setTrackingNumber("SF123456789");
            req.setContactId(1);
            doNothing().when(returnRequestService).submitReturnLogistics(anyLong(), anyString(), any());
            mockMvc.perform(post("/api/user/order/{orderId}/return-logistics", "ORDER001")
                            .header("X-User-Id", "100")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("退货物流已提交"));
            verify(returnRequestService).submitReturnLogistics(100L, "ORDER001", req);
        }

        @Test
        @DisplayName("提交退货物流-参数校验失败")
        void submitReturnLogistics_validationFail() throws Exception {
            SubmitReturnLogisticsRequest req = new SubmitReturnLogisticsRequest();
            req.setTrackingNumber("");
            req.setContactId(null);
            mockMvc.perform(post("/api/user/order/{orderId}/return-logistics", "ORDER001")
                            .header("X-User-Id", "100")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ==================== 查询 (OR-046 ~ OR-049, OR-055) ====================

    @Test
    @DisplayName("OR-046 用户查询订单列表")
    void listOrders_success() throws Exception {
        UserOrderCardDTO dto = new UserOrderCardDTO();
        dto.setOrderId("ORDER001");
        dto.setOrderStatus("PAID");
        dto.setTotalPrice(BigDecimal.valueOf(100));
        dto.setQuantity(2);
        when(orderService.getOrdersByUserId(100L)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/user/order/list")
                        .header("X-User-Id", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].orderId").value("ORDER001"))
                .andExpect(jsonPath("$.data[0].orderStatus").value("PAID"));
    }

    @Test
    @DisplayName("OR-055 查询空列表")
    void listOrders_empty() throws Exception {
        when(orderService.getOrdersByUserId(100L)).thenReturn(List.of());

        mockMvc.perform(get("/api/user/order/list")
                        .header("X-User-Id", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("OR-047 用户查询订单详情")
    void getOrderDetail_success() throws Exception {
        OrderDetailDTO dto = new OrderDetailDTO();
        dto.setOrderId("ORDER001");
        dto.setOrderStatus("PAID");
        dto.setContactName("张三");
        dto.setContactPhone("13800138001");
        dto.setContactAddress("北京市");
        when(orderService.getOrderDetailByUser(100L, "ORDER001")).thenReturn(dto);

        mockMvc.perform(get("/api/user/order/{orderId}", "ORDER001")
                        .header("X-User-Id", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.orderId").value("ORDER001"))
                .andExpect(jsonPath("$.data.contactName").value("张三"));
    }

    @Test
    @DisplayName("OR-048 用户查询详情 - 订单不属于当前用户")
    void getOrderDetail_notOwner() throws Exception {
        when(orderService.getOrderDetailByUser(999L, "ORDER001"))
                .thenThrow(new OrderException("订单不存在或无权限查看"));

        mockMvc.perform(get("/api/user/order/{orderId}", "ORDER001")
                        .header("X-User-Id", "999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("订单不存在或无权限查看"));
    }

    @Test
    @DisplayName("OR-049 用户查询详情 - 订单已删除")
    void getOrderDetail_deleted() throws Exception {
        when(orderService.getOrderDetailByUser(100L, "DELETED001"))
                .thenThrow(new OrderException("订单不存在或无权限查看"));

        mockMvc.perform(get("/api/user/order/{orderId}", "DELETED001")
                        .header("X-User-Id", "100"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("订单不存在或无权限查看"));
    }
}
