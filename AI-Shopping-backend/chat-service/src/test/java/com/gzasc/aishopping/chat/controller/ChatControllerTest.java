package com.gzasc.aishopping.chat.controller;

import com.gzasc.aishopping.chat.AiService.Assistant;
import com.gzasc.aishopping.chat.config.web.UserContext;
import com.gzasc.aishopping.chat.dto.AiResponse;
import com.gzasc.aishopping.chat.dto.OrderData;
import com.gzasc.aishopping.chat.dto.OrderItem;
import com.gzasc.aishopping.chat.dto.ProductData;
import com.gzasc.aishopping.chat.dto.ProductItem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    private MockMvc mockMvc;
    private MockedStatic<UserContext> userContextMock;

    @Mock
    private Assistant assistant;

    @BeforeEach
    void setUp() {
        userContextMock = Mockito.mockStatic(UserContext.class);
        userContextMock.when(UserContext::getUserId).thenReturn(1L);

        var controller = new ChatController(assistant);
        var validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = standaloneSetup(controller)
                .setValidator(validator)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void tearDown() {
        userContextMock.close();
    }

    @Test
    @DisplayName("CH-001 正常聊天 - 纯文本回复（无工具调用）")
    void chat_textReply() throws Exception {
        when(assistant.chat(1L, "你好")).thenReturn(new AiResponse("你好！我是小物", "greeting", null));

        mockMvc.perform(post("/chat/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"你好\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.message").value("你好！我是小物"))
                .andExpect(jsonPath("$.data.reason").value("greeting"))
                .andExpect(jsonPath("$.data.data").isEmpty());
    }

    @Test
    @DisplayName("CH-002 正常聊天 - 商品查询（工具调用后返回 ProductData）")
    void chat_productQuery() throws Exception {
        var products = List.of(
                new ProductItem(1L, "手机", 2999.0, "电子产品", "最新款", 100, "url1", "shopA"),
                new ProductItem(2L, "耳机", 199.0, "配件", "无线", 200, "url2", "shopB")
        );
        var response = new AiResponse("为您找到以下商品", "called getAllProducts", new ProductData(products));
        when(assistant.chat(1L, "有哪些商品")).thenReturn(response);

        mockMvc.perform(post("/chat/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"有哪些商品\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.data.type").value("product"))
                .andExpect(jsonPath("$.data.data.products").isArray())
                .andExpect(jsonPath("$.data.data.products[0].id").value(1))
                .andExpect(jsonPath("$.data.data.products[0].name").value("手机"))
                .andExpect(jsonPath("$.data.data.products[0].price").value(2999.0))
                .andExpect(jsonPath("$.data.data.products[0].shopName").value("shopA"))
                .andExpect(jsonPath("$.data.data.products[1].id").value(2))
                .andExpect(jsonPath("$.data.data.products[1].name").value("耳机"));
    }

    @Test
    @DisplayName("CH-003 正常聊天 - 订单查询（工具调用后返回 OrderData）")
    void chat_orderQuery() throws Exception {
        var orders = List.of(
                new OrderItem("ORD001", "P001", 2, BigDecimal.valueOf(5998), "PAID", "2026-05-28", "张三", "138xxx", "地址1"),
                new OrderItem("ORD002", "P002", 1, BigDecimal.valueOf(199), "SHIPPED", "2026-05-27", "李四", "139xxx", "地址2")
        );
        var response = new AiResponse("您的订单", "called getOrderById", new OrderData(orders));
        when(assistant.chat(1L, "查一下我的订单")).thenReturn(response);

        mockMvc.perform(post("/chat/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"查一下我的订单\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.data.type").value("order"))
                .andExpect(jsonPath("$.data.data.orders").isArray())
                .andExpect(jsonPath("$.data.data.orders[0].orderId").value("ORD001"))
                .andExpect(jsonPath("$.data.data.orders[0].orderStatus").value("PAID"))
                .andExpect(jsonPath("$.data.data.orders[0].totalPrice").value(5998))
                .andExpect(jsonPath("$.data.data.orders[1].orderId").value("ORD002"))
                .andExpect(jsonPath("$.data.data.orders[1].orderStatus").value("SHIPPED"));
    }

    @Test
    @DisplayName("CH-004 空消息校验 - message 为空字符串")
    void chat_emptyMessage() throws Exception {
        mockMvc.perform(post("/chat/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("消息内容不能为空"));
    }

    @Test
    @DisplayName("CH-005 空白消息校验 - message 为纯空格字符串")
    void chat_blankMessage() throws Exception {
        mockMvc.perform(post("/chat/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("CH-006 消息体缺少 message 字段")
    void chat_missingMessage() throws Exception {
        mockMvc.perform(post("/chat/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("CH-007 null 请求体 - 空 body 导致 HttpMessageNotReadableException")
    void chat_nullBody() throws Exception {
        mockMvc.perform(post("/chat/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("系统错误，请稍后重试"));
    }

    @Test
    @DisplayName("CH-008 超长消息（10001字符）")
    void chat_longMessage() throws Exception {
        String longMsg = "a".repeat(10001);
        when(assistant.chat(1L, longMsg)).thenReturn(new AiResponse("收到长消息", "long_input", null));

        mockMvc.perform(post("/chat/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"" + longMsg + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.message").value("收到长消息"));
    }

    @Test
    @DisplayName("CH-009 特殊字符消息 - HTML/JS 注入")
    void chat_specialChars() throws Exception {
        String msg = "<script>alert(1)</script>";
        when(assistant.chat(1L, msg)).thenReturn(new AiResponse("收到特殊字符", "special_chars", null));

        mockMvc.perform(post("/chat/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"<script>alert(1)</script>\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.message").value("收到特殊字符"));
    }
}
