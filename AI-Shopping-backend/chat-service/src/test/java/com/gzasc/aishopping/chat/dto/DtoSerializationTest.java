package com.gzasc.aishopping.chat.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DtoSerializationTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
    }

    @Test
    @DisplayName("CH-045 AiResponse 完整序列化与反序列化")
    void aiResponse_serialization() throws Exception {
        AiResponse original = new AiResponse("hello", "test", null);

        String json = objectMapper.writeValueAsString(original);
        assertTrue(json.contains("\"message\":\"hello\""));
        assertTrue(json.contains("\"reason\":\"test\""));
        assertTrue(json.contains("\"data\":null"));

        AiResponse deserialized = objectMapper.readValue(json, AiResponse.class);
        assertEquals("hello", deserialized.getMessage());
        assertEquals("test", deserialized.getReason());
        assertNull(deserialized.getData());
    }

    @Test
    @DisplayName("CH-046 ProductData 序列化 - type=product")
    void productData_serialization() throws Exception {
        var items = List.of(new ProductItem(1L, "手机", 2999.0, "tag", "desc", 100, "url", "shop"));
        ProductData data = new ProductData(items);

        String json = objectMapper.writeValueAsString(data);
        assertTrue(json.contains("\"type\":\"product\""));
        assertTrue(json.contains("\"products\""));
    }

    @Test
    @DisplayName("CH-047 ProductData 反序列化 - 识别 type=product")
    void productData_deserialization() throws Exception {
        String json = "{\"type\":\"product\",\"products\":[{\"id\":1,\"name\":\"手机\",\"price\":2999.0}]}";

        Data data = objectMapper.readValue(json, Data.class);
        assertInstanceOf(ProductData.class, data);
        ProductData pd = (ProductData) data;
        assertEquals(1, pd.products().size());
        assertEquals("手机", pd.products().get(0).name());
    }

    @Test
    @DisplayName("CH-048 OrderData 序列化 - type=order")
    void orderData_serialization() throws Exception {
        var items = List.of(new OrderItem("O001", "P001", 2, BigDecimal.valueOf(5998), "PAID", "2026-05-28", "张三", "138xxx", "地址"));
        OrderData data = new OrderData(items);

        String json = objectMapper.writeValueAsString(data);
        assertTrue(json.contains("\"type\":\"order\""));
        assertTrue(json.contains("\"orders\""));
    }

    @Test
    @DisplayName("CH-049 OrderData 反序列化 - 识别 type=order")
    void orderData_deserialization() throws Exception {
        String json = "{\"type\":\"order\",\"orders\":[{\"orderId\":\"O001\",\"orderStatus\":\"PAID\",\"totalPrice\":5998.0}]}";

        Data data = objectMapper.readValue(json, Data.class);
        assertInstanceOf(OrderData.class, data);
        OrderData od = (OrderData) data;
        assertEquals(1, od.orders().size());
        assertEquals("O001", od.orders().get(0).orderId());
    }

    @Test
    @DisplayName("CH-050 ProductItem 完整字段")
    void productItem_allFields() throws Exception {
        ProductItem item = new ProductItem(1L, "手机", 2999.0, "电子产品", "desc", 100, "url", "shopA");

        String json = objectMapper.writeValueAsString(item);
        assertTrue(json.contains("\"id\":1"));
        assertTrue(json.contains("\"name\":\"手机\""));
        assertTrue(json.contains("\"price\":2999.0"));
        assertTrue(json.contains("\"stock\":100"));
        assertTrue(json.contains("\"shopName\":\"shopA\""));

        ProductItem deserialized = objectMapper.readValue(json, ProductItem.class);
        assertEquals(1L, deserialized.id());
        assertEquals("手机", deserialized.name());
        assertEquals(2999.0, deserialized.price());
        assertEquals(100, deserialized.stock());
        assertEquals("shopA", deserialized.shopName());
    }

    @Test
    @DisplayName("CH-051 ProductItem 空字段")
    void productItem_nullFields() throws Exception {
        ProductItem item = new ProductItem(null, null, null, null, null, null, null, null);

        String json = objectMapper.writeValueAsString(item);
        assertTrue(json.contains("\"id\":null"));
        assertTrue(json.contains("\"name\":null"));

        ProductItem deserialized = objectMapper.readValue(json, ProductItem.class);
        assertNull(deserialized.id());
        assertNull(deserialized.name());
        assertNull(deserialized.price());
    }

    @Test
    @DisplayName("CH-052 OrderItem 完整字段")
    void orderItem_allFields() throws Exception {
        OrderItem item = new OrderItem("O001", "P001", 2, BigDecimal.valueOf(5998), "PAID", "2026-05-28", "张三", "138xxx", "地址");

        String json = objectMapper.writeValueAsString(item);
        assertTrue(json.contains("\"orderId\":\"O001\""));
        assertTrue(json.contains("\"productId\":\"P001\""));
        assertTrue(json.contains("\"quantity\":2"));
        assertTrue(json.contains("\"totalPrice\":5998"));
        assertTrue(json.contains("\"orderStatus\":\"PAID\""));

        OrderItem deserialized = objectMapper.readValue(json, OrderItem.class);
        assertEquals("O001", deserialized.orderId());
        assertEquals("P001", deserialized.productId());
        assertEquals(2, deserialized.quantity());
        assertEquals(BigDecimal.valueOf(5998), deserialized.totalPrice());
        assertEquals("PAID", deserialized.orderStatus());
    }

    @Test
    @DisplayName("CH-053 AiResponse 中 data 为 null")
    void aiResponse_nullData() throws Exception {
        AiResponse response = new AiResponse("你好", "greeting", null);

        String json = objectMapper.writeValueAsString(response);
        assertTrue(json.contains("\"data\":null"));
    }

    @Test
    @DisplayName("CH-054 未知 type 反序列化")
    void unknownType_deserialization() {
        String json = "{\"type\":\"unknown\"}";

        assertThrows(JsonMappingException.class, () -> objectMapper.readValue(json, Data.class));
    }

    @Test
    @DisplayName("CH-055 Data 接口 sealed - ProductData 和 OrderData 是实现类")
    void data_sealedInterface() {
        Data productData = new ProductData(List.of());
        Data orderData = new OrderData(List.of());

        assertInstanceOf(ProductData.class, productData);
        assertInstanceOf(OrderData.class, orderData);
        assertInstanceOf(Data.class, productData);
        assertInstanceOf(Data.class, orderData);
    }

    @Test
    @DisplayName("AiResponse 完整序列化 - 包含 ProductData")
    void aiResponse_withProductData() throws Exception {
        var items = List.of(new ProductItem(1L, "手机", 2999.0, "tag", "desc", 100, "url", "shop"));
        AiResponse response = new AiResponse("结果", "product_query", new ProductData(items));

        String json = objectMapper.writeValueAsString(response);
        assertTrue(json.contains("\"message\":\"结果\""));
        assertTrue(json.contains("\"type\":\"product\""));
        assertTrue(json.contains("\"products\""));
        assertTrue(json.contains("\"shopName\":\"shop\""));

        AiResponse deserialized = objectMapper.readValue(json, AiResponse.class);
        assertEquals("结果", deserialized.getMessage());
        assertInstanceOf(ProductData.class, deserialized.getData());
    }

    @Test
    @DisplayName("AiResponse 完整序列化 - 包含 OrderData")
    void aiResponse_withOrderData() throws Exception {
        var items = List.of(new OrderItem("O001", "P001", 2, BigDecimal.valueOf(5998), "PAID", "2026-05-28", "张三", "138xxx", "地址"));
        AiResponse response = new AiResponse("订单", "order_query", new OrderData(items));

        String json = objectMapper.writeValueAsString(response);
        assertTrue(json.contains("\"message\":\"订单\""));
        assertTrue(json.contains("\"type\":\"order\""));
        assertTrue(json.contains("\"orders\""));

        AiResponse deserialized = objectMapper.readValue(json, AiResponse.class);
        assertEquals("订单", deserialized.getMessage());
        assertInstanceOf(OrderData.class, deserialized.getData());
    }

    @Test
    @DisplayName("CH-056 MessageVO 序列化与反序列化 - 无 products")
    void messageVO_withoutProducts() throws Exception {
        ObjectMapper om = new ObjectMapper();
        om.findAndRegisterModules();
        MessageVO vo = new MessageVO("user", "你好", null);

        String json = om.writeValueAsString(vo);
        assertTrue(json.contains("\"role\":\"user\""));
        assertTrue(json.contains("\"text\":\"你好\""));
        assertTrue(json.contains("\"products\":null"));

        MessageVO deserialized = om.readValue(json, MessageVO.class);
        assertEquals("user", deserialized.getRole());
        assertEquals("你好", deserialized.getText());
        assertNull(deserialized.getProducts());
    }

    @Test
    @DisplayName("CH-057 MessageVO 序列化与反序列化 - 带 products")
    void messageVO_withProducts() throws Exception {
        ObjectMapper om = new ObjectMapper();
        om.findAndRegisterModules();
        var products = List.of(new ProductItem(1L, "手机", 2999.0, "tag", "desc", 100, "url", "shop"));
        MessageVO vo = new MessageVO("ai", "为您找到商品", products);

        String json = om.writeValueAsString(vo);
        assertTrue(json.contains("\"role\":\"ai\""));
        assertTrue(json.contains("\"text\":\"为您找到商品\""));
        assertTrue(json.contains("\"products\""));

        MessageVO deserialized = om.readValue(json, MessageVO.class);
        assertEquals("ai", deserialized.getRole());
        assertEquals(1, deserialized.getProducts().size());
        assertEquals("手机", deserialized.getProducts().get(0).name());
    }
}
