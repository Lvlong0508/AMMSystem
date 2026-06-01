package com.gzasc.aishopping.logistics.service.impl;

import com.gzasc.aishopping.logistics.converter.LogisticsConverter;
import com.gzasc.aishopping.logistics.dto.CreateLogisticsRequest;
import com.gzasc.aishopping.logistics.dto.LogisticsResponse;
import com.gzasc.aishopping.logistics.exception.LogisticsException;
import com.gzasc.aishopping.logistics.mapper.LogisticsMapper;
import com.gzasc.aishopping.logistics.model.Logistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LogisticsServiceImplTest {

    @Mock
    private LogisticsMapper logisticsMapper;

    @Mock
    private LogisticsConverter logisticsConverter;

    @InjectMocks
    private LogisticsServiceImpl logisticsService;

    @Test
    @DisplayName("LG-001 正常创建交付类型物流记录 - CreateLogisticsRequest")
    void createLogistics_withRequest_success() {
        CreateLogisticsRequest request = new CreateLogisticsRequest();
        request.setOrderId("ORD001");
        request.setType("DELIVERY");
        request.setContactId(1);
        request.setTrackingNumber("SF1234567890");

        Logistics logistics = new Logistics();
        logistics.setOrderId("ORD001");
        logistics.setType("DELIVERY");
        logistics.setContactId(1);
        logistics.setTrackingNumber("SF1234567890");

        LogisticsResponse response = LogisticsResponse.builder()
                .id(1)
                .orderId("ORD001")
                .type("DELIVERY")
                .contactId(1)
                .trackingNumber("SF1234567890")
                .build();

        when(logisticsConverter.toModel(request)).thenReturn(logistics);
        when(logisticsMapper.insertLogistics(logistics)).thenReturn(1);
        when(logisticsConverter.toResponse(logistics)).thenReturn(response);

        LogisticsResponse result = logisticsService.createLogistics(request);

        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("ORD001", result.getOrderId());
        assertEquals("DELIVERY", result.getType());
        assertEquals("SF1234567890", result.getTrackingNumber());

        verify(logisticsConverter).toModel(request);
        verify(logisticsMapper).insertLogistics(logistics);
        verify(logisticsConverter).toResponse(logistics);
    }

    @Test
    @DisplayName("LG-002 正常创建退货类型物流记录 - CreateLogisticsRequest")
    void createLogistics_withRequest_returnType_success() {
        CreateLogisticsRequest request = new CreateLogisticsRequest();
        request.setOrderId("ORD002");
        request.setType("RETURN");
        request.setContactId(2);
        request.setTrackingNumber("SF0987654321");

        Logistics logistics = new Logistics();
        logistics.setOrderId("ORD002");
        logistics.setType("RETURN");
        logistics.setContactId(2);
        logistics.setTrackingNumber("SF0987654321");

        LogisticsResponse response = LogisticsResponse.builder()
                .id(2)
                .orderId("ORD002")
                .type("RETURN")
                .contactId(2)
                .trackingNumber("SF0987654321")
                .build();

        when(logisticsConverter.toModel(request)).thenReturn(logistics);
        when(logisticsMapper.insertLogistics(logistics)).thenReturn(1);
        when(logisticsConverter.toResponse(logistics)).thenReturn(response);

        LogisticsResponse result = logisticsService.createLogistics(request);

        assertEquals("RETURN", result.getType());
        assertEquals("SF0987654321", result.getTrackingNumber());
    }

    @Test
    @DisplayName("LG-001 正常创建物流记录 - Logistics模型直接创建")
    void createLogistics_withModel_success() {
        Logistics logistics = new Logistics();
        logistics.setOrderId("ORD001");
        logistics.setType("DELIVERY");
        logistics.setContactId(1);
        logistics.setTrackingNumber("SF1234567890");

        LogisticsResponse response = LogisticsResponse.builder()
                .id(1)
                .orderId("ORD001")
                .type("DELIVERY")
                .contactId(1)
                .trackingNumber("SF1234567890")
                .build();

        when(logisticsMapper.insertLogistics(logistics)).thenReturn(1);
        when(logisticsConverter.toResponse(logistics)).thenReturn(response);

        LogisticsResponse result = logisticsService.createLogistics(logistics);

        assertNotNull(result);
        assertEquals(1, result.getId());
        verify(logisticsMapper).insertLogistics(logistics);
        verify(logisticsConverter).toResponse(logistics);
    }

    @Test
    @DisplayName("LG-001 创建物流 - insert返回0抛出异常")
    void createLogistics_insertFailed_throwsException() {
        Logistics logistics = new Logistics();
        logistics.setOrderId("ORD001");
        logistics.setTrackingNumber("SF1234567890");

        when(logisticsMapper.insertLogistics(logistics)).thenReturn(0);

        assertThrows(LogisticsException.class, () -> logisticsService.createLogistics(logistics));
        verify(logisticsMapper).insertLogistics(logistics);
        verify(logisticsConverter, never()).toResponse(any());
    }

    @Test
    @DisplayName("LG-006 查询全部物流列表 - 有数据")
    void getAllLogistics_withData_success() {
        Logistics l1 = new Logistics();
        l1.setId(1);
        l1.setOrderId("ORD001");
        Logistics l2 = new Logistics();
        l2.setId(2);
        l2.setOrderId("ORD002");

        LogisticsResponse r1 = LogisticsResponse.builder().id(1).orderId("ORD001").build();
        LogisticsResponse r2 = LogisticsResponse.builder().id(2).orderId("ORD002").build();

        when(logisticsMapper.selectAllLogistics()).thenReturn(List.of(l1, l2));
        when(logisticsConverter.toResponse(l1)).thenReturn(r1);
        when(logisticsConverter.toResponse(l2)).thenReturn(r2);

        List<LogisticsResponse> result = logisticsService.getAllLogistics();

        assertEquals(2, result.size());
        assertEquals("ORD001", result.get(0).getOrderId());
        assertEquals("ORD002", result.get(1).getOrderId());
    }

    @Test
    @DisplayName("LG-014 查询全部物流列表 - 空数据")
    void getAllLogistics_emptyList_success() {
        when(logisticsMapper.selectAllLogistics()).thenReturn(List.of());

        List<LogisticsResponse> result = logisticsService.getAllLogistics();

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("LG-007 按运单号查询物流记录 - 存在")
    void getLogisticsByTrackingNumber_found_success() {
        Logistics logistics = new Logistics();
        logistics.setId(1);
        logistics.setTrackingNumber("SF1234567890");
        logistics.setOrderId("ORD001");

        LogisticsResponse response = LogisticsResponse.builder()
                .id(1).trackingNumber("SF1234567890").orderId("ORD001").build();

        when(logisticsMapper.selectLogisticsByTrackingNumber("SF1234567890")).thenReturn(logistics);
        when(logisticsConverter.toResponse(logistics)).thenReturn(response);

        LogisticsResponse result = logisticsService.getLogisticsByTrackingNumber("SF1234567890");

        assertEquals("SF1234567890", result.getTrackingNumber());
    }

    @Test
    @DisplayName("LG-011 按不存在的运单号查询 - 抛出LogisticsException")
    void getLogisticsByTrackingNumber_notFound_throwsException() {
        when(logisticsMapper.selectLogisticsByTrackingNumber("NONEXIST")).thenReturn(null);

        LogisticsException ex = assertThrows(LogisticsException.class,
                () -> logisticsService.getLogisticsByTrackingNumber("NONEXIST"));
        assertEquals("物流信息不存在", ex.getMessage());
    }

    @Test
    @DisplayName("LG-008 按订单ID查询物流列表 - 有数据")
    void getLogisticsByOrderId_withData_success() {
        Logistics l1 = new Logistics();
        l1.setId(1);
        l1.setOrderId("ORD001");
        LogisticsResponse r1 = LogisticsResponse.builder().id(1).orderId("ORD001").build();

        when(logisticsMapper.selectLogisticsByOrderId("ORD001")).thenReturn(List.of(l1));
        when(logisticsConverter.toResponse(l1)).thenReturn(r1);

        List<LogisticsResponse> result = logisticsService.getLogisticsByOrderId("ORD001");

        assertEquals(1, result.size());
        assertEquals("ORD001", result.get(0).getOrderId());
    }

    @Test
    @DisplayName("LG-012 按订单ID查询物流列表 - 空数据返回空列表")
    void getLogisticsByOrderId_emptyList_success() {
        when(logisticsMapper.selectLogisticsByOrderId("ORD999")).thenReturn(List.of());

        List<LogisticsResponse> result = logisticsService.getLogisticsByOrderId("ORD999");

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("LG-009 查询订单最新物流记录 - 存在")
    void getLatestLogistics_found_success() {
        Logistics logistics = new Logistics();
        logistics.setId(1);
        logistics.setOrderId("ORD001");
        logistics.setType("DELIVERY");
        logistics.setCreatedAt(Timestamp.valueOf(LocalDateTime.of(2026, 5, 28, 10, 0)));

        LogisticsResponse response = LogisticsResponse.builder()
                .id(1).orderId("ORD001").type("DELIVERY")
                .createdAt(Timestamp.valueOf(LocalDateTime.of(2026, 5, 28, 10, 0)))
                .build();

        when(logisticsMapper.selectLatestLogisticsByOrderIdAndType("ORD001", "DELIVERY")).thenReturn(logistics);
        when(logisticsConverter.toResponse(logistics)).thenReturn(response);

        LogisticsResponse result = logisticsService.getLatestLogistics("ORD001", "DELIVERY");

        assertNotNull(result);
        assertEquals("ORD001", result.getOrderId());
        assertEquals("DELIVERY", result.getType());
    }

    @Test
    @DisplayName("LG-013 查询最新物流 - 不存在抛出LogisticsException")
    void getLatestLogistics_notFound_throwsException() {
        when(logisticsMapper.selectLatestLogisticsByOrderIdAndType("ORD001", "RETURN")).thenReturn(null);

        LogisticsException ex = assertThrows(LogisticsException.class,
                () -> logisticsService.getLatestLogistics("ORD001", "RETURN"));
        assertEquals("物流信息不存在", ex.getMessage());
    }

    @Test
    @DisplayName("LG-015 删除存在的物流记录")
    void deleteLogisticsById_exists_success() {
        Logistics logistics = new Logistics();
        logistics.setId(1);

        when(logisticsMapper.selectLogisticsById(1)).thenReturn(logistics);
        when(logisticsMapper.deleteLogisticsById(1)).thenReturn(1);

        logisticsService.deleteLogisticsById(1);

        verify(logisticsMapper).selectLogisticsById(1);
        verify(logisticsMapper).deleteLogisticsById(1);
    }

    @Test
    @DisplayName("LG-016 删除不存在的物流记录")
    void deleteLogisticsById_notFound_throwsException() {
        when(logisticsMapper.selectLogisticsById(9999)).thenReturn(null);

        LogisticsException ex = assertThrows(LogisticsException.class,
                () -> logisticsService.deleteLogisticsById(9999));
        assertEquals("物流信息不存在", ex.getMessage());

        verify(logisticsMapper, never()).deleteLogisticsById(any());
    }

    @Test
    @DisplayName("LG-012 按ID查询物流 - 不存在抛出LogisticsException")
    void getLogisticsById_notFound_throwsException() {
        when(logisticsMapper.selectLogisticsById(9999)).thenReturn(null);

        LogisticsException ex = assertThrows(LogisticsException.class,
                () -> logisticsService.getLogisticsById(9999));
        assertEquals("物流信息不存在", ex.getMessage());
    }

    @Test
    @DisplayName("LG-001 按ID查询物流 - 存在")
    void getLogisticsById_found_success() {
        Logistics logistics = new Logistics();
        logistics.setId(1);
        logistics.setOrderId("ORD001");

        LogisticsResponse response = LogisticsResponse.builder().id(1).orderId("ORD001").build();

        when(logisticsMapper.selectLogisticsById(1)).thenReturn(logistics);
        when(logisticsConverter.toResponse(logistics)).thenReturn(response);

        LogisticsResponse result = logisticsService.getLogisticsById(1);

        assertEquals("ORD001", result.getOrderId());
    }

    @Test
    @DisplayName("LG-023 Mapper抛出RuntimeException - 向上传播")
    void createLogistics_mapperThrowsException_propagates() {
        Logistics logistics = new Logistics();
        logistics.setOrderId("ORD001");
        logistics.setTrackingNumber("SF1234567890");

        when(logisticsMapper.insertLogistics(logistics)).thenThrow(new RuntimeException("DB连接失败"));

        assertThrows(RuntimeException.class, () -> logisticsService.createLogistics(logistics));
    }

    @Test
    @DisplayName("LG-005a type为空字符串 - 转换器兜底为DELIVERY")
    void createLogistics_withEmptyType_defaultsToDelivery() {
        CreateLogisticsRequest request = new CreateLogisticsRequest();
        request.setOrderId("ORD-EMPTY-TYPE");
        request.setType("");
        request.setContactId(1);
        request.setTrackingNumber("SF-EMPTY");

        Logistics logistics = new Logistics();
        logistics.setOrderId("ORD-EMPTY-TYPE");
        logistics.setType("DELIVERY");
        logistics.setContactId(1);
        logistics.setTrackingNumber("SF-EMPTY");

        LogisticsResponse response = LogisticsResponse.builder()
                .id(6).orderId("ORD-EMPTY-TYPE").type("DELIVERY")
                .trackingNumber("SF-EMPTY").build();

        when(logisticsConverter.toModel(request)).thenReturn(logistics);
        when(logisticsMapper.insertLogistics(logistics)).thenReturn(1);
        when(logisticsConverter.toResponse(logistics)).thenReturn(response);

        LogisticsResponse result = logisticsService.createLogistics(request);

        assertEquals("DELIVERY", result.getType());
    }

    @Test
    @DisplayName("LG-005 type为无效值 - 直接写入不校验")
    void createLogistics_withInvalidType_success() {
        CreateLogisticsRequest request = new CreateLogisticsRequest();
        request.setOrderId("ORD005");
        request.setType("INVALID");
        request.setContactId(1);
        request.setTrackingNumber("SF0000000000");

        Logistics logistics = new Logistics();
        logistics.setOrderId("ORD005");
        logistics.setType("INVALID");
        logistics.setContactId(1);
        logistics.setTrackingNumber("SF0000000000");

        LogisticsResponse response = LogisticsResponse.builder()
                .id(5).orderId("ORD005").type("INVALID")
                .trackingNumber("SF0000000000").build();

        when(logisticsConverter.toModel(request)).thenReturn(logistics);
        when(logisticsMapper.insertLogistics(logistics)).thenReturn(1);
        when(logisticsConverter.toResponse(logistics)).thenReturn(response);

        LogisticsResponse result = logisticsService.createLogistics(request);

        assertEquals("INVALID", result.getType());
    }
}
