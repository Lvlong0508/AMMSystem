package com.gzasc.aishopping.contact.dto;

import com.gzasc.aishopping.contact.model.Contact;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ContactResponseTest {

    @Test
    @DisplayName("CT-SRV-031 fromContact-正常映射所有字段")
    void fromContact_Normal() {
        LocalDateTime now = LocalDateTime.now();
        Contact contact = new Contact(1, "张三", "13800138000", "北京市朝阳区", 1, now, now);

        ContactResponse response = ContactResponse.fromContact(contact);

        assertNotNull(response);
        assertEquals(1, response.getId());
        assertEquals("张三", response.getName());
        assertEquals("13800138000", response.getPhone());
        assertEquals("北京市朝阳区", response.getAddress());
        assertEquals(1, response.getIsDefault());
        assertEquals(now, response.getCreatedAt());
        assertEquals(now, response.getUpdatedAt());
    }

    @Test
    @DisplayName("CT-SRV-032 fromContact-输入为null返回null")
    void fromContact_NullInput() {
        ContactResponse response = ContactResponse.fromContact(null);

        assertNull(response);
    }

    @Test
    @DisplayName("CT-SRV-033 fromContact-部分字段为null时正常处理")
    void fromContact_PartialNullFields() {
        Contact contact = new Contact(null, "李四", null, "地址", null, null, null);

        ContactResponse response = ContactResponse.fromContact(contact);

        assertNotNull(response);
        assertNull(response.getId());
        assertEquals("李四", response.getName());
        assertNull(response.getPhone());
        assertEquals("地址", response.getAddress());
        assertNull(response.getIsDefault());
        assertNull(response.getCreatedAt());
        assertNull(response.getUpdatedAt());
    }
}
