package com.gzasc.aishopping.order.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.*;

class OrderExceptionTest {

    @Test
    @DisplayName("unwrap - cause 为 OrderException 时原样返回")
    void unwrap_orderExceptionCause_returnsSameInstance() {
        OrderException original = new OrderException("商品不存在（错误代码：O-003）");
        CompletionException wrapped = new CompletionException(original);

        OrderException result = OrderException.unwrap(wrapped);

        assertSame(original, result);
        assertEquals("商品不存在（错误代码：O-003）", result.getMessage());
    }

    @Test
    @DisplayName("unwrap - cause 为非 OrderException 时返回通用 OrderException")
    void unwrap_nonOrderExceptionCause_returnsGenericOrderException() {
        RuntimeException original = new RuntimeException("网络抖动");
        CompletionException wrapped = new CompletionException(original);

        OrderException result = OrderException.unwrap(wrapped);

        assertNotSame(original, result);
        assertTrue(result.getMessage().contains("系统繁忙"));
    }

    @Test
    @DisplayName("unwrap - cause 为 null 时返回通用 OrderException")
    void unwrap_nullCause_returnsGenericOrderException() {
        CompletionException wrapped = new CompletionException("no cause", null);

        OrderException result = OrderException.unwrap(wrapped);

        assertNotNull(result);
        assertTrue(result.getMessage().contains("系统繁忙"));
    }
}