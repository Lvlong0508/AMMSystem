package com.gzasc.aishopping.order.converter;

import com.gzasc.aishopping.common.dto.contact.ContactDTO;
import com.gzasc.aishopping.order.dto.OrderAbstractSellerDTO;
import com.gzasc.aishopping.order.dto.OrderAbstractUserDTO;
import com.gzasc.aishopping.order.dto.OrderDetailDTO;
import com.gzasc.aishopping.order.model.Order;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OrderConverter 单元测试")
class OrderConverterTest {

    private OrderConverter converter;

    @BeforeEach
    void setUp() {
        converter = new OrderConverter();
    }

    private Order buildOrder() {
        Order o = new Order();
        o.setOrderId("ORDER001");
        o.setUserId(100L);
        o.setShopId("SHOP001");
        o.setProductId("PROD001");
        o.setQuantity(3);
        o.setTotalPrice(new BigDecimal("299.70"));
        o.setOrderStatus("PAID");
        o.setOrderDate(Timestamp.valueOf("2026-05-28 10:00:00"));
        o.setContactId(7);
        return o;
    }

    // ==================== toUserAbstractDTO ====================

    @Test
    @DisplayName("CV-001 toUserAbstractDTO - 字段全部映射")
    void toUserAbstractDTO_allFields() {
        Order order = buildOrder();
        OrderAbstractUserDTO dto = converter.toUserAbstractDTO(order);

        assertNotNull(dto);
        assertEquals("ORDER001", dto.getOrderId());
        assertEquals("PROD001", dto.getProductId());
        assertEquals("SHOP001", dto.getShopId());
        assertEquals(new BigDecimal("299.70"), dto.getTotalPrice());
        assertEquals(3, dto.getQuantity());
        assertEquals("PAID", dto.getOrderStatus());
    }

    @Test
    @DisplayName("CV-002 toUserAbstractDTO - DTO 不含 userId/orderDate/contactId 字段")
    void toUserAbstractDTO_excludesUserIdAndDate() {
        var fields = OrderAbstractUserDTO.class.getDeclaredFields();
        for (var f : fields) {
            String name = f.getName().toLowerCase();
            assertFalse(name.contains("userid"), "OrderAbstractUserDTO 不应包含 userId 字段");
            assertFalse(name.contains("orderdate"), "OrderAbstractUserDTO 不应包含 orderDate 字段");
            assertFalse(name.contains("contactid"), "OrderAbstractUserDTO 不应包含 contactId 字段");
        }
    }

    // ==================== toUserAbstractDTOList ====================

    @Test
    @DisplayName("CV-003 toUserAbstractDTOList - 多元素映射")
    void toUserAbstractDTOList_multiple() {
        Order o1 = buildOrder();
        Order o2 = buildOrder();
        o2.setOrderId("ORDER002");
        o2.setOrderStatus("SHIPPED");

        List<OrderAbstractUserDTO> list = converter.toUserAbstractDTOList(Arrays.asList(o1, o2));

        assertEquals(2, list.size());
        assertEquals("ORDER001", list.get(0).getOrderId());
        assertEquals("PAID", list.get(0).getOrderStatus());
        assertEquals("ORDER002", list.get(1).getOrderId());
        assertEquals("SHIPPED", list.get(1).getOrderStatus());
    }

    @Test
    @DisplayName("CV-004 toUserAbstractDTOList - 空列表返回空列表")
    void toUserAbstractDTOList_empty() {
        List<OrderAbstractUserDTO> list = converter.toUserAbstractDTOList(List.of());
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    // ==================== toSellerAbstractDTO ====================

    @Test
    @DisplayName("CV-005 toSellerAbstractDTO - 包含 contactId 不含 totalPrice/userId")
    void toSellerAbstractDTO_allFields() {
        Order order = buildOrder();
        OrderAbstractSellerDTO dto = converter.toSellerAbstractDTO(order);

        assertNotNull(dto);
        assertEquals("ORDER001", dto.getOrderId());
        assertEquals("PROD001", dto.getProductId());
        assertEquals(7, dto.getContactId());
        assertEquals(3, dto.getQuantity());
        assertEquals("PAID", dto.getOrderStatus());
    }

    @Test
    @DisplayName("CV-006 toSellerAbstractDTO - contactId 为 null 时不抛异常")
    void toSellerAbstractDTO_nullContactId() {
        Order order = buildOrder();
        order.setContactId(null);
        OrderAbstractSellerDTO dto = converter.toSellerAbstractDTO(order);

        assertNull(dto.getContactId());
        assertEquals("ORDER001", dto.getOrderId());
    }

    // ==================== toSellerAbstractDTOList ====================

    @Test
    @DisplayName("CV-007 toSellerAbstractDTOList - 多个订单正确转换")
    void toSellerAbstractDTOList_multiple() {
        List<OrderAbstractSellerDTO> list = converter.toSellerAbstractDTOList(
                Arrays.asList(buildOrder(), buildOrder(), buildOrder()));
        assertEquals(3, list.size());
    }

    @Test
    @DisplayName("CV-008 toSellerAbstractDTOList - 空列表返回空")
    void toSellerAbstractDTOList_empty() {
        List<OrderAbstractSellerDTO> list = converter.toSellerAbstractDTOList(List.of());
        assertTrue(list.isEmpty());
    }

    // ==================== toDetailDTO ====================

    @Test
    @DisplayName("CV-009 toDetailDTO - 9 个字段全部映射")
    void toDetailDTO_allFields() {
        Order order = buildOrder();
        OrderDetailDTO dto = converter.toDetailDTO(order);

        assertNotNull(dto);
        assertEquals("ORDER001", dto.getOrderId());
        assertEquals(100L, dto.getUserId());
        assertEquals("SHOP001", dto.getShopId());
        assertEquals("PROD001", dto.getProductId());
        assertEquals(3, dto.getQuantity());
        assertEquals(new BigDecimal("299.70"), dto.getTotalPrice());
        assertEquals("PAID", dto.getOrderStatus());
        assertEquals(Timestamp.valueOf("2026-05-28 10:00:00"), dto.getOrderDate());
        assertEquals(7, dto.getContactId());
    }

    @Test
    @DisplayName("CV-010 toDetailDTO - 大数值不丢失精度")
    void toDetailDTO_bigDecimalPrecision() {
        Order order = buildOrder();
        order.setTotalPrice(new BigDecimal("0.10").add(new BigDecimal("0.20")));
        OrderDetailDTO dto = converter.toDetailDTO(order);

        assertEquals(0, new BigDecimal("0.30").compareTo(dto.getTotalPrice()));
    }

    // ==================== enrichDetailDTO ====================

    @Test
    @DisplayName("CV-011 enrichDetailDTO - contactInfo 与 logisticsInfo 均为 null 时不报错")
    void enrichDetailDTO_bothNull() {
        OrderDetailDTO dto = new OrderDetailDTO();
        dto.setOrderId("ORDER001");

        OrderDetailDTO result = converter.enrichDetailDTO(dto, null, null);

        assertNotNull(result);
        assertEquals("ORDER001", result.getOrderId());
        assertNull(result.getContactName());
        assertNull(result.getContactPhone());
        assertNull(result.getContactAddress());
        assertNull(result.getTrackingNumber());
    }

    @Test
    @DisplayName("CV-012 enrichDetailDTO - 仅 contactInfo 不为 null")
    void enrichDetailDTO_onlyContact() {
        OrderDetailDTO dto = new OrderDetailDTO();
        ContactDTO contact = new ContactDTO();
        contact.setName("张三");
        contact.setPhone("13800138000");
        contact.setAddress("北京市朝阳区");

        OrderDetailDTO result = converter.enrichDetailDTO(dto, contact, null);

        assertEquals("张三", result.getContactName());
        assertEquals("13800138000", result.getContactPhone());
        assertEquals("北京市朝阳区", result.getContactAddress());
        assertNull(result.getTrackingNumber());
    }

    @Test
    @DisplayName("CV-013 enrichDetailDTO - 仅 logisticsInfo 不为 null 且 data 是 Map")
    void enrichDetailDTO_onlyLogistics() {
        OrderDetailDTO dto = new OrderDetailDTO();
        Map<String, Object> data = new HashMap<>();
        data.put("trackingNumber", "SF1234567890");
        Map<String, Object> logisticsInfo = Map.of("data", data);

        OrderDetailDTO result = converter.enrichDetailDTO(dto, null, logisticsInfo);

        assertEquals("SF1234567890", result.getTrackingNumber());
        assertNull(result.getContactName());
    }

    @Test
    @DisplayName("CV-014 enrichDetailDTO - logisticsInfo.data 不是 Map 时不抛异常")
    void enrichDetailDTO_logisticsDataNotMap() {
        OrderDetailDTO dto = new OrderDetailDTO();
        Map<String, Object> logisticsInfo = Map.of("data", "not a map");

        OrderDetailDTO result = converter.enrichDetailDTO(dto, null, logisticsInfo);

        assertNull(result.getTrackingNumber());
    }

    @Test
    @DisplayName("CV-015 enrichDetailDTO - logisticsInfo.data 缺 trackingNumber 字段")
    void enrichDetailDTO_logisticsDataMissingKey() {
        OrderDetailDTO dto = new OrderDetailDTO();
        Map<String, Object> data = new HashMap<>();
        data.put("otherKey", "value");
        Map<String, Object> logisticsInfo = Map.of("data", data);

        OrderDetailDTO result = converter.enrichDetailDTO(dto, null, logisticsInfo);

        assertNull(result.getTrackingNumber());
    }

    @Test
    @DisplayName("CV-016 enrichDetailDTO - logisticsInfo 缺 data 键")
    void enrichDetailDTO_logisticsMissingDataKey() {
        OrderDetailDTO dto = new OrderDetailDTO();
        Map<String, Object> logisticsInfo = Map.of("otherKey", "value");

        OrderDetailDTO result = converter.enrichDetailDTO(dto, null, logisticsInfo);

        assertNull(result.getTrackingNumber());
    }

    @Test
    @DisplayName("CV-017 enrichDetailDTO - 联系人+物流同时填充")
    void enrichDetailDTO_both() {
        OrderDetailDTO dto = new OrderDetailDTO();
        ContactDTO contact = new ContactDTO();
        contact.setName("李四");
        contact.setPhone("13900139000");
        contact.setAddress("上海市浦东新区");
        Map<String, Object> data = new HashMap<>();
        data.put("trackingNumber", "YT9876543210");
        Map<String, Object> logisticsInfo = Map.of("data", data);

        OrderDetailDTO result = converter.enrichDetailDTO(dto, contact, logisticsInfo);

        assertEquals("李四", result.getContactName());
        assertEquals("13900139000", result.getContactPhone());
        assertEquals("上海市浦东新区", result.getContactAddress());
        assertEquals("YT9876543210", result.getTrackingNumber());
    }

    @Test
    @DisplayName("CV-018 enrichDetailDTO - trackingNumber 非 String 类型时抛 ClassCastException（按原代码行为）")
    void enrichDetailDTO_trackingNumberNotString() {
        OrderDetailDTO dto = new OrderDetailDTO();
        Map<String, Object> data = new HashMap<>();
        data.put("trackingNumber", 12345);
        Map<String, Object> logisticsInfo = Map.of("data", data);

        assertThrows(ClassCastException.class,
                () -> converter.enrichDetailDTO(dto, null, logisticsInfo));
    }
}
