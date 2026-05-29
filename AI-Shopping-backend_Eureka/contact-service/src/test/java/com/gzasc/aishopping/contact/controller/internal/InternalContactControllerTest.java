package com.gzasc.aishopping.contact.controller.internal;

import com.gzasc.aishopping.contact.controller.GlobalExceptionHandler;
import com.gzasc.aishopping.contact.model.Contact;
import com.gzasc.aishopping.contact.service.UserContactService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class InternalContactControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserContactService userContactService;

    @BeforeEach
    void setUp() {
        var controller = new InternalContactController(userContactService);
        mockMvc = standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("CT-046 正常查询联系人")
    void getContactById_success() throws Exception {
        Contact contact = new Contact(1, "张三", "13800138000", "北京市朝阳区", 0, null, null);
        when(userContactService.getContactById(1)).thenReturn(contact);

        mockMvc.perform(get("/internal/contact/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("张三"))
                .andExpect(jsonPath("$.data.phone").value("13800138000"))
                .andExpect(jsonPath("$.data.address").value("北京市朝阳区"));
    }

    @Test
    @DisplayName("CT-047 查询不存在的联系人")
    void getContactById_notFound() throws Exception {
        when(userContactService.getContactById(99999)).thenReturn(null);

        mockMvc.perform(get("/internal/contact/99999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("联系人不存在"));
    }

    @Test
    @DisplayName("CT-048 查询 id 为 0 或负数")
    void getContactById_invalidId() throws Exception {
        when(userContactService.getContactById(0)).thenReturn(null);
        when(userContactService.getContactById(-1)).thenReturn(null);

        mockMvc.perform(get("/internal/contact/0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("联系人不存在"));

        mockMvc.perform(get("/internal/contact/-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("联系人不存在"));
    }
}
