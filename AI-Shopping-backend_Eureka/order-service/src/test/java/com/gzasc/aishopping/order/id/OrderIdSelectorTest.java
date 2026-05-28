package com.gzasc.aishopping.order.id;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderIdSelectorTest {

    @Mock
    private OrderIdGenerator mockGenerator;

    @Test
    @DisplayName("OR-060 选择器委托给generator生成ID")
    void generate() {
        when(mockGenerator.generate()).thenReturn("2026052800001ABCDE");
        OrderIdSelector selector = new OrderIdSelector(List.of(mockGenerator));

        String orderId = selector.generate();
        assertEquals("2026052800001ABCDE", orderId);
    }

    @Test
    @DisplayName("无generator时抛出异常")
    void noGenerators() {
        assertThrows(IllegalStateException.class,
                () -> new OrderIdSelector(List.of()));
    }

    @Test
    @DisplayName("switchTo替换generator并生成新ID")
    void switchTo() {
        OrderIdGenerator newGen = () -> "SWITCHED_ID";
        OrderIdSelector selector = new OrderIdSelector(List.of(mockGenerator));
        selector.switchTo(newGen);

        assertEquals("SWITCHED_ID", selector.generate());
    }
}
