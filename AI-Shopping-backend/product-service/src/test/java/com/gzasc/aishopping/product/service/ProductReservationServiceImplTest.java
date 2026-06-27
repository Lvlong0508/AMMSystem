package com.gzasc.aishopping.product.service.impl;

import com.gzasc.aishopping.product.exception.ProductException;
import com.gzasc.aishopping.product.mapper.ProductReservationMapper;
import com.gzasc.aishopping.product.mapper.ProductStockMapper;
import com.gzasc.aishopping.product.model.ProductReservation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductReservationServiceImplTest {

    @Mock
    private ProductReservationMapper mapper;

    @Mock
    private ProductStockMapper productStockMapper;

    @InjectMocks
    private ProductReservationServiceImpl reservationService;

    @Captor
    private ArgumentCaptor<ProductReservation> reservationCaptor;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(reservationService, "paymentTimeoutMinutes", 30);
    }

    @Test
    @DisplayName("PR-040 - 正常预占库存")
    void testReserveSuccess() {
        when(mapper.selectProductStockForUpdate(6001L)).thenReturn(100);
        when(mapper.sumReservedQty(6001L)).thenReturn(0);
        when(mapper.insertReservation(any(ProductReservation.class))).thenReturn(1);

        reservationService.reserve("ORDER001", "6001", 3);

        verify(mapper).selectProductStockForUpdate(6001L);
        verify(mapper).sumReservedQty(6001L);
        verify(mapper).insertReservation(reservationCaptor.capture());
        ProductReservation captured = reservationCaptor.getValue();
        assertEquals("ORDER001", captured.getOrderId());
        assertEquals("6001", captured.getProductId());
        assertEquals(3, captured.getQuantity());
        assertEquals(ProductReservation.RESERVED, captured.getStatus());
    }

    @Test
    @DisplayName("PR-041 - 库存足够但接近上限")
    void testReserveExactStock() {
        when(mapper.selectProductStockForUpdate(6001L)).thenReturn(5);
        when(mapper.sumReservedQty(6001L)).thenReturn(0);
        when(mapper.insertReservation(any(ProductReservation.class))).thenReturn(1);

        reservationService.reserve("ORDER002", "6001", 5);

        verify(mapper).insertReservation(any(ProductReservation.class));
    }

    @Test
    @DisplayName("PR-042 - 预占数量大于库存")
    void testReserveInsufficientStock() {
        when(mapper.selectProductStockForUpdate(6001L)).thenReturn(3);
        when(mapper.sumReservedQty(6001L)).thenReturn(0);

        ProductException exception = assertThrows(ProductException.class,
                () -> reservationService.reserve("ORDER003", "6001", 5));
        assertTrue(exception.getMessage().contains("库存不足"));
        verify(mapper, never()).insertReservation(any());
    }

    @Test
    @DisplayName("PR-043 - 预占数量大于可用量（已有预占占用）")
    void testReserveInsufficientAvailable() {
        when(mapper.selectProductStockForUpdate(6001L)).thenReturn(10);
        when(mapper.sumReservedQty(6001L)).thenReturn(8);

        ProductException exception = assertThrows(ProductException.class,
                () -> reservationService.reserve("ORDER004", "6001", 5));
        assertTrue(exception.getMessage().contains("库存不足"));
        verify(mapper, never()).insertReservation(any());
    }

    @Test
    @DisplayName("PR-046 - 商品不存在时预占返回400")
    void testReserveProductNotFound() {
        when(mapper.selectProductStockForUpdate(99999L)).thenReturn(null);

        ProductException exception = assertThrows(ProductException.class,
                () -> reservationService.reserve("ORDER005", "99999", 1));
        assertTrue(exception.getMessage().contains("商品不存在"));
    }

    @Test
    @DisplayName("PR-048 - 正常确认预占并扣减库存")
    void testConfirmSuccess() {
        ProductReservation reservation = new ProductReservation();
        reservation.setProductId("6001");
        reservation.setQuantity(3);
        reservation.setStatus(ProductReservation.RESERVED);
        reservation.setOrderId("ORDER006");
        when(mapper.selectByOrderId("ORDER006")).thenReturn(reservation);
        when(mapper.confirmReservation("ORDER006")).thenReturn(1);
        when(productStockMapper.deductStock(6001L, 3)).thenReturn(1);

        reservationService.confirm("ORDER006");

        verify(mapper).confirmReservation("ORDER006");
        verify(productStockMapper).deductStock(6001L, 3);
    }

    @Test
    @DisplayName("PR-049 - 确认不存在的预占")
    void testConfirmOrderNotFound() {
        when(mapper.selectByOrderId("NOT_EXIST")).thenReturn(null);

        ProductException exception = assertThrows(ProductException.class,
                () -> reservationService.confirm("NOT_EXIST"));
        assertTrue(exception.getMessage().contains("不存在"));
    }

    @Test
    @DisplayName("PR-050 - 重复确认已确认的预占")
    void testConfirmAlreadyConfirmed() {
        ProductReservation reservation = new ProductReservation();
        reservation.setStatus(ProductReservation.CONFIRMED);
        when(mapper.selectByOrderId("ORDER007")).thenReturn(reservation);

        ProductException exception = assertThrows(ProductException.class,
                () -> reservationService.confirm("ORDER007"));
        assertTrue(exception.getMessage().contains("状态已变更"));
    }

    @Test
    @DisplayName("PR-051 - 确认已释放的预占")
    void testConfirmAlreadyReleased() {
        ProductReservation reservation = new ProductReservation();
        reservation.setStatus(ProductReservation.RELEASED);
        when(mapper.selectByOrderId("ORDER008")).thenReturn(reservation);

        ProductException exception = assertThrows(ProductException.class,
                () -> reservationService.confirm("ORDER008"));
        assertTrue(exception.getMessage().contains("状态已变更"));
    }

    @Test
    @DisplayName("PR-052 - 正常释放预占（RESERVED -> RELEASED）")
    void testReleaseSuccess() {
        ProductReservation reservation = new ProductReservation();
        reservation.setStatus(ProductReservation.RESERVED);
        reservation.setOrderId("ORDER009");
        when(mapper.selectByOrderId("ORDER009")).thenReturn(reservation);
        when(mapper.releaseReservation("ORDER009")).thenReturn(1);

        reservationService.release("ORDER009");

        verify(mapper).releaseReservation("ORDER009");
    }

    @Test
    @DisplayName("PR-054 - 释放不存在的预占（静默跳过）")
    void testReleaseOrderNotFound() {
        when(mapper.selectByOrderId("NOT_EXIST")).thenReturn(null);

        assertDoesNotThrow(() -> reservationService.release("NOT_EXIST"));
    }

    @Test
    @DisplayName("PR-053 - 释放已确认的预占抛出异常")
    void testReleaseConfirmedOrderThrows() {
        ProductReservation reservation = new ProductReservation();
        reservation.setStatus(ProductReservation.CONFIRMED);
        when(mapper.selectByOrderId("ORDER010")).thenReturn(reservation);

        ProductException exception = assertThrows(ProductException.class,
                () -> reservationService.release("ORDER010"));
        assertTrue(exception.getMessage().contains("不允许释放"));
    }

    @Test
    @DisplayName("PR-052 - 释放已释放的预占（静默跳过）")
    void testReleaseAlreadyReleased() {
        ProductReservation reservation = new ProductReservation();
        reservation.setStatus(ProductReservation.RELEASED);
        when(mapper.selectByOrderId("ORDER011")).thenReturn(reservation);

        assertDoesNotThrow(() -> reservationService.release("ORDER011"));
        verify(mapper, never()).releaseReservation(anyString());
    }

    @Test
    @DisplayName("PR-069 - 清理过期RESERVED预占")
    void testReleaseExpiredReservations() {
        ProductReservation expired = new ProductReservation();
        expired.setOrderId("EXPIRED_ORDER");
        expired.setProductId("6001");
        expired.setQuantity(3);
        expired.setStatus(ProductReservation.RESERVED);
        when(mapper.selectExpiredReservations(any(Date.class))).thenReturn(List.of(expired));
        when(mapper.selectByOrderId("EXPIRED_ORDER")).thenReturn(expired);
        when(mapper.releaseReservation("EXPIRED_ORDER")).thenReturn(1);

        reservationService.releaseExpiredReservations();

        verify(mapper).selectExpiredReservations(any(Date.class));
        verify(mapper).releaseReservation("EXPIRED_ORDER");
    }

    @Test
    @DisplayName("PR-071 - CONFIRMED状态的不被定时任务清理")
    void testReleaseExpiredReservationsSkipsConfirmed() {
        ProductReservation confirmed = new ProductReservation();
        confirmed.setOrderId("CONFIRMED_ORDER");
        confirmed.setStatus(ProductReservation.CONFIRMED);
        when(mapper.selectExpiredReservations(any(Date.class))).thenReturn(List.of(confirmed));

        reservationService.releaseExpiredReservations();

        verify(mapper).selectExpiredReservations(any(Date.class));
        verify(mapper, never()).releaseReservation(anyString());
    }
}
