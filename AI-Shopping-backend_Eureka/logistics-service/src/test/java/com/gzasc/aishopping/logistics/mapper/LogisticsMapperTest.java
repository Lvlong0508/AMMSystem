package com.gzasc.aishopping.logistics.mapper;

import com.gzasc.aishopping.logistics.model.Logistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
@DisplayName("LogisticsMapper 集成测试")
class LogisticsMapperTest {

    @Autowired
    private LogisticsMapper logisticsMapper;

    @Nested
    @DisplayName("插入操作")
    class InsertTests {

        @Test
        @DisplayName("插入物流记录并返回自增ID")
        void insertLogistics_shouldReturnGeneratedId() {
            Logistics logistics = buildLogistics(generateOrderId(), "DELIVERY", generateTrackingNumber(), 1);
            int affected = logisticsMapper.insertLogistics(logistics);

            assertThat(affected).isEqualTo(1);
            assertThat(logistics.getId()).isNotNull();
        }

        @Test
        @DisplayName("插入多种类型物流记录")
        void insertLogistics_differentTypes_shouldSucceed() {
            Logistics delivery = buildLogistics(generateOrderId(), "DELIVERY", generateTrackingNumber(), 1);
            Logistics ret = buildLogistics(generateOrderId(), "RETURN", generateTrackingNumber(), 2);

            assertThat(logisticsMapper.insertLogistics(delivery)).isEqualTo(1);
            assertThat(logisticsMapper.insertLogistics(ret)).isEqualTo(1);
            assertThat(delivery.getId()).isNotNull();
            assertThat(ret.getId()).isNotNull();
            assertThat(ret.getId()).isNotEqualTo(delivery.getId());
        }

        @Test
        @DisplayName("同订单可插入多条物流记录")
        void insertLogistics_sameOrderMultiple_shouldSucceed() {
            String orderId = generateOrderId();
            Logistics l1 = buildLogistics(orderId, "DELIVERY", generateTrackingNumber(), 1);
            Logistics l2 = buildLogistics(orderId, "DELIVERY", generateTrackingNumber(), 1);

            assertThat(logisticsMapper.insertLogistics(l1)).isEqualTo(1);
            assertThat(logisticsMapper.insertLogistics(l2)).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("查询操作")
    class SelectTests {

        @Test
        @DisplayName("根据ID查询物流记录")
        void selectLogisticsById_shouldReturnLogistics() {
            Logistics logistics = insertAndReturn("DELIVERY", 1);

            Logistics found = logisticsMapper.selectLogisticsById(logistics.getId());

            assertThat(found).isNotNull();
            assertThat(found.getOrderId()).isEqualTo(logistics.getOrderId());
            assertThat(found.getTrackingNumber()).isEqualTo(logistics.getTrackingNumber());
            assertThat(found.getType()).isEqualTo("DELIVERY");
        }

        @Test
        @DisplayName("查询不存在的ID返回null")
        void selectLogisticsById_notFound_shouldReturnNull() {
            Logistics found = logisticsMapper.selectLogisticsById(99999);
            assertThat(found).isNull();
        }

        @Test
        @DisplayName("查询所有物流记录")
        void selectAllLogistics_shouldReturnAll() {
            insertAndReturn("DELIVERY", 1);
            insertAndReturn("RETURN", 2);

            List<Logistics> list = logisticsMapper.selectAllLogistics();

            assertThat(list).isNotEmpty();
            assertThat(list).anyMatch(l -> l.getType().equals("DELIVERY"));
            assertThat(list).anyMatch(l -> l.getType().equals("RETURN"));
        }

        @Test
        @DisplayName("根据运单号查询物流记录")
        void selectLogisticsByTrackingNumber_shouldReturnMatch() {
            Logistics logistics = insertAndReturn("DELIVERY", 1);

            Logistics found = logisticsMapper.selectLogisticsByTrackingNumber(logistics.getTrackingNumber());

            assertThat(found).isNotNull();
            assertThat(found.getId()).isEqualTo(logistics.getId());
        }

        @Test
        @DisplayName("查询不存在的运单号返回null")
        void selectLogisticsByTrackingNumber_notFound_shouldReturnNull() {
            Logistics found = logisticsMapper.selectLogisticsByTrackingNumber("NONEXISTENT123");
            assertThat(found).isNull();
        }

        @Test
        @DisplayName("根据订单ID查询物流记录")
        void selectLogisticsByOrderId_shouldReturnRecords() {
            String orderId = generateOrderId();
            Logistics l1 = insertAndReturnWithOrder(orderId, "DELIVERY", 1);
            Logistics l2 = insertAndReturnWithOrder(orderId, "RETURN", 2);

            List<Logistics> list = logisticsMapper.selectLogisticsByOrderId(orderId);

            assertThat(list).hasSize(2);
            assertThat(list).extracting(Logistics::getId).containsExactlyInAnyOrder(l1.getId(), l2.getId());
        }

        @Test
        @DisplayName("查询无物流记录的订单返回空列表")
        void selectLogisticsByOrderId_noLogistics_shouldReturnEmpty() {
            List<Logistics> list = logisticsMapper.selectLogisticsByOrderId("ORD-NONEXISTENT");
            assertThat(list).isEmpty();
        }

        @Test
        @DisplayName("根据订单ID和类型查询最新物流记录")
        void selectLatestLogisticsByOrderIdAndType_shouldReturnOneRecord() {
            String orderId = generateOrderId();
            insertAndReturnWithOrder(orderId, "DELIVERY", 1);
            Logistics latest = insertAndReturnWithOrder(orderId, "DELIVERY", 1);

            Logistics found = logisticsMapper.selectLatestLogisticsByOrderIdAndType(orderId, "DELIVERY");

            assertThat(found).isNotNull();
            assertThat(found.getOrderId()).isEqualTo(orderId);
            assertThat(found.getType()).isEqualTo("DELIVERY");
        }

        @Test
        @DisplayName("查询不存在的订单类型组合返回null")
        void selectLatestLogisticsByOrderIdAndType_notFound_shouldReturnNull() {
            Logistics found = logisticsMapper.selectLatestLogisticsByOrderIdAndType("ORD-NONEXISTENT", "DELIVERY");
            assertThat(found).isNull();
        }
    }

    @Nested
    @DisplayName("删除操作")
    class DeleteTests {

        @Test
        @DisplayName("根据ID删除物流记录")
        void deleteLogisticsById_shouldDelete() {
            Logistics logistics = insertAndReturn("DELIVERY", 1);

            int affected = logisticsMapper.deleteLogisticsById(logistics.getId());

            assertThat(affected).isEqualTo(1);
            assertThat(logisticsMapper.selectLogisticsById(logistics.getId())).isNull();
        }

        @Test
        @DisplayName("删除不存在的记录返回0")
        void deleteLogisticsById_notFound_shouldReturnZero() {
            int affected = logisticsMapper.deleteLogisticsById(99999);
            assertThat(affected).isEqualTo(0);
        }
    }

    private static Logistics buildLogistics(String orderId, String type, String trackingNumber, Integer contactId) {
        Logistics l = new Logistics();
        l.setOrderId(orderId);
        l.setType(type);
        l.setTrackingNumber(trackingNumber);
        l.setContactId(contactId);
        return l;
    }

    private static String generateOrderId() {
        return "ORD-" + System.nanoTime();
    }

    private static String generateTrackingNumber() {
        return "SF" + System.nanoTime();
    }

    private Logistics insertAndReturn(String type, Integer contactId) {
        Logistics l = buildLogistics(generateOrderId(), type, generateTrackingNumber(), contactId);
        logisticsMapper.insertLogistics(l);
        return l;
    }

    private Logistics insertAndReturnWithOrder(String orderId, String type, Integer contactId) {
        Logistics l = buildLogistics(orderId, type, generateTrackingNumber(), contactId);
        logisticsMapper.insertLogistics(l);
        return l;
    }


}
