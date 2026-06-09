package com.gzasc.aishopping.order.service;

import com.gzasc.aishopping.common.dto.logistics.LogisticsRequest;
import com.gzasc.aishopping.common.feign.logistics.LogisticsFeignClient;
import com.gzasc.aishopping.common.response.ApiResponse;
import com.gzasc.aishopping.order.dto.CreateReturnRequest;
import com.gzasc.aishopping.order.dto.ReviewReturnRequest;
import com.gzasc.aishopping.order.dto.ReturnRequestDTO;
import com.gzasc.aishopping.order.dto.SubmitReturnLogisticsRequest;
import com.gzasc.aishopping.order.dto.OrderDetailDTO;
import com.gzasc.aishopping.order.exception.OrderException;
import com.gzasc.aishopping.order.mapper.ReturnRequestMapper;
import com.gzasc.aishopping.order.model.Order;
import com.gzasc.aishopping.order.model.ReturnRequest;
import com.gzasc.aishopping.order.service.impl.ReturnRequestServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReturnRequestServiceImplTest {
    @Mock private ReturnRequestMapper returnRequestMapper;
    @Mock private OrderService orderService;
    @Mock private LogisticsFeignClient logisticsFeignClient;

    private ReturnRequestServiceImpl returnRequestService;

    private final Long userId = 100L;
    private final String shopId = "SHOP001";
    private final String orderId = "ORDER001";

    @BeforeEach
    void setUp() {
        returnRequestService = new ReturnRequestServiceImpl(returnRequestMapper, orderService, logisticsFeignClient);
    }

    @Test
    void createReturnRequest_createsApplyingRequestForShippedOrder() {
        CreateReturnRequest req = new CreateReturnRequest();
        req.setReturnReason("商品有瑕疵");

        OrderDetailDTO orderDto = new OrderDetailDTO();
        orderDto.setOrderId(orderId);
        orderDto.setUserId(userId);
        orderDto.setShopId(shopId);
        orderDto.setOrderStatus(Order.SHIPPED);

        when(orderService.getOrderDetailByUser(userId, orderId)).thenReturn(orderDto);
        when(returnRequestMapper.selectByOrderId(orderId)).thenReturn(null);
        when(returnRequestMapper.insert(any())).thenReturn(1);

        returnRequestService.createReturnRequest(userId, orderId, req);

        ArgumentCaptor<ReturnRequest> captor = ArgumentCaptor.forClass(ReturnRequest.class);
        verify(returnRequestMapper).insert(captor.capture());
        assertEquals(orderId, captor.getValue().getOrderId());
        assertEquals(userId, captor.getValue().getUserId());
        assertEquals(shopId, captor.getValue().getShopId());
        assertEquals("商品有瑕疵", captor.getValue().getReturnReason());
        assertEquals(ReturnRequest.APPLYING, captor.getValue().getStatus());
    }

    @Test
    void createReturnRequest_wrapsInsertRuntimeExceptionAsOrderException() {
        CreateReturnRequest req = new CreateReturnRequest();
        req.setReturnReason("商品有瑕疵");

        OrderDetailDTO orderDto = new OrderDetailDTO();
        orderDto.setOrderStatus(Order.SHIPPED);
        when(orderService.getOrderDetailByUser(userId, orderId)).thenReturn(orderDto);
        when(returnRequestMapper.selectByOrderId(orderId)).thenReturn(null);
        when(returnRequestMapper.insert(any())).thenThrow(new RuntimeException("duplicate key"));

        OrderException ex = assertThrows(OrderException.class, () -> returnRequestService.createReturnRequest(userId, orderId, req));
        assertEquals("创建退货申请失败", ex.getMessage());
    }

    @Test
    void createReturnRequest_rejectsMissingInvalidOrDuplicateOrder() {
        CreateReturnRequest req = new CreateReturnRequest();
        req.setReturnReason("原因");

        when(orderService.getOrderDetailByUser(userId, orderId)).thenReturn(null);
        assertThrows(OrderException.class, () -> returnRequestService.createReturnRequest(userId, orderId, req));

        OrderDetailDTO pendingOrder = new OrderDetailDTO();
        pendingOrder.setOrderStatus(Order.PENDING);
        when(orderService.getOrderDetailByUser(userId, orderId)).thenReturn(pendingOrder);
        assertThrows(OrderException.class, () -> returnRequestService.createReturnRequest(userId, orderId, req));

        OrderDetailDTO deliveredOrder = new OrderDetailDTO();
        deliveredOrder.setUserId(userId);
        deliveredOrder.setShopId(shopId);
        deliveredOrder.setOrderStatus(Order.DELIVERED);
        deliveredOrder.setOrderId(orderId);
        when(orderService.getOrderDetailByUser(userId, orderId)).thenReturn(deliveredOrder);
        when(returnRequestMapper.selectByOrderId(orderId)).thenReturn(returnRequest(ReturnRequest.APPLYING));
        assertThrows(OrderException.class, () -> returnRequestService.createReturnRequest(userId, orderId, req));
    }

    @Test
    void reviewReturnRequest_agreeUpdatesRequestAndOrderToReturnPending() {
        ReviewReturnRequest req = new ReviewReturnRequest();
        req.setStatus(ReturnRequest.AGREED);
        when(returnRequestMapper.selectByOrderIdAndShop(orderId, shopId)).thenReturn(returnRequest(ReturnRequest.APPLYING));
        when(returnRequestMapper.updateStatus(orderId, ReturnRequest.AGREED)).thenReturn(1);
        doNothing().when(orderService).agreeReturnRequest(shopId, orderId);

        returnRequestService.reviewReturnRequest(shopId, orderId, req);

        verify(returnRequestMapper).updateStatus(orderId, ReturnRequest.AGREED);
        verify(orderService).agreeReturnRequest(shopId, orderId);
    }

    @Test
    void reviewReturnRequest_rejectDoesNotChangeOrder() {
        ReviewReturnRequest req = new ReviewReturnRequest();
        req.setStatus(ReturnRequest.REJECTED);
        when(returnRequestMapper.selectByOrderIdAndShop(orderId, shopId)).thenReturn(returnRequest(ReturnRequest.APPLYING));
        when(returnRequestMapper.updateStatus(orderId, ReturnRequest.REJECTED)).thenReturn(1);

        returnRequestService.reviewReturnRequest(shopId, orderId, req);

        verify(returnRequestMapper).updateStatus(orderId, ReturnRequest.REJECTED);
        verify(orderService, never()).agreeReturnRequest(anyString(), anyString());
    }

    @Test
    void reviewReturnRequest_rejectsInvalidMissingProcessedOrCasFailure() {
        ReviewReturnRequest req = new ReviewReturnRequest();
        req.setStatus("invalid");
        assertThrows(OrderException.class, () -> returnRequestService.reviewReturnRequest(shopId, orderId, req));

        req.setStatus(ReturnRequest.AGREED);
        when(returnRequestMapper.selectByOrderIdAndShop(orderId, shopId)).thenReturn(null);
        assertThrows(OrderException.class, () -> returnRequestService.reviewReturnRequest(shopId, orderId, req));

        when(returnRequestMapper.selectByOrderIdAndShop(orderId, shopId)).thenReturn(returnRequest(ReturnRequest.REJECTED));
        assertThrows(OrderException.class, () -> returnRequestService.reviewReturnRequest(shopId, orderId, req));

        when(returnRequestMapper.selectByOrderIdAndShop(orderId, shopId)).thenReturn(returnRequest(ReturnRequest.APPLYING));
        when(returnRequestMapper.updateStatus(orderId, ReturnRequest.AGREED)).thenReturn(1);
        doThrow(new OrderException("订单状态变更失败，请重试"))
                .when(orderService).agreeReturnRequest(shopId, orderId);
        assertThrows(OrderException.class, () -> returnRequestService.reviewReturnRequest(shopId, orderId, req));
    }

    @Test
    void submitReturnLogistics_createsLogisticsStoresIdAndMovesOrderToReturning() {
        SubmitReturnLogisticsRequest req = new SubmitReturnLogisticsRequest();
        req.setTrackingNumber("SF123456789");
        req.setContactId(1);
        when(returnRequestMapper.selectByOrderIdAndUser(orderId, userId)).thenReturn(returnRequest(ReturnRequest.AGREED));

        OrderDetailDTO orderDto = new OrderDetailDTO();
        orderDto.setOrderId(orderId);
        orderDto.setUserId(userId);
        orderDto.setOrderStatus(Order.RETURN_PENDING);
        when(orderService.getOrderDetailByUser(userId, orderId)).thenReturn(orderDto);

        when(logisticsFeignClient.createLogistics(any(LogisticsRequest.class))).thenReturn(ApiResponse.success(Map.of("id", 42)));
        when(returnRequestMapper.updateLogisticsId(orderId, 42)).thenReturn(1);
        doNothing().when(orderService).submitReturnLogisticsStatus(userId, orderId);

        returnRequestService.submitReturnLogistics(userId, orderId, req);

        ArgumentCaptor<LogisticsRequest> captor = ArgumentCaptor.forClass(LogisticsRequest.class);
        verify(logisticsFeignClient).createLogistics(captor.capture());
        assertEquals(orderId, captor.getValue().getOrderId());
        assertEquals("RETURN", captor.getValue().getType());
        assertEquals(1, captor.getValue().getContactId());
        assertEquals("SF123456789", captor.getValue().getTrackingNumber());
        verify(returnRequestMapper).updateLogisticsId(orderId, 42);
        verify(orderService).submitReturnLogisticsStatus(userId, orderId);
    }

    @Test
    void submitReturnLogistics_rejectsInvalidStatesAndFailures() {
        SubmitReturnLogisticsRequest req = new SubmitReturnLogisticsRequest();
        req.setTrackingNumber("SF123");
        req.setContactId(1);
        when(returnRequestMapper.selectByOrderIdAndUser(orderId, userId)).thenReturn(null);
        assertThrows(OrderException.class, () -> returnRequestService.submitReturnLogistics(userId, orderId, req));

        when(returnRequestMapper.selectByOrderIdAndUser(orderId, userId)).thenReturn(returnRequest(ReturnRequest.APPLYING));
        assertThrows(OrderException.class, () -> returnRequestService.submitReturnLogistics(userId, orderId, req));

        ReturnRequest withLogistics = returnRequest(ReturnRequest.AGREED);
        withLogistics.setLogisticsId(99);
        when(returnRequestMapper.selectByOrderIdAndUser(orderId, userId)).thenReturn(withLogistics);
        assertThrows(OrderException.class, () -> returnRequestService.submitReturnLogistics(userId, orderId, req));

        when(returnRequestMapper.selectByOrderIdAndUser(orderId, userId)).thenReturn(returnRequest(ReturnRequest.AGREED));
        OrderDetailDTO shippedOrder = new OrderDetailDTO();
        shippedOrder.setOrderStatus(Order.SHIPPED);
        when(orderService.getOrderDetailByUser(userId, orderId)).thenReturn(shippedOrder);
        assertThrows(OrderException.class, () -> returnRequestService.submitReturnLogistics(userId, orderId, req));

        OrderDetailDTO returnPendingOrder = new OrderDetailDTO();
        returnPendingOrder.setOrderStatus(Order.RETURN_PENDING);
        when(orderService.getOrderDetailByUser(userId, orderId)).thenReturn(returnPendingOrder);
        when(logisticsFeignClient.createLogistics(any())).thenReturn(ApiResponse.error(500, "创建失败"));
        assertThrows(OrderException.class, () -> returnRequestService.submitReturnLogistics(userId, orderId, req));

        when(logisticsFeignClient.createLogistics(any())).thenReturn(ApiResponse.success(Map.of("id", 42)));
        when(returnRequestMapper.updateLogisticsId(orderId, 42)).thenReturn(0);
        assertThrows(OrderException.class, () -> returnRequestService.submitReturnLogistics(userId, orderId, req));

        when(returnRequestMapper.updateLogisticsId(orderId, 42)).thenReturn(1);
        doThrow(new OrderException("订单状态变更失败，请重试"))
                .when(orderService).submitReturnLogisticsStatus(userId, orderId);
        assertThrows(OrderException.class, () -> returnRequestService.submitReturnLogistics(userId, orderId, req));
    }

    @Test
    void submitReturnLogistics_rejectsNonSuccessApiResponseEvenWhenDataExists() {
        SubmitReturnLogisticsRequest req = returnLogisticsRequest();
        when(returnRequestMapper.selectByOrderIdAndUser(orderId, userId)).thenReturn(returnRequest(ReturnRequest.AGREED));

        OrderDetailDTO orderDto = new OrderDetailDTO();
        orderDto.setOrderStatus(Order.RETURN_PENDING);
        when(orderService.getOrderDetailByUser(userId, orderId)).thenReturn(orderDto);

        when(logisticsFeignClient.createLogistics(any())).thenReturn(new ApiResponse<>(500, "创建失败", Map.of("id", 42)));

        assertThrows(OrderException.class, () -> returnRequestService.submitReturnLogistics(userId, orderId, req));

        verify(returnRequestMapper, never()).updateLogisticsId(any(), any());
        verify(orderService, never()).submitReturnLogisticsStatus(anyLong(), anyString());
    }

    @Test
    void submitReturnLogistics_wrapsInvalidLogisticsIdsAsOrderException() {
        SubmitReturnLogisticsRequest req = returnLogisticsRequest();
        when(returnRequestMapper.selectByOrderIdAndUser(orderId, userId)).thenReturn(returnRequest(ReturnRequest.AGREED));

        OrderDetailDTO orderDto = new OrderDetailDTO();
        orderDto.setOrderStatus(Order.RETURN_PENDING);
        when(orderService.getOrderDetailByUser(userId, orderId)).thenReturn(orderDto);

        when(logisticsFeignClient.createLogistics(any())).thenReturn(ApiResponse.success(Map.of("id", "abc")));
        assertThrows(OrderException.class, () -> returnRequestService.submitReturnLogistics(userId, orderId, req));

        when(logisticsFeignClient.createLogistics(any())).thenReturn(ApiResponse.success(Map.of("id", 2147483648L)));
        assertThrows(OrderException.class, () -> returnRequestService.submitReturnLogistics(userId, orderId, req));

        verify(returnRequestMapper, never()).updateLogisticsId(any(), any());
        verify(orderService, never()).submitReturnLogisticsStatus(anyLong(), anyString());
    }

    @Test
    void submitReturnLogistics_wrapsFeignRuntimeExceptionAsOrderException() {
        SubmitReturnLogisticsRequest req = returnLogisticsRequest();
        when(returnRequestMapper.selectByOrderIdAndUser(orderId, userId)).thenReturn(returnRequest(ReturnRequest.AGREED));

        OrderDetailDTO orderDto = new OrderDetailDTO();
        orderDto.setOrderStatus(Order.RETURN_PENDING);
        when(orderService.getOrderDetailByUser(userId, orderId)).thenReturn(orderDto);

        when(logisticsFeignClient.createLogistics(any())).thenThrow(new RuntimeException("timeout"));

        assertThrows(OrderException.class, () -> returnRequestService.submitReturnLogistics(userId, orderId, req));

        verify(returnRequestMapper, never()).updateLogisticsId(any(), any());
        verify(orderService, never()).submitReturnLogisticsStatus(anyLong(), anyString());
    }

    @Test
    void listAndGetReturnRequestMapEntitiesToDtos() {
        when(returnRequestMapper.selectByShopAndStatus(shopId, ReturnRequest.APPLYING)).thenReturn(List.of(returnRequest(ReturnRequest.APPLYING)));
        List<ReturnRequestDTO> list = returnRequestService.listByShop(shopId, ReturnRequest.APPLYING);
        assertEquals(1, list.size());
        assertEquals(orderId, list.get(0).getOrderId());

        when(returnRequestMapper.selectByOrderId(orderId)).thenReturn(returnRequest(ReturnRequest.AGREED));
        ReturnRequestDTO dto = returnRequestService.getReturnRequestByOrderId(orderId);
        assertEquals(orderId, dto.getOrderId());
        assertEquals(ReturnRequest.AGREED, dto.getStatus());

        when(returnRequestMapper.selectByOrderId(orderId)).thenReturn(null);
        assertThrows(OrderException.class, () -> returnRequestService.getReturnRequestByOrderId(orderId));
    }

    private SubmitReturnLogisticsRequest returnLogisticsRequest() {
        SubmitReturnLogisticsRequest request = new SubmitReturnLogisticsRequest();
        request.setTrackingNumber("SF123");
        request.setContactId(1);
        return request;
    }

    private ReturnRequest returnRequest(String status) {
        ReturnRequest request = new ReturnRequest();
        request.setOrderId(orderId);
        request.setUserId(userId);
        request.setShopId(shopId);
        request.setReturnReason("商品有瑕疵");
        request.setStatus(status);
        request.setCreatedDate(new Timestamp(System.currentTimeMillis()));
        request.setUpdatedDate(new Timestamp(System.currentTimeMillis()));
        return request;
    }
}
