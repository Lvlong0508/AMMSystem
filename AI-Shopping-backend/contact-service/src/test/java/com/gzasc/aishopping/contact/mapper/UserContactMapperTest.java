package com.gzasc.aishopping.contact.mapper;

import com.gzasc.aishopping.contact.model.Contact;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
@DisplayName("UserContactMapper 集成测试")
class UserContactMapperTest {

    @Autowired
    private UserContactMapper userContactMapper;

    @Nested
    @DisplayName("插入操作")
    class InsertTests {

        @Test
        @DisplayName("插入联系人并返回自增ID")
        void insertContact_shouldReturnGeneratedId() {
            Contact contact = buildContact("张三", "13800138000", "北京市朝阳区");
            int affected = userContactMapper.insertContact(contact);

            assertThat(affected).isEqualTo(1);
            assertThat(contact.getId()).isNotNull();
        }

        @Test
        @DisplayName("插入用户-联系人关联")
        void insertUserRelContact_shouldInsertRel() {
            Contact contact = insertAndReturn(buildContact("张三", "13800138000", "北京市朝阳区"));
            int affected = userContactMapper.insertUserRelContact(1001L, contact.getId());

            assertThat(affected).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("查询操作")
    class SelectTests {

        @Test
        @DisplayName("根据用户ID查询联系人列表")
        void selectByUserId_shouldReturnContacts() {
            Contact contact = insertAndReturn(buildContact("张三", "13800138000", "北京市朝阳区"));
            userContactMapper.insertUserRelContact(1001L, contact.getId());

            List<Contact> contacts = userContactMapper.selectByUserId(1001L);

            assertThat(contacts).hasSize(1);
            assertThat(contacts.get(0).getName()).isEqualTo("张三");
        }

        @Test
        @DisplayName("查询无联系人的用户返回空列表")
        void selectByUserId_noContact_shouldReturnEmpty() {
            List<Contact> contacts = userContactMapper.selectByUserId(9999L);
            assertThat(contacts).isEmpty();
        }

        @Test
        @DisplayName("根据ID查询联系人")
        void selectContactById_shouldReturnContact() {
            Contact contact = insertAndReturn(buildContact("李四", "13900139000", "上海市浦东新区"));

            Contact found = userContactMapper.selectContactById(contact.getId());

            assertThat(found).isNotNull();
            assertThat(found.getName()).isEqualTo("李四");
            assertThat(found.getPhone()).isEqualTo("13900139000");
        }

        @Test
        @DisplayName("查询不存在的联系人返回null")
        void selectContactById_notFound_shouldReturnNull() {
            Contact found = userContactMapper.selectContactById(99999);
            assertThat(found).isNull();
        }

        @Test
        @DisplayName("统计联系人关联的用户数")
        void countByContactIdAndUserId_shouldReturnCount() {
            Contact contact = insertAndReturn(buildContact("王五", "13700137000", "广州市天河区"));
            userContactMapper.insertUserRelContact(1001L, contact.getId());
            userContactMapper.insertUserRelContact(1002L, contact.getId());

            int count1 = userContactMapper.countByContactIdAndUserId(contact.getId(), 1001L);
            int count2 = userContactMapper.countByContactIdAndUserId(contact.getId(), 9999L);

            assertThat(count1).isEqualTo(1);
            assertThat(count2).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("更新操作")
    class UpdateTests {

        @Test
        @DisplayName("更新联系人信息")
        void updateContact_shouldUpdateFields() {
            Contact contact = insertAndReturn(buildContact("旧名字", "13800138000", "旧地址"));
            contact.setName("新名字");
            contact.setPhone("13900139000");
            contact.setAddress("新地址");

            int affected = userContactMapper.updateContact(contact);

            assertThat(affected).isEqualTo(1);
            Contact updated = userContactMapper.selectContactById(contact.getId());
            assertThat(updated.getName()).isEqualTo("新名字");
            assertThat(updated.getAddress()).isEqualTo("新地址");
        }

        @Test
        @DisplayName("设置默认联系人")
        void setDefaultById_shouldSetDefault() {
            Contact contact = insertAndReturn(buildContact("赵六", "13600136000", "深圳市南山区"));

            int affected = userContactMapper.setDefaultById(contact.getId());

            assertThat(affected).isEqualTo(1);
            Contact updated = userContactMapper.selectContactById(contact.getId());
            assertThat(updated.getIsDefault()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("删除操作")
    class DeleteTests {

        @Test
        @DisplayName("删除联系人")
        void deleteContactById_shouldDelete() {
            Contact contact = insertAndReturn(buildContact("孙七", "13500135000", "杭州市西湖区"));

            int affected = userContactMapper.deleteContactById(contact.getId());

            assertThat(affected).isEqualTo(1);
            assertThat(userContactMapper.selectContactById(contact.getId())).isNull();
        }

        @Test
        @DisplayName("删除不存在的联系人返回0")
        void deleteContactById_notFound_shouldReturnZero() {
            int affected = userContactMapper.deleteContactById(99999);
            assertThat(affected).isEqualTo(0);
        }

        @Test
        @DisplayName("删除用户-联系人关联")
        void deleteRelByContactId_shouldDeleteRel() {
            Contact contact = insertAndReturn(buildContact("周八", "13400134000", "成都市锦江区"));
            userContactMapper.insertUserRelContact(1001L, contact.getId());

            int affected = userContactMapper.deleteRelByContactId(contact.getId());

            assertThat(affected).isEqualTo(1);
            assertThat(userContactMapper.countByContactIdAndUserId(contact.getId(), 1001L)).isEqualTo(0);
        }
    }

    private static Contact buildContact(String name, String phone, String address) {
        Contact c = new Contact();
        c.setName(name);
        c.setPhone(phone);
        c.setAddress(address);
        c.setIsDefault(0);
        return c;
    }

    private Contact insertAndReturn(Contact contact) {
        userContactMapper.insertContact(contact);
        return contact;
    }
}
