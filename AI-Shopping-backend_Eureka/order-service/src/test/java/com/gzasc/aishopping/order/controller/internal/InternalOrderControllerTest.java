package com.gzasc.aishopping.order.controller.internal;

import com.gzasc.aishopping.order.controller.GlobalExceptionHandler;
import com.gzasc.aishopping.order.dto.OrderDetailDTO;
import com.gzasc.aishopping.order.dto.UserOrderCardDTO;
import com.gzasc.aishopping.order.exception.OrderException;
import com.gzasc.aishopping.order.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class InternalOrderControllerTest {

    private MockMvc mockMvc;

    @Mock
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        var controller = new InternalOrderController(orderService);
        mockMvc = standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("OR-053 内部查询订单详情")
    void getOrderById_success() throws Exception {
        OrderDetailDTO dto = new OrderDetailDTO();
        dto.setOrderId("ORDER001");
        dto.setOrderStatus("PAID");
        when(orderService.getOrderDetailByUser(100L, "ORDER001")).thenReturn(dto);

        mockMvc.perform(get("/internal/order/{orderId}", "ORDER001")
                        .header("X-User-Id", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value("ORDER001"))
                .andExpect(jsonPath("$.orderStatus").value("PAID"));
    }

    @Test
    @DisplayName("OR-054 内部查询订单列表")
    void getAllOrders_success() throws Exception {
        UserOrderCardDTO dto = new UserOrderCardDTO();
        dto.setOrderId("ORDER001");
        when(orderService.getOrdersByUserId(100L)).thenReturn(List.of(dto));

        mockMvc.perform(get("/internal/order/list")
                        .header("X-User-Id", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderId").value("ORDER001"));
    }

    @Test
    @DisplayName("OR-053 内部查询详情 - 订单不存在")
    void getOrderById_notFound() throws Exception {
        when(orderService.getOrderDetailByUser(anyLong(), anyString()))
                .thenThrow(new OrderException("订单不存在或无权限查看"));

        mockMvc.perform(get("/internal/order/{orderId}", "NONEXISTENT")
                        .header("X-User-Id", "100"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("订单不存在或无权限查看"));
    }

    @Test
    @DisplayName("OR-055 内部查询空列表")
    void getAllOrders_empty() throws Exception {
        when(orderService.getOrdersByUserId(100L)).thenReturn(List.of());

        mockMvc.perform(get("/internal/order/list")
                        .header("X-User-Id", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
