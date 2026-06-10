package com.gzasc.aishopping.order.mapper;

import com.gzasc.aishopping.order.model.DeletedOrder;
import com.gzasc.aishopping.order.model.Order;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
@DisplayName("DeletedOrderMapper 集成测试")
class DeletedOrderMapperTest {

    @Autowired
    private DeletedOrderMapper deletedOrderMapper;

    private static String nextOrderId() {
        return "ORD" + System.nanoTime();
    }

    private static DeletedOrder createTestDeletedOrder(String orderId) {
        DeletedOrder d = new DeletedOrder();
        d.setOrderId(orderId);
        d.setUserId(1001L);
        d.setShopId("SHOP001");
        d.setProductId("PROD001");
        d.setQuantity(2);
        d.setTotalPrice(new BigDecimal("99.99"));
        d.setOrderStatus(Order.PENDING);
        d.setOrderDate(new Timestamp(System.currentTimeMillis()));
        d.setDeletedAt(new Timestamp(System.currentTimeMillis()));
        return d;
    }

    @Nested
    @DisplayName("插入操作")
    class InsertTests {

        @Test
        @DisplayName("插入已删除订单成功")
        void insertDeletedOrder_shouldSucceed() {
            DeletedOrder d = createTestDeletedOrder(nextOrderId());
            int affected = deletedOrderMapper.insertDeletedOrder(d);
            assertThat(affected).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("查询操作")
    class SelectTests {

        @Test
        @DisplayName("根据ID查询已删除订单")
        void selectDeletedOrderById_shouldReturn() {
            String orderId = nextOrderId();
            deletedOrderMapper.insertDeletedOrder(createTestDeletedOrder(orderId));

            DeletedOrder byOrderId = deletedOrderMapper.selectDeletedOrderByOrderId(orderId);
            assertThat(byOrderId).isNotNull();

            DeletedOrder found = deletedOrderMapper.selectDeletedOrderById(byOrderId.getId());

            assertThat(found).isNotNull();
            assertThat(found.getOrderId()).isEqualTo(orderId);
            assertThat(found.getOrderStatus()).isEqualTo(Order.PENDING);
        }

        @Test
        @DisplayName("查询不存在的已删除订单返回null")
        void selectDeletedOrderById_notFound_shouldReturnNull() {
            DeletedOrder found = deletedOrderMapper.selectDeletedOrderById(99999);
            assertThat(found).isNull();
        }

        @Test
        @DisplayName("查询所有已删除订单")
        void selectAllDeletedOrders_shouldReturnAll() {
            deletedOrderMapper.insertDeletedOrder(createTestDeletedOrder(nextOrderId()));
            deletedOrderMapper.insertDeletedOrder(createTestDeletedOrder(nextOrderId()));

            List<DeletedOrder> list = deletedOrderMapper.selectAllDeletedOrders();

            assertThat(list).hasSize(2);
        }

        @Test
        @DisplayName("根据orderId查询已删除订单")
        void selectDeletedOrderByOrderId_shouldReturn() {
            String orderId = nextOrderId();
            deletedOrderMapper.insertDeletedOrder(createTestDeletedOrder(orderId));

            DeletedOrder found = deletedOrderMapper.selectDeletedOrderByOrderId(orderId);

            assertThat(found).isNotNull();
            assertThat(found.getOrderId()).isEqualTo(orderId);
        }

        @Test
        @DisplayName("根据不存在的orderId查询返回null")
        void selectDeletedOrderByOrderId_notFound_shouldReturnNull() {
            DeletedOrder found = deletedOrderMapper.selectDeletedOrderByOrderId("NONEXIST");
            assertThat(found).isNull();
        }
    }
}
