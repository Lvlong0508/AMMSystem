package com.gzasc.aishopping.contact.service.impl;

import com.gzasc.aishopping.contact.mapper.UserContactMapper;
import com.gzasc.aishopping.contact.model.Contact;
import com.gzasc.aishopping.contact.service.ContactService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 联系人服务实现类
 * 提供联系人的 CRUD 操作，支持用户隔离
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContactServiceImpl implements ContactService {

    private final UserContactMapper userContactMapper;

    @Override
    @Transactional
    public int createContact(Contact contact, int userId) {
        log.info("createContact, userId={}", userId);
        int result = userContactMapper.insertContact(contact);
        if (result > 0) {
            userContactMapper.insertUserContact(userId, contact.getId());
        }
        return result;
    }

    @Override
    @Transactional
    public int deleteContact(int id, int userId) {
        log.info("deleteContact, id={}, userId={}", id, userId);
        List<Integer> userIds = userContactMapper.selectUserIdsByContactId(id);
        if (userIds.isEmpty() || userIds.stream().noneMatch(uid -> uid == userId)) {
            return 0;
        }
        userContactMapper.deleteByContactId(id);
        return userContactMapper.deleteContactById(id);
    }

    @Override
    @Transactional
    public int updateContact(Contact contact, int userId) {
        log.info("updateContact, id={}, userId={}", contact.getId(), userId);
        List<Integer> userIds = userContactMapper.selectUserIdsByContactId(contact.getId());
        if (userIds.isEmpty() || userIds.stream().noneMatch(uid -> uid == userId)) {
            return 0;
        }
        return userContactMapper.updateContact(contact);
    }

    @Override
    public Contact getContactById(int id, int userId) {
        log.info("getContactById, id={}, userId={}", id, userId);
        List<Integer> userIds = userContactMapper.selectUserIdsByContactId(id);
        if (userIds.isEmpty() || userIds.stream().noneMatch(uid -> uid == userId)) {
            return null;
        }
        return userContactMapper.selectContactById(id);
    }

    @Override
    public Contact getContactByIdNoAuth(int id) {
        log.info("getContactByIdNoAuth, id={}", id);
        return userContactMapper.selectContactById(id);
    }

    @Override
    public List<Contact> getContactsByUserId(int userId) {
        log.info("getContactsByUserId, userId={}", userId);
        return userContactMapper.selectByUserId(userId);
    }

    @Override
    public int setDefaultContact(int id, int userId) {
        log.info("setDefaultContact, id={}, userId={}", id, userId);
        Contact contact = getContactById(id, userId);
        if (contact == null) {
            return 0;
        }
        return userContactMapper.setDefaultById(id);
    }
}