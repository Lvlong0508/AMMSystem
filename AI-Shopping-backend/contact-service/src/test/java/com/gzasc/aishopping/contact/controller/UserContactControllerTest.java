package com.gzasc.aishopping.contact.controller;

import com.gzasc.aishopping.contact.dto.ContactResponse;
import com.gzasc.aishopping.contact.model.Contact;
import com.gzasc.aishopping.contact.service.UserContactService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class UserContactControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserContactService userContactService;

    @BeforeEach
    void setUp() {
        var controller = new UserContactController(userContactService);
        var validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = standaloneSetup(controller)
                .setValidator(validator)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ==================== POST /api/user/contact/create ====================

    @Test
    @DisplayName("CT-001 正常创建联系人")
    void createContact_success() throws Exception {
        when(userContactService.createContact(any(Contact.class), eq(1001L))).thenReturn(1);

        mockMvc.perform(post("/api/user/contact/create")
                        .header("X-User-Id", "1001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"张三","phone":"13800138000","address":"北京市朝阳区"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("创建地址成功"))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("CT-002 创建联系人时 name 为空")
    void createContact_nameBlank() throws Exception {
        mockMvc.perform(post("/api/user/contact/create")
                        .header("X-User-Id", "1001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","phone":"13800138000","address":"北京市"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("参数错误：姓名为空"));
    }

    @Test
    @DisplayName("CT-003 创建联系人时 phone 为空")
    void createContact_phoneBlank() throws Exception {
        mockMvc.perform(post("/api/user/contact/create")
                        .header("X-User-Id", "1001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"张三","phone":"","address":"北京市"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("参数错误：电话为空"));
    }

    @Test
    @DisplayName("CT-004 创建联系人时 address 为空")
    void createContact_addressBlank() throws Exception {
        mockMvc.perform(post("/api/user/contact/create")
                        .header("X-User-Id", "1001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"张三","phone":"13800138000","address":""}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("参数错误：地址为空"));
    }

    @Test
    @DisplayName("CT-005 创建联系人时缺少 X-User-Id")
    void createContact_missingUserId() throws Exception {
        mockMvc.perform(post("/api/user/contact/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"张三","phone":"13800138000","address":"北京市"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("未登录"));
    }

    // ==================== DELETE /api/user/contact/delete/{id} ====================

    @Test
    @DisplayName("CT-008 正常删除联系人")
    void deleteContact_success() throws Exception {
        when(userContactService.deleteContact(1, 1001L)).thenReturn(1);

        mockMvc.perform(delete("/api/user/contact/delete/1")
                        .header("X-User-Id", "1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("删除地址成功"));
    }

    @Test
    @DisplayName("CT-009 删除不存在的联系人")
    void deleteContact_notFound() throws Exception {
        when(userContactService.deleteContact(99999, 1001L)).thenReturn(0);

        mockMvc.perform(delete("/api/user/contact/delete/99999")
                        .header("X-User-Id", "1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("删除地址失败：地址不存在"));
    }

    @Test
    @DisplayName("CT-010 删除不属于当前用户的联系人")
    void deleteContact_noPermission() throws Exception {
        when(userContactService.deleteContact(2, 1001L)).thenReturn(0);

        mockMvc.perform(delete("/api/user/contact/delete/2")
                        .header("X-User-Id", "1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("删除地址失败：地址不存在"));
    }

    @Test
    @DisplayName("CT-011 删除联系人时缺少 X-User-Id")
    void deleteContact_missingUserId() throws Exception {
        mockMvc.perform(delete("/api/user/contact/delete/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("未登录"));
    }

    @Test
    @DisplayName("CT-012 删除时 id 为负数")
    void deleteContact_negativeId() throws Exception {
        when(userContactService.deleteContact(-1, 1001L)).thenReturn(0);

        mockMvc.perform(delete("/api/user/contact/delete/-1")
                        .header("X-User-Id", "1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("删除地址失败：地址不存在"));
    }

    // ==================== PUT /api/user/contact/update ====================

    @Test
    @DisplayName("CT-013 正常更新联系人")
    void updateContact_success() throws Exception {
        when(userContactService.updateContact(any(Contact.class), eq(1001L))).thenReturn(1);

        mockMvc.perform(put("/api/user/contact/update")
                        .header("X-User-Id", "1001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id":1,"name":"李四","phone":"13700137000","address":"上海市浦东新区"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("更新地址成功"));
    }

    @Test
    @DisplayName("CT-014 更新不存在的联系人")
    void updateContact_notFound() throws Exception {
        when(userContactService.updateContact(any(Contact.class), eq(1001L))).thenReturn(0);

        mockMvc.perform(put("/api/user/contact/update")
                        .header("X-User-Id", "1001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id":99999,"name":"李四","phone":"13700137000","address":"上海市"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("更新联系人失败：地址不存在"));
    }

    @Test
    @DisplayName("CT-015 更新不属于当前用户的联系人")
    void updateContact_noPermission() throws Exception {
        when(userContactService.updateContact(any(Contact.class), eq(1001L))).thenReturn(0);

        mockMvc.perform(put("/api/user/contact/update")
                        .header("X-User-Id", "1001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id":2,"name":"李四","phone":"13700137000","address":"上海市"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("更新联系人失败：地址不存在"));
    }

    @Test
    @DisplayName("CT-016 更新请求中 id 为空")
    void updateContact_idNull() throws Exception {
        mockMvc.perform(put("/api/user/contact/update")
                        .header("X-User-Id", "1001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"李四","phone":"13700137000","address":"上海市"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("参数错误：ID不能为空"));
    }

    // ==================== GET /api/user/contact/list ====================

    @Test
    @DisplayName("CT-018 正常查询联系人列表")
    void listContact_hasData() throws Exception {
        Contact c1 = new Contact(1, "张三", "13800138000", "北京市", 0, LocalDateTime.now(), LocalDateTime.now());
        Contact c2 = new Contact(2, "李四", "13900139000", "上海市", 1, LocalDateTime.now(), LocalDateTime.now());
        Contact c3 = new Contact(3, "王五", "13700137000", "广州市", 0, LocalDateTime.now(), LocalDateTime.now());
        when(userContactService.getContactsByUserId(1001L)).thenReturn(List.of(c1, c2, c3));

        mockMvc.perform(get("/api/user/contact/list")
                        .header("X-User-Id", "1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.contacts.length()").value(3))
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.contacts[0].name").value("张三"))
                .andExpect(jsonPath("$.data.contacts[1].name").value("李四"))
                .andExpect(jsonPath("$.data.contacts[2].name").value("王五"));
    }

    @Test
    @DisplayName("CT-019 查询无联系人的用户")
    void listContact_empty() throws Exception {
        when(userContactService.getContactsByUserId(2000L)).thenReturn(List.of());

        mockMvc.perform(get("/api/user/contact/list")
                        .header("X-User-Id", "2000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.contacts.length()").value(0))
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    @DisplayName("CT-020 查询列表时缺少 X-User-Id")
    void listContact_missingUserId() throws Exception {
        mockMvc.perform(get("/api/user/contact/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("未登录"));
    }

    @Test
    @DisplayName("CT-021 查询列表时默认联系人标记正确")
    void listContact_defaultFlag() throws Exception {
        Contact c1 = new Contact(4, "张三", "13800138000", "北京市", 0, LocalDateTime.now(), LocalDateTime.now());
        Contact c2 = new Contact(5, "李四", "13900139000", "上海市", 1, LocalDateTime.now(), LocalDateTime.now());
        when(userContactService.getContactsByUserId(1001L)).thenReturn(List.of(c1, c2));

        mockMvc.perform(get("/api/user/contact/list")
                        .header("X-User-Id", "1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.contacts[0].isDefault").value(0))
                .andExpect(jsonPath("$.data.contacts[1].isDefault").value(1));
    }

    // ==================== PUT /api/user/contact/set-default/{id} ====================

    @Test
    @DisplayName("CT-022 正常设置默认联系人")
    void setDefault_success() throws Exception {
        when(userContactService.setDefaultContact(1, 1001L)).thenReturn(1);

        mockMvc.perform(put("/api/user/contact/set-default/1")
                        .header("X-User-Id", "1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("设置成功"));
    }

    @Test
    @DisplayName("CT-023 设置不存在的联系人为默认")
    void setDefault_notFound() throws Exception {
        when(userContactService.setDefaultContact(99999, 1001L)).thenReturn(0);

        mockMvc.perform(put("/api/user/contact/set-default/99999")
                        .header("X-User-Id", "1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("设置失败：地址不存在"));
    }

    @Test
    @DisplayName("CT-024 设置不属于当前用户的联系人为默认")
    void setDefault_noPermission() throws Exception {
        when(userContactService.setDefaultContact(10, 1001L)).thenReturn(0);

        mockMvc.perform(put("/api/user/contact/set-default/10")
                        .header("X-User-Id", "1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("设置失败：地址不存在"));
    }

    @Test
    @DisplayName("CT-025 对已经是默认的联系人重复设置默认（幂等）")
    void setDefault_alreadyDefault() throws Exception {
        when(userContactService.setDefaultContact(1, 1001L)).thenReturn(1);

        mockMvc.perform(put("/api/user/contact/set-default/1")
                        .header("X-User-Id", "1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("设置成功"));
    }

    // ==================== Service Exception Tests ====================

    @Test
    @DisplayName("CT-006 创建联系人时 Service 抛异常")
    void createContact_serviceException() throws Exception {
        when(userContactService.createContact(any(Contact.class), eq(1001L)))
                .thenThrow(new RuntimeException("数据库异常"));
        mockMvc.perform(post("/api/user/contact/create")
                        .header("X-User-Id", "1001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"张三","phone":"13800138000","address":"北京市"}
                                """))
                .andExpect(status().is5xxServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("系统错误，请稍后重试"));
    }

    @Test
    @DisplayName("CT-007 删除联系人时 Service 抛异常")
    void deleteContact_serviceException() throws Exception {
        when(userContactService.deleteContact(anyInt(), eq(1001L)))
                .thenThrow(new RuntimeException("数据库异常"));
        mockMvc.perform(delete("/api/user/contact/delete/1")
                        .header("X-User-Id", "1001"))
                .andExpect(status().is5xxServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("系统错误，请稍后重试"));
    }

    @Test
    @DisplayName("CT-016A 更新联系人时缺少 X-User-Id")
    void updateContact_missingUserId() throws Exception {
        mockMvc.perform(put("/api/user/contact/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id":1,"name":"李四","phone":"13700137000","address":"上海市"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("未登录"));
    }

    @Test
    @DisplayName("CT-016B 更新联系人时 name 为空")
    void updateContact_nameBlank() throws Exception {
        mockMvc.perform(put("/api/user/contact/update")
                        .header("X-User-Id", "1001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id":1,"name":"","phone":"13700137000","address":"上海市"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("参数错误：姓名为空"));
    }

    @Test
    @DisplayName("CT-016C 更新联系人时 phone 为空")
    void updateContact_phoneBlank() throws Exception {
        mockMvc.perform(put("/api/user/contact/update")
                        .header("X-User-Id", "1001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id":1,"name":"李四","phone":"","address":"上海市"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("参数错误：电话为空"));
    }

    @Test
    @DisplayName("CT-016D 更新联系人时 address 为空")
    void updateContact_addressBlank() throws Exception {
        mockMvc.perform(put("/api/user/contact/update")
                        .header("X-User-Id", "1001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id":1,"name":"李四","phone":"13700137000","address":""}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("参数错误：地址为空"));
    }

    @Test
    @DisplayName("CT-016E 更新联系人时 Service 抛异常")
    void updateContact_serviceException() throws Exception {
        when(userContactService.updateContact(any(Contact.class), eq(1001L)))
                .thenThrow(new RuntimeException("数据库异常"));
        mockMvc.perform(put("/api/user/contact/update")
                        .header("X-User-Id", "1001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id":1,"name":"李四","phone":"13700137000","address":"上海市"}
                                """))
                .andExpect(status().is5xxServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("系统错误，请稍后重试"));
    }

    @Test
    @DisplayName("CT-021A 查询列表时 Service 抛异常")
    void listContact_serviceException() throws Exception {
        when(userContactService.getContactsByUserId(anyLong()))
                .thenThrow(new RuntimeException("数据库异常"));
        mockMvc.perform(get("/api/user/contact/list")
                        .header("X-User-Id", "1001"))
                .andExpect(status().is5xxServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("系统错误，请稍后重试"));
    }

    @Test
    @DisplayName("CT-025A 设置默认联系人时 Service 抛异常")
    void setDefault_serviceException() throws Exception {
        when(userContactService.setDefaultContact(anyInt(), eq(1001L)))
                .thenThrow(new RuntimeException("数据库异常"));
        mockMvc.perform(put("/api/user/contact/set-default/1")
                        .header("X-User-Id", "1001"))
                .andExpect(status().is5xxServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("系统错误，请稍后重试"));
    }

    @Test
    @DisplayName("CT-025B 设置默认联系人时缺少 X-User-Id")
    void setDefault_missingUserId() throws Exception {
        mockMvc.perform(put("/api/user/contact/set-default/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("未登录"));
    }
}
