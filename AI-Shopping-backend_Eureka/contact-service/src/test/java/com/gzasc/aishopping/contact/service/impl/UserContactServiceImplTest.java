package com.gzasc.aishopping.contact.service.impl;

import com.gzasc.aishopping.contact.mapper.UserContactMapper;
import com.gzasc.aishopping.contact.model.Contact;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import org.mockito.InOrder;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserContactServiceImplTest {

    @Mock
    private UserContactMapper userContactMapper;

    private UserContactServiceImpl userContactService;

    @BeforeEach
    void setUp() {
        userContactService = new UserContactServiceImpl(userContactMapper);
    }

    // ==================== createContact ====================

    @Test
    @DisplayName("CT-SRV-001 正常创建联系人，插入关联记录")
    void createContact_Success() {
        Contact contact = new Contact(null, "张三", "13800138000", "北京市朝阳区", 0, null, null);
        when(userContactMapper.insertContact(contact)).thenAnswer(invocation -> {
            Contact c = invocation.getArgument(0);
            c.setId(100);
            return 1;
        });

        int result = userContactService.createContact(contact, 1001L);

        assertEquals(1, result);
        verify(userContactMapper).insertContact(contact);
        verify(userContactMapper).insertUserRelContact(1001L, contact.getId());
        InOrder inOrder = inOrder(userContactMapper);
        inOrder.verify(userContactMapper).insertContact(contact);
        inOrder.verify(userContactMapper).insertUserRelContact(1001L, contact.getId());
    }

    @Test
    @DisplayName("CT-SRV-002 创建联系人失败（插入返回0），不插入关联记录")
    void createContact_Fail_NoInsert() {
        Contact contact = new Contact(null, "李四", "13900139000", "上海市浦东新区", 0, null, null);
        when(userContactMapper.insertContact(contact)).thenReturn(0);

        int result = userContactService.createContact(contact, 1001L);

        assertEquals(0, result);
        verify(userContactMapper).insertContact(contact);
        verify(userContactMapper, never()).insertUserRelContact(anyLong(), anyInt());
    }

    // ==================== deleteContact ====================

    @Test
    @DisplayName("CT-SRV-003 正常删除联系人（有权限）")
    void deleteContact_Success() {
        when(userContactMapper.selectUserIdsByContactId(1)).thenReturn(List.of(1001L));
        when(userContactMapper.deleteRelByContactId(1)).thenReturn(1);
        when(userContactMapper.deleteContactById(1)).thenReturn(1);

        int result = userContactService.deleteContact(1, 1001L);

        assertEquals(1, result);
        verify(userContactMapper).deleteRelByContactId(1);
        verify(userContactMapper).deleteContactById(1);
    }

    @Test
    @DisplayName("CT-SRV-004 删除联系人-关联用户列表为空，返回0")
    void deleteContact_NotFound_EmptyUserIds() {
        when(userContactMapper.selectUserIdsByContactId(99999)).thenReturn(List.of());

        int result = userContactService.deleteContact(99999, 1001L);

        assertEquals(0, result);
        verify(userContactMapper, never()).deleteRelByContactId(anyInt());
        verify(userContactMapper, never()).deleteContactById(anyInt());
    }

    @Test
    @DisplayName("CT-SRV-005 删除联系人-当前用户不在关联列表中，返回0")
    void deleteContact_NotFound_NotOwner() {
        when(userContactMapper.selectUserIdsByContactId(2)).thenReturn(List.of(2000L, 3000L));

        int result = userContactService.deleteContact(2, 1001L);

        assertEquals(0, result);
        verify(userContactMapper, never()).deleteRelByContactId(anyInt());
        verify(userContactMapper, never()).deleteContactById(anyInt());
    }

    // ==================== updateContact ====================

    @Test
    @DisplayName("CT-SRV-006 正常更新联系人（有权限）")
    void updateContact_Success() {
        Contact contact = new Contact(1, "张三_更新", "13800138001", "北京市海淀区", 0, null, null);
        when(userContactMapper.selectUserIdsByContactId(1)).thenReturn(List.of(1001L));
        when(userContactMapper.updateContact(contact)).thenReturn(1);

        int result = userContactService.updateContact(contact, 1001L);

        assertEquals(1, result);
        verify(userContactMapper).updateContact(contact);
    }

    @Test
    @DisplayName("CT-SRV-007 更新联系人-关联用户列表为空")
    void updateContact_NotFound_EmptyUserIds() {
        Contact contact = new Contact(99999, "测试", "13800000000", "地址", 0, null, null);
        when(userContactMapper.selectUserIdsByContactId(99999)).thenReturn(List.of());

        int result = userContactService.updateContact(contact, 1001L);

        assertEquals(0, result);
        verify(userContactMapper, never()).updateContact(any());
    }

    @Test
    @DisplayName("CT-SRV-008 更新联系人-无权限")
    void updateContact_NoPermission() {
        Contact contact = new Contact(2, "测试", "13800000000", "地址", 0, null, null);
        when(userContactMapper.selectUserIdsByContactId(2)).thenReturn(List.of(2000L));

        int result = userContactService.updateContact(contact, 1001L);

        assertEquals(0, result);
        verify(userContactMapper, never()).updateContact(any());
    }

    // ==================== getContactsByUserId ====================

    @Test
    @DisplayName("CT-SRV-009 查询联系人列表-有数据")
    void getContactsByUserId_HasData() {
        Contact c1 = new Contact(1, "张三", "13800138000", "北京市", 0, LocalDateTime.now(), LocalDateTime.now());
        Contact c2 = new Contact(2, "李四", "13900139000", "上海市", 1, LocalDateTime.now(), LocalDateTime.now());
        when(userContactMapper.selectByUserId(1001L)).thenReturn(List.of(c1, c2));

        List<Contact> result = userContactService.getContactsByUserId(1001L);

        assertEquals(2, result.size());
        assertEquals("张三", result.get(0).getName());
        assertEquals("李四", result.get(1).getName());
    }

    @Test
    @DisplayName("CT-SRV-010 查询联系人列表-无数据")
    void getContactsByUserId_Empty() {
        when(userContactMapper.selectByUserId(2000L)).thenReturn(List.of());

        List<Contact> result = userContactService.getContactsByUserId(2000L);

        assertTrue(result.isEmpty());
    }

    // ==================== setDefaultContact ====================

    @Test
    @DisplayName("CT-SRV-011 正常设置默认联系人")
    void setDefaultContact_Success() {
        when(userContactMapper.selectUserIdsByContactId(1)).thenReturn(List.of(1001L));
        when(userContactMapper.clearDefaultByUserId(1001L, 1)).thenReturn(1);
        when(userContactMapper.setDefaultById(1)).thenReturn(1);

        int result = userContactService.setDefaultContact(1, 1001L);

        assertEquals(1, result);
        verify(userContactMapper).clearDefaultByUserId(1001L, 1);
        verify(userContactMapper).setDefaultById(1);
    }

    @Test
    @DisplayName("CT-SRV-012 设置默认联系人-关联用户列表为空")
    void setDefaultContact_NotFound_EmptyUserIds() {
        when(userContactMapper.selectUserIdsByContactId(99999)).thenReturn(List.of());

        int result = userContactService.setDefaultContact(99999, 1001L);

        assertEquals(0, result);
        verify(userContactMapper, never()).clearDefaultByUserId(anyLong(), anyInt());
        verify(userContactMapper, never()).setDefaultById(anyInt());
    }

    @Test
    @DisplayName("CT-SRV-013 设置默认联系人-无权限")
    void setDefaultContact_NoPermission() {
        when(userContactMapper.selectUserIdsByContactId(2)).thenReturn(List.of(2000L));

        int result = userContactService.setDefaultContact(2, 1001L);

        assertEquals(0, result);
        verify(userContactMapper, never()).clearDefaultByUserId(anyLong(), anyInt());
        verify(userContactMapper, never()).setDefaultById(anyInt());
    }

    // ==================== getContactById (内部微服务) ====================

    @Test
    @DisplayName("CT-SRV-014 根据ID查询联系人-存在")
    void getContactById_Success() {
        Contact contact = new Contact(1, "张三", "13800138000", "北京市朝阳区", 0, null, null);
        when(userContactMapper.selectContactById(1)).thenReturn(contact);

        Contact result = userContactService.getContactById(1);

        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("张三", result.getName());
    }

    @Test
    @DisplayName("CT-SRV-015 根据ID查询联系人-不存在")
    void getContactById_NotFound() {
        when(userContactMapper.selectContactById(99999)).thenReturn(null);

        Contact result = userContactService.getContactById(99999);

        assertNull(result);
    }

    // ==================== selectUserIdsByContactId 返回 null（防御性）====================

    @Test
    @DisplayName("CT-SRV-031 删除联系人-关联用户列表返回null，触发NPE")
    void deleteContact_UserIdsReturnNull() {
        when(userContactMapper.selectUserIdsByContactId(1)).thenReturn(null);

        assertThrows(NullPointerException.class, () -> userContactService.deleteContact(1, 1001L));
    }

    @Test
    @DisplayName("CT-SRV-032 更新联系人-关联用户列表返回null，触发NPE")
    void updateContact_UserIdsReturnNull() {
        Contact contact = new Contact(1, "测试", "13800000000", "地址", 0, null, null);
        when(userContactMapper.selectUserIdsByContactId(1)).thenReturn(null);

        assertThrows(NullPointerException.class, () -> userContactService.updateContact(contact, 1001L));
    }

    @Test
    @DisplayName("CT-SRV-033 设置默认联系人-关联用户列表返回null，触发NPE")
    void setDefaultContact_UserIdsReturnNull() {
        when(userContactMapper.selectUserIdsByContactId(1)).thenReturn(null);

        assertThrows(NullPointerException.class, () -> userContactService.setDefaultContact(1, 1001L));
    }

    // ==================== userId 为 null ====================

    @Test
    @DisplayName("CT-SRV-034 创建联系人时userId为null")
    void createContact_NullUserId() {
        Contact contact = new Contact(null, "张三", "13800138000", "北京市朝阳区", 0, null, null);
        when(userContactMapper.insertContact(contact)).thenAnswer(invocation -> {
            Contact c = invocation.getArgument(0);
            c.setId(100);
            return 1;
        });

        int result = userContactService.createContact(contact, null);

        assertEquals(1, result);
        verify(userContactMapper).insertContact(contact);
        verify(userContactMapper).insertUserRelContact(null, contact.getId());
    }

    @Test
    @DisplayName("CT-SRV-035 删除联系人时userId为null")
    void deleteContact_NullUserId() {
        when(userContactMapper.selectUserIdsByContactId(1)).thenReturn(java.util.Arrays.asList(1001L));

        int result = userContactService.deleteContact(1, null);

        assertEquals(0, result);
        verify(userContactMapper, never()).deleteRelByContactId(anyInt());
        verify(userContactMapper, never()).deleteContactById(anyInt());
    }

    @Test
    @DisplayName("CT-SRV-036 更新联系人时userId为null")
    void updateContact_NullUserId() {
        Contact contact = new Contact(1, "测试", "13800000000", "地址", 0, null, null);
        when(userContactMapper.selectUserIdsByContactId(1)).thenReturn(java.util.Arrays.asList(1001L));

        int result = userContactService.updateContact(contact, null);

        assertEquals(0, result);
        verify(userContactMapper, never()).updateContact(any());
    }

    @Test
    @DisplayName("CT-SRV-037 设置默认联系人时userId为null")
    void setDefaultContact_NullUserId() {
        when(userContactMapper.selectUserIdsByContactId(1)).thenReturn(java.util.Arrays.asList(1001L));

        int result = userContactService.setDefaultContact(1, null);

        assertEquals(0, result);
        verify(userContactMapper, never()).clearDefaultByUserId(anyLong(), anyInt());
        verify(userContactMapper, never()).setDefaultById(anyInt());
    }

    @Test
    @DisplayName("CT-SRV-038 查询联系人列表时userId为null")
    void getContactsByUserId_NullUserId() {
        when(userContactMapper.selectByUserId(null)).thenReturn(List.of());

        List<Contact> result = userContactService.getContactsByUserId(null);

        assertTrue(result.isEmpty());
        verify(userContactMapper).selectByUserId(null);
    }

    // ==================== Mapper 幂等返回 0 ====================

    @Test
    @DisplayName("CT-SRV-039 删除联系人-有权限但deleteRel返回0，deleteContactById仍然执行")
    void deleteContact_RelAlreadyDeleted() {
        when(userContactMapper.selectUserIdsByContactId(1)).thenReturn(List.of(1001L));
        when(userContactMapper.deleteRelByContactId(1)).thenReturn(0);
        when(userContactMapper.deleteContactById(1)).thenReturn(1);

        int result = userContactService.deleteContact(1, 1001L);

        assertEquals(1, result);
        verify(userContactMapper).deleteRelByContactId(1);
        verify(userContactMapper).deleteContactById(1);
    }

    @Test
    @DisplayName("CT-SRV-040 设置默认联系人-无其他默认需清除，clearDefault返回0")
    void setDefaultContact_NoOtherDefault() {
        when(userContactMapper.selectUserIdsByContactId(1)).thenReturn(List.of(1001L));
        when(userContactMapper.clearDefaultByUserId(1001L, 1)).thenReturn(0);
        when(userContactMapper.setDefaultById(1)).thenReturn(1);

        int result = userContactService.setDefaultContact(1, 1001L);

        assertEquals(1, result);
        verify(userContactMapper).clearDefaultByUserId(1001L, 1);
        verify(userContactMapper).setDefaultById(1);
    }

    @Test
    @DisplayName("CT-SRV-041 设置默认联系人-setDefaultById返回0（地址已被删除）")
    void setDefaultContact_SetDefaultFails() {
        when(userContactMapper.selectUserIdsByContactId(1)).thenReturn(List.of(1001L));
        when(userContactMapper.clearDefaultByUserId(1001L, 1)).thenReturn(1);
        when(userContactMapper.setDefaultById(1)).thenReturn(0);

        int result = userContactService.setDefaultContact(1, 1001L);

        assertEquals(0, result);
    }
}
